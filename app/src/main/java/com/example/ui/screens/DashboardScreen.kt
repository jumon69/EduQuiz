package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.QuizHistoryEntity
import com.example.data.SubjectEntity
import com.example.ui.theme.*
import com.example.viewmodel.QuizViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: QuizViewModel,
    onNavigateToParser: () -> Unit,
    onStartQuiz: (SubjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val history by viewModel.allHistory.collectAsStateWithLifecycle()
    val wrongAnswers by viewModel.allWrongAnswers.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectInputName by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Subtle cyber canvas grid markings
        CanvasBgGrid()

        Scaffold(
            topBar = {
                HighDensityHeader(
                    title = "এডুকুইজ",
                    subtitle = "ডাটাবেজ: এসকিউলাইট সচল",
                    showBackButton = false
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = CyberPink,
                    contentColor = Color.White,
                    modifier = Modifier
                        .testTag("add_subject_fab")
                        .border(1.dp, CyberCyan, RoundedCornerShape(16.dp))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Glow Stats Banner
                item {
                    StatsBanner(history = history, subjects = subjects)
                }

                // Parser Link Selector
                item {
                    ParserRedirectionCard(onNavigateToParser = onNavigateToParser)
                }

                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "কুইজ বিষয়সমূহ [${subjects.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 15.sp
                        )
                    }
                }

                // Subjects List
                if (subjects.isEmpty()) {
                    item {
                        EmptySubjectsPlaceholder { showAddDialog = true }
                    }
                } else {
                    items(subjects) { subject ->
                        SubjectRowCard(
                            subject = subject,
                            onStartQuiz = { onStartQuiz(subject) },
                            onDelete = { viewModel.deleteSubject(subject.id) }
                        )
                    }
                }

                // Revision / Wrong Answers Review Bank Sector
                if (wrongAnswers.isNotEmpty()) {
                    item {
                        Text(
                            text = "রিভিশন ব্যাংক [${wrongAnswers.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontSize = 15.sp
                        )
                    }

                    items(wrongAnswers) { mcq ->
                        RevisionMcqCard(
                            mcq = mcq,
                            onResolve = { viewModel.deleteWrongAnswer(mcq.id) }
                        )
                    }
                }

                // History Header
                if (history.isNotEmpty()) {
                    item {
                        Text(
                            text = "পরীক্ষার ইতিহাস রেকর্ড [${history.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberPink,
                            fontSize = 15.sp
                        )
                    }

                    items(history.take(6)) { log ->
                        HistoryLogCard(log = log)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        // Subject Addition Dialog
        if (showAddDialog) {
            AddSubjectDialog(
                name = subjectInputName,
                onNameChange = { subjectInputName = it },
                onDismiss = {
                    showAddDialog = false
                    subjectInputName = ""
                },
                onConfirm = {
                    if (subjectInputName.isNotBlank()) {
                        val iconNames = listOf("Terminal", "Science", "School", "Calculate")
                        val colors = listOf("#FF007F", "#00F0FF", "#9400D3", "#39FF14")
                        viewModel.addNewSubject(
                            name = subjectInputName,
                            colorHex = colors.random(),
                            iconName = iconNames.random()
                        )
                        showAddDialog = false
                        subjectInputName = ""
                    }
                }
            )
        }
    }
}

// Background custom vector drawing of a coordinate cyberpunk technical grid
@Composable
fun CanvasBgGrid() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val gridDensity = 45.dp.toPx()
                val width = size.width
                val height = size.height
                val paintColor = Color(0xFF1E293B).copy(alpha = 0.25f)

                // vertical grid lines
                var x = 0f
                while (x < width) {
                    drawLine(paintColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, height), strokeWidth = 1f)
                    x += gridDensity
                }

                // horizontal grid lines
                var y = 0f
                while (y < height) {
                    drawLine(paintColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 1f)
                    y += gridDensity
                }
            }
    )
}

@Composable
fun StatsBanner(history: List<QuizHistoryEntity>, subjects: List<SubjectEntity>) {
    val totalQuizzes = history.size
    val averageScore = if (history.isNotEmpty()) history.map { it.scorePercentage }.average().toInt() else 0
    val totalQuestions = history.sumOf { it.totalQuestions }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CyberPink, CyberCyan)),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "⚡ সিস্টেম ইন্টিগ্রিটি: সচল রয়েছে",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = CyberCyan,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatSubBlock(label = "গড় নির্ভুলতা", value = "$averageScore%", tint = CyberPink)
                DividerVertical()
                StatSubBlock(label = "অংশগ্রহণ সংখ্যা", value = totalQuizzes.toString(), tint = CyberCyan)
                DividerVertical()
                StatSubBlock(label = "বিষয়ের সংখ্যা", value = subjects.size.toString(), tint = CyberPurple)
            }
        }
    }
}

