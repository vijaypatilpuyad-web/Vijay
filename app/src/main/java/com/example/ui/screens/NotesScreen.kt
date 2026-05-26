package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val allNotes by viewModel.notes.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Study Notes, 1: Electrical Symbols, 2: Video Lectures
    var showAddNoteDialog by remember { mutableStateOf(false) }

    // Forms states
    var noteTitle by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("Basic Electricity") }
    var noteContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (language == "mr") "अभ्यास नोट्स आणि साधने" else "Study Notes & Tools") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeTab == 0) {
                        IconButton(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.testTag("add_note_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Custom Note")
                        }
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
            // High fidelity Tabs Row picker
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text(if (language == "mr") "ट्रेड नोट्स" else "Study Cards", fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text(if (language == "mr") "विद्युत चिन्हे" else "CBT Drawing Symbols", fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text(if (language == "mr") "व्हिडिओ मार्गदर्शन" else "Video Tutorial", fontSize = 12.sp) }
                )
            }

            when (activeTab) {
                0 -> {
                    // TAB 0: Study Notes List
                    if (allNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (language == "mr") "अजून कोणतीही वैयक्तिक नोट नाही." else "No custom study notes found.")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(allNotes) { note ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = note.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Add, // Using simple icons
                                                    contentDescription = "Delete Note",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Category: ${note.category}",
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Electrical Drawing Symbols via Canvas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (language == "mr") "नकाशातील प्रमाणित विद्युत चिन्हे" else "Engineering Drawing Symbols Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (language == "mr") {
                                "इंजिनिअरिंग ड्रॉइंग (Engineering Drawing) परीक्षेत विचारण्यात येणारी महत्त्वाची चिन्हे खालीलप्रमाणे आहेत. त्यांच्या रेखाटनावर लक्ष द्या:"
                            } else {
                                "Crucial electrical drawings symbols test items in NCVT exam syllabus. Observe active paint canvases below:"
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        // Vector symbol 1: Resistor Symbol Canvas
                        CbtSymbolCard(
                            title = if (language == "mr") "१. रेझिस्टर (Fixed Resistor)" else "1. Fixed Resistor Symbol",
                            desc = if (language == "mr") "प्रवाहाचा अडथळा दर्शवते - झिग-झॅग रचना." else "Represents opposition to current flow. Zigzag drawing style.",
                            canvasContent = { color ->
                                drawPath(
                                    path = Path().apply {
                                        moveTo(10f, 50f)
                                        lineTo(40f, 50f)
                                        lineTo(50f, 20f)
                                        lineTo(70f, 80f)
                                        lineTo(90f, 20f)
                                        lineTo(110f, 80f)
                                        lineTo(130f, 20f)
                                        lineTo(140f, 50f)
                                        lineTo(170f, 50f)
                                    },
                                    color = color,
                                    style = Stroke(width = 4f)
                                )
                            }
                        )

                        // Vector symbol 2: Capacitor Symbol Canvas
                        CbtSymbolCard(
                            title = if (language == "mr") "२. कॅपेसिटर (Capacitor)" else "2. Fixed Capacitor Plate",
                            desc = if (language == "mr") "दोन समांतर इलेक्ट्रिक प्लेट्स दर्शवते." else "Depicts parallel electrostatic plates with insulating gap.",
                            canvasContent = { color ->
                                drawLine(color, Offset(20f, 50f), Offset(75f, 50f), strokeWidth = 5f)
                                drawLine(color, Offset(75f, 15f), Offset(75f, 85f), strokeWidth = 8f)
                                drawLine(color, Offset(105f, 15f), Offset(105f, 85f), strokeWidth = 8f)
                                drawLine(color, Offset(105f, 50f), Offset(160f, 50f), strokeWidth = 5f)
                            }
                        )

                        // Vector symbol 3: Earth Grounding Symbol
                        CbtSymbolCard(
                            title = if (language == "mr") "३. अर्थ ग्राउंड (Earth Connection)" else "3. Earth Ground System",
                            desc = if (language == "mr") "क्रमाने कमी होणाऱ्या तीन समांतर आडव्या रेषा." else "Three progressively smaller horizontal parallel lines.",
                            canvasContent = { color ->
                                drawLine(color, Offset(90f, 10f), Offset(90f, 50f), strokeWidth = 6f)
                                drawLine(color, Offset(40f, 50f), Offset(140f, 50f), strokeWidth = 6f)
                                drawLine(color, Offset(60f, 65f), Offset(120f, 65f), strokeWidth = 5f)
                                drawLine(color, Offset(80f, 80f), Offset(100f, 80f), strokeWidth = 4f)
                            }
                        )

                        // Vector symbol 4: AC Alternating Current Volt Source
                        CbtSymbolCard(
                            title = if (language == "mr") "४. AC व्होल्टेज सोर्स (AC Volts)" else "4. Alternating Current Source",
                            desc = if (language == "mr") "साइन वेव्ह (Sine wave) दर्शवणारे वर्तुळ." else "Circle housing an internal wave depicting sinusoidal AC load.",
                            canvasContent = { color ->
                                drawCircle(color, radius = 35f, center = Offset(90f, 50f), style = Stroke(width = 5f))
                                // Draw simple sine shape wave path inside circle
                                drawPath(
                                    path = Path().apply {
                                        moveTo(70f, 50f)
                                        cubicTo(77f, 30f, 83f, 30f, 90f, 50f)
                                        cubicTo(97f, 70f, 103f, 70f, 110f, 50f)
                                    },
                                    color = color,
                                    style = Stroke(width = 4f)
                                )
                            }
                        )
                    }
                }

                2 -> {
                    // TAB 2: Video Learning Tutorials Reference List
                    val videos = listOf(
                        VideoLectureData(
                            title = if (language == "mr") "डी.सी. जनरेटर (DC Generator Mechanism)" else "DC Generator Core Theory Lectures",
                            description = "Bharat Skills content covering commutator plates & armature wind rules.",
                            url = "https://www.youtube.com/results?search_query=iti+electrician+dc+generator"
                        ),
                        VideoLectureData(
                            title = if (language == "mr") "ट्रान्सफॉर्मर वर्किंग तत्व (Transformer Working Principle)" else "Single Phase Transformer Basics",
                            description = "Step-up and step-down EMF calculations & oil cooling tests.",
                            url = "https://www.youtube.com/results?search_query=iti+electrician+transformer"
                        ),
                        VideoLectureData(
                            title = if (language == "mr") "घरगुती अर्थिंग आणि वायरिंग धडे" else "House Installation & Earth Grounding Lessons",
                            description = "Plate earthing vs pipe earthing methods and NCVT standards codes.",
                            url = "https://www.youtube.com/results?search_query=iti+electrician+earthing"
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (language == "mr") "खालील मार्गदर्शन व्हिडिओ पाहण्यासाठी इंटरनेट सुरू ठेवा." else "Continuous internet connectivity allows reviewing lectures online below.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        items(videos) { vid ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = vid.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = vid.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vid.url))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.testTag("launch_vid_${vid.title.take(5)}")
                                    ) {
                                        Text(if (language == "mr") "व्हिडिओ मार्गदर्शन पहा" else "Open Video Tutorial")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Note Dialog setup
        if (showAddNoteDialog) {
            AlertDialog(
                onDismissRequest = { showAddNoteDialog = false },
                title = { Text(if (language == "mr") "नवीन ट्रेड अभ्यास नोट जोडा" else "Add Key Trade Note") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text(if (language == "mr") "शीर्षक (Title)" else "Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_title_input")
                        )

                        OutlinedTextField(
                            value = noteCategory,
                            onValueChange = { noteCategory = it },
                            label = { Text(if (language == "mr") "श्रेणी (Category)" else "Category") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = noteContent,
                            onValueChange = { noteContent = it },
                            label = { Text(if (language == "mr") "नोट माहिती (Syllabus Concept Content)" else "Concept Note") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("note_content_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noteTitle.isNotEmpty() && noteContent.isNotEmpty()) {
                                viewModel.addNewNote(noteTitle, noteCategory, noteContent)
                                noteTitle = ""
                                noteContent = ""
                                showAddNoteDialog = false
                            }
                        },
                        modifier = Modifier.testTag("save_note_confirm")
                    ) {
                        Text(if (language == "mr") "जतन करा" else "Save Note")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddNoteDialog = false }) {
                        Text(if (language == "mr") "रद्द करा" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CbtSymbolCard(
    title: String,
    desc: String,
    canvasContent: DrawScope.(Color) -> Unit
) {
    val drawingColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Paint Canvas for native symbol
            Box(
                modifier = Modifier
                    .size(100.dp, 80.dp)
                    .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    canvasContent(drawingColor)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

data class VideoLectureData(
    val title: String,
    val description: String,
    val url: String
)
