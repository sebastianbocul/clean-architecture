package com.sebix.cleanarchitecture.business.interactors.splash

import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.model.NoteFactory
import com.sebix.cleanarchitecture.di.DependencyContainer
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SyncDeletedNotesTest {
    // system in test
    private val syncDeletedNotes: SyncDeletedNotes

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
        syncDeletedNotes = SyncDeletedNotes(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    fun deleteNetworkNotes_confirmCacheSync() = runBlocking {
        val networkNotes = noteNetworkDataSource.getAllNotes()
        val notesToDelete = arrayListOf<Note>()
        networkNotes.forEach {
            notesToDelete.add(it)
            noteNetworkDataSource.deleteNote(it.id)
            if (notesToDelete.size > 4) return@forEach
        }
        syncDeletedNotes.syncDeletedNotes()

        notesToDelete.forEach {
            val cachedNote = noteCacheDataSource.searchNoteById(it.id)
            assertTrue(cachedNote == null)
        }
    }
}