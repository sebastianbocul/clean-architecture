package com.sebix.cleanarchitecture.business.interactors.splash

import com.sebix.cleanarchitecture.business.data.cache.CacheResponseHandler
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.ApiResponseHandler
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.data.util.safeApiCall
import com.sebix.cleanarchitecture.business.data.util.safeCacheCall
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.state.DataState
import com.sebix.cleanarchitecture.business.domain.util.DateUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {
    suspend fun syncNotes() {
        val cachedNotesList = getCachedNotes()
        syncNetworkNotesWithCachedNotes(ArrayList(cachedNotesList))
    }

    private suspend fun getCachedNotes(): List<Note> {
        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.getAllNotes()
        }

        val response = object : CacheResponseHandler<List<Note>, List<Note>>(
            response = cacheResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: List<Note>): DataState<List<Note>> {
                return DataState.data(
                    response = null,
                    data = resultObj,
                    stateEvent = null
                )
            }
        }.getResult()
        return response?.data ?: arrayListOf()
    }

    private suspend fun syncNetworkNotesWithCachedNotes(
        cacheNotes: ArrayList<Note>
    ) = withContext(IO) {
        val networkResult = safeApiCall(IO) {
            noteNetworkDataSource.getAllNotes()
        }
        val response = object : ApiResponseHandler<List<Note>, List<Note>?>(
            response = networkResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: List<Note>?): DataState<List<Note>> {
                return DataState.data(
                    response = null,
                    data = resultObj,
                    stateEvent = null
                )
            }
        }.getResult()
        val noteList = response.data ?: arrayListOf()
        val job = launch {
            for (note in noteList) {
                noteCacheDataSource.searchNoteById(note.id)?.let {
                    cacheNotes.remove(it)
                    checkIfCachedNoteRequiresUpdate(it, note)
                } ?: noteCacheDataSource.insertNote(note)
            }
        }
        job.join()

        for (cachedNote in cacheNotes) {
            safeApiCall(IO) {
                noteNetworkDataSource.insertOrUpdateNote(cachedNote)
            }
        }
    }

    private suspend fun checkIfCachedNoteRequiresUpdate(cachedNote: Note, networkNote: Note) {
        val cacheUpdatedAt = cachedNote.updated_at
        val networkUpdatedAt = networkNote.updated_at
        if (networkUpdatedAt > cacheUpdatedAt) {
            safeCacheCall(IO) {
                noteCacheDataSource.updateNote(
                    primary = networkNote.id,
                    newTitle = networkNote.title,
                    newBody = networkNote.body
                )
            }
        } else {
            safeApiCall(IO) {
                noteNetworkDataSource.insertOrUpdateNote(
                    cachedNote
                )
            }
        }
    }
}