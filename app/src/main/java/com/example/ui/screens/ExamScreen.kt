package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Question
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val language by viewModel.appLanguage.collectAsState()
    val isExamActive by viewModel.isExamActive.collectAsState()
    val examTitle by viewModel.examTitle.collectAsState()
    val examQuestions by viewModel.examQuestions.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val markedForReview by viewModel.markedForReview.collectAsState()
    val timeRemainingSeconds by viewModel.timeRemainingSeconds.collectAsState()

    // Local In-Question Language Override specifically for reading
    var inQuestionLanguageOverride by remember { mutableStateOf<String?>(null) }
    val activeLang = inQuestionLanguageOverride ?: language

    var showSubmitConfirmation by remember { mutableStateOf(false) }
    var showPaletteDrawer by remember { mutableStateOf(false) }

    // If the exam is closed unexpectedly, go back
    LaunchedEffect(isExamActive) {
        if (!isExamActive && viewModel.latestFinishedResult.value != null) {
            onNavigateToResult()
        }
    }

    // Reset language override when the question changes
    LaunchedEffect(currentQuestionIndex) {
        inQuestionLanguageOverride = null
    }

    if (examQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentQuestion = examQuestions.getOrNull(currentQuestionIndex) ?: return
    val totalQuestions = examQuestions.size

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    
    // Timer Warning Highlights
    val isTimerLow = timeRemainingSeconds < 120 // warning under 2 mins
    val timerColor = if (isTimerLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val timerBg = if (isTimerLow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    // Layout configuration: side palette for wide layouts/landscape
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val showSidePalette = isLandscape && configuration.screenWidthDp > 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = examTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "NCVT Online Simulator",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    // Timer box
                    Surface(
                        color = timerBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = timerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedTime,
                                color = timerColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Palette Toggle button (only on portrait/mobile)
                    if (!showSidePalette) {
                        IconButton(
                            onClick = { showPaletteDrawer = true },
                            modifier = Modifier.testTag("palette_toggle_button")
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = "View Palette")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left main panel: Question & Options Selector
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header of Question Card: Question No and Local Switch Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Prompt Level Instant Language Switch Overlay
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "मराठी",
                            fontSize = 10.sp,
                            fontWeight = if (activeLang == "mr") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeLang == "mr") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeLang == "mr") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { inQuestionLanguageOverride = "mr" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "EN",
                            fontSize = 10.sp,
                            fontWeight = if (activeLang == "en") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeLang == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeLang == "en") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { inQuestionLanguageOverride = "en" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question container Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Question Body - Marathi or English
                        val questionBodyText = if (activeLang == "mr") currentQuestion.questionTextMr else currentQuestion.questionTextEn
                        Text(
                            text = questionBodyText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Render options
                        val selectedOpt = selectedAnswers[currentQuestionIndex]
                        
                        CbtOptionRow(
                            label = "A",
                            text = if (activeLang == "mr") currentQuestion.optionAMr else currentQuestion.optionAEn,
                            isSelected = selectedOpt == "A",
                            onClick = { viewModel.selectAnswer(currentQuestionIndex, "A") }
                        )

                        CbtOptionRow(
                            label = "B",
                            text = if (activeLang == "mr") currentQuestion.optionBMr else currentQuestion.optionBEn,
                            isSelected = selectedOpt == "B",
                            onClick = { viewModel.selectAnswer(currentQuestionIndex, "B") }
                        )

                        CbtOptionRow(
                            label = "C",
                            text = if (activeLang == "mr") currentQuestion.optionCMr else currentQuestion.optionCEn,
                            isSelected = selectedOpt == "C",
                            onClick = { viewModel.selectAnswer(currentQuestionIndex, "C") }
                        )

                        CbtOptionRow(
                            label = "D",
                            text = if (activeLang == "mr") currentQuestion.optionDMr else currentQuestion.optionDEn,
                            isSelected = selectedOpt == "D",
                            onClick = { viewModel.selectAnswer(currentQuestionIndex, "D") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom control bars: Prev, Mark for Review, Next/Submit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = { viewModel.navigateToQuestion(currentQuestionIndex - 1) },
                        enabled = currentQuestionIndex > 0,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "mr") "मागे" else "Prev")
                    }

                    // Mark for Review
                    val isMarked = markedForReview.contains(currentQuestionIndex)
                    IconButton(
                        onClick = { viewModel.toggleMarkForReview(currentQuestionIndex) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isMarked) StatusReview.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .testTag("mark_for_review_button")
                    ) {
                        Icon(
                            imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Review Option",
                            tint = if (isMarked) StatusReview else Color.Gray
                        )
                    }

                    // Next / Finish Quiz Button
                    val isLast = currentQuestionIndex == totalQuestions - 1
                    if (isLast) {
                        Button(
                            onClick = { showSubmitConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCorrect),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("submit_exam_trigger")
                        ) {
                            Text(if (language == "mr") "प्रस्तुत करा" else "Submit")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Check, contentDescription = "Submit Exam", modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.navigateToQuestion(currentQuestionIndex + 1) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (language == "mr") "पुढे" else "Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            }

            // Right side persistent panel (Wide devices / landscapes only)
            if (showSidePalette) {
                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .padding(top = 8.dp, bottom = 8.dp, end = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = if (language == "mr") "प्रश्न पॅलेट (CBT Palette)" else "Questions Index",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ExamPaletteGrid(
                            questions = examQuestions,
                            selectedAnswers = selectedAnswers,
                            markedForReview = markedForReview,
                            currentIndex = currentQuestionIndex,
                            onGridItemClick = { index -> viewModel.navigateToQuestion(index) }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { showSubmitConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCorrect),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (language == "mr") "परीक्षा सबमिट करा" else "Finish CBT Exam")
                        }
                    }
                }
            }
        }

        // Left slide in overlay drawer sheet of palette (mobile devices only)
        if (showPaletteDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showPaletteDrawer = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (language == "mr") "प्रश्न पॅलेट (NCVT CBT Panel)" else "NCVT CBT Palette Panel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExamPaletteGrid(
                        questions = examQuestions,
                        selectedAnswers = selectedAnswers,
                        markedForReview = markedForReview,
                        currentIndex = currentQuestionIndex,
                        onGridItemClick = { index -> 
                            viewModel.navigateToQuestion(index)
                            showPaletteDrawer = false
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showPaletteDrawer = false
                            showSubmitConfirmation = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCorrect),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (language == "mr") "परीक्षा सबमिट करा (Submit Exam)" else "Finish & Submit Exam")
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Submit Examination dialog verification box
        if (showSubmitConfirmation) {
            AlertDialog(
                onDismissRequest = { showSubmitConfirmation = false },
                title = {
                    Text(if (language == "mr") "परीक्षा सबमिट करायची?" else "Submit Examination?")
                },
                text = {
                    val attempted = selectedAnswers.size
                    val unattempted = totalQuestions - attempted
                    Text(
                        text = if (language == "mr") {
                            "तुम्ही $totalQuestions पैकी $attempted प्रश्नांची उत्तरे दिली आहेत. आणि $unattempted प्रश्न अनुत्तरीत आहेत.\n\nखरोखर परीक्षा सबमिट करून निकाल पाहायचा आहे का?"
                        } else {
                            "You have attempted $attempted out of $totalQuestions questions ($unattempted unattempted).\n\nAre you sure you want to finish the exam and compile your CBT report card?"
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitConfirmation = false
                            viewModel.submitActiveExam()
                            onNavigateToResult()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCorrect),
                        modifier = Modifier.testTag("confirm_submit_button")
                    ) {
                        Text(if (language == "mr") "होय, सबमिट करा" else "Yes, Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitConfirmation = false }) {
                        Text(if (language == "mr") "नाही, सराव सुरू ठेवा" else "Keep Practicing")
                    }
                }
            )
        }
    }
}

@Composable
fun CbtOptionRow(
    label: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val outlineColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
    val containerBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(2.dp, outlineColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ExamPaletteGrid(
    questions: List<Question>,
    selectedAnswers: Map<Int, String>,
    markedForReview: Set<Int>,
    currentIndex: Int,
    onGridItemClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(questions) { idx, _ ->
            val hasAnswered = selectedAnswers.containsKey(idx)
            val isMarked = markedForReview.contains(idx)
            val isCurrent = idx == currentIndex

            // Color status based on standard CBT algorithms
            val cardBg = when {
                isCurrent -> Color.LightGray.copy(alpha = 0.5f)
                isMarked -> StatusReview
                hasAnswered -> StatusCorrect
                else -> Color.LightGray.copy(alpha = 0.2f)
            }

            val contentColor = when {
                isCurrent -> MaterialTheme.colorScheme.onSurface
                isMarked || hasAnswered -> Color.White
                else -> Color.DarkGray
            }

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardBg)
                    .border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onGridItemClick(idx) }
                    .testTag("palette_item_${idx}"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${idx + 1}",
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    // Mini overlay ticks
                    if (isMarked && hasAnswered) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Answered",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
