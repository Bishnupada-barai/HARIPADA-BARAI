package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNoteByIdFlow(id: Int): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Int) = noteDao.deleteNoteById(id)

    suspend fun getDraftById(id: Int): NoteDraft? = noteDao.getDraftById(id)

    suspend fun saveDraft(draft: NoteDraft) = noteDao.insertDraft(draft)

    suspend fun deleteDraftById(id: Int) = noteDao.deleteDraftById(id)
}
