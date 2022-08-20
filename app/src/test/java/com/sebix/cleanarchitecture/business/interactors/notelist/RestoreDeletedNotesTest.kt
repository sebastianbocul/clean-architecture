package com.sebix.cleanarchitecture.business.interactors.notelist

import com.sebix.cleanarchitecture.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.sebix.cleanarchitecture.business.data.cache.FORCE_GENERAL_FAILURE
import com.sebix.cleanarchitecture.business.data.cache.FORCE_NEW_NOTE_EXCEPTION
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_FAILED
import com.sebix.cleanarchitecture.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_SUCCESS
import com.sebix.cleanarchitecture.di.DependencyContainer
import com.sebix.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import junit.framework.Assert.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

class RestoreDeletedNotesTest {
    // system in test
    private val restoreDeletedNote: RestoreDeletedNote

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
        restoreDeletedNote = RestoreDeletedNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    internal fun restoreNote_success_confirmCacheAndNetworkUpdate() = runBlocking {
        val restoredNote = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        noteNetworkDataSource.insertDeletedNote(restoredNote)
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(
                note = restoredNote
            )
        ).collect {
            assertEquals(
                it?.stateMessage?.response?.message,
                RESTORE_NOTE_SUCCESS
            )
        }
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertTrue(noteInCache == restoredNote)

        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertTrue(noteInNetwork == restoredNote)

        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertFalse(deletedNotes.contains(restoredNote))
    }

    @Test
    internal fun restoreNote_failure_confirmCacheAndNetworkUnchanged() = runBlocking {
        val restoredNote = noteFactory.createSingleNote(
            id = FORCE_GENERAL_FAILURE,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        noteNetworkDataSource.insertDeletedNote(restoredNote)
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(
                note = restoredNote
            )
        ).collect {
            assertEquals(
                it?.stateMessage?.response?.message,
                RESTORE_NOTE_FAILED
            )
        }

        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertFalse(noteInCache == restoredNote)

        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertFalse(noteInNetwork == restoredNote)

        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))
    }

    @Test
    internal fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        val restoredNote = noteFactory.createSingleNote(
            id = FORCE_NEW_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        noteNetworkDataSource.insertDeletedNote(restoredNote)
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(
                note = restoredNote
            )
        ).collect {
            assertTrue(
                it?.stateMessage?.response?.message?.contains(CACHE_ERROR_UNKNOWN) ?: false
            )
        }

        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertFalse(noteInCache == restoredNote)

        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertFalse(noteInNetwork == restoredNote)

        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))
    }
}