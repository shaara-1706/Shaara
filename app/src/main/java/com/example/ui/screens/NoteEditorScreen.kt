package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.ShaaraViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: ShaaraViewModel,
    noteId: Long,
    notebookId: Long,
    modifier: Modifier = Modifier
) {
    val note = viewModel.activeNote

    var titleText by remember { mutableStateOf("") }
    var contentBodyText by remember { mutableStateOf("") }
    var isDrawingMode by remember { mutableStateOf(false) }
    var activeMarkerColor by remember { mutableStateOf(Color(0xFFD4A31C)) } // Default gold
    
    // Canvas handwriting points representation
    val drawingStrokes = remember { mutableStateListOf<StrokeLine>() }
    var currentStrokePoints = remember { mutableStateListOf<Offset>() }
    var activePenColor by remember { mutableStateOf(Color(0xFF2C5E43)) } // Sage Green Pen
    var activePenSize by remember { mutableStateOf(5f) }

    // Stickies state
    val stickyNotes = remember { mutableStateListOf<StickyNote>() }
    var showAddStickyDialog by remember { mutableStateOf(false) }
    var stickyText by remember { mutableStateOf("") }

    // Assistant Side-Drawer sheet
    var showAIAssistantSheet by remember { mutableStateOf(false) }
    var activeAssistantTab by remember { mutableStateOf("SUMMARY") } // SUMMARY, QUIZ, FLASHCARDS

    LaunchedEffect(noteId) {
        viewModel.loadNoteEditor(noteId)
    }

    // Capture initial values once note state syncs
    LaunchedEffect(note) {
        if (note != null && titleText.isEmpty()) {
            titleText = note.title
            contentBodyText = note.content.replace("<[^>]*>".toRegex(), "") // Strip html for raw edit simplicity
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Study Pad Editor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Save states back to database on exit
                        if (note != null) {
                            viewModel.updateNoteContentAndTitle(
                                noteId = note.id,
                                newTitle = titleText,
                                newContent = "<p>${contentBodyText}</p>"
                            )
                        }
                        viewModel.navigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Bookmark toggle
                    IconButton(onClick = { note?.let { viewModel.toggleBookmarkNote(it) } }) {
                        Icon(
                            imageVector = if (note?.isBookmarked == true) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (note?.isBookmarked == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    // Toggle handwriting canvas mode
                    IconButton(onClick = { isDrawingMode = !isDrawingMode }) {
                        Icon(
                            imageVector = if (isDrawingMode) Icons.Default.Gesture else Icons.Default.Edit,
                            contentDescription = "Handwriting pencil Mode",
                            tint = if (isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Open stickies placement screen
                    IconButton(onClick = { showAddStickyDialog = true }) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Place Sticky comments", tint = MaterialTheme.colorScheme.onBackground)
                    }

                    // Share option dialog shortcut
                    IconButton(onClick = { showAIAssistantSheet = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Study panel trigger", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (note == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Base Editor Page View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    
                    // Lecture Chapter tracker label
                    if (note.chapter.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.SnippetFolder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(
                                note.chapter.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Note Page editable title
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = { Text("Note Title", style = MaterialTheme.typography.displayMedium) },
                        textStyle = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional OCR extraction segment review (if extracted from physical page scanners!)
                    if (note.ocrText != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scanned", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Text(
                                        "ORIGINAL SCANNED BOOK PAGE RECORD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                ExpandableOcrSection(text = note.ocrText)
                            }
                        }
                    }

                    // Text Annotator helper highlight row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Add Markers:", style = MaterialTheme.typography.labelMedium)
                        listOf(
                            Color(0x99FFE082), // Yellow outline
                            Color(0x99A5D6A7), // Green marker
                            Color(0x9990CAF9), // Blue marker
                            Color(0x99F48FB1)  // Pink marker
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { activeMarkerColor = color }
                                    .border(
                                        width = if (activeMarkerColor == color) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Big Text Body editor block
                    OutlinedTextField(
                        value = contentBodyText,
                        onValueChange = { contentBodyText = it },
                        placeholder = { Text("Write down key class takeaways, formulas or outlines...", style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .background(activeMarkerColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Drag-placed Sticky Cards
                stickyNotes.forEach { sticky ->
                    StickyNoteCard(
                        sticky = sticky,
                        onDelete = { stickyNotes.remove(sticky) }
                    )
                }

                // Interactive Overlaid Drawing Canvas layer (Active when pencil icon is toggled!)
                if (isDrawingMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStrokePoints.clear()
                                            currentStrokePoints.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentStrokePoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                drawingStrokes.add(
                                                    StrokeLine(
                                                        points = currentStrokePoints.toList(),
                                                        color = activePenColor,
                                                        strokeWidth = activePenSize
                                                    )
                                                )
                                                currentStrokePoints.clear()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Render old strokes
                            drawingStrokes.forEach { stroke ->
                                val path = Path().apply {
                                    if (stroke.points.isNotEmpty()) {
                                        moveTo(stroke.points.first().x, stroke.points.first().y)
                                        for (i in 1 until stroke.points.size) {
                                            lineTo(stroke.points[i].x, stroke.points[i].y)
                                        }
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = stroke.color,
                                    style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            // Render current active stroke
                            if (currentStrokePoints.isNotEmpty()) {
                                val path = Path().apply {
                                    moveTo(currentStrokePoints.first().x, currentStrokePoints.first().y)
                                    for (i in 1 until currentStrokePoints.size) {
                                        lineTo(currentStrokePoints[i].x, currentStrokePoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = activePenColor,
                                    style = Stroke(width = activePenSize, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Drawing controls footer menu
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pen colors
                                    listOf(
                                        Color(0xFF2C5E43), // Deep Green
                                        Color(0xFFD4A31C), // Gold
                                        Color(0xFFD32F2F), // Red mark
                                        Color(0xFF1976D2)  // Blue ink
                                    ).forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .clickable { activePenColor = color }
                                                .border(
                                                    width = if (activePenColor == color) 2.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TextButton(onClick = { drawingStrokes.clear() }) {
                                        Text("Clear Canvas", color = Color.Red.copy(alpha = 0.8f))
                                    }
                                    Button(
                                        onClick = { isDrawingMode = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Save Sketch")
                                    }
                                }
                            }
                        }
                    }
                }

                // Smart AI Study Assistant Overlaid bottom sheet
                if (showAIAssistantSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAIAssistantSheet = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Selector tabs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "SUMMARY" to "AI Summary",
                                    "QUIZ" to "Revision Quiz",
                                    "FLASHCARDS" to "Flashcards"
                                ).forEach { (tab, label) ->
                                    Button(
                                        onClick = { activeAssistantTab = tab },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activeAssistantTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                            contentColor = if (activeAssistantTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }

                            // Dynamic panel loading states
                            if (viewModel.aiLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Shaara AI is synthesizing content...", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                when (activeAssistantTab) {
                                    "SUMMARY" -> {
                                        // Summary block
                                        val summaryStr = note.summary ?: viewModel.aiResultBody
                                        if (summaryStr.isEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Let AI condense your school text into quick reminders:")
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Button(
                                                    onClick = { viewModel.actionAISummarize(note) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Spark", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Synthesize Notes")
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    "Primary takeaways summary:", 
                                                    style = MaterialTheme.typography.titleSmall, 
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    summaryStr,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                OutlinedButton(onClick = { viewModel.actionAISummarize(note) }) {
                                                    Text("Regenerate Summary")
                                                }
                                            }
                                        }
                                    }
                                    
                                    "QUIZ" -> {
                                        // Quiz Block
                                        if (viewModel.parsedQuizQuestions.isEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Build a custom revision test pool instantly:")
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Button(
                                                    onClick = { viewModel.actionAIGenerateQuiz(note) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.Task, contentDescription = "Build quiz", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Draft Revision Quiz")
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("Revision Quiz Mode:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                                
                                                var activeQuizIdx by remember { mutableStateOf(0) }
                                                var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
                                                
                                                val q = viewModel.parsedQuizQuestions.getOrNull(activeQuizIdx)
                                                if (q != null) {
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                                                    ) {
                                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            Text("Q${activeQuizIdx+1}: ${q.question}", style = MaterialTheme.typography.titleSmall)
                                                            
                                                            q.options.forEachIndexed { idx, opt ->
                                                                val isSelected = selectedOptionIdx == idx
                                                                val isCorrect = idx == q.correctIndex
                                                                
                                                                val bgColor = when {
                                                                    selectedOptionIdx == null -> MaterialTheme.colorScheme.surface
                                                                    isSelected && isCorrect -> Color(0xFFC8E6C9) // Clean Green
                                                                    isSelected && !isCorrect -> Color(0xFFFFCDD2) // Red Alert
                                                                    isCorrect -> Color(0xFFC8E6C9)
                                                                    else -> MaterialTheme.colorScheme.surface
                                                                }

                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(bgColor)
                                                                        .clickable { selectedOptionIdx = idx }
                                                                        .padding(12.dp)
                                                                ) {
                                                                    Text(opt, style = MaterialTheme.typography.bodyMedium)
                                                                }
                                                            }

                                                            if (selectedOptionIdx != null) {
                                                                Text("Explanation: ${q.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                                            }
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        TextButton(onClick = {
                                                            selectedOptionIdx = null
                                                            activeQuizIdx = (activeQuizIdx + 1) % viewModel.parsedQuizQuestions.size
                                                        }) {
                                                            Text("Next Question →")
                                                        }
                                                        TextButton(onClick = { viewModel.actionAIGenerateQuiz(note) }) {
                                                            Text("Regenerate Quiz")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    "FLASHCARDS" -> {
                                        // Flashcard block
                                        if (viewModel.parsedFlashcards.isEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Build active-recall card deck to memorize definitions:")
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Button(
                                                    onClick = { viewModel.actionAIGenerateFlashcards(note) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.Style, contentDescription = "Cards", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Compile Flashcards")
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("Active Recall Flashcards:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                                
                                                var activeCardIdx by remember { mutableStateOf(0) }
                                                var cardFaceFlipped by remember { mutableStateOf(false) }
                                                
                                                val c = viewModel.parsedFlashcards.getOrNull(activeCardIdx)
                                                if (c != null) {
                                                    // Beautiful Flipped Card
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(140.dp)
                                                            .clickable { cardFaceFlipped = !cardFaceFlipped },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (cardFaceFlipped) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                        )
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                                                Text(
                                                                    text = if (cardFaceFlipped) "BACK (DEFINITION)" else "FRONT (TERM)",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text(
                                                                    text = if (cardFaceFlipped) c.back else c.front,
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        TextButton(onClick = {
                                                            cardFaceFlipped = false
                                                            activeCardIdx = (activeCardIdx + 1) % viewModel.parsedFlashcards.size
                                                        }) {
                                                            Text("Next Card →")
                                                        }
                                                        TextButton(onClick = { viewModel.actionAIGenerateFlashcards(note) }) {
                                                            Text("Recompile Deck")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sticky placement pop dialog
    if (showAddStickyDialog) {
        AlertDialog(
            onDismissRequest = { showAddStickyDialog = false },
            title = { Text("New Sticky Note Comment") },
            text = {
                OutlinedTextField(
                    value = stickyText,
                    onValueChange = { stickyText = it },
                    label = { Text("Comment/Observation") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (stickyText.isNotEmpty()) {
                            stickyNotes.add(
                                StickyNote(
                                    text = stickyText,
                                    offsetX = (100..400).random().toFloat(),
                                    offsetY = (200..600).random().toFloat()
                                )
                            )
                            stickyText = ""
                            showAddStickyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Place card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStickyDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}

/**
 * Expandable original scanned doc extraction block
 */
@Composable
fun ExpandableOcrSection(text: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = if (expanded) 20 else 2,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                if (expanded) "Collapse scanned view" else "View whole scanned page OCR", 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Beautiful drag-and-place Sticky card
 */
@Composable
fun StickyNoteCard(
    sticky: StickyNote,
    onDelete: () -> Unit
) {
    var offset by remember { mutableStateOf(Offset(sticky.offsetX, sticky.offsetY)) }

    Card(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            }
            .width(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)), // Classic yellow sticky post-it!
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = Color.Red, modifier = Modifier.size(12.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close sticky", tint = Color.Gray, modifier = Modifier.size(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                sticky.text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.Black),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helpers
data class StrokeLine(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class StickyNote(
    val text: String,
    val offsetX: Float,
    val offsetY: Float
)
