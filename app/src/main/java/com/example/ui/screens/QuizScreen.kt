package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.TutorChatMessage
import com.example.data.McqEntity
import com.example.data.SubjectEntity
import com.example.ui.theme.*
import com.example.viewmodel.QuizViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subject by viewModel.activeSubject.collectAsStateWithLifecycle()
    val questions by viewModel.activeQuizQuestions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val choices by viewModel.userChoices.collectAsStateWithLifecycle()
    val checkedMap by viewModel.isQuestionChecked.collectAsStateWithLifecycle()
    val quizFinished by viewModel.quizFinished.collectAsStateWithLifecycle()
    
    val tutorMessages by viewModel.aiTutorMessages.collectAsStateWithLifecycle()
    val tutorThinking by viewModel.isAiTutorThinking.collectAsStateWithLifecycle()

    var showTutorDrawerByClick by remember { mutableStateOf(false) }
    var chatInputFieldText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val currentMcq = questions.getOrNull(currentIndex)
    val totalCount = questions.size

    val isCurrentChecked = checkedMap[currentIndex] == true
    val currentChoice = choices[currentIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        CanvasBgGrid()

        Scaffold(
            topBar = {
                val headerTitle = if (quizFinished) "কুইজ ফলাফল প্রাপ্ত" else "কুইজ সেশন চলমান"
                val headerSubtitle = "বিষয়: ${subject?.name?.uppercase() ?: "অজানা বিষয়"}"
                HighDensityHeader(
                    title = headerTitle,
                    subtitle = headerSubtitle,
                    showBackButton = true,
                    backButtonTestTag = "quiz_back_btn",
                    customNavigationIcon = if (quizFinished) Icons.Default.ArrowBack else Icons.Default.Close,
                    onBackClick = onNavigateBack
                ) {
                    // Quick toggle for AI Tutor Sidebar
                    if (currentMcq != null && !quizFinished) {
                        IconButton(
                            onClick = { showTutorDrawerByClick = !showTutorDrawerByClick },
                            modifier = Modifier.testTag("toggle_tutor_icon_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (tutorThinking) {
                                        Badge(containerColor = CyberPink)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "AI Tutor",
                                    tint = if (showTutorDrawerByClick) CyberPink else CyberCyan
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            if (questions.isEmpty()) {
                // No questions empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = CyberPink, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ত্রুটি: কোনো এমসিকিউ পাওয়া যায়নি", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "দয়া করে প্রথমে কোনো পিডিএফ বা কুইজ নোড থেকে এমসিকিউ এক্সট্র্যাক্ট করে নিন।",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 30.dp, end = 30.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)) {
                            Text("হোম স্ক্রিনে ফিরে যান", color = Color.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else if (quizFinished) {
                // Post quiz comprehensive score matrix
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val correctCount = questions.filterIndexed { index, mcq -> choices[index] == mcq.correctAnswer }.size
                    val percent = (correctCount * 100) / totalCount

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberPink, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("কুইজ সেশন সফলভাবে সম্পন্ন হয়েছে 🎉", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "$percent%",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 48.sp,
                                color = if (percent >= 80) CyberGreen else if (percent >= 50) CyberCyan else CyberPink
                            )
                            Text(
                                text = "নির্ভুলতার অনুপাত",
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
 
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "মোট সমাধানকৃত প্রশ্ন: $totalCount টি\nসঠিক উত্তর: $correctCount টি",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
 
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("score_panel_exit_btn")
                            ) {
                                Text("হোমে ফিরে যান এবং ট্র্যাকার আপডেট করুন", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (currentMcq != null) {
                // Split Screen layout: Left is MCQ options, Right is AI Tutor Sidebar (if enabled)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Left Main MCQ side
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Progress Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "প্রশ্ন: [${currentIndex + 1} / $totalCount]",
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "বিষয়: ${subject?.name}",
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        // Linear Progress Indicators
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(4.dp)
                                .background(Color.DarkGray, RoundedCornerShape(2.dp))
                        ) {
                            val progressionFraction = (currentIndex + 1).toFloat() / totalCount.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressionFraction)
                                    .background(CyberCyan, RoundedCornerShape(2.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Question Board Panel with custom brand accent on left hand side
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightSlateSurface, RoundedCornerShape(12.dp))
                                .drawBehind {
                                    val strokeW = 4.dp.toPx()
                                    drawLine(
                                        color = CyberPink,
                                        start = androidx.compose.ui.geometry.Offset(strokeW / 2f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(strokeW / 2f, size.height),
                                        strokeWidth = strokeW
                                    )
                                }
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
                                ) {
                                    Text(
                                        text = "প্রশ্ন ${currentIndex + 1} / $totalCount",
                                        fontWeight = FontWeight.Bold,
                                        color = CyberPink,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(Color(0x1AFFFFFF))
                                    )
                                }
                                Text(
                                    text = currentMcq.question,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // A, B, C, D Option Lists
                        McqOptionButton(tag = "A", optionText = currentMcq.optionA, currentMcq = currentMcq, isChecked = isCurrentChecked, isSelected = currentChoice == "A", onClick = { viewModel.selectOption("A") })
                        Spacer(modifier = Modifier.height(10.dp))
                        McqOptionButton(tag = "B", optionText = currentMcq.optionB, currentMcq = currentMcq, isChecked = isCurrentChecked, isSelected = currentChoice == "B", onClick = { viewModel.selectOption("B") })
                        Spacer(modifier = Modifier.height(10.dp))
                        McqOptionButton(tag = "C", optionText = currentMcq.optionC, currentMcq = currentMcq, isChecked = isCurrentChecked, isSelected = currentChoice == "C", onClick = { viewModel.selectOption("C") })
                        Spacer(modifier = Modifier.height(10.dp))
                        McqOptionButton(tag = "D", optionText = currentMcq.optionD, currentMcq = currentMcq, isChecked = isCurrentChecked, isSelected = currentChoice == "D", onClick = { viewModel.selectOption("D") })

                        Spacer(modifier = Modifier.height(24.dp))

                        // Bottom Actions Controls
                        if (!isCurrentChecked) {
                            Button(
                                onClick = { viewModel.checkAnswer() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                enabled = currentChoice != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("quiz_check_answer_btn")
                            ) {
                                Text("উত্তর যাচাই করুন 🔒", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.nextQuestion() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("quiz_next_question_btn")
                            ) {
                                Text(
                                    text = if (currentIndex + 1 < totalCount) "পরবর্তী প্রশ্নে যান ➡️" else "ফলাফল ও স্কোর কার্ড দেখুন 📊",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Compact inline tutoring summons (shown if drawing panel isn't open)
                        if (isCurrentChecked && !showTutorDrawerByClick) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showTutorDrawerByClick = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                border = BorderStroke(1.dp, CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("এআই টিউটর থেকে বিস্তারিত ব্যাখ্যা জানুন 💡", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }

                    // Slideable AI Tutor Chat drawer
                    AnimatedVisibility(
                        visible = showTutorDrawerByClick,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ChronosTutorSidebarPanel(
                            messages = tutorMessages,
                            isThinking = tutorThinking,
                            inputText = chatInputFieldText,
                            onInputChange = { chatInputFieldText = it },
                            onSendQuery = { query ->
                                viewModel.askTutorSpecificQuestion(query)
                                chatInputFieldText = ""
                            },
                            onClose = { showTutorDrawerByClick = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun McqOptionButton(
    tag: String,
    optionText: String,
    currentMcq: McqEntity,
    isChecked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Dynamic border color depending on checked correctness & selection
    val borderCol = when {
        isChecked && currentMcq.correctAnswer == tag -> CyberGreen.copy(alpha = 0.7f)
        isChecked && isSelected && currentMcq.correctAnswer != tag -> CyberPink.copy(alpha = 0.7f)
        isSelected -> CyberCyan.copy(alpha = 0.5f)
        else -> Color(0x1AFFFFFF) // white/10 subtle border
    }

    val containerCol = when {
        isChecked && currentMcq.correctAnswer == tag -> CyberGreen.copy(alpha = 0.08f)
        isChecked && isSelected && currentMcq.correctAnswer != tag -> CyberPink.copy(alpha = 0.08f)
        isSelected -> CyberCyan.copy(alpha = 0.08f) // cyan-500/10
        else -> Color(0x0DFFFFFF) // white/5 neutral fill
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderCol, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = !isChecked) { onClick() }
            .testTag("option_${tag}_card"),
        colors = CardDefaults.cardColors(containerColor = containerCol),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Density custom letter holding container
            val letterBg = when {
                isChecked && currentMcq.correctAnswer == tag -> CyberGreen
                isChecked && isSelected && currentMcq.correctAnswer != tag -> CyberPink
                isSelected -> CyberCyan
                else -> Color(0x0DFFFFFF) // white/5
            }
            val letterBorder = when {
                isChecked && currentMcq.correctAnswer == tag -> CyberGreen
                isChecked && isSelected && currentMcq.correctAnswer != tag -> CyberPink
                isSelected -> CyberCyan
                else -> Color(0x33FFFFFF) // white/20
            }
            val letterTextCol = when {
                isChecked && currentMcq.correctAnswer == tag -> Color.Black
                isChecked && isSelected && currentMcq.correctAnswer != tag -> Color.White
                isSelected -> Color.Black
                else -> Color(0xFF94A3B8) // slate-400
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(letterBg)
                    .border(1.dp, letterBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tag,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = letterTextCol,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = optionText,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text labels for statuses
            val statusLabel = when {
                isChecked && currentMcq.correctAnswer == tag -> "সঠিক"
                isChecked && isSelected && currentMcq.correctAnswer != tag -> "ভুল"
                isSelected -> "নির্বাচিত"
                else -> null
            }
            val labelColor = when {
                isChecked && currentMcq.correctAnswer == tag -> CyberGreen
                isChecked && isSelected && currentMcq.correctAnswer != tag -> CyberPink
                isSelected -> CyberCyan
                else -> Color.Transparent
            }

            if (statusLabel != null) {
                Text(
                    text = statusLabel,
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ChronosTutorSidebarPanel(
    messages: List<TutorChatMessage>,
    isThinking: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendQuery: (String) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to latest responses automatically
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 4.dp, end = 12.dp, bottom = 12.dp)
            .border(width = 1.dp, color = CyberCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 12.dp, bottomEnd = 12.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Sidebar Header (High Density layout style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(CyberPink, RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "অফলাইন এআই টিউটর",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 11.sp,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "জেমিনি এআই লোকাল",
                            color = Color(0xFF64748B), // Slate-500
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp).testTag("close_tutor_icon_btn")
                ) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Hide Drawer", tint = CyberPink)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chat Feed Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        TutorBubbleLayout(message = msg)
                    }

                    if (isThinking) {
                        item {
                            TutorThinkingIndicator()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Console prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("প্রশ্ন সম্পর্কিত ব্যাখ্যা বা অতিরিক্ত তথ্য জিজ্ঞাসা করুন...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    singleLine = true
                )

                IconButton(
                    onClick = { if (inputText.isNotBlank()) onSendQuery(inputText) },
                    modifier = Modifier
                        .background(CyberCyan, RoundedCornerShape(8.dp))
                        .size(44.dp)
                        .testTag("chat_send_btn")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun TutorBubbleLayout(message: TutorChatMessage) {
    val isUser = message.sender == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val alignTxt = if (isUser) TextAlign.End else TextAlign.Start
    val bgCol = if (isUser) CyberCyan.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.3f)
    val textCol = if (isUser) CyberCyan else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Text(
            text = if (isUser) "SCHOLAR" else "CHRONOS",
            fontFamily = FontFamily.Monospace,
            color = if (isUser) CyberCyan.copy(alpha = 0.7f) else CyberPink.copy(alpha = 0.7f),
            fontSize = 8.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgCol)
                .padding(10.dp)
                .widthIn(max = 220.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 11.sp,
                textAlign = alignTxt,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun TutorThinkingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CyberCyan) // pulse accent
        )
        Text(
            text = "উত্তরের ব্যাখ্যা তৈরি হচ্ছে...",
            fontFamily = FontFamily.Monospace,
            color = CyberCyan,
            fontSize = 10.sp
        )
    }
}
