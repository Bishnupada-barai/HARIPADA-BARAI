package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NoteViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormScreen(
    noteId: Int?, // if null, represent Add Note. Otherwise Edit Note
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val draftId = noteId ?: 0

    // Gather existing fields if editing
    val uiState by viewModel.uiState.collectAsState()
    val activeDraft by viewModel.activeDraft.collectAsState()

    // Original properties loaded from the DB
    var originalTitle by remember { mutableStateOf("") }
    var originalSubject by remember { mutableStateOf("General") }
    var originalContent by remember { mutableStateOf("") }

    // Temporary editor states
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    var content by remember { mutableStateOf("") }

    var isLoaded by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDraftBanner by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var draftStatusMessage by remember { mutableStateOf("Autosave idle") }

    androidx.activity.compose.BackHandler(enabled = true) {
        val hasUnsavedChanges = title != originalTitle || subject != originalSubject || content != originalContent
        if (hasUnsavedChanges) {
            showCancelDialog = true
        } else {
            onBack()
        }
    }

    // 1. First-time Loader: Load existing values from DB
    LaunchedEffect(noteId) {
        if (noteId != null) {
            val existing = uiState.notes.firstOrNull { it.id == noteId }
            if (existing != null) {
                originalTitle = existing.title
                originalSubject = existing.subject
                originalContent = existing.content

                title = existing.title
                subject = existing.subject
                content = existing.content
            }
        } else {
            originalTitle = ""
            originalSubject = "General"
            originalContent = ""

            title = ""
            subject = "General"
            content = ""
        }
        isLoaded = true
        // Check if there is any previously saved draft in Room Database
        viewModel.checkForDraft(draftId)
    }

    // 2. Draft detection Alert
    LaunchedEffect(activeDraft) {
        activeDraft?.let {
            // Only show banner if active draft is actually different from current filled inputs
            if (it.title != title || it.content != content || it.subject != subject) {
                showDraftBanner = true
            }
        }
    }

    // 3. Debounced Autosave engine
    // Watches changes to title, subject, content. Writes to drafts DB every 2 seconds after user stops typing
    LaunchedEffect(title, subject, content, isLoaded) {
        if (!isLoaded) return@LaunchedEffect

        // Check if values differ from original (unsaved)
        val hasChanges = title != originalTitle || subject != originalSubject || content != originalContent

        if (hasChanges) {
            draftStatusMessage = "Typing..."
            delay(1500) // Wait 1.5s after user stops typing
            draftStatusMessage = "Saving draft..."
            viewModel.writeDraft(draftId, title, subject, content)
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            draftStatusMessage = "Draft saved at ${format.format(Date())}"
        }
    }

    // Actual screen UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == null) "Add Note Book" else "Update Note",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val hasUnsavedChanges = title != originalTitle || subject != originalSubject || content != originalContent
                            if (hasUnsavedChanges) {
                                showCancelDialog = true
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    // Check button (SAVE)
                    IconButton(
                        onClick = {
                            if (noteId == null) {
                                viewModel.addNote(title, subject, content) {
                                    onBack()
                                }
                            } else {
                                viewModel.updateNote(noteId, title, subject, content) {
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("action_save_note"),
                        enabled = title.isNotBlank() || content.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = if (title.isNotBlank() || content.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Draft Banner
            if (showDraftBanner && activeDraft != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unsaved draft found for this note!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A version with different content was auto-saved on " +
                                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(activeDraft!!.updatedDate)) +
                                    ". Do you want to restore it?",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    activeDraft?.let {
                                        title = it.title
                                        subject = it.subject
                                        content = it.content
                                    }
                                    showDraftBanner = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("draft_restore_btn")
                            ) {
                                Text("Restore Draft")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearDraftById(draftId)
                                    showDraftBanner = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                modifier = Modifier.testTag("draft_discard_btn")
                            ) {
                                Text("Discard Draft")
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Topic/Subject selector dropdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Subject Theme Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_selector_card"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(getSubjectColor(subject), shape = androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Text(
                                        text = subject,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open Categories Selector")
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f).testTag("subject_dropdown")
                        ) {
                            SubjectsList.filter { it != "All" }.forEach { subOption ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(getSubjectColor(subOption), shape = androidx.compose.foundation.shape.CircleShape)
                                            )
                                            Text(subOption)
                                        }
                                    },
                                    onClick = {
                                        subject = subOption
                                        dropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("subject_menu_item_$subOption")
                                )
                            }
                        }
                    }
                }

                // Note Title Entry
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("E.g., Newton's Laws of Motion") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                // Note Content multiline text input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content description") },
                    placeholder = { Text("Start typing and sketching study notes here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                        .testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    maxLines = 50
                )

                // Actions: Save & Cancel Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasUnsavedChanges = title != originalTitle || subject != originalSubject || content != originalContent
                            if (hasUnsavedChanges) {
                                showCancelDialog = true
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("form_cancel_button")
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (noteId == null) {
                                viewModel.addNote(title, subject, content) {
                                    onBack()
                                }
                            } else {
                                viewModel.updateNote(noteId, title, subject, content) {
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("form_save_button"),
                        enabled = title.isNotBlank() || content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Notes", fontWeight = FontWeight.Bold)
                    }
                }

                // Autogenerated banner matching "Autosave drafts"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Offline indicator",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Device Offline - $draftStatusMessage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Double cancel dialog confirmation
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Discard Draft Changes?") },
            text = { Text("You have unsaved form text content. Do you want to preserve your changes as a draft for next time, or completely discard them?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Discard
                        viewModel.clearDraftById(draftId)
                        showCancelDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_confirm_discard")
                ) {
                    Text("Discard Draft")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Keep draft (already autosaved to room!)
                        showCancelDialog = false
                        onBack()
                    },
                    modifier = Modifier.testTag("dialog_confirm_keep_draft")
                ) {
                    Text("Save Draft & Quit")
                }
            }
        )
    }
}
