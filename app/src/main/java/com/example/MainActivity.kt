package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ParserScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.FocusLockOverlay
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.QuizViewModel
import com.example.viewmodel.QuizViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: QuizViewModel = viewModel(
                    factory = QuizViewModelFactory(application)
                )

                val statusState by viewModel.focusGuardStatus.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToParser = { navController.navigate("parser") },
                                onStartQuiz = { subject ->
                                    viewModel.startQuizSession(subject)
                                    navController.navigate("quiz")
                                }
                            )
                        }
                        composable("parser") {
                            ParserScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("quiz") {
                            QuizScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Cyberpunk Study Guard Enforcer Intervention
                    statusState?.let { currentStatus ->
                        if (currentStatus.isLocked) {
                            FocusLockOverlay(
                                status = currentStatus,
                                onLaunchQuickQuiz = {
                                    viewModel.startQuickQuiz {
                                        navController.navigate("quiz")
                                    }
                                },
                                onBypassStatus = { count ->
                                    viewModel.setMockCorrectCount(count)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
