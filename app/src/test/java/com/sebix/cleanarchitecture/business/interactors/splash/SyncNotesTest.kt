package com.sebix.cleanarchitecture.business.interactors.splash

import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.di.DependencyContainer
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

class SyncNotesTest {
    // system in test
    private val syncNotes: SyncNotes

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
        syncNotes = SyncNotes(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    fun insertNetworkNotesIntoCache() = runBlocking {
        val newNotes = noteFactory.createNoteList(50)
        noteNetworkDataSource.insertOrUpdateNotes(newNotes)
        syncNotes.syncNotes()

        for (note in newNotes) {
            val cachedNote = noteCacheDataSource.searchNoteById(note.id)
            assertTrue(cachedNote != null)
        }
    }

    @Test
    fun insertCachedNotesIntoNetwork() = runBlocking {
        val newNotes = noteFactory.createNoteList(50)
        noteCacheDataSource.insertNotes(newNotes)
        syncNotes.syncNotes()
        for (note in newNotes) {
            val networkNote = noteNetworkDataSource.searchNote(note)
            assertTrue(networkNote != null)
        }
    }

    @Test
    fun checkCacheUpdateLogicSync() = runBlocking {
        val cachedNotes = noteCacheDataSource.searchNotes("", "", 1)
        val notesToUpdate = arrayListOf<Note>()
        cachedNotes.forEach {
            val updateNote = noteFactory.createSingleNote(
                id = it.id,
                title = UUID.randomUUID().toString(),
                body = UUID.randomUUID().toString()
            )
            notesToUpdate.add(updateNote)
            if (notesToUpdate.size > 4) {
                return@forEach
            }
        }
        noteCacheDataSource.insertNotes(notesToUpdate)
        syncNotes.syncNotes()

        notesToUpdate.forEach {
            val networkNote = noteNetworkDataSource.searchNote(it)
            assertEquals(it.id, networkNote?.id)
            assertEquals(it.title, networkNote?.title)
            assertEquals(it.body, networkNote?.body)
            assertEquals(it.updated_at, networkNote?.updated_at)
        }
    }

    @Test
    fun checkNetworkUpdateLogicSync() = runBlocking {
        val networkNotes = noteNetworkDataSource.getAllNotes()
        val notesToUpdate = arrayListOf<Note>()
        networkNotes.forEach {
            val updateNote = noteFactory.createSingleNote(
                id = it.id,
                title = UUID.randomUUID().toString(),
                body = UUID.randomUUID().toString()
            )
            notesToUpdate.add(updateNote)
            if (notesToUpdate.size > 4) {
                return@forEach
            }
        }
        noteNetworkDataSource.insertOrUpdateNotes(notesToUpdate)
        syncNotes.syncNotes()

        notesToUpdate.forEach {
            val cachedNote = noteCacheDataSource.searchNoteById(it.id)
            assertEquals(it.id, cachedNote?.id)
            assertEquals(it.title, cachedNote?.title)
            assertEquals(it.body, cachedNote?.body)
            assertEquals(it.updated_at, cachedNote?.updated_at)
        }
    }
}