package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge support
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Secure Exam Screen: Prevent Screenshots automatically during active test countdown
        lifecycleScope.launch {
            viewModel.isExamActive.collectLatest { active ->
                if (active) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            MyApplicationTheme {
                AppNavigationHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppNavigationHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.fillMaxSize()
    ) {
        // HOME/DASHBOARD SCREEN
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToExam = { navController.navigate("exam") },
                onNavigateToBookmarks = { navController.navigate("bookmarks") },
                onNavigateToNotes = { navController.navigate("notes") },
                onNavigateToAdmin = { navController.navigate("admin") }
            )
        }

        // REAL CBT EXAM INTERFACE SCREEN
        composable("exam") {
            ExamScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToResult = {
                    navController.navigate("result") {
                        popUpTo("dashboard") // Clean intermediate transitions
                    }
                }
            )
        }

        // CBT EVALUATION & AUTO REPORT CARD SCREEN
        composable("result") {
            ResultScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        // BOOKMARKS REVIEW DIRECTORY SCREEN
        composable("bookmarks") {
            BookmarksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // ELECTRICAL FORMULAS & CANVAS DRAWING SCREEN
        composable("notes") {
            NotesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // ADMIN CONTROLLER PANEL SCREEN
        composable("admin") {
            AdminScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
