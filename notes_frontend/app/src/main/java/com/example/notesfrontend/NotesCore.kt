package com.example.notesfrontend

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Note model.
 * @property id Unique note ID (timestamp).
 * @property title Note title.
 * @property content Note content.
 */
data class Note(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val content: String
)

/**
 * Interface for notes data storage.
 */
interface NotesRepository {
    // PUBLIC_INTERFACE
    fun getAllNotes(): List<Note>
    fun getNote(id: Long): Note?
    fun addNote(note: Note)
    fun updateNote(note: Note)
    fun deleteNote(id: Long)
}

/**
 * Local note storage using SharedPreferences and Gson serialization.
 */
class SharedPrefsNotesRepository(context: Context) : NotesRepository {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notes_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY = "notes"

    private fun loadNotes(): MutableList<Note> {
        val json = prefs.getString(KEY, "[]")
        val type = object : TypeToken<List<Note>>() {}.type
        return gson.fromJson<List<Note>>(json, type)?.toMutableList() ?: mutableListOf()
    }

    private fun saveNotes(notes: List<Note>) {
        prefs.edit().putString(KEY, gson.toJson(notes)).apply()
    }

    // PUBLIC_INTERFACE
    override fun getAllNotes(): List<Note> = loadNotes().sortedByDescending { it.id }

    // PUBLIC_INTERFACE
    override fun getNote(id: Long): Note? = loadNotes().find { it.id == id }

    // PUBLIC_INTERFACE
    override fun addNote(note: Note) {
        val notes = loadNotes()
        notes.add(note)
        saveNotes(notes)
    }

    // PUBLIC_INTERFACE
    override fun updateNote(note: Note) {
        val notes = loadNotes()
        val idx = notes.indexOfFirst { it.id == note.id }
        if (idx != -1) {
            notes[idx] = note
            saveNotes(notes)
        }
    }

    // PUBLIC_INTERFACE
    override fun deleteNote(id: Long) {
        val notes = loadNotes()
        saveNotes(notes.filterNot { it.id == id })
    }
}

/**
 * ViewModel serving as the main app-level state holder for notes.
 */
class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: NotesRepository = SharedPrefsNotesRepository(application.applicationContext)

    // Backing state
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> = _notes

    private val _lastUpdate = mutableStateOf(System.currentTimeMillis())

    init {
        refreshNotes()
    }

    // PUBLIC_INTERFACE
    fun refreshNotes() {
        _notes.clear()
        _notes.addAll(repo.getAllNotes())
        _lastUpdate.value = System.currentTimeMillis()
    }

    // PUBLIC_INTERFACE
    fun getNote(id: Long): Note? = repo.getNote(id)

    // PUBLIC_INTERFACE
    fun addNote(note: Note) {
        repo.addNote(note)
        refreshNotes()
    }

    // PUBLIC_INTERFACE
    fun updateNote(note: Note) {
        repo.updateNote(note)
        refreshNotes()
    }

    // PUBLIC_INTERFACE
    fun deleteNote(id: Long) {
        repo.deleteNote(id)
        refreshNotes()
    }
}

// ------------------------------------------------------
// Minimal Compose UI
// ------------------------------------------------------

/**
 * Main screen: AppBar + notes list + FAB.
 */
@Composable
fun NotesListScreen(
    notesViewModel: NotesViewModel,
    onNoteSelected: (Long) -> Unit,
    onAddNote: () -> Unit
) {
    val notes = notesViewModel.notes
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_header)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3F51B5),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = Color(0xFFFF4081)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note), tint = Color.White)
            }
        },
        content = { padding ->
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
            ) {
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.note_empty_list),
                            color = Color(0xFF7986CB)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(horizontal = 8.dp)
                    ) {
                        items(notes) { note ->
                            NoteListItem(
                                note = note,
                                onClick = { onNoteSelected(note.id) }
                            )
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun NoteListItem(note: Note, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(
            text = note.title,
            color = Color(0xFF3F51B5),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = note.content.lines().firstOrNull()?.let {
                if (it.length > 48) it.take(48) + "…" else it
            } ?: "",
            color = Color(0xFF777777),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

/**
 * Detail screen: view a note and afford to edit/delete/back.
 */
@Composable
fun NoteDetailScreen(
    noteId: Long,
    notesViewModel: NotesViewModel,
    onEditNote: () -> Unit,
    onBack: () -> Unit
) {
    val note = notesViewModel.getNote(noteId)
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(note?.title ?: "", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onEditNote) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF3F51B5)
                )
            )
        },
        floatingActionButton = {
            if (note != null) {
                FloatingActionButton(
                    onClick = {
                        notesViewModel.deleteNote(note.id)
                        onBack()
                    },
                    containerColor = Color(0xFFFF4081)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                }
            }
        },
        content = { padding ->
            if (note == null) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
                ) {
                    Text(text = "Note not found.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(16.dp)
                        .padding(padding)
                ) {
                    Text(text = note.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF3F51B5))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = note.content, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF222222))
                }
            }
        }
    )
}

/**
 * Edit/Create screen for a note.
 */
@Composable
fun NoteEditScreen(
    noteId: Long,
    notesViewModel: NotesViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val isNew = noteId < 0
    val originNote = if (!isNew) notesViewModel.getNote(noteId) else null
    var title by remember { mutableStateOf(TextFieldValue(originNote?.title ?: "")) }
    var content by remember { mutableStateOf(TextFieldValue(originNote?.content ?: "")) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(if (isNew) stringResource(R.string.new_note) else stringResource(R.string.edit_note), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF3F51B5)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val trimmedTitle = title.text.trim()
                    val trimmedContent = content.text.trim()
                    if (trimmedTitle.isNotEmpty() || trimmedContent.isNotEmpty()) {
                        if (isNew) {
                            notesViewModel.addNote(
                                Note(
                                    title = trimmedTitle.ifBlank { "Untitled" },
                                    content = trimmedContent
                                )
                            )
                        } else if (originNote != null) {
                            notesViewModel.updateNote(
                                originNote.copy(
                                    title = trimmedTitle.ifBlank { "Untitled" },
                                    content = trimmedContent
                                )
                            )
                        }
                        onSave()
                    }
                },
                containerColor = Color(0xFFFF4081)
            ) {
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save), tint = Color.White)
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp)
                    .padding(padding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.note_title_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3F51B5),
                        unfocusedBorderColor = Color(0xFF7986CB)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.note_content_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    maxLines = 16,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3F51B5),
                        unfocusedBorderColor = Color(0xFF7986CB)
                    )
                )
            }
        }
    )
}
