package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Notebook::class, Note::class, SharedNotebook::class], version = 1, exportSchema = false)
abstract class NotebookDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao

    companion object {
        @Volatile
        private var INSTANCE: NotebookDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NotebookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotebookDatabase::class.java,
                    "shaara_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDb(database.notebookDao())
                }
            }
        }

        suspend fun populateDb(dao: NotebookDao) {
            // Seed a "Shaara Onboarding" notebook with gold highlight style
            val onboardId = dao.insertNotebook(
                Notebook(
                    name = "Shaara Quick Guide 🌿",
                    subject = "Orientation",
                    isPinned = true,
                    isFavorite = true,
                    colorHex = "#2C5E43" // Deep Leaf Sage Green
                )
            )

            dao.insertNote(
                Note(
                    notebookId = onboardId,
                    title = "Welcome & OCR scanner introduction 🔎",
                    chapter = "Chapter 1",
                    content = """
                        <h3>Welcome to Shaara! 🎉</h3>
                        <p>Shaara is your student-focused organic digital study companion. Designed cleanly for high school and college students, it allows you to store, draw, capture pages, and structure folders beautifully.</p>
                        
                        <h4>1. Image & Document Scan OCR</h4>
                        <p>Have physical handouts or textbook pages? Simply tap <b>Upload Document</b> on the Home Dashboard. Upload a JPEG or camera photograph, and Shaara's integrated AI engine will extract the entire page formatted correctly into digital notes with searchable text!</p>
                        
                        <h4>2. Interactive PDF Viewer</h4>
                        <p>Upload lecture PDFs, review them, and place sticky notes with questions, answers, and bookmarks directly onto the layout so that you never forget high-priority exam formulas.</p>
                    """.trimIndent(),
                    ocrText = "Shaara notebook digital pdf camera capture book page scanner OCR integration study assistant",
                    stickyNotesJson = """[{"x":150,"y":200,"text":"Try making your first note by clicking the green button!"}]""",
                    isBookmarked = true
                )
            )

            dao.insertNote(
                Note(
                    notebookId = onboardId,
                    title = "AI Study Assistant & Smart Tools ✨",
                    chapter = "Chapter 2",
                    content = """
                        <h3>Futuristic Study Tools on Shaara</h3>
                        <p>With our built-in <b>OpenAI / Gemini AI Study Assistant</b>, you can digest complex homework topics in three powerful modes:</p>
                        <ul>
                            <li><b>Summarizer:</b> Turn dense 10-page chapters into an active primary digest instantly.</li>
                            <li><b>Quiz Creator:</b> Test yourself using automatically formed multiple-choice revision test banks.</li>
                            <li><b>Interactive Flashcards:</b> Keep active recall sharp by studying simulated card decks of difficult definitions.</li>
                        </ul>
                    """.trimIndent(),
                    ocrText = "Gemini revision tools smart flashcards quiz summarization smart study aids and schedules",
                    stickyNotesJson = """[]"""
                )
            )

            // Seed Biology Notebook
            val bioId = dao.insertNotebook(
                Notebook(
                    name = "Apical Genetics & Botany 🧬",
                    subject = "Biology",
                    isPinned = false,
                    isFavorite = true,
                    colorHex = "#D4A31C" // Warm Pollen Amber Gold
                )
            )

            dao.insertNote(
                Note(
                    notebookId = bioId,
                    title = "Plant Metabolism & Aerobes",
                    chapter = "Chapter 4 — Metabolism",
                    content = """
                        <h3>Mitochondria vs. Chloroplastic ATP Synthesase</h3>
                        <p>During respiration, hydrogen protons assemble along the intermembrane boundary, falling down ATP-Synthase channels. In plants, chloroplast thylakoids trigger photochemical water-splitting to construct active coenzymes.</p>
                        <p>Key terms: Aerobic oxidization, Photolysis, NADH generation.</p>
                    """.trimIndent(),
                    ocrText = "Chloroplast mitochondria atp synthesase ap biology bio notes textbook cell respiration photosynthesis",
                    stickyNotesJson = """[{"x":200,"y":350,"text":"Review for exam on Friday!"}]"""
                )
            )

            // Seed a Shared Notebook row
            dao.insertSharedNotebook(
                SharedNotebook(
                    notebookId = bioId,
                    notebookName = "Apical Genetics & Botany 🧬",
                    subject = "Biology",
                    sharedWith = "adisha9260@gmail.com",
                    role = "READ_ONLY",
                    code = "shaara-share-apbio-8822"
                )
            )
        }
    }
}
