package com.sebix.cleanarchitecture.business.data.cache.abstraction

import com.sebix.cleanarchitecture.business.domain.model.Note

interface NoteCacheDataSource {
    suspend fun insertNote(note: Note): Long

    suspend fun deleteNote(primaryKey: String): Int

    suspend fun deleteNotes(notes: List<Note>): Int

    suspend fun updateNote(primary: String, newTitle: String, newBody: String): Int

    suspend fun searchNotes(
        query: String,
        filterAndOrder: String,
        page: Int
    ): List<Note>

    suspend fun searchNoteById(primaryKey: String): Note?

    suspend fun getNumNotes(): Int

    suspend fun insertNotes(notes: List<Note>): LongArray

    suspend fun getAllNotes(): List<Note>
}