package com.sebix.cleanarchitecture.business.interactors.notedetail

import com.sebix.cleanarchitecture.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.sebix.cleanarchitecture.business.data.cache.FORCE_UPDATE_NOTE_EXCEPTION
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.business.interactors.notedetail.UpdateNote.Companion.UPDATE_NOTE_FAILED
import com.sebix.cleanarchitecture.business.interactors.notedetail.UpdateNote.Companion.UPDATE_NOTE_SUCCESS
import com.sebix.cleanarchitecture.di.DependencyContainer
import com.sebix.cleanarchitecture.framework.presentation.notedetail.state.NoteDetailStateEvent
import junit.framework.Assert.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

class UpdateNoteTest {
    // system in test
    private val updateNote: UpdateNote

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        updateNote = UpdateNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    internal fun updateNote_success_confirmNetworkAndCacheUpdated() = runBlocking {
        val randomNote = noteCacheDataSource.searchNotes("", "", 1)[0]
        val updatedNote = noteFactory.createSingleNote(
            id = randomNote.id,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        updateNote.updateNote(
            note = updatedNote,
            stateEvent = NoteDetailStateEvent.UpdateNoteEvent()
        ).collect {
            assertEquals(
                it?.stateMessage?.response?.message,
                UPDATE_NOTE_SUCCESS
            )
        }

        val cacheNote = noteCacheDataSource.searchNoteById(updatedNote.id)
        assertTrue(
            cacheNote == updatedNote
        )
        val networkNote = noteNetworkDataSource.searchNote(
            updatedNote
        )
        assertTrue(
            networkNote == updatedNote
        )
    }

    @Test
    internal fun updateNote_fail_confirmNetworkAndCacheUnchanged() = runBlocking {
        val noteToUpdate = Note(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString()
        )
        updateNote.updateNote(
            note = noteToUpdate,
            stateEvent = NoteDetailStateEvent.UpdateNoteEvent()
        ).collect {
            assertEquals(
                it?.stateMessage?.response?.message,
                UPDATE_NOTE_FAILED
            )
        }
        val cacheNote = noteCacheDataSource.searchNoteById(noteToUpdate.id)
        assertNull(
            cacheNote
        )
        val networkNote = noteNetworkDataSource.searchNote(
            noteToUpdate
        )
        assertNull(
            networkNote
        )
    }

    @Test
    internal fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        val noteToUpdate = Note(
            id = FORCE_UPDATE_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString()
        )
        updateNote.updateNote(
            note = noteToUpdate,
            stateEvent = NoteDetailStateEvent.UpdateNoteEvent()
        ).collect {
            assertTrue(
                it?.stateMessage?.response?.message?.contains(CACHE_ERROR_UNKNOWN) ?: false
            )
        }
        val cacheNote = noteCacheDataSource.searchNoteById(noteToUpdate.id)
        assertNull(
            cacheNote
        )
        val networkNote = noteNetworkDataSource.searchNote(
            noteToUpdate
        )
        assertNull(
            networkNote
        )
    }
}