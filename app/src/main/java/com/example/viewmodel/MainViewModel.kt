package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AppRepository(db)

    // Language Preference: "mr" for Marathi, "en" for English
    private val _appLanguage = MutableStateFlow("mr")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // Security Settings
    private val _preventScreenshots = MutableStateFlow(false)
    val preventScreenshots: StateFlow<Boolean> = _preventScreenshots.asStateFlow()

    // All database flows exposed reactively
    val allQuestions: StateFlow<List<Question>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testHistory: StateFlow<List<TestHistory>> = repository.testHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProgress: StateFlow<List<UserProgress>> = repository.userProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<String>> = repository.subjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CBT Active Exam State
    private val _isExamActive = MutableStateFlow(false)
    val isExamActive: StateFlow<Boolean> = _isExamActive.asStateFlow()

    private val _examQuestions = MutableStateFlow<List<Question>>(emptyList())
    val examQuestions: StateFlow<List<Question>> = _examQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Map of question index -> selected option ("A", "B", "C", "D")
    private val _selectedAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, String>> = _selectedAnswers.asStateFlow()

    // Set of indices marked for review
    private val _markedForReview = MutableStateFlow<Set<Int>>(emptySet())
    val markedForReview: StateFlow<Set<Int>> = _markedForReview.asStateFlow()

    // Seconds remaining for the active test
    private val _timeRemainingSeconds = MutableStateFlow(0)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _examTitle = MutableStateFlow("Mock CBT Exam")
    val examTitle: StateFlow<String> = _examTitle.asStateFlow()

    private val _examSubject = MutableStateFlow("Electrician Theory")
    val examSubject: StateFlow<String> = _examSubject.asStateFlow()

    private var timerJob: Job? = null

    // Options configuration
    var includeNegativeMarking = mutableStateOf(false) // -0.25 on wrong answers, similar to real exams!
    var customQuestionCount = mutableStateOf(10) // default 10 questions for tests

    // Finished Test Result state for the summary view
    private val _latestFinishedResult = MutableStateFlow<TestHistory?>(null)
    val latestFinishedResult: StateFlow<TestHistory?> = _latestFinishedResult.asStateFlow()

    // AI states
    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _aiExplanationText = MutableStateFlow("")
    val aiExplanationText: StateFlow<String> = _aiExplanationText.asStateFlow()

    private val _aiExplanationLoading = MutableStateFlow(false)
    val aiExplanationLoading: StateFlow<Boolean> = _aiExplanationLoading.asStateFlow()

    // Admin state
    private val _adminCategoriesList = MutableStateFlow<List<String>>(emptyList())
    val adminCategoriesList: StateFlow<List<String>> = _adminCategoriesList.asStateFlow()

    init {
        // Populate sample mock questions if DB is clean
        viewModelScope.launch {
            repository.populatePresetQuestionsIfEmpty()
        }
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
    }

    fun toggleScreenshots(prevent: Boolean) {
        _preventScreenshots.value = prevent
    }

    // Bookmarks Toggle
    fun toggleBookmark(questionId: Long) {
        viewModelScope.launch {
            repository.toggleBookmark(questionId)
        }
    }

    // Start a customized CBT Exam Mock Test
    fun startCbtTest(
        title: String,
        subject: String,
        chapter: String? = null,
        count: Int = customQuestionCount.value,
        durationMinutes: Int = 15
    ) {
        viewModelScope.launch {
            _examTitle.value = title
            _examSubject.value = subject
            _selectedAnswers.value = emptyMap()
            _markedForReview.value = emptySet()
            _currentQuestionIndex.value = 0
            _timeRemainingSeconds.value = durationMinutes * 60

            val questions = if (chapter != null) {
                repository.getQuestionsByChapter(subject, chapter).first().shuffled().take(count)
            } else {
                repository.getRandomQuestionsBySubject(subject, count)
            }

            if (questions.isEmpty()) {
                // Fetch random from entire pool if none in subject yet
                _examQuestions.value = repository.getRandomQuestions(count)
            } else {
                _examQuestions.value = questions
            }

            _isExamActive.value = true
            startTimer()
        }
    }

    fun selectAnswer(questionIndex: Int, option: String) {
        val updated = _selectedAnswers.value.toMutableMap()
        updated[questionIndex] = option
        _selectedAnswers.value = updated
    }

    fun toggleMarkForReview(questionIndex: Int) {
        val updated = _markedForReview.value.toMutableSet()
        if (updated.contains(questionIndex)) {
            updated.remove(questionIndex)
        } else {
            updated.add(questionIndex)
        }
        _markedForReview.value = updated
    }

    fun navigateToQuestion(index: Int) {
        if (index in 0 until _examQuestions.value.size) {
            _currentQuestionIndex.value = index
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0 && _isExamActive.value) {
                delay(1000)
                _timeRemainingSeconds.value -= 1
            }
            if (_timeRemainingSeconds.value == 0 && _isExamActive.value) {
                submitActiveExam()
            }
        }
    }

    // Force finishes and generates score sheet
    fun submitActiveExam() {
        timerJob?.cancel()
        _isExamActive.value = false

        val questions = _examQuestions.value
        val answers = _selectedAnswers.value

        var correct = 0
        var wrong = 0
        var unattempted = 0

        for (i in questions.indices) {
            val selected = answers[i]
            if (selected == null) {
                unattempted++
            } else if (selected == questions[i].correctOption) {
                correct++
            } else {
                wrong++
            }
        }

        // Standard CBT score calculation: e.g. 2 marks per correct, -0.5 (-0.25 multiplier) per wrong if negative marking enabled
        val totalQ = questions.size
        val scorePercent = if (totalQ > 0) {
            val rawScore = if (includeNegativeMarking.value) {
                (correct * 2.0f) - (wrong * 0.5f)
            } else {
                correct * 2.0f
            }
            val maxPossible = totalQ * 2.0f
            val percent = (rawScore / maxPossible) * 100f
            if (percent < 0) 0f else percent
        } else {
            0f
        }

        val testResult = TestHistory(
            testTitle = _examTitle.value,
            subjectName = _examSubject.value,
            totalQuestions = totalQ,
            correctCount = correct,
            wrongCount = wrong,
            unattemptedCount = unattempted,
            scorePercentage = scorePercent,
            durationSeconds = (15 * 60) - _timeRemainingSeconds.value // total time spent
        )

        _latestFinishedResult.value = testResult

        viewModelScope.launch {
            repository.saveTestHistory(testResult)
        }
    }

    // Study notes add logic
    fun addNewNote(title: String, category: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(Note(title = title, category = category, content = content))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Manual Question Add (Admin Control)
    fun insertManualQuestion(
        subject: String,
        chapter: String,
        qEn: String,
        qMr: String,
        opAEn: String,
        opAMr: String,
        opBEn: String,
        opBMr: String,
        opCEn: String,
        opCMr: String,
        opDEn: String,
        opDMr: String,
        correct: String,
        exp: String,
        diff: String
    ) {
        viewModelScope.launch {
            repository.insertQuestion(
                Question(
                    subject = subject,
                    chapter = chapter,
                    questionTextEn = qEn,
                    questionTextMr = qMr,
                    optionAEn = opAEn,
                    optionAMr = opAMr,
                    optionBEn = opBEn,
                    optionBMr = opBMr,
                    optionCEn = opCEn,
                    optionCMr = opCMr,
                    optionDEn = opDEn,
                    optionDMr = opDMr,
                    correctOption = correct,
                    explanation = exp,
                    difficulty = diff
                )
            )
        }
    }

    // AI Generation (Syllabus topic analysis to MCQs import)
    fun autoGenerateAiQuestions(subject: String, chapter: String, promptText: String) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            try {
                repository.generateMCQsFromText(subject, chapter, promptText)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to generate AI questions: ${e.message}")
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    // Real-time AI answer analysis / explanations
    fun loadAiExplanationForQuestion(question: Question) {
        viewModelScope.launch {
            _aiExplanationLoading.value = true
            _aiExplanationText.value = ""
            try {
                val exp = repository.getAIExplanation(question)
                _aiExplanationText.value = exp
            } catch (e: Exception) {
                _aiExplanationText.value = "Error generating explanation offline. Fallback: ${question.explanation}"
            } finally {
                _aiExplanationLoading.value = false
            }
        }
    }
}
