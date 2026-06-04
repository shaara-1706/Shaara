package com.example.ui.navigation

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object SignUp : Screen()
    object HomeDashboard : Screen()
    object NotebookList : Screen()
    data class NotebookDetail(val notebookId: Long) : Screen()
    data class NoteEditor(val noteId: Long, val notebookId: Long) : Screen()
    data class PdfViewer(val noteId: Long) : Screen()
    object Upload : Screen()
    object Search : Screen()
    object SharedNotes : Screen()
    object Profile : Screen()
    object Settings : Screen()
}
