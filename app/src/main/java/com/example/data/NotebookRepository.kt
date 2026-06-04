package com.example.data

import kotlinx.coroutines.flow.Flow

class NotebookRepository(private val dao: NotebookDao) {

    // --- Notebooks ---
    val activeNotebooks: Flow<List<Notebook>> = dao.getActiveNotebooks()
    val archivedNotebooks: Flow<List<Notebook>> = dao.getArchivedNotebooks()
    val favoriteNotebooks: Flow<List<Notebook>> = dao.getFavoriteNotebooks()

    suspend fun getNotebookById(id: Long): Notebook? = dao.getNotebookById(id)

    suspend fun insertNotebook(notebook: Notebook): Long = dao.insertNotebook(notebook)

    suspend fun updateNotebook(notebook: Notebook) = dao.updateNotebook(notebook)

    suspend fun deleteNotebook(notebook: Notebook) = dao.deleteNotebook(notebook)

    // --- Notes ---
    fun getNotesForNotebook(notebookId: Long): Flow<List<Note>> = dao.getNotesForNotebook(notebookId)

    fun getNoteById(id: Long): Flow<Note?> = dao.getNoteById(id)

    suspend fun getNoteByIdDirect(id: Long): Note? = dao.getNoteByIdDirect(id)

    suspend fun insertNote(note: Note): Long = dao.insertNote(note)

    suspend fun updateNote(note: Note) = dao.updateNote(note)

    suspend fun deleteNote(note: Note) = dao.deleteNote(note)

    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes("%$query%")

    // --- Sharing ---
    val sharedNotebooks: Flow<List<SharedNotebook>> = dao.getSharedNotebooks()

    suspend fun insertSharedNotebook(sharedNotebook: SharedNotebook): Long = dao.insertSharedNotebook(sharedNotebook)

    suspend fun deleteSharedNotebook(sharedNotebook: SharedNotebook) = dao.deleteSharedNotebook(sharedNotebook)
}
