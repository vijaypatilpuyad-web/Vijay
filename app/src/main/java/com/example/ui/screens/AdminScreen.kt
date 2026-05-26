package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val language by viewModel.appLanguage.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()

    var activeView by remember { mutableStateOf(0) } // 0: Dashboard/Database Metrics, 1: Add Manual Question, 2: AI Bulk PDF Scanner

    // Manual Fields State
    var subInput by remember { mutableStateOf("Electrician Theory") }
    var chapInput by remember { mutableStateOf("") }
    var qEnInput by remember { mutableStateOf("") }
    var qMrInput by remember { mutableStateOf("") }
    var sizeEnA by remember { mutableStateOf("") }
    var sizeMrA by remember { mutableStateOf("") }
    var sizeEnB by remember { mutableStateOf("") }
    var sizeMrB by remember { mutableStateOf("") }
    var sizeEnC by remember { mutableStateOf("") }
    var sizeMrC by remember { mutableStateOf("") }
    var sizeEnD by remember { mutableStateOf("") }
    var sizeMrD by remember { mutableStateOf("") }
    var correctOptInput by remember { mutableStateOf("A") }
    var expInput by remember { mutableStateOf("") }
    var diffInput by remember { mutableStateOf("Medium") }

    // AI bulk Generator State
    var aiSubjectInput by remember { mutableStateOf("Electrician Theory") }
    var aiChapterInput by remember { mutableStateOf("") }
    var aiPdfTextInput by remember { mutableStateOf("") }

    var feedbackMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (language == "mr") "प्रशासक नियंत्रण (Admin Panel)" else "CBT Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Segment Tab Row Bar
            TabRow(selectedTabIndex = activeView) {
                Tab(
                    selected = activeView == 0,
                    onClick = { activeView = 0 },
                    text = { Text(if (language == "mr") "डॅशबोर्ड" else "Database", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeView == 1,
                    onClick = { activeView = 1 },
                    text = { Text(if (language == "mr") "नवीन प्रश्न" else "Manual MCQs", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeView == 2,
                    onClick = { activeView = 2 },
                    text = { Text(if (language == "mr") "AI जनरेटर" else "AI PDF Parse", fontSize = 11.sp) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Happy feedback message
                if (feedbackMsg.isNotEmpty()) {
                    LaunchedEffect(feedbackMsg) {
                        kotlinx.coroutines.delay(4000)
                        feedbackMsg = ""
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMsg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                when (activeView) {
                    0 -> {
                        // SUB VIEW 0: Database Metrics & Sample list
                        Text(
                            text = if (language == "mr") "डेटाबेस स्थिती आणि विहंगावलोकन" else "CBT Database Status Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${allQuestions.size}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (language == "mr") "एकूण उपलब्ध MCQ प्रश्न" else "Active Questions Loaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Subject lists distribution summary count
                        Text(
                            text = if (language == "mr") "विषय निहाय वितरण" else "Question Category Distribution",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val subjectsList = listOf(
                            "Electrician Theory",
                            "Employability Skills",
                            "Engineering Drawing",
                            "Workshop Calculation & Sci."
                        )

                        subjectsList.forEach { s ->
                            val count = allQuestions.count { it.subject.startsWith(s.take(12)) || it.subject == s }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = s, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(text = "$count Qs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    1 -> {
                        // SUB VIEW 1: Manual Questions Add Form Compiler
                        Text(
                            text = if (language == "mr") "नवीन MCQ प्रश्न संकलित करा" else "Add Manual Mock MCQ Question",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Subject selector dropdown text
                        OutlinedTextField(
                            value = subInput,
                            onValueChange = { subInput = it },
                            label = { Text(if (language == "mr") "विषय नाव (Subject)" else "Subject Name") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Electrician Theory") }
                        )

                        OutlinedTextField(
                            value = chapInput,
                            onValueChange = { chapInput = it },
                            label = { Text(if (language == "mr") "धडा नाव (Chapter)" else "Chapter / Unit Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_chapter_input")
                        )

                        Divider()

                        // Question En
                        OutlinedTextField(
                            value = qEnInput,
                            onValueChange = { qEnInput = it },
                            label = { Text("Question in English") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Question Mr
                        OutlinedTextField(
                            value = qMrInput,
                            onValueChange = { qMrInput = it },
                            label = { Text("प्रश्न मराठी मध्ये (Question in Marathi)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        // En Options
                        Text("Options English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = sizeEnA, onValueChange = { sizeEnA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sizeEnB, onValueChange = { sizeEnB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sizeEnC, onValueChange = { sizeEnC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sizeEnD, onValueChange = { sizeEnD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())

                        Divider()

                        // Marathi Options
                        Text("पर्याय मराठी मध्ये (Options Marathi)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = sizeMrA, onValueChange = { sizeMrA = it }, label = { Text("पर्याय A") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sizeMrB, onValueChange = { sizeMrB = it }, label = { Text("पर्याय B") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sizeMrC, onValueChange = { sizeMrC = it }, label = { Text("पर्याय C") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sizeMrD, onValueChange = { sizeMrD = it }, label = { Text("पर्याय D") }, modifier = Modifier.fillMaxWidth())

                        Divider()

                        // Correct options selection row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (language == "mr") "योग्य पर्याय (Correct Answer)" else "Correct Option Option", fontWeight = FontWeight.Bold)
                            val options = listOf("A", "B", "C", "D")
                            options.forEach { op ->
                                FilterChip(
                                    selected = correctOptInput == op,
                                    onClick = { correctOptInput = op },
                                    label = { Text(op) }
                                )
                            }
                        }

                        // Explanation & Difficulty
                        OutlinedTextField(
                            value = expInput,
                            onValueChange = { expInput = it },
                            label = { Text(if (language == "mr") "स्पष्टीकरण (Detailed Explanation)" else "Detailed Explanation") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (chapInput.isNotEmpty() && qEnInput.isNotEmpty() && sizeEnA.isNotEmpty()) {
                                    viewModel.insertManualQuestion(
                                        subject = subInput,
                                        chapter = chapInput,
                                        qEn = qEnInput,
                                        qMr = qMrInput.ifEmpty { qEnInput },
                                        opAEn = sizeEnA,
                                        opAMr = sizeMrA.ifEmpty { sizeEnA },
                                        opBEn = sizeEnB,
                                        opBMr = sizeMrB.ifEmpty { sizeEnB },
                                        opCEn = sizeEnC,
                                        opCMr = sizeMrC.ifEmpty { sizeEnC },
                                        opDEn = sizeEnD,
                                        opDMr = sizeMrD.ifEmpty { sizeEnD },
                                        correct = correctOptInput,
                                        exp = expInput,
                                        diff = diffInput
                                    )
                                    chapInput = ""
                                    qEnInput = ""
                                    qMrInput = ""
                                    sizeEnA = ""
                                    sizeEnB = ""
                                    sizeEnC = ""
                                    sizeEnD = ""
                                    sizeMrA = ""
                                    sizeMrB = ""
                                    sizeMrC = ""
                                    sizeMrD = ""
                                    feedbackMsg = if (language == "mr") "प्रश्न यशस्वीरित्या डेटाबेसमध्ये जतन झाला!" else "MCQ successfully saved into database!"
                                } else {
                                    feedbackMsg = if (language == "mr") "कृपया आवश्यक त्रुटी भरा!" else "Please compile mandatory fields!"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_manual_question_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (language == "mr") "प्रश्न डेटाबेसला जोडा" else "Save MCQ to Database")
                        }
                    }

                    2 -> {
                        // SUB VIEW 2: AI Bulk PDF Parse / Prompt Analyzer
                        Text(
                            text = if (language == "mr") "AI द्वारे स्वयंचलित MCQ प्रश्न जनरेटर" else "Bulk AI Question Compiler (Syllabus Parsers)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (language == "mr") {
                                "भारत स्किल्स नमुना पीडीएफ किंवा ट्रेड अभ्यासक्रम मजकूर खालील रकान्यात पेस्ट करा. आमची कृत्रिम बुद्धिमत्ता (Gemini AI) आपोआप मराठी आणि इंग्रजीतून ४ प्रमाणित बहुपर्यायी प्रश्न तयार करून डेटाबेसमध्ये जतन करेल!"
                            } else {
                                "Paste study descriptions or Bharat Skills sample PDF strings. Our Gemini-3.5-flash AI engine reads, translates, and injects 4 CBT exam-compliant items straight to the DB."
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        OutlinedTextField(
                            value = aiSubjectInput,
                            onValueChange = { aiSubjectInput = it },
                            label = { Text(if (language == "mr") "विषय (AI Subject)" else "AI Destination Category") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = aiChapterInput,
                            onValueChange = { aiChapterInput = it },
                            label = { Text(if (language == "mr") "धड्याचे नाव (AI Chapter)" else "Destination Chapter Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_chapter_input")
                        )

                        OutlinedTextField(
                            value = aiPdfTextInput,
                            onValueChange = { aiPdfTextInput = it },
                            label = { Text(if (language == "mr") "पीडीएफ / अभ्यासक्रम माहिती पेस्ट करा" else "Paste Syllabus Core Text block") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("ai_pdf_text_input"),
                            placeholder = { Text("e.g. DC generators EMF equations, commutation process, transformer vector groups...") }
                        )

                        if (isAiGenerating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (language == "mr") "AI प्रश्न तयार करत आहे आणि भाषांतर करत आहे..." else "AI compiler processing content & styling...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (aiChapterInput.isNotEmpty() && aiPdfTextInput.isNotEmpty()) {
                                        viewModel.autoGenerateAiQuestions(
                                            subject = aiSubjectInput,
                                            chapter = aiChapterInput,
                                            promptText = aiPdfTextInput
                                        )
                                        aiPdfTextInput = ""
                                        feedbackMsg = if (language == "mr") {
                                            "AI प्रश्न संकलित करण्याचे काम सुरू झाले आहे! पूर्ण झाल्यावर डेटाबेसमध्ये जोडले जातील."
                                        } else {
                                            "AI compiling jobs triggered successfully! Verifying and inserting to DB."
                                        }
                                    } else {
                                        feedbackMsg = if (language == "mr") "कृपया धड्याचे नाव आणि माहिती भरा." else "Please type heading and prompt text."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_bulk_compile_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == "mr") "AI प्रश्न आपोआप संकलित करा" else "Compile with Gemini AI")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
