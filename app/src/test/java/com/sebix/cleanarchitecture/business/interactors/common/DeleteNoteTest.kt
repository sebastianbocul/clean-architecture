package com.sebix.cleanarchitecture.business.interactors.common

import com.sebix.cleanarchitecture.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.sebix.cleanarchitecture.business.data.cache.FORCE_DELETE_NOTE_EXCEPTION
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_FAILURE
import com.sebix.cleanarchitecture.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.sebix.cleanarchitecture.di.DependencyContainer
import com.sebix.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import com.sebix.cleanarchitecture.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

@InternalCoroutinesApi
class DeleteNoteTest {
    // system in test
    private val deleteNote: DeleteNote<NoteListViewState>

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
        deleteNote = DeleteNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    internal fun deleteNote_success_confirmNetworkUpdate() = runBlocking {
        val noteToDelete = noteCacheDataSource.searchNotes(
            "",
            "",
            1
        )[0]
        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect {
            assertEquals(
                it?.stateMessage?.response?.message,
                DELETE_NOTE_SUCCESS
            )
        }
        val wasNoteDelete = !noteNetworkDataSource.getAllNotes().contains(noteToDelete)
        assertTrue(wasNoteDelete)
        val wasDeletedNoteInserted = noteNetworkDataSource.getDeletedNotes().contains(noteToDelete)
        assertTrue(wasDeletedNoteInserted)
    }

    @Test
    internal fun deleteNote_fail_confirmNetworkUnchanged() = runBlocking {
        val noteToDelete = Note(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )
        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect {
            assertEquals(
                it?.stateMessage?.response?.message,
                DELETE_NOTE_FAILURE
            )
        }
        val notes = noteNetworkDataSource.getAllNotes()
        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(notes.size == numNotesInCache)
        val wasDeletedNoteInserted = !noteNetworkDataSource.getDeletedNotes().contains(noteToDelete)
        assertTrue(wasDeletedNoteInserted)
    }

    @Test
    internal fun throwException_checkGenericError_confirmNetworkUnchanged() = runBlocking {
        val noteToDelete = Note(
            id = FORCE_DELETE_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )
        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect {
            assertTrue(
                it?.stateMessage?.response?.message?.contains(CACHE_ERROR_UNKNOWN)?:false
            )
        }
        val notes = noteNetworkDataSource.getAllNotes()
        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(notes.size == numNotesInCache)
        val wasDeletedNoteInserted = !noteNetworkDataSource.getDeletedNotes().contains(noteToDelete)
        assertTrue(wasDeletedNoteInserted)
    }
}