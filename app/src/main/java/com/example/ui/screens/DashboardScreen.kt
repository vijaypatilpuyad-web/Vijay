package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.data.TestHistory
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.CBTPrimary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToExam: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val language by viewModel.appLanguage.collectAsState()
    val testHistoryList by viewModel.testHistory.collectAsState()
    val allQuestionsList by viewModel.allQuestions.collectAsState()
    
    // Test dialog configuration state
    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedSubjectForCbt by remember { mutableStateOf("Electrician Theory") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Sleek Header with Rounded Corners & Embedded Stats Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .drawBehind {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.3f),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "E",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = if (language == "mr") "आयटीआय इलेक्ट्रिशियन मास्टर" else "ITI Electrician Master",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = if (language == "mr") "NCVT CBT परीक्षा • CBT पोर्टल" else "NCVT CBT Exam Portal",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Language Switch Toggle
                            TextButton(
                                onClick = { viewModel.setLanguage(if (language == "mr") "en" else "mr") },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .height(34.dp)
                                    .testTag("lang_toggle_button")
                            ) {
                                Text(
                                    text = if (language == "mr") "EN" else "MR",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            // Admin Button
                            IconButton(
                                onClick = onNavigateToAdmin,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .testTag("admin_nav_button")
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Area",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quick Stats Card (Rank, Tests, Accuracy) from Sleek Design Specifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .drawBehind {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.15f),
                                    style = Stroke(width = 1.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val totalTests = testHistoryList.size
                        val averageScore = if (totalTests > 0) testHistoryList.map { it.scorePercentage }.average().toInt() else 0
                        val rankStr = if (totalTests > 0) "#${150 - totalTests * 3}" else "#124"

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "mr") "रँक • RANK" else "Rank • रँक",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rankStr,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "mr") "चाचण्या • TESTS" else "Tests • चाचण्या",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$totalTests",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "mr") "अचूकता • Accuracy" else "Accuracy • अचूकता",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$averageScore%",
                                color = Color(0xFF34D399),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action: Full Mock Test Banner (Lined boundary with primary border highlight)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        viewModel.startCbtTest(
                            title = "Annual Full Mock Test",
                            subject = "Electrician Theory",
                            count = 25,
                            durationMinutes = 35
                        )
                        onNavigateToExam()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                color = CBTPrimary,
                                topLeft = Offset.Zero,
                                size = this.size.copy(width = 4.dp.toPx())
                            )
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            text = if (language == "mr") "वार्षिक पूर्ण ट्रेड मॉक टेस्ट" else "Annual Full Mock Test",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "mr") "पूर्ण ट्रेड मॉक टेस्ट • २५ प्रश्न • ३५ मिनिटे" else "Full Trade Mock Test • 25 Questions • 35 Mins",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    Button(
                        onClick = {
                            viewModel.startCbtTest(
                                title = "Annual Full Mock Test",
                                subject = "Electrician Theory",
                                count = 25,
                                durationMinutes = 35
                            )
                            onNavigateToExam()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("start_mock_test_button")
                    ) {
                        Text(
                            text = if (language == "mr") "सुरू करा" else "START",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject Categories Section Title
            Text(
                text = if (language == "mr") "विषय • Subjects" else "Subjects • विषय",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Column Grid for Subject Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Electrician Theory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedSubjectForCbt = "Electrician Theory"
                            showConfigDialog = true
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == "mr") "इलेक्ट्रिशियन थ्योरी" else "Electrician Theory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "mr") "१,२५०+ प्रश्नपेढी" else "1,250 MCQs",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Employability Skills
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedSubjectForCbt = "Employability Skills"
                            showConfigDialog = true
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF3E8FF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == "mr") "एम्प्लॉयबिलिटी स्किल्स" else "Employability Skills",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "mr") "४५०+ प्रश्नपेढी" else "450 MCQs",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Engineering Drawing
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedSubjectForCbt = "Engineering Drawing"
                            showConfigDialog = true
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFFEDD5), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📐", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == "mr") "इंजिनिअरिंग ड्रॉइंग" else "Engineering Drawing",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "mr") "३००+ प्रश्नपेढी" else "300 MCQs",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Workshop Calculation & Sci.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedSubjectForCbt = "Workshop Calculation & Sci."
                            showConfigDialog = true
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFD1FAE5), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == "mr") "वर्कशॉप कॅल्क्युलेशन" else "W/C & Science",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "mr") "६८०+ प्रश्नपेढी" else "680 MCQs",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Practice modes section title
            Text(
                text = if (language == "mr") "सराव • Exam Prep" else "Exam Prep • सराव",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Scrolling Modes List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Daily Random Quiz (Slate 900)
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.startCbtTest(
                                title = "Daily Mock CBT ${SimpleDateFormat("dd MMM", Locale.US).format(Date())}",
                                subject = "Electrician Theory",
                                count = 10,
                                durationMinutes = 10
                            )
                            onNavigateToExam()
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = if (language == "mr") "दैनिक खेळा" else "DAILY",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == "mr") "रँडम क्विज" else "Random Quiz",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Card 2: Formulas & Notes (Sleek Blue)
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .background(CBTPrimary, RoundedCornerShape(16.dp))
                        .clickable {
                            onNavigateToNotes()
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = if (language == "mr") "स्मरणपत्र" else "NOTES",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == "mr") "अभ्यास नोट्स" else "Important Notes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Card 3: Pinned / Bookmarks (Rose-500 equivalent)
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .background(Color(0xFFF43F5E), RoundedCornerShape(16.dp))
                        .clickable {
                            onNavigateToBookmarks()
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = if (language == "mr") "जतन केलेले" else "PINNED",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == "mr") "बुकमार्क सराव" else "Bookmarks Practice",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Diagnostic CBT configurations panel before start
        if (showConfigDialog) {
            AlertDialog(
                onDismissRequest = { showConfigDialog = false },
                title = {
                    Text(
                        text = if (language == "mr") "थेट CBT चाचणी सेटअप" else "CBT Simulator Setup",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = if (language == "mr") {
                                "विषय: ${if (selectedSubjectForCbt == "Electrician Theory") "इलेक्ट्रिशियन थ्योरी" else selectedSubjectForCbt}\n" +
                                "चाचणी सराव सेटिंग्ज निवडा:"
                            } else {
                                "Subject: $selectedSubjectForCbt\nSelect your simulator thresholds:"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Question Count Selector
                        Column {
                            Text(
                                text = if (language == "mr") {
                                    "प्रश्न संख्या: ${viewModel.customQuestionCount.value}"
                                } else {
                                    "Total Questions: ${viewModel.customQuestionCount.value}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = viewModel.customQuestionCount.value.toFloat(),
                                onValueChange = { viewModel.customQuestionCount.value = it.toInt() },
                                valueRange = 5f..25f,
                                steps = 3
                            )
                        }

                        // Negative marking option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == "mr") "ऋणात्मक गुण (Negative Marking)" else "Negative Marking (-0.25)",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (language == "mr") "चुकीच्या उत्तरासाठी गुण कापले जातील" else "Penalty applied on incorrect attempts",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = viewModel.includeNegativeMarking.value,
                                onCheckedChange = { viewModel.includeNegativeMarking.value = it },
                                modifier = Modifier.testTag("negative_marking_switch")
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfigDialog = false
                            viewModel.startCbtTest(
                                title = if (language == "mr") "$selectedSubjectForCbt प्रॅक्टिस टेस्ट" else "$selectedSubjectForCbt CBT Drill",
                                subject = selectedSubjectForCbt,
                                count = viewModel.customQuestionCount.value,
                                durationMinutes = if (viewModel.customQuestionCount.value <= 10) 10 else 25
                            )
                            onNavigateToExam()
                        },
                        modifier = Modifier.testTag("start_cbt_button")
                    ) {
                        Text(if (language == "mr") "परीक्षा सुरू करा" else "Start Examination")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfigDialog = false }) {
                        Text(if (language == "mr") "रद्द करा" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StatsSummaryView(language: String, historyList: List<TestHistory>, totalQuestionsPool: Int) {
    val totalTests = historyList.size
    val averageScore = if (totalTests > 0) historyList.map { it.scorePercentage }.average().toInt() else 0
    val bestScore = if (totalTests > 0) historyList.maxOf { it.scorePercentage }.toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = if (language == "mr") "तुमची प्रगती सूची (Your Report Card)" else "CBT Activity Summary",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalTests",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (language == "mr") "एकूण चाचण्या" else "Tests Taken",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$averageScore%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (language == "mr") "सरासरी गुण" else "Avg Score",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$bestScore%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (language == "mr") "सर्वोत्कृष्ट गुण" else "Best Attempt",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalQuestionsPool",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (language == "mr") "उपलब्ध प्रश्न" else "Exam Qs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectListCard(
    data: SubjectCardData,
    language: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(data.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    data.icon,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == "mr") data.titleMr else data.titleEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (language == "mr") data.descriptionMr else data.descriptionEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun CbtModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

data class SubjectCardData(
    val titleEn: String,
    val titleMr: String,
    val descriptionEn: String,
    val descriptionMr: String,
    val icon: ImageVector,
    val color: Color
)
