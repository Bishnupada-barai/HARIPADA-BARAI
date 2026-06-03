package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.NoteDatabase
import com.example.data.NoteRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.NoteViewModel
import com.example.ui.NoteViewModelFactory
import com.example.ui.Screen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NoteFormScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Let's initialize offline SQLite Room dependency instances
        val database = NoteDatabase.getDatabase(applicationContext)
        val repository = NoteRepository(database.noteDao())
        
        // Initialize our ViewModel with constructor injection
        val viewModel: NoteViewModel by viewModels {
            NoteViewModelFactory(repository)
        }

        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val recentNotes by viewModel.recentNotes.collectAsState()

            // Dynamic light/dark theme supporting manual overrides
            val isDarkTheme = when (uiState.isDarkThemeOverride) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Main screen animation deck with transition gestures
                AnimatedContent(
                    targetState = uiState.currentScreen,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "screen_navigation_animator",
                    modifier = Modifier.fillMaxSize()
                ) { currentScreen ->
                    when (currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                recentNotes = recentNotes,
                                onNoteClick = { noteId ->
                                    viewModel.navigateTo(Screen.NoteDetail(noteId))
                                },
                                onAddNoteClick = {
                                    viewModel.navigateTo(Screen.AddNote)
                                }
                            )
                        }
                        is Screen.AddNote -> {
                            NoteFormScreen(
                                noteId = null,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.Home) }
                            )
                        }
                        is Screen.EditNote -> {
                            NoteFormScreen(
                                noteId = currentScreen.noteId,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.NoteDetail(currentScreen.noteId)) }
                            )
                        }
                        is Screen.NoteDetail -> {
                            NoteDetailScreen(
                                noteId = currentScreen.noteId,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Screen.Home) },
                                onEdit = { noteId ->
                                    viewModel.navigateTo(Screen.EditNote(noteId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
