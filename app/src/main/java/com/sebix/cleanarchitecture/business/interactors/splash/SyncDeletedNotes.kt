package com.sebix.cleanarchitecture.business.interactors.splash

import com.sebix.cleanarchitecture.business.data.cache.CacheResponseHandler
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.ApiResponseHandler
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.data.util.safeApiCall
import com.sebix.cleanarchitecture.business.data.util.safeCacheCall
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.state.DataState
import com.sebix.cleanarchitecture.util.printLogD
import kotlinx.coroutines.Dispatchers.IO

class SyncDeletedNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {
    suspend fun syncDeletedNotes() {
        val apiResult = safeApiCall(IO) {
            noteNetworkDataSource.getDeletedNotes()
        }
        val response = object : ApiResponseHandler<List<Note>, List<Note>?>(
            response = apiResult,
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
        val notes = response.data ?: arrayListOf()
        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.deleteNotes(notes)
        }
        object : CacheResponseHandler<Int, Int>(
            response = cacheResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: Int): DataState<Int>? {
                printLogD("SyncNotes", "num deleted notes: $resultObj")
                return DataState.data(
                    response = null,
                    data = null,
                    stateEvent = null
                )
            }
        }.getResult()
    }
}