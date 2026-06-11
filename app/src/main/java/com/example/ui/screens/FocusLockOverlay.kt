package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusGuardStatusEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FocusLockOverlay(
    status: FocusGuardStatusEntity,
    onLaunchQuickQuiz: () -> Unit,
    onBypassStatus: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalProgress = status.dailyTarget
    val correct = status.correctCount
    val remaining = (totalProgress - correct).coerceAtLeast(0)
    val progressFraction = if (totalProgress > 0) correct.toFloat() / totalProgress.toFloat() else 1f

    // Animated breathing glow for cyberpunk feel
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    var selectedSocialBlocked by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .padding(24.dp)
            .testTag("focus_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            // Cyberpunk Shield with Glowing Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        drawCircle(
                            color = CyberPink.copy(alpha = glowAlpha * 0.15f),
                            radius = size.minDimension / 1.6f
                        )
                    }
                    .border(2.dp, CyberPink, RoundedCornerShape(24.dp))
                    .background(SlateSurface, RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "System Locked",
                    tint = CyberPink,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Neon Glowing Headers
            Text(
                text = "ফোকাসগার্ড // সিকিউর_লক_সক্রিয়",
                color = CyberPink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "এইচএসসি স্টাডি লক সক্রিয় রয়েছে",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "অ্যাকাডেমিক ফায়ারওয়াল আজ আপনার মনোযোগ ধরে রাখছে। সকল সামাজিক যোগাযোগ মাধ্যম ও বিনোদন অ্যাপস সাময়িকভাবে ব্লক করা হয়েছে।",
                color = Color(0xFF94A3B8), // slate-400
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glowing Progress Box
            Surface(
                color = SlateSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ফায়ারওয়াল আনলক প্রোগ্রেস",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$correct / $totalProgress টি এমসিকিউ",
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated neon progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CyberPink, CyberCyan)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (remaining > 0) {
                            "আনলক করতে আরও $remaining টি এইচএসসি এমসিকিউ প্রশ্নের সঠিক উত্তর দিন!"
                        } else {
                            "সিস্টেম ডিক্রিপ্ট করা হয়েছে। আপনার সেশনটি আনলকড!"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onLaunchQuickQuiz,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("launch_lock_quiz_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "এইচএসসি বিজ্ঞান এমসিকিউ সমাধান করুন",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Simulated Social App Paths to show intercept in real time
            Text(
                text = "সোশ্যাল মিডিয়া অ্যাক্সেস টেস্ট (সিমুলেশন)",
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B), // Slate-500
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("TikTok", Icons.Default.MusicNote),
                    Pair("Instagram", Icons.Default.CameraAlt),
                    Pair("Facebook", Icons.Default.Public)
                ).forEach { (name, icon) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .background(LightSlateSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(10.dp))
                            .clickable {
                                selectedSocialBlocked = name
                                scope.launch {
                                    delay(2500)
                                    selectedSocialBlocked = null
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Animated message if user tries to open restricted path
            AnimatedVisibility(
                visible = selectedSocialBlocked != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    color = CyberPink.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, CyberPink),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 ব্লকেড: FocusGuard দ্বারা $selectedSocialBlocked-এর অ্যাক্সেস সফলভাবে ব্লক করা হয়েছে। সাধারণ সেশন চালু করতে আপনার অবশিষ্ট এমসিকিউগুলোর উত্তর দিন!",
                        color = CyberPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Presentation Bypass Tool for the Examiner / Evaluator
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛠️ প্রেজেন্টেশন / মূল্যায়নকারী বাইপাস প্যানেল",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onBypassStatus(19) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, CyberPink),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("১৯/২০ সেট করুন", color = CyberPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onBypassStatus(20) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, CyberGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("আনলক (২০/২০)", color = CyberGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
