package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.NoteUiState
import com.example.ui.NoteViewModel
import com.example.ui.Screen
import com.example.ui.SortMode
import java.text.SimpleDateFormat
import java.util.*

val SubjectsList = listOf(
    "All",
    "Mathematics",
    "Physics",
    "Chemistry",
    "Biology",
    "English",
    "Computer Science",
    "General"
)

fun getSubjectColor(subject: String): Color {
    return when (subject) {
        "Mathematics" -> Color(0xFFEF4444)
        "Physics" -> Color(0xFF8B5CF6)
        "Chemistry" -> Color(0xFF06B6D4)
        "Biology" -> Color(0xFF10B981)
        "English" -> Color(0xFFF59E0B)
        "Computer Science" -> Color(0xFF3B82F6)
        else -> Color(0xFF64748B)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    uiState: NoteUiState,
    recentNotes: List<Note>,
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "App Icon Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Smart Notes",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Filter Favorites
                    IconButton(
                        onClick = { viewModel.toggleFavoritesOnly() },
                        modifier = Modifier.testTag("action_toggle_favorites")
                    ) {
                        Icon(
                            imageVector = if (uiState.showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Show Favorites Only Toggle",
                            tint = if (uiState.showFavoritesOnly) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle Theme Override
                    IconButton(
                        onClick = { viewModel.toggleThemeOverride() },
                        modifier = Modifier.testTag("action_toggle_theme")
                    ) {
                        Icon(
                            imageVector = when (uiState.isDarkThemeOverride) {
                                true -> Icons.Default.LightMode
                                false -> Icons.Default.DarkMode
                                else -> Icons.Default.SettingsSuggest
                            },
                            contentDescription = "Toggle Light/Dark Theme Override"
                        )
                    }

                    // Sort menu button
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("action_sort_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SortByAlpha,
                            contentDescription = "Change Notes Sort Criteria"
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest Draft/Edit") },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            onClick = {
                                viewModel.setSortMode(SortMode.UPDATED_DESC)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_updated_desc")
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest Draft/Edit") },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                            onClick = {
                                viewModel.setSortMode(SortMode.UPDATED_ASC)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_updated_asc")
                        )
                        DropdownMenuItem(
                            text = { Text("Newest Created") },
                            leadingIcon = { Icon(Icons.Default.FiberNew, contentDescription = null) },
                            onClick = {
                                viewModel.setSortMode(SortMode.CREATED_DESC)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_created_desc")
                        )
                        DropdownMenuItem(
                            text = { Text("Alphabetical (A-Z)") },
                            leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                            onClick = {
                                viewModel.setSortMode(SortMode.TITLE_ASC)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_title_asc")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNoteClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("fab_add_note")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Notes", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Full Width Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by title or body contents...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("note_search_bar"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search Active Query")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            // 2. Horizontal Scrolling Subject Filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SubjectsList) { subject ->
                    val isSelected = uiState.selectedSubject == subject
                    val colorAccent = getSubjectColor(subject)
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedSubject(subject) },
                        label = {
                            Text(
                                text = subject,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (subject == "All") MaterialTheme.colorScheme.primary else colorAccent,
                            selectedLabelColor = if (subject == "All") MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        modifier = Modifier.testTag("subject_chip_$subject")
                    )
                }
            }

            // Real body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3. Optional Recent Notes Section (Shows if search is empty, no filter is applied)
                if (uiState.searchQuery.isBlank() && !uiState.showFavoritesOnly && recentNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Updates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentNotes) { note ->
                                RecentNoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Header for All Notes / Filtered Notes list
                item {
                    val headerText = when {
                        uiState.showFavoritesOnly && uiState.selectedSubject != "All" -> "Favorite ${uiState.selectedSubject} Notes"
                        uiState.showFavoritesOnly -> "My Favorite Notes"
                        uiState.selectedSubject != "All" -> "${uiState.selectedSubject} Notes"
                        else -> "All Study Notes"
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                }

                // 4. List of matching Notes
                if (uiState.notes.isEmpty()) {
                    item {
                        EmptyNotesPlaceholder(
                            isSearching = uiState.searchQuery.isNotEmpty(),
                            isFavoritesOnly = uiState.showFavoritesOnly
                        )
                    }
                } else {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            searchQuery = uiState.searchQuery,
                            onClick = { onNoteClick(note.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(note) },
                            onPinToggle = { viewModel.togglePin(note) },
                            onDeleteClick = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    }

    // Confirmation delete Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete This Note?") },
            text = { Text("Are you sure you want to delete note \"${note.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_confirm_delete")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecentNoteCard(note: Note, onClick: () -> Unit) {
    val subjectColor = getSubjectColor(note.subject)
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag("recent_note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(subjectColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = note.subject,
                        style = MaterialTheme.typography.labelSmall,
                        color = subjectColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(note.updatedDate)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardItem(
    note: Note,
    searchQuery: String,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onPinToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val subjectColor = getSubjectColor(note.subject)
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val isPinned = note.pinned
    val isFavorite = note.favorite

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            )
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isPinned) androidx.compose.foundation.BorderStroke(
            1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        ) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Subject Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(subjectColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.subject,
                        style = MaterialTheme.typography.labelMedium,
                        color = subjectColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pin / Favorite Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPinned) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp).clickable { onPinToggle() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Unpin Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onPinToggle, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin Note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite status",
                            tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note Button",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Highlight query match
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modified: ${formatter.format(Date(note.updatedDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (isPinned) {
                    Text(
                        text = "PINNED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNotesPlaceholder(isSearching: Boolean, isFavoritesOnly: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = when {
                        isSearching -> Icons.Default.SearchOff
                        isFavoritesOnly -> Icons.Default.HeartBroken
                        else -> Icons.Default.FolderOpen
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                )
            }
            Text(
                text = when {
                    isSearching -> "No Notes Found"
                    isFavoritesOnly -> "No Favorite Notes Yet"
                    else -> "No Notes added yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    isSearching -> "Try rewriting your match query words so we can find your notes."
                    isFavoritesOnly -> "Tap the Heart icon on any Note card to add them to your Quick Favorites!"
                    else -> "Click the big primary (+) floating button below to create your very first study subject notebook!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