@Composable
fun StatSubBlock(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = tint,
            fontSize = 20.sp
        )
    }
}

@Composable
fun DividerVertical() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(35.dp)
            .background(Color.DarkGray.copy(alpha = 0.5f))
    )
}

@Composable
fun ParserRedirectionCard(onNavigateToParser: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToParser() }
            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload",
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ডকুমেন্ট পার্সার গেটওয়ে",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "পিডিএফ বা ক্যামেরা স্ন্যাপ থেকে এমসিকিউ তৈরি করুন",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = CyberCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SubjectRowCard(
    subject: SubjectEntity,
    onStartQuiz: () -> Unit,
    onDelete: () -> Unit
) {
    val outlineColor = Color(android.graphics.Color.parseColor(subject.colorHex))
    val iconVector = when (subject.iconName) {
        "Terminal" -> Icons.Default.Terminal
        "Science" -> Icons.Default.Science
        "School" -> Icons.Default.School
        "Calculate" -> Icons.Default.Calculate
        "Camera" -> Icons.Default.PhotoCamera
        else -> Icons.Default.MenuBook
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, outlineColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable { onStartQuiz() }
            .testTag("subject_card_${subject.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(outlineColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = outlineColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "ম্যাট্রিক্স নোড আইডি: #00${subject.id}",
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_subject_btn_${subject.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete Subject",
                    tint = CyberPink.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start Program",
                tint = CyberCyan,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun HistoryLogCard(log: QuizHistoryEntity) {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = log.subjectName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = "পরীক্ষার সময়: $dateString",
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${log.scorePercentage}% নির্ভুলতা",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (log.scorePercentage >= 80) CyberGreen else if (log.scorePercentage >= 50) CyberCyan else CyberPink,
                    fontSize = 13.sp
                )
                Text(
                    text = "স্কোর: ${log.correctAnswers}/${log.totalQuestions}",
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun EmptySubjectsPlaceholder(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alert",
            tint = CyberPink,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "জরুরী সংকেত: কোনো বিষয় যুক্ত করা নেই",
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "কুইজ শুরু করতে নিচে একটি নতুন বিষয় বা পিডিএফ যুক্ত করুন।",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            modifier = Modifier.testTag("init_subject_btn")
        ) {
            Text(text = "নতুন বিষয় যোগ করুন", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddSubjectDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPink, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "নতুন বিষয় যোগ করুন",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberPink,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("বিষয়ের নাম লিখুন", color = TextSecondary, fontFamily = FontFamily.Monospace) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_subject_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_subject_cancel")) {
                        Text(text = "বাতিল", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                        modifier = Modifier.testTag("dialog_subject_confirm")
                    ) {
                        Text(text = "সম্পন্ন", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RevisionMcqCard(
    mcq: com.example.data.McqEntity,
    onResolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ভুল উত্তর রিভিউ ফ্ল্যাগ",
                    color = CyberGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                        .clickable { onResolve() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "সমাধান / বাদ দিন",
                        color = CyberGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mcq.question,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options Display
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "A) ${mcq.optionA}", color = if (mcq.correctAnswer == "A") CyberGreen else TextSecondary, fontSize = 11.sp, fontWeight = if (mcq.correctAnswer == "A") FontWeight.Bold else FontWeight.Normal)
                Text(text = "B) ${mcq.optionB}", color = if (mcq.correctAnswer == "B") CyberGreen else TextSecondary, fontSize = 11.sp, fontWeight = if (mcq.correctAnswer == "B") FontWeight.Bold else FontWeight.Normal)
                Text(text = "C) ${mcq.optionC}", color = if (mcq.correctAnswer == "C") CyberGreen else TextSecondary, fontSize = 11.sp, fontWeight = if (mcq.correctAnswer == "C") FontWeight.Bold else FontWeight.Normal)
                Text(text = "D) ${mcq.optionD}", color = if (mcq.correctAnswer == "D") CyberGreen else TextSecondary, fontSize = 11.sp, fontWeight = if (mcq.correctAnswer == "D") FontWeight.Bold else FontWeight.Normal)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = CyberGreen.copy(alpha = 0.05f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 সঠিক উত্তর অপশন: [${mcq.correctAnswer}]\n${mcq.explanation}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
