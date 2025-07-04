package com.example.notesfrontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import java.util.*

/**
 * Notes app main activity - provides navigation between note list and note detail/edit screens.
 */
class MainActivity : ComponentActivity() {
    // PUBLIC_INTERFACE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesApp()
        }
    }
}

@Composable
fun NotesApp(notesViewModel: NotesViewModel = viewModel()) {
    val navController = rememberNavController()
    // Use Compose Material3, with custom colors.
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF3F51B5),
            secondary = Color(0xFF7986CB),
            tertiary = Color(0xFFFF4081),
            background = Color.White,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White
        )
    ) {
        NavHost(
            navController = navController,
            startDestination = "notes_list"
        ) {
            composable("notes_list") {
                NotesListScreen(
                    notesViewModel = notesViewModel,
                    onNoteSelected = { noteId -> navController.navigate("note_detail/$noteId") },
                    onAddNote = { navController.navigate("note_edit/-1") }
                )
            }
            composable("note_detail/{noteId}") { backStackEntry ->
                val noteId =
                    backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: -1L
                NoteDetailScreen(
                    noteId = noteId,
                    notesViewModel = notesViewModel,
                    onEditNote = { navController.navigate("note_edit/$noteId") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("note_edit/{noteId}") { backStackEntry ->
                val noteId =
                    backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: -1L
                NoteEditScreen(
                    noteId = noteId,
                    notesViewModel = notesViewModel,
                    onSave = { navController.popBackStack("notes_list", false) },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
