package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.viewmodel.QuizViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParserScreen(
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val progress by viewModel.parsingProgress.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.parsingConsoleLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: PDF Simulator, 1: Camera/OCR, 2: Paste notes

    // Local inputs
    var subjectNamePdf by remember { mutableStateOf("Matrix Informatics") }
    var subjectNameOcr by remember { mutableStateOf("Mechanical Dynamics") }
    var subjectNameText by remember { mutableStateOf("Neurobiology Core") }
    var bodyTextNote by remember { mutableStateOf("") }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // PDF Selection properties and utility
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    var selectedPdfHash by remember { mutableStateOf<String?>(null) }

    fun getFileName(uri: Uri): String {
        var name = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        if (name.isEmpty()) {
            name = uri.path?.substringAfterLast('/') ?: "document.pdf"
        }
        return name
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            val name = getFileName(uri)
            selectedPdfName = name
            
            // Auto fill subject name based on selected file name
            val cleanName = name.substringBeforeLast(".")
            if (cleanName.isNotEmpty() && (subjectNamePdf.isEmpty() || subjectNamePdf == "Matrix Informatics")) {
                subjectNamePdf = cleanName
            }

            // Calculate SHA-256 hash asynchronously
            coroutineScope.launch {
                try {
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    inputStream?.use { input ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            digest.update(buffer, 0, bytesRead)
                        }
                    }
                    val hashBytes = digest.digest()
                    selectedPdfHash = hashBytes.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    selectedPdfHash = "pdf_hash_" + name.hashCode()
                }
            }
        }
    }

    // Handlers for real gallery pick
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                capturedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        CanvasBgGrid()

        Scaffold(
            topBar = {
                HighDensityHeader(
                    title = "ডকুমেন্ট পার্সার",
                    subtitle = "এইচএসসি ক্যুইজ জেনারেটর",
                    showBackButton = true,
                    backButtonTestTag = "parser_back_btn",
                    customNavigationIcon = Icons.Default.ArrowBack,
                    onBackClick = onNavigateBack
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                // Header details
                Text(
                    text = "সম্পূর্ণ অফলাইনে কুইজ ও এইচএসসি এমসিকিউ স্টাডি ম্যাটেরিয়াল তৈরি করতে নিচের যেকোনো একটি অপশন সিলেক্ট করুন।",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Sub tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SlateSurface,
                    contentColor = CyberCyan,
                    edgePadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("পিডিএফ আপলোড", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("ক্যামেরা ও ওসিআর", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("নোট টেক্সট স্টাডি", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Ingestion Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        // Displaying processing telemetry
                        ParsingLogsActivePanel(progress = progress, consoleLogs = consoleLogs)
                    } else {
                        when (selectedTab) {
                            0 -> PdfParserSubPanel(
                                subjectName = subjectNamePdf,
                                onSubjectNameChange = { subjectNamePdf = it },
                                selectedPdfName = selectedPdfName,
                                onPickPdf = { pdfLauncher.launch("application/pdf") },
                                onEngage = { viewModel.run160MbPdfStreamingParser(subjectNamePdf, selectedPdfHash) }
                            )
                            1 -> CameraOcrSubPanel(
                                subjectName = subjectNameOcr,
                                onSubjectNameChange = { subjectNameOcr = it },
                                bitmap = capturedBitmap,
                                onTriggerCamera = { cameraLauncher.launch(null) },
                                onPickGallery = { galleryLauncher.launch("image/*") },
                                onTriggerMockCameraScan = {
                                    // Generate a clean programmatical placeholder image
                                    val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888).apply {
                                        eraseColor(android.graphics.Color.DKGRAY)
                                    }
                                    capturedBitmap = bitmap
                                    viewModel.parseMcqsFromCapturedImage(bitmap, subjectNameOcr)
                                },
                                onTriggerRealOcr = {
                                    val bmp = capturedBitmap
                                    if (bmp != null) {
                                        viewModel.parseMcqsFromCapturedImage(bmp, subjectNameOcr)
                                    }
                                }
                            )
                            2 -> StudyNotesSubPanel(
                                subjectName = subjectNameText,
                                onSubjectNameChange = { subjectNameText = it },
                                bodyText = bodyTextNote,
                                onBodyTextChange = { bodyTextNote = it },
                                onEngage = {
                                    viewModel.parseMcqsFromRawNotes(subjectNameText, bodyTextNote)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParsingLogsActivePanel(progress: Float, consoleLogs: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(width = 1.dp, color = CyberPink.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // High Density System Progress Monitor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "পার্সিং প্রোগ্রেস: ${(progress * 160).toInt()}MB / 160MB",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberCyan,
                    fontSize = 11.sp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High Density Gradient Progress bar with glow style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(listOf(CyberCyan, CyberPink))
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "কনসোল সিস্টেম রিয়েলটাইম আউটপুট:",
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable real-time terminal diagnostics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true // Keeping latest logs displayed at bottom
                ) {
                    items(consoleLogs.reversed()) { log ->
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            color = if (log.contains("ERROR")) ErrorNeon else if (log.contains("SUCCESS") || log.contains("COMPLETE")) CyberGreen else CyberCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfParserSubPanel(
    subjectName: String,
    onSubjectNameChange: (String) -> Unit,
    selectedPdfName: String?,
    onPickPdf: () -> Unit,
    onEngage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SettingsSystemDaydream,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(56.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "পিডিএফ থেকে এমসিকিউ জেনারেটর",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )

        Text(
            text = "আপনার যেকোনো এইচএসসি প্রশ্ন বা টেক্সটবুক পিডিএফ ফাইল আপলোড করুন। এআই ইঞ্জিন স্বয়ংক্রিয়ভাবে পিডিএফ ফাইল বিশ্লেষণ করে সেখান থেকে এমসিকিউ প্রশ্নসমূহ লোকাল স্কুয়্যার ডাটাবেজে সংরক্ষণ করে রাখবে যাতে আপনি পরবর্তীতে যেকোনো সময় অফলাইনে পরীক্ষা দিতে পারেন।",
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Beautiful Neon PDF Selection Button
        OutlinedButton(
            onClick = onPickPdf,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
            border = BorderStroke(1.5.dp, CyberCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pdf_file_picker_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "পিডিএফ ফাইল সিলেক্ট করুন 📂",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedPdfName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Selected: $selectedPdfName",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ingestion properties inputs
        OutlinedTextField(
            value = subjectName,
            onValueChange = onSubjectNameChange,
            label = { Text("TARGET_SUBJECT_NAME", color = TextSecondary, fontFamily = FontFamily.Monospace) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pdf_subject_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEngage,
            colors = ButtonDefaults.buttonColors(containerColor = if (selectedPdfName != null) CyberPink else CyberCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pdf_engage_btn")
                .border(1.dp, Color.White, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "কুইজ জেনারেট করুন ⚡",
                color = if (selectedPdfName != null) Color.White else Color.Black,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CameraOcrSubPanel(
    subjectName: String,
    onSubjectNameChange: (String) -> Unit,
    bitmap: Bitmap?,
    onTriggerCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onTriggerMockCameraScan: () -> Unit,
    onTriggerRealOcr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap == null) {
            // Camera scanner mockup element
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .border(2.dp, Brush.sweepGradient(listOf(CyberPink, CyberCyan)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Diagonal scan overlays
                CanvasScanOverlay()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = CyberPink,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ছবি তোলার জন্য ক্যামেরা প্রস্তুত রয়েছে",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // Preview captured snapshot image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurface)
                    .border(1.dp, CyberCyan, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Snapshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Holographic scanned scanning bar
                CanvasScanOverlay()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = subjectName,
            onValueChange = onSubjectNameChange,
            label = { Text("সংরক্ষণ করার বিষয়ের নাম লিখুন", color = TextSecondary, fontFamily = FontFamily.Monospace) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberPink,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ocr_subject_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Capture Controls Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onTriggerCamera,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                border = BorderStroke(1.dp, CyberCyan),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("trigger_camera_btn")
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ক্যামেরা", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            OutlinedButton(
                onClick = onPickGallery,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPink),
                border = BorderStroke(1.dp, CyberPink),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("trigger_gallery_btn")
            ) {
                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("গ্যালারি", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger OCR Submit
        if (bitmap != null) {
            Button(
                onClick = onTriggerRealOcr,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ocr_real_engage_btn")
            ) {
                Text("জেমিনি এআই দিয়ে ছবি এনালাইজ করুন", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = onTriggerMockCameraScan,
            colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("camera_ocr_mock_btn")
                .border(1.dp, Color.White, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "ডিমো টেক্সটবুক ক্যুইজ জেনারেট করুন",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CanvasScanOverlay() {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val linePaintColor = CyberCyan.copy(alpha = 0.3f)
        val strokeW = 2.dp.toPx()
        val heightY = size.height

        // Moving scanline effect represented statically
        drawLine(
            color = linePaintColor,
            start = androidx.compose.ui.geometry.Offset(0f, heightY / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, heightY / 2f),
            strokeWidth = strokeW
        )
    }
}

@Composable
fun StudyNotesSubPanel(
    subjectName: String,
    onSubjectNameChange: (String) -> Unit,
    bodyText: String,
    onBodyTextChange: (String) -> Unit,
    onEngage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "এইচএসসি কুইজ নোট পেস্ট করুন",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = subjectName,
            onValueChange = onSubjectNameChange,
            label = { Text("বিষয়ের নাম (যেমন: রসায়ন ২য় পত্র)", color = TextSecondary, fontFamily = FontFamily.Monospace) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_subject_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bodyText,
            onValueChange = onBodyTextChange,
            label = { Text("আপনার নোটসমূহ এখানে পেস্ট করুন...", color = TextSecondary, fontFamily = FontFamily.Monospace) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("notes_body_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEngage,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("notes_engage_btn")
                .border(1.dp, Color.White, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "বিশ্লেষণ ও কুইজ তৈরি করুন ⚡",
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
