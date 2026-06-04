package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subject: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val colorHex: String = "#2C5E43" // Default deep sage green
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notebookId: Long,
    val title: String,
    val chapter: String = "",
    val content: String,
    val summary: String? = null,
    val quizJson: String? = null,      // List of questions/options/answers
    val flashcardsJson: String? = null,// Front/Back flashcards
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val imageAttachmentUri: String? = null, // URI of PDF/book camera image
    val ocrText: String? = null,       // OCR text extracted
    val stickyNotesJson: String? = null, // sticky notes stored as JSON array of coords/text
    val drawingsJson: String? = null,    // stroke coordinates as JSON
    val isBookmarked: Boolean = false,
    val hasOriginalFormatting: Boolean = true
)

@Entity(tableName = "shared_notebooks")
data class SharedNotebook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notebookId: Long,
    val notebookName: String,
    val subject: String,
    val sharedWith: String,       // Selected target user / email
    val role: String = "READ_ONLY", // COLLABORATOR or READ_ONLY
    val sharedBy: String = "user@shaara.edu",
    val dateShared: Long = System.currentTimeMillis(),
    val code: String = ""         // Public share code link
)
