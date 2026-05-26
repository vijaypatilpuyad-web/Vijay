package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Question
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val language by viewModel.appLanguage.collectAsState()
    val examQuestions by viewModel.examQuestions.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val latestResult by viewModel.latestFinishedResult.collectAsState()

    // AI explanation states
    val aiExplanationText by viewModel.aiExplanationText.collectAsState()
    val aiExplanationLoading by viewModel.aiExplanationLoading.collectAsState()
    var activeAiQuestionId by remember { mutableStateOf<Long?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }

    if (latestResult == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (language == "mr") "कोणताही अलीकडील निकाल आढळला नाही." else "No latest history found.")
        }
        return
    }

    val result = latestResult!!
    
    // Custom electrical rank generation estimate
    val score = result.scorePercentage
    val rankingTitle = when {
        score >= 90 -> if (language == "mr") "प्रथम श्रेणी (A+ Expert)" else "Electrical Super-Intendent"
        score >= 75 -> if (language == "mr") "उत्कृष्ट (Distinction)" else "Master Electrician"
        score >= 50 -> if (language == "mr") "यशस्वी (Passed)" else "Junior Wireman"
        else -> if (language == "mr") "पुनरावृत्ती आवश्यक (Need Work)" else "Apprentice trainee"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (language == "mr") "CBT परीक्षा निकाल" else "CBT Examination Results") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Home")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Score and Grade card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = rankingTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == "mr") "तुमची अंतिम टक्केवारी" else "Compile Score Percentage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "${String.format("%.1f", result.scorePercentage)}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats metrics bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Check, contentDescription = "Correct", tint = StatusCorrect)
                                Text("${result.correctCount}", fontWeight = FontWeight.Bold)
                                Text(if (language == "mr") "बरोबर" else "Correct", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, contentDescription = "Wrong", tint = StatusWrong)
                                Text("${result.wrongCount}", fontWeight = FontWeight.Bold)
                                Text(if (language == "mr") "चुकीचे" else "Wrong", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Circle, contentDescription = "Skipped", tint = StatusUnattempted)
                                Text("${result.unattemptedCount}", fontWeight = FontWeight.Bold)
                                Text(if (language == "mr") "सोडले" else "Skipped", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // AI Recommendations Tips Segment (Weak area practice)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "AI Coach Recommendations", tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "mr") "AI स्टडी कोच शिफारस (AI Recommendations)" else "AI Study Coach Predictions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            val recommendationText = when {
                                score >= 85 -> if (language == "mr") {
                                    "उत्कृष्ट गती! तुमचे डीसी सर्किट्स पक्के आहेत. पुढील सरावसाठी 'इंजिनिअरिंग ड्रॉइंग' प्रमाणित चिन्हे नक्की अभ्यासा."
                                } else {
                                    "Stellar grasp! Your Electrician Theory is solid. Proceed with complex Electrical symbols in Engineering drawing."
                                }
                                result.wrongCount >= 3 -> if (language == "mr") {
                                    "थोड्या चुका होत आहेत. वरील चुकांच्या स्पष्टीकरणासाठी 'AI Tutor' चिन्हावर क्लिक करा आणि 'कमकुवत प्रश्नांची' ओळख करून घ्या."
                                } else {
                                    "Warning: High wrong attempts noticed. Utilize the 'Ask AI Tutor' button on difficult questions for step-by-step reasoning."
                                }
                                else -> if (language == "mr") {
                                    "थोड्या सरावाची गरज आहे. रोजचे 'दैनिक मॉक चाचण्या' घेत रहा आणि अर्थिंग रेझिस्टन्स संकल्पना बळकट करा."
                                } else {
                                    "Adequate effort! Focus on Earth Grounding and safety formulas. Ensure continuous practice drills daily."
                                }
                            }
                            Text(
                                text = recommendationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Header for Question breakdown View
            item {
                Text(
                    text = if (language == "mr") "प्रश्न पुनरावलोकन आणि स्पष्टीकरण (Answer Key Review)" else "CBT Answer Review Sheets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Details list of all questions in active test
            itemsIndexed(examQuestions) { index, q ->
                val userSelection = selectedAnswers[index]
                val isCorrect = userSelection == q.correctOption

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCorrect) StatusCorrect.copy(alpha = 0.5f) else StatusWrong.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Title row: Status pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isCorrect) StatusCorrect.copy(alpha = 0.1f) else StatusWrong.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isCorrect) {
                                        if (language == "mr") "योग्य उत्तर" else "Correct"
                                    } else {
                                        if (language == "mr") "चुकीचे उत्तर" else "Incorrect"
                                    },
                                    color = if (isCorrect) StatusCorrect else StatusWrong,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Difficulty: ${q.difficulty}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Question texts
                        Text(
                            text = "Q.${index + 1} ${if (language == "mr") q.questionTextMr else q.questionTextEn}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected vs Correct options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == "mr") "तुमचे उत्तर:" else "Your Choice:",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = if (userSelection != null) {
                                        val optText = when (userSelection) {
                                            "A" -> if (language == "mr") q.optionAMr else q.optionAEn
                                            "B" -> if (language == "mr") q.optionBMr else q.optionBEn
                                            "C" -> if (language == "mr") q.optionCMr else q.optionCEn
                                            else -> if (language == "mr") q.optionDMr else q.optionDEn
                                        }
                                        "($userSelection) $optText"
                                    } else {
                                        if (language == "mr") "सोडले (Skipped)" else "Unattempted"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) StatusCorrect else StatusWrong
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == "mr") "योग्य उत्तर:" else "Correct Answer:",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                val correctText = when (q.correctOption) {
                                    "A" -> if (language == "mr") q.optionAMr else q.optionAEn
                                    "B" -> if (language == "mr") q.optionBMr else q.optionBEn
                                    "C" -> if (language == "mr") q.optionCMr else q.optionCEn
                                    else -> if (language == "mr") q.optionDMr else q.optionDEn
                                }
                                Text(
                                    text = "(${q.correctOption}) $correctText",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusCorrect
                                )
                            }
                        }

                        // Local Explanation display
                        if (q.explanation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 ${q.explanation}",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = Color.DarkGray
                                )
                            }
                        }

                        // Dynamic Ask AI Explanations button trigger
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                activeAiQuestionId = q.id
                                viewModel.loadAiExplanationForQuestion(q)
                                showAiDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("ask_ai_${index}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach Logo", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "mr") "AI स्पष्टीकरण विचारा" else "Ask AI Coach Explanation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Go back actions box
            item {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("result_done_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (language == "mr") "मुख्यपृष्ठावर जा" else "Back to Main Home")
                }
            }
        }

        // Expanded Interactive AI tutor Explanation overlay Card
        if (showAiDialog) {
            AlertDialog(
                onDismissRequest = { showAiDialog = false },
                icon = {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Expert",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (language == "mr") "AI शिक्षक स्पष्टीकरण" else "AI Coach Deep Explanation",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (aiExplanationLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Text(
                                text = aiExplanationText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAiDialog = false }
                    ) {
                        Text(if (language == "mr") "समजले, धन्यवाद" else "Understood, Thanks")
                    }
                }
            )
        }
    }
}
