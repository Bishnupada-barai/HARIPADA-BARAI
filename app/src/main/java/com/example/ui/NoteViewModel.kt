package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Note
import com.example.data.NoteDraft
import com.example.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object AddNote : Screen
    data class EditNote(val noteId: Int) : Screen
    data class NoteDetail(val noteId: Int) : Screen
}

enum class SortMode {
    UPDATED_DESC, // Newest Modified first
    UPDATED_ASC,  // Oldest Modified first
    CREATED_DESC, // Newest Created first
    TITLE_ASC     // Alphabetical
}

data class NoteUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val selectedSubject: String = "All", // "All" or a subject value
    val sortMode: SortMode = SortMode.UPDATED_DESC,
    val showFavoritesOnly: Boolean = false,
    val currentScreen: Screen = Screen.Home,
    val isDarkThemeOverride: Boolean? = null // null means system default
)

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    // Main states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSubject = MutableStateFlow("All")
    val selectedSubject = _selectedSubject.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    val sortMode = _sortMode.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly = _showFavoritesOnly.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen = _currentScreen.asStateFlow()

    private val _isDarkThemeOverride = MutableStateFlow<Boolean?>(null)
    val isDarkThemeOverride = _isDarkThemeOverride.asStateFlow()

    // Form inputs & draft state
    val noteTitleInput = MutableStateFlow("")
    val noteSubjectInput = MutableStateFlow("General")
    val noteContentInput = MutableStateFlow("")
    
    private val _activeDraft = MutableStateFlow<NoteDraft?>(null)
    val activeDraft = _activeDraft.asStateFlow()

    // Combined UI notes flow
    val uiState: StateFlow<NoteUiState> = combine(
        repository.allNotes,
        _searchQuery,
        _selectedSubject,
        _sortMode,
        _showFavoritesOnly,
        _currentScreen,
        _isDarkThemeOverride
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val allNotes = flows[0] as List<Note>
        val query = flows[1] as String
        val subject = flows[2] as String
        val sort = flows[3] as SortMode
        val favorsOnly = flows[4] as Boolean
        val screen = flows[5] as Screen
        val darkTheme = flows[6] as? Boolean

        // 1. Filter by query
        var filtered = if (query.isBlank()) {
            allNotes
        } else {
            allNotes.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }

        // 2. Filter by subject
        if (subject != "All") {
            filtered = filtered.filter { it.subject == subject }
        }

        // 3. Filter by favorites
        if (favorsOnly) {
            filtered = filtered.filter { it.favorite }
        }

        // 4. Sort notes. Rules: pinned notes always go to the top, then sort by selected SortMode
        val (pinned, unpinned) = filtered.partition { it.pinned }
        
        val sortedUnpinned = when (sort) {
            SortMode.UPDATED_DESC -> unpinned.sortedByDescending { it.updatedDate }
            SortMode.UPDATED_ASC -> unpinned.sortedBy { it.updatedDate }
            SortMode.CREATED_DESC -> unpinned.sortedByDescending { it.createdDate }
            SortMode.TITLE_ASC -> unpinned.sortedBy { it.title.lowercase() }
        }

        val sortedPinned = when (sort) {
            SortMode.UPDATED_DESC -> pinned.sortedByDescending { it.updatedDate }
            SortMode.UPDATED_ASC -> pinned.sortedBy { it.updatedDate }
            SortMode.CREATED_DESC -> pinned.sortedByDescending { it.createdDate }
            SortMode.TITLE_ASC -> pinned.sortedBy { it.title.lowercase() }
        }

        NoteUiState(
            notes = sortedPinned + sortedUnpinned,
            searchQuery = query,
            selectedSubject = subject,
            sortMode = sort,
            showFavoritesOnly = favorsOnly,
            currentScreen = screen,
            isDarkThemeOverride = darkTheme
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteUiState()
    )

    // Derived State Flow: Recent Notes (top 4 recently updated notes)
    val recentNotes: StateFlow<List<Note>> = repository.allNotes
        .combine(_selectedSubject) { allNotes, subject ->
            val pool = if (subject == "All") allNotes else allNotes.filter { it.subject == subject }
            pool.sortedByDescending { it.updatedDate }.take(4)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleThemeOverride() {
        _isDarkThemeOverride.value = when (_isDarkThemeOverride.value) {
            null -> true
            true -> false
            false -> null
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        // Clear editor fields when going home
        if (screen is Screen.Home) {
            noteTitleInput.value = ""
            noteSubjectInput.value = "General"
            noteContentInput.value = ""
            _activeDraft.value = null
        }
    }

    // Toggle Pin
    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(pinned = !note.pinned, updatedDate = System.currentTimeMillis()))
        }
    }

    // Toggle Favorite
    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(favorite = !note.favorite, updatedDate = System.currentTimeMillis()))
        }
    }

    // Core note insert/update/delete
    fun addNote(title: String, subject: String, content: String, onComplete: () -> Unit) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val note = Note(
                title = title.ifBlank { "Untitled Note" },
                subject = subject,
                content = content,
                createdDate = System.currentTimeMillis(),
                updatedDate = System.currentTimeMillis()
            )
            repository.insertNote(note)
            clearDraftById(0) // clear new note draft
            onComplete()
        }
    }

    fun updateNote(noteId: Int, title: String, subject: String, content: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val existing = repository.getNoteById(noteId)
            if (existing != null) {
                val updated = existing.copy(
                    title = title.ifBlank { existing.title },
                    subject = subject,
                    content = content,
                    updatedDate = System.currentTimeMillis()
                )
                repository.updateNote(updated)
                clearDraftById(noteId) // clear specific note edit draft
            }
            onComplete()
        }
    }

    fun deleteNote(note: Note, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNote(note)
            clearDraftById(note.id)
            onComplete()
        }
    }

    fun deleteNoteById(id: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
            clearDraftById(id)
            onComplete()
        }
    }

    // Draft operations
    fun checkForDraft(id: Int) {
        viewModelScope.launch {
            val draft = repository.getDraftById(id)
            _activeDraft.value = draft
        }
    }

    fun writeDraft(id: Int, title: String, subject: String, content: String) {
        viewModelScope.launch {
            val draft = NoteDraft(
                id = id,
                title = title,
                subject = subject,
                content = content,
                updatedDate = System.currentTimeMillis()
            )
            repository.saveDraft(draft)
            _activeDraft.value = draft
        }
    }

    fun restoreDraft(draft: NoteDraft) {
        noteTitleInput.value = draft.title
        noteSubjectInput.value = draft.subject
        noteContentInput.value = draft.content
        _activeDraft.value = null
    }

    fun clearDraftById(id: Int) {
        viewModelScope.launch {
            repository.deleteDraftById(id)
            _activeDraft.value = null
        }
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
