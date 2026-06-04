package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.ShaaraTheme
import com.example.ui.viewmodel.ShaaraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge enables content graphics to flow beautifully behind the taskbar
        enableEdgeToEdge()
        
        // Obtain our single-source-of-truth Shaara state manager
        val viewModel: ShaaraViewModel by viewModels()

        setContent {
            ShaaraTheme(darkTheme = viewModel.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ShaaraAppRouter(viewModel = viewModel, activity = this)
                }
            }
        }
    }
}

@Composable
fun ShaaraAppRouter(
    viewModel: ShaaraViewModel,
    activity: ComponentActivity
) {
    // Intercept native physical/gesture Android Back button clicks
    val backEnabled = viewModel.currentScreen !is Screen.Splash && viewModel.currentScreen !is Screen.Login
    BackHandler(enabled = backEnabled) {
        val wentBack = viewModel.navigateBack()
        if (!wentBack) {
            activity.finish() // Safely exit app if backstack is completed
        }
    }

    // Dynamic screen routing with animated transitions
    Crossfade(
        targetState = viewModel.currentScreen,
        animationSpec = tween(350),
        label = "ScreenCrossfade"
    ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(viewModel = viewModel)
            }
            is Screen.Login -> {
                LoginScreen(viewModel = viewModel)
            }
            is Screen.SignUp -> {
                SignUpScreen(viewModel = viewModel)
            }
            is Screen.HomeDashboard -> {
                HomeDashboardScreen(viewModel = viewModel)
            }
            is Screen.NotebookList -> {
                NotebookListScreen(viewModel = viewModel)
            }
            is Screen.NotebookDetail -> {
                NotebookDetailScreen(viewModel = viewModel, notebookId = screen.notebookId)
            }
            is Screen.NoteEditor -> {
                NoteEditorScreen(viewModel = viewModel, noteId = screen.noteId, notebookId = screen.notebookId)
            }
            is Screen.PdfViewer -> {
                PdfViewerScreen(viewModel = viewModel, noteId = screen.noteId)
            }
            is Screen.Upload -> {
                UploadScreen(viewModel = viewModel)
            }
            is Screen.Search -> {
                SearchScreen(viewModel = viewModel)
            }
            is Screen.SharedNotes -> {
                SharedNotesScreen(viewModel = viewModel)
            }
            is Screen.Profile -> {
                ProfileScreen(viewModel = viewModel)
            }
            is Screen.Settings -> {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
