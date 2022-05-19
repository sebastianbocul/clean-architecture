package com.sebix.cleanarchitecture.business.interactors.notelist

import com.sebix.cleanarchitecture.business.data.cache.CacheErrors
import com.sebix.cleanarchitecture.business.data.cache.FORCE_GENERAL_FAILURE
import com.sebix.cleanarchitecture.business.data.cache.FORCE_NEW_NOTE_EXCEPTION
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.business.domain.state.DataState
import com.sebix.cleanarchitecture.business.interactors.notelist.InsertNewNote.Companion.INSERT_NOTE_SUCCESS
import com.sebix.cleanarchitecture.di.DependencyContainer
import com.sebix.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import com.sebix.cleanarchitecture.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*


class InsertNewNoteTest {
    // system in test
    private val insertNewNote: InsertNewNote

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
        insertNewNote = InsertNewNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource,
            noteFactory = noteFactory
        )
    }

    @Test
    fun insertNote_success_confirmNetworkAndCacheUpdated() = runBlocking {
        val newNote = noteFactory.createSingleNote(
            id = null,
            title = UUID.randomUUID().toString()
        )
        insertNewNote.insertNewNote(
            id = newNote.id,
            title = newNote.title,
            stateEvent = NoteListStateEvent.InsertNewNoteEvent(
                title = newNote.title,
                body = newNote.body
            )
        ).collect {
            object : FlowCollector<DataState<NoteListViewState>> {
                override suspend fun emit(value: DataState<NoteListViewState>) {
                    Assertions.assertEquals(
                        INSERT_NOTE_SUCCESS,
                        value.stateMessage?.response?.message
                    )
                }
            }
        }
        // confirm cache was updated
        val cacheNoteThatWasInserted = noteCacheDataSource.searchNoteById(newNote.id)
        Assertions.assertTrue(
            cacheNoteThatWasInserted == newNote
        )

        // confirm network was update
        val networkNoteThatWasInserted = noteNetworkDataSource.searchNote(newNote)
        Assertions.assertTrue(
            networkNoteThatWasInserted == newNote
        )
    }


    @Test
    fun insertNote_fail_confirmNetworkAndCacheUnchanged() = runBlocking {
        val newNote = noteFactory.createSingleNote(
            id = FORCE_GENERAL_FAILURE,
            title = UUID.randomUUID().toString()
        )
        insertNewNote.insertNewNote(
            id = newNote.id,
            title = newNote.title,
            stateEvent = NoteListStateEvent.InsertNewNoteEvent(
                title = newNote.title,
                body = newNote.body
            )
        ).collect {
            object : FlowCollector<DataState<NoteListViewState>> {
                override suspend fun emit(value: DataState<NoteListViewState>) {
                    Assertions.assertEquals(
                        InsertNewNote.INSERT_NOTE_FAILED,
                        value.stateMessage?.response?.message
                    )
                }
            }
        }
        // confirm cache was not updated
        val cacheNoteThatWasInserted = noteCacheDataSource.searchNoteById(newNote.id)
        Assertions.assertFalse(
            cacheNoteThatWasInserted == newNote
        )

        // confirm network was not update
        val networkNoteThatWasInserted = noteNetworkDataSource.searchNote(newNote)
        Assertions.assertFalse(
            networkNoteThatWasInserted == newNote
        )
    }

    @Test
    fun insertNote_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        val newNote = noteFactory.createSingleNote(
            id = FORCE_NEW_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString()
        )
        insertNewNote.insertNewNote(
            id = newNote.id,
            title = newNote.title,
            stateEvent = NoteListStateEvent.InsertNewNoteEvent(
                title = newNote.title,
                body = newNote.body
            )
        ).collect {
            object : FlowCollector<DataState<NoteListViewState>> {
                override suspend fun emit(value: DataState<NoteListViewState>) {
                    Assertions.assertTrue(
                        value.stateMessage?.response?.message?.contains(
                            CacheErrors.CACHE_ERROR_UNKNOWN
                        ) ?: false
                    )
                }
            }
        }
        // confirm cache was not updated
        val cacheNoteThatWasInserted = noteCacheDataSource.searchNoteById(newNote.id)
        Assertions.assertFalse(
            cacheNoteThatWasInserted == newNote
        )

        // confirm network was not update
        val networkNoteThatWasInserted = noteNetworkDataSource.searchNote(newNote)
        Assertions.assertFalse(
            networkNoteThatWasInserted == newNote
        )
    }
}
