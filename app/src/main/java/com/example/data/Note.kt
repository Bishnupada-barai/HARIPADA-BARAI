package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String, // e.g. "Mathematics", "Physics", "Chemistry", "Biology", "English", "Computer Science", "General"
    val content: String,
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val pinned: Boolean = false
)

@Entity(tableName = "drafts")
data class NoteDraft(
    @PrimaryKey val id: Int, // 0 for new note, or key of the note code being edited
    val title: String,
    val subject: String,
    val content: String,
    val updatedDate: Long = System.currentTimeMillis()
)
