package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    // --- Notebook Queries ---
    @Query("SELECT * FROM notebooks WHERE isArchived = 0 ORDER BY isPinned DESC, dateModified DESC")
    fun getActiveNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE isArchived = 1 ORDER BY dateModified DESC")
    fun getArchivedNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE isFavorite = 1 AND isArchived = 0 ORDER BY dateModified DESC")
    fun getFavoriteNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: Long): Notebook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook): Long

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    // --- Note Queries ---
    @Query("SELECT * FROM notes WHERE notebookId = :notebookId ORDER BY dateCreated ASC")
    fun getNotesForNotebook(notebookId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteByIdDirect(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query OR ocrText LIKE :query")
    fun searchNotes(query: String): Flow<List<Note>>

    // --- Sharing Queries ---
    @Query("SELECT * FROM shared_notebooks ORDER BY dateShared DESC")
    fun getSharedNotebooks(): Flow<List<SharedNotebook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedNotebook(sharedNotebook: SharedNotebook): Long

    @Delete
    suspend fun deleteSharedNotebook(sharedNotebook: SharedNotebook)
}
