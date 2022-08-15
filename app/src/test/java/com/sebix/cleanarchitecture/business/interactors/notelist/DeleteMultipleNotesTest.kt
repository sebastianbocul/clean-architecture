package com.sebix.cleanarchitecture.business.interactors.notelist

import com.sebix.cleanarchitecture.business.data.cache.FORCE_DELETE_NOTE_EXCEPTION
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.sebix.cleanarchitecture.business.interactors.notelist.DeleteMultipleNotes.Companion.DELETE_NOTES_ERRORS
import com.sebix.cleanarchitecture.business.interactors.notelist.DeleteMultipleNotes.Companion.DELETE_NOTES_SUCCESS
import com.sebix.cleanarchitecture.di.DependencyContainer
import com.sebix.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent.DeleteMultipleNotesEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class DeleteMultipleNotesTest {
    // system in test
    private lateinit var deleteMultipleNotes: DeleteMultipleNotes

    // dependencies
    private lateinit var dependencyContainer: DependencyContainer
    private lateinit var noteCacheDataSource: NoteCacheDataSource
    private lateinit var noteNetworkDataSource: NoteNetworkDataSource
    private lateinit var noteFactory: NoteFactory

    @BeforeEach
    fun beforeEach() {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        deleteMultipleNotes = DeleteMultipleNotes(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @AfterEach
    fun afterEach() {

    }

    @Test
    internal fun deleteNotes_success_confirmNetworkAndCacheUpdated() = runBlocking {
        val randomNotes = arrayListOf<Note>()
        val notesInCache = noteCacheDataSource.searchNotes("", "", 1)
        notesInCache.forEach {
            randomNotes.add(it)
            if (randomNotes.size > 4) {
                return@forEach
            }
        }
        deleteMultipleNotes.deleteNotes(
            notes = randomNotes,
            stateEvent = DeleteMultipleNotesEvent(
                randomNotes
            )
        ).collect {
            assertEquals(
                it.stateMessage?.response?.message,
                DELETE_NOTES_SUCCESS
            )
        }

        val deletedNetworkNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(
            deletedNetworkNotes.containsAll(randomNotes)
        )

        val doNotesExistInNetwork = noteNetworkDataSource.getAllNotes().containsAll(randomNotes)
        assertFalse(doNotesExistInNetwork)

        randomNotes.forEach {
            val noteInCache = noteCacheDataSource.searchNoteById(it.id)
            assertTrue(noteInCache == null)
        }
    }

    @Test
    internal fun deleteNotes_fail_confirmCorrectDeletesMade() = runBlocking {
        val validNotes = arrayListOf<Note>()
        val invalidNotes = arrayListOf<Note>()
        val notesInCache = noteCacheDataSource.searchNotes("", "", 1)

        for (index in 0..notesInCache.size) {
            var note: Note
            if (index % 2 == 0) {
                note = noteFactory.createSingleNote(
                    id = UUID.randomUUID().toString(),
                    title = notesInCache[index].title,
                    body = notesInCache[index].body
                )
                invalidNotes.add(note)
            } else {
                note = notesInCache[index]
                validNotes.add(note)
            }
            if ((invalidNotes.size + validNotes.size) > 4) break
        }

        val notesToDelete = ArrayList(validNotes + invalidNotes)

        deleteMultipleNotes.deleteNotes(
            notes = notesToDelete,
            stateEvent = DeleteMultipleNotesEvent(
                notesToDelete
            )
        ).collect {
            assertEquals(
                it.stateMessage?.response?.message,
                DELETE_NOTES_ERRORS
            )
        }

        val networkNotes = noteNetworkDataSource.getAllNotes()
        assertFalse(networkNotes.containsAll(validNotes))

        val deletedNetworkNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNetworkNotes.containsAll(validNotes))
        assertFalse(deletedNetworkNotes.containsAll(invalidNotes))

        validNotes.forEach {
            val noteInCache = noteCacheDataSource.searchNoteById(it.id)
            assertTrue(noteInCache == null)
        }

        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(numNotesInCache == (notesInCache.size - validNotes.size))
    }

    @Test
    internal fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking{
        val validNotes = arrayListOf<Note>()
        val invalidNotes = arrayListOf<Note>()
        val notesInCache = noteCacheDataSource.searchNotes("", "", 1)

        for (note in notesInCache) {
            validNotes.add(note)
            if (validNotes.size > 4) break
        }

        val errorNote  = Note(
            id = FORCE_DELETE_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )
        invalidNotes.add(errorNote)

        val notesToDelete = ArrayList(validNotes + invalidNotes)
        deleteMultipleNotes.deleteNotes(
            notes = notesToDelete,
            stateEvent = DeleteMultipleNotesEvent(
                notesToDelete
            )
        ).collect {
            assertEquals(
                it.stateMessage?.response?.message,
                DELETE_NOTES_ERRORS
            )
        }


        val networkNotes = noteNetworkDataSource.getAllNotes()
        assertFalse(networkNotes.containsAll(validNotes))

        val deletedNetworkNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNetworkNotes.containsAll(validNotes))
        assertFalse(deletedNetworkNotes.containsAll(invalidNotes))

        validNotes.forEach {
            val noteInCache = noteCacheDataSource.searchNoteById(it.id)
            assertTrue(noteInCache == null)
        }

        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(numNotesInCache == (notesInCache.size - validNotes.size))
    }
}