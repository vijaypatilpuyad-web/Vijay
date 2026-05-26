package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(private val db: AppDatabase) {

    val allQuestions: Flow<List<Question>> = db.questionDao().getAllQuestions()
    val allBookmarks: Flow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()
    val allNotes: Flow<List<Note>> = db.noteDao().getAllNotes()
    val testHistory: Flow<List<TestHistory>> = db.testHistoryDao().getAllTestHistory()
    val userProgress: Flow<List<UserProgress>> = db.userProgressDao().getAllProgress()
    val subjects: Flow<List<String>> = db.questionDao().getSubjects()

    fun getQuestionsBySubject(subject: String): Flow<List<Question>> =
        db.questionDao().getQuestionsBySubject(subject)

    fun getQuestionsByChapter(subject: String, chapter: String): Flow<List<Question>> =
        db.questionDao().getQuestionsByChapter(subject, chapter)

    fun getChaptersBySubject(subject: String): Flow<List<String>> =
        db.questionDao().getChaptersBySubject(subject)

    suspend fun getQuestionById(id: Long): Question? = withContext(Dispatchers.IO) {
        db.questionDao().getQuestionById(id)
    }

    suspend fun getRandomQuestions(limit: Int): List<Question> = withContext(Dispatchers.IO) {
        db.questionDao().getRandomQuestions(limit)
    }

    suspend fun getRandomQuestionsBySubject(subject: String, limit: Int): List<Question> = withContext(Dispatchers.IO) {
        db.questionDao().getRandomQuestionsBySubject(subject, limit)
    }

    suspend fun insertQuestion(question: Question) = withContext(Dispatchers.IO) {
        db.questionDao().insertQuestion(question)
    }

    suspend fun deleteQuestion(question: Question) = withContext(Dispatchers.IO) {
        db.questionDao().deleteQuestion(question)
    }

    suspend fun deleteQuestionById(id: Long) = withContext(Dispatchers.IO) {
        db.questionDao().deleteQuestionById(id)
    }

    // Bookmarks logic
    suspend fun toggleBookmark(questionId: Long) = withContext(Dispatchers.IO) {
        val exists = db.bookmarkDao().isBookmarked(questionId)
        if (exists) {
            db.bookmarkDao().removeBookmark(questionId)
        } else {
            db.bookmarkDao().addBookmark(Bookmark(questionId = questionId))
        }
    }

    fun isBookmarkedFlow(questionId: Long): Flow<Boolean> = db.bookmarkDao().isBookmarkedFlow(questionId)

    // Notes logic
    suspend fun insertNote(note: Note) = withContext(Dispatchers.IO) {
        db.noteDao().insertNote(note)
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        db.noteDao().deleteNote(note)
    }

    // Test History logic
    suspend fun saveTestHistory(history: TestHistory) = withContext(Dispatchers.IO) {
        db.testHistoryDao().insertTestHistory(history)

        // Also update daily user progress analytics
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayStr = sdf.format(java.util.Date())
        val existingProgress = db.userProgressDao().getProgressForDate(todayStr)

        if (existingProgress != null) {
            db.userProgressDao().saveProgress(
                UserProgress(
                    dateString = todayStr,
                    questionsAttempted = existingProgress.questionsAttempted + history.totalQuestions - history.unattemptedCount,
                    correctAnswers = existingProgress.correctAnswers + history.correctCount
                )
            )
        } else {
            db.userProgressDao().saveProgress(
                UserProgress(
                    dateString = todayStr,
                    questionsAttempted = history.totalQuestions - history.unattemptedCount,
                    correctAnswers = history.correctCount
                )
            )
        }
    }

    suspend fun deleteHistoryById(id: Long) = withContext(Dispatchers.IO) {
        db.testHistoryDao().deleteHistoryById(id)
    }

    // AI MCQ Generation from Text via Gemini REST API
    suspend fun generateMCQsFromText(subject: String, chapter: String, promptText: String): List<Question> = withContext(Dispatchers.IO) {
        val apiKey = GeminiRetrofitClient.getApiKey()
        if (apiKey.isEmpty()) {
            Log.w("AppRepository", "Gemini API key is empty. Using Fallback Question Generation.")
            return@withContext generateLocalMockAIQuestions(subject, chapter, promptText)
        }

        val prompt = """
            You are an expert ITI Electrician exam compiler for NCVT / Bharat Skills CBT exams.
            Please generate exactly 4 premium multiple-choice questions (MCQs) for the subject "$subject" and chapter "$chapter" based on the following concepts or syllabus:
            
            $promptText
            
            Each generated question must support English + Marathi language translation.
            Return ONLY a valid JSON array of objects with the following fields and NO enclosing markdown code blocks (such as ```json):
            [
              {
                "questionTextEn": "English question text here",
                "questionTextMr": "मराठीतील प्रश्न मजकूर",
                "optionAEn": "Option A in English",
                "optionAMr": "पर्याय ए मराठीत",
                "optionBEn": "Option B in English",
                "optionBMr": "पर्याय बी मराठीत",
                "optionCEn": "Option C in English",
                "optionCMr": "पर्याय सी मराठीत",
                "optionDEn": "Option D in English",
                "optionDMr": "पर्याय डी मराठीत",
                "correctOption": "A", // must be A, B, C, or D
                "explanation": "Brief explanation in English + Marathi",
                "difficulty": "Medium" // Easy, Medium, or Hard
              }
            ]
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(ContentWrapper(parts = listOf(PartWrapper(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.7)
            )
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Parse custom clean JSON block from markdown response
            val cleanJson = if (rawText.contains("```json")) {
                rawText.substringAfter("```json").substringBefore("```").trim()
            } else if (rawText.contains("```")) {
                rawText.substringAfter("```").substringBefore("```").trim()
            } else {
                rawText.trim()
            }

            val jsonArray = JSONArray(cleanJson)
            val parsedQuestions = mutableListOf<Question>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                parsedQuestions.add(
                    Question(
                        subject = subject,
                        chapter = chapter,
                        questionTextEn = obj.optString("questionTextEn", "Identify the safety sign."),
                        questionTextMr = obj.optString("questionTextMr", "सुरक्षा चिन्ह ओळखा."),
                        optionAEn = obj.optString("optionAEn", "Mandatory Sign"),
                        optionAMr = obj.optString("optionAMr", "आवश्यक चिन्ह"),
                        optionBEn = obj.optString("optionBEn", "Warning Sign"),
                        optionBMr = obj.optString("optionBMr", "चेतावणी चिन्ह"),
                        optionCEn = obj.optString("optionCEn", "Prohibition Sign"),
                        optionCMr = obj.optString("optionCMr", "निषेध चिन्ह"),
                        optionDEn = obj.optString("optionDEn", "Information Sign"),
                        optionDMr = obj.optString("optionDMr", "माहिती चिन्ह"),
                        correctOption = obj.optString("correctOption", "B"),
                        explanation = obj.optString("explanation", "Warning signs are triangular in shape. चेतावणी चिन्हे त्रिकोणी आकाराची असतात."),
                        difficulty = obj.optString("difficulty", "Medium")
                    )
                )
            }
            if (parsedQuestions.isNotEmpty()) {
                db.questionDao().insertQuestions(parsedQuestions)
            }
            parsedQuestions
        } catch (e: Exception) {
            Log.e("AppRepository", "Error calling Gemini API: ${e.message}", e)
            generateLocalMockAIQuestions(subject, chapter, promptText)
        }
    }

    private suspend fun generateLocalMockAIQuestions(subject: String, chapter: String, promptText: String): List<Question> = withContext(Dispatchers.IO) {
        // Fallback generator for offline/empty-API keys. Generates realistic Electrician MCQs based on user keywords.
        val isSafety = promptText.contains("safety", ignoreCase = true) || promptText.contains("सुरक्षा", ignoreCase = true)
        val isWire = promptText.contains("wire", ignoreCase = true) || promptText.contains("wiring", ignoreCase = true)
        
        val fallback = if (isSafety) {
            listOf(
                Question(
                    subject = subject,
                    chapter = chapter,
                    questionTextEn = "Which fire extinguisher is recommended for electric equipment fires?",
                    questionTextMr = "इलेक्ट्रिकल उपकरणाला लागलेल्या आगीसाठी कोणता अग्निशामक वापरणे योग्य आहे?",
                    optionAEn = "Water Type Extinguisher", optionAMr = "पाण्याचा अग्निशामक",
                    optionBEn = "Halon/CO2 Carbon Dioxide Extinguisher", optionBMr = "हॅलॉन किंवा CO2 कार्बन डायऑक्साइड अग्निशामक",
                    optionCEn = "Foam Extinguisher", optionCMr = "फेस अग्निशामक",
                    optionDEn = "Soda Acid Extinguisher", optionDMr = "सोडा ॲसिड अग्निशामक",
                    correctOption = "B",
                    explanation = "Water and foam cannot be used as they conduct electricity. Halon/CO2 is perfect. पाण्यामुळे शॉक लागू शकतो, म्हणून हॅलॉन किंवा CO2 चा वापर केला जातो.",
                    difficulty = "Medium"
                )
            )
        } else if (isWire) {
            listOf(
                Question(
                    subject = subject,
                    chapter = chapter,
                    questionTextEn = "Which wire size is standard for high-current electric geyser outlets in house wiring?",
                    questionTextMr = "घरगुती वायरिंगमध्ये जास्त करंटच्या गिझर आउटलेटसाठी वायरचा कोणता आकार मानला जातो?",
                    optionAEn = "1.0 sq.mm", optionAMr = "१.० चौ.मी.मी.",
                    optionBEn = "1.5 sq.mm", optionBMr = "१.५ चौ.मी.मी.",
                    optionCEn = "4.0 sq.mm Copper wire", optionCMr = "४.० चौ.मी.मी. तांब्याची वायर",
                    optionDEn = "0.75 sq.mm", optionDMr = "०.७५ चौ.मी.मी.",
                    correctOption = "C",
                    explanation = "High loads like water geysers and AC require heavy 4.0 sq.mm copper wires to prevent heating. गिझरसाठी ४.० चौ.मी.मी. चे कॉपर वायर वापरणे सुरक्षित असते.",
                    difficulty = "Hard"
                )
            )
        } else {
            listOf(
                Question(
                    subject = subject,
                    chapter = chapter,
                    questionTextEn = "What is the function of a fuse in an electrical distribution board?",
                    questionTextMr = "इलेक्ट्रिकल वितरण बोर्डमध्ये फ्युजचे काय कार्य असते?",
                    optionAEn = "To increase voltage level", optionAMr = "व्होल्टेज वाढवण्यासाठी",
                    optionBEn = "To protect circuit from short-circuits and overloads", optionBMr = "शॉर्ट सर्किट्स आणि ओव्हरलोडपासून सर्किट सुरक्षित ठेवण्यासाठी",
                    optionCEn = "To measure energy consumption", optionCMr = "ऊर्जा वापर मोजण्यासाठी",
                    optionDEn = "To convert AC to DC current", optionDMr = "AC चे DC मध्ये रूपांतर करण्यासाठी",
                    correctOption = "B",
                    explanation = "Fuses melt on high current and break circuit safely. फ्युज हा सर्किटचे ओव्हरलोडिंग व फॉल्ट्सपासून रक्षण करतो.",
                    difficulty = "Easy"
                )
            )
        }
        db.questionDao().insertQuestions(fallback)
        fallback
    }

    // AI Explanation for a question via Gemini API
    suspend fun getAIExplanation(question: Question): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiRetrofitClient.getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext question.explanation.ifEmpty { 
                "Verify correctness of ${question.correctOption} based on NCVT curriculum rules. (API Key missing for live AI explanation)" 
            }
        }

        val prompt = """
            You are an expert ITI Electrician Tutor. Explain clearly why Option ${question.correctOption} is the correct answer for the following question.
            Provide explanation in both English and Marathi:
            
            Question (EN): ${question.questionTextEn}
            Question (MR): ${question.questionTextMr}
            Options:
            A: ${question.optionAEn} | ${question.optionAMr}
            B: ${question.optionBEn} | ${question.optionBMr}
            C: ${question.optionCEn} | ${question.optionCMr}
            D: ${question.optionDEn} | ${question.optionDMr}
            
            Keep the response to 4 sentences maximum. Focus on core electrical properties, safety rules, or formulas.
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(ContentWrapper(parts = listOf(PartWrapper(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.5)
            )
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: question.explanation
        } catch (e: Exception) {
            question.explanation
        }
    }

    // Prepopulate DB with high-quality NCVT/Bharat Skills Exam Questions on first launch
    suspend fun populatePresetQuestionsIfEmpty() = withContext(Dispatchers.IO) {
        val count = db.questionDao().getAllQuestions().first().size
        if (count > 0) return@withContext

        val presetQuestions = listOf(
            // --- Electrician Theory ---
            Question(
                subject = "Electrician Theory",
                chapter = "Safety and First Aid",
                questionTextEn = "Which fire extinguisher is used for Class B flammable liquid fires?",
                questionTextMr = "वर्ग B च्या ज्वलनशील रसायनांच्या आगीसाठी कोणते अग्निशामक वापरले जाते?",
                optionAEn = "Water type extinguisher", optionAMr = "पाण्याचा अग्निशामक",
                optionBEn = "Dry powder or Carbon Dioxide (CO2)", optionBMr = "ड्राय पावडर किंवा कार्बन डायऑक्साइड (CO2)",
                optionCEn = "Halon extinguisher", optionCMr = "हॅलॉन अग्निशामक",
                optionDEn = "Liquid chemical extinguisher", optionDMr = "द्रव रासायनिक उपशामक",
                correctOption = "B",
                explanation = "Dry powder blocks supply of oxygen to extinguish Class B fires perfectly. वर्ग B च्या आगीसाठी ड्राय पावडर किंवा CO2 उपशामक ऑक्सीजन तोडण्याचे काम करतो.",
                difficulty = "Easy"
            ),
            Question(
                subject = "Electrician Theory",
                chapter = "Basic Electricity",
                questionTextEn = "What is the formula of Ohm's Law?",
                questionTextMr = "ओहमच्या नियमाचे मुख्य सूत्र काय आहे?",
                optionAEn = "V = I * R", optionAMr = "व्ही = आय * आर",
                optionBEn = "V = I / R", optionBMr = "व्ही = आय / आर",
                optionCEn = "R = V * I", optionCMr = "आर = व्ही * आय",
                optionDEn = "I = V * R", optionDMr = "आय = व्ही * आर",
                correctOption = "A",
                explanation = "Ohm's Law states Voltage (V) is product of Current (I) and Resistance (R). ओहमच्या नियमानुसार व्होल्टेज = करंट * रेझिस्टन्स असते (V = I * R).",
                difficulty = "Easy"
            ),
            Question(
                subject = "Electrician Theory",
                chapter = "Transformer",
                questionTextEn = "Which material is used in Transformer cores to minimize hysteresis losses?",
                questionTextMr = "ट्रान्सफॉर्मर कोरमध्ये हिस्टेरेसिस तोटा कमी करण्यासाठी कोणते मटेरियल वापरले जाते?",
                optionAEn = "Cast Iron", optionAMr = "कास्ट आयर्न (बीड)",
                optionBEn = "Silicon Steel", optionBMr = "सिलिकॉन स्टील",
                optionCEn = "Pure Copper", optionCMr = "शुद्ध तांबे",
                optionDEn = "High Carbon Steel", optionDMr = "हाय कार्बन स्टील",
                correctOption = "B",
                explanation = "Silicon steel has low magnetic hysteresis, which prevents load magnetic heating. सिलिकॉन स्टीलमध्ये हिस्टेरेसिस तोटा अत्यंत कमी असतो म्हणून गाभा गाभ्यासाठी त्याचा उपयोग होतो.",
                difficulty = "Medium"
            ),
            Question(
                subject = "Electrician Theory",
                chapter = "Earth Grounding",
                questionTextEn = "What is the primary method to reduce earth resistance in house installations?",
                questionTextMr = "घरगुती अर्थिंग प्रणालीमध्ये अर्थ रेझिस्टन्स कमी करण्याचा मुख्य उपाय कोणता?",
                optionAEn = "By increasing conductor length", optionAMr = "वाहक तारेची लांबी वाढवून",
                optionBEn = "By adding water and charcoal powder in earth pit", optionBMr = "अर्थ खड्ड्यामध्ये पाणी आणि कोळशाची पूड टाकून",
                optionCEn = "By using high gauge insulation", optionCMr = "उच्च गेज इन्सुलेशन वापरून",
                optionDEn = "By removing coal and salt completely", optionDMr = "मीठ आणि कोळसा पूर्णपणे काढून",
                correctOption = "B",
                explanation = "Water and charcoal maintain dampness, ensuring low resistance grounding paths. कोळसा आणि पाणी खड्ड्यात ओलावा टिकवून ठेवतात ज्यामुळे जमिनीचा रेझिस्टन्स कमी होतो.",
                difficulty = "Medium"
            ),
            Question(
                subject = "Electrician Theory",
                chapter = "Measuring Instruments",
                questionTextEn = "Which electrical instrument is used to check motor stator winding insulation?",
                questionTextMr = "मोटरच्या स्टेटर वाइंडिंगचे इन्सुलेशन तपासण्यासाठी कोणत्या यंत्राचा उपयोग होतो?",
                optionAEn = "Multimeter", optionAMr = "मल्टिमीटर",
                optionBEn = "Megger (Insulation Tester)", optionBMr = "मेगर (इन्सुलेशन परीक्षक)",
                optionCEn = "Solenoid Tester", optionCMr = "सोलेनॉइड टेस्टर",
                optionDEn = "Ammeter", optionDMr = "अमीटर",
                correctOption = "B",
                explanation = "Meggers generate high voltage currents used to verify non-conducting resistance thresholds. उच्च प्रतिकार मोजण्यासाठी आणि वाइंडिंग दोषांसाठी मेगर हे उत्तम साधन आहे.",
                difficulty = "Medium"
            ),
            Question(
                subject = "Electrician Theory",
                chapter = "Alternating Current",
                questionTextEn = "What is the power factor of a pure resistive electrical circuit?",
                questionTextMr = "शुद्ध रोधकांपासून (Resistive) बनवलेल्या एसी सर्किटचा पॉवर फॅक्टर किती असतो?",
                optionAEn = "Zero", optionAMr = "शून्य",
                optionBEn = "Lagging factor (0.8)", optionBMr = "लॅगिंग फॅक्टर (०.८)",
                optionCEn = "Unity (1.0)", optionCMr = "युनिटी (१.०)",
                optionDEn = "Leading factor (0.5)", optionDMr = "लीडिंग फॅक्टर (०.५)",
                correctOption = "C",
                explanation = "In purely resistive AC load, current and voltage are in face, making cosφ = 1.0 (Unity). शुद्ध रोधकीय लोडमध्ये करंट आणि व्होल्टेज एकाच फेझमध्ये असतात म्हणून युनिटी फॅक्टर मिळतो.",
                difficulty = "Hard"
            ),

            // --- Employability Skills ---
            Question(
                subject = "Employability Skills",
                chapter = "Exam Guidelines",
                questionTextEn = "What is full form of CBT in ITI exams?",
                questionTextMr = "ITI परीक्षा प्रणालीमध्ये CBT चे पूर्ण रूप काय आहे?",
                optionAEn = "Computer Based Test", optionAMr = "कॉम्प्युटर बेस्ड टेस्ट (संगणक आधारित परीक्षा)",
                optionBEn = "Central Balance Test", optionBMr = "सेंट्रल बॅलन्स टेस्ट",
                optionCEn = "Circuit Building Practice", optionCMr = "सर्किट बिल्डिंग प्रॅक्टिस",
                optionDEn = "Cable Business Theory", optionDMr = "केबल बिझनेस थ्योरी",
                correctOption = "A",
                explanation = "All NCVT trade exams are conducted in client computer consoles, called Computer Based Test (CBT). आयटीआय कडील सर्व परीक्षा संगणकावर CBT स्वरूपात पार पडतात.",
                difficulty = "Easy"
            ),
            Question(
                subject = "Employability Skills",
                chapter = "Communication Skills",
                questionTextEn = "Which of these is a vital parameter of verbal communication?",
                questionTextMr = "मौखिक संवादाचा (Verbal Communication) खालीलपैकी मुख्य घटक कोणता?",
                optionAEn = "Eye movement", optionAMr = "डोळ्यांच्या हालचाली",
                optionBEn = "Spoken clear words & tone", optionBMr = "स्पष्ट बोललेले शब्द आणि बोलण्याचा टोन",
                optionCEn = "Waving hands", optionCMr = "हात हलवणे",
                optionDEn = "Physical dress up", optionDMr = "शारीरिक पेहराव",
                correctOption = "B",
                explanation = "Using precise oral words and tone forms the primary vehicle of verbal dialogue. स्पष्ट शब्द वापरून भाषा बोलणे हाच मौखिक संवादाचा प्राण आहे.",
                difficulty = "Easy"
            ),

            // --- Engineering Drawing ---
            Question(
                subject = "Engineering Drawing",
                chapter = "Drawing Instruments",
                questionTextEn = "Which standard pencil hardness grade is used for drawing dark outlines?",
                questionTextMr = "इंजिनिअरिंग ड्रॉइंगमध्ये गडद बाह्यरेखा (Outlines) काढण्यासाठी कोणत्या पेन्सिलचा वापर होतो?",
                optionAEn = "9H Pencil", optionAMr = "९एच पेन्सिल (अत्यंत फिकट)",
                optionBEn = "HB Pencil", optionBMr = "एचबी पेन्सिल",
                optionCEn = "9B Pencil", optionCMr = "९बी पेन्सिल",
                optionDEn = "4H Pencil", optionDMr = "४एच पेन्सिल",
                correctOption = "B",
                explanation = "HB pencil is standard medium-soft, providing neat dark visible lines. एचबी पेन्सिल गडद आणि स्पष्ट बाह्यरेखा काढण्यासाठी वापरली जाते.",
                difficulty = "Medium"
            ),
            Question(
                subject = "Engineering Drawing",
                chapter = "Electrical Symbols",
                questionTextEn = "What electrical device is represented of two horizontal lines separated by a safe air gap?",
                questionTextMr = "दोन समांतर रेषा मधील गॅप कोणत्या इलेक्ट्रिकल घटकाचे प्रतीक दर्शवते?",
                optionAEn = "Battery Cell", optionAMr = "बॅटरी सेल",
                optionBEn = "Fixed Inductor", optionBMr = "फिक्स्ड इंडक्टर",
                optionCEn = "Capacitor", optionCMr = "कॅपेसिटर (विद्युत धारक)",
                optionDEn = "Transformer", optionDMr = "ट्रान्सफॉर्मर",
                correctOption = "C",
                explanation = "Two identical parallel plates separated represent dielectric capacitor capacity. दोन समांतर रेषा कॅपेसिटरचे प्रतीक दाखवतात.",
                difficulty = "Hard"
            ),

            // --- Workshop Calculation & Science ---
            Question(
                subject = "Workshop Calculation & Science",
                chapter = "Units and Measurements",
                questionTextEn = "What is the SI unit of electric quantity or charge?",
                questionTextMr = "विद्युत प्रभार (Electric Charge) मोजण्याचे मुख्य सिस्टिम एकक कोणते?",
                optionAEn = "Coulomb", optionAMr = "कूलॉम (Coulomb)",
                optionBEn = "Ampere-Hour", optionBMr = "अँपिअर-तास",
                optionCEn = "Ohm-meter", optionCMr = "ओहम-मीटर",
                optionDEn = "Farad", optionDMr = "फॅरड",
                correctOption = "A",
                explanation = "The SI unit of electrical charge is Coulomb (Q). विद्युत प्रभाराचे एस.आय एकक कूलॉम आहे.",
                difficulty = "Easy"
            ),
            Question(
                subject = "Workshop Calculation & Science",
                chapter = "Energy Calculations",
                questionTextEn = "Calculate energy consumed by a 1000 Watt water heater running for 2 hours.",
                questionTextMr = "१००० वॅटचा पाण्याचा हिटर २ तास सुरू ठेवल्यास किती युनिट ऊर्जा खर्च होईल?",
                optionAEn = "0.5 Units (kWh)", optionAMr = "०.५ युनिट",
                optionBEn = "2.0 Units (kWh)", optionBMr = "२.० युनिट (kWh)",
                optionCEn = "10 Units (kWh)", optionCMr = "१० युनिट",
                optionDEn = "2000 Units (kWh)", optionDMr = "२००० युनिट",
                correctOption = "B",
                explanation = "Energy = Power * Time = 1000W * 2 Hrs = 2000 Wh = 2.0 kWh (Units). ऊर्जा = विद्युत शक्ती * तास. १००० वॅट * २ = २ kWh = २ युनिट.",
                difficulty = "Hard"
            )
        )

        db.questionDao().insertQuestions(presetQuestions)

        // Populate preset notes for safety symbols and AC formulas
        db.noteDao().insertNote(
            Note(
                title = "Important Safety Signs (महत्वाचे सुरक्षा चिन्ह)",
                category = "Safety Signs",
                content = """
                    १. Prohibition Signs (निषेधार्थक चिन्हे): वर्तुळाकार आकार, लाल रंगाची बॉर्डर आणि क्रॉस बार. (उदा. सिगारेट पिण्यास मनाई).
                    २. Mandatory Signs (आवश्यक चिन्हे): निळा गोल रंग, पांढरे चिन्ह. (उदा. गॉगल घालणे, बूट घालणे).
                    ३. Warning Signs (धोक्याची चिन्हे): त्रिकोणी आकार, पिवळा बॅकग्राउंड आणि काळी बॉर्डर. (उदा. विजेचा धक्का लागण्याचा धोका).
                    ४. Information Signs (माहितीदर्शक चिन्हे): चौरस किंवा आयताकार आकार, हिरवा बॅकग्राउंड. (उदा. प्रथमोपचार पेटी).
                """.trimIndent()
            )
        )
        db.noteDao().insertNote(
            Note(
                title = "AC Circuit Core Formulas (एसी सर्किट्सचे महत्त्वाचे सूत्रे)",
                category = "AC Circuits",
                content = """
                    १. Impedance (Z) = √(R² + XL²) or √(R² + (XL - XC)²)
                    २. Apparent Power (S) = V * I (एकक: VA किंवा kVA)
                    ३. Active Power (P) = V * I * cosφ (एकk: Watt किंवा kW)
                    ४. Reactive Power (Q) = V * I * sinφ (एकक: VAR किंवा kVAR)
                    ५. Power Factor (PF) = R / Z = cosφ = Active Power / Apparent Power
                """.trimIndent()
            )
        )
    }
}
