package com.sebix.cleanarchitecture.business.interactors.common

import com.sebix.cleanarchitecture.business.data.cache.CacheResponseHandler
import com.sebix.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.sebix.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.sebix.cleanarchitecture.business.data.util.safeApiCall
import com.sebix.cleanarchitecture.business.data.util.safeCacheCall
import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.state.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteNote<ViewState>(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {
    fun deleteNote(
        note: Note,
        stateEvent: StateEvent
    ): Flow<DataState<ViewState>?> = flow {
        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.deleteNote(note.id)
        }
        val response = object : CacheResponseHandler<ViewState, Int>(
            response = cacheResult,
            stateEvent = stateEvent
        ) {
            override fun handleSuccess(resultObj: Int): DataState<ViewState> {
                return if (resultObj > 0) {
                    DataState.data(
                        response = Response(
                            message = DELETE_NOTE_SUCCESS,
                            uiComponentType = UIComponentType.None(),
                            messageType = MessageType.Success()
                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                } else {
                    DataState.data(
                        response = Response(
                            message = DELETE_NOTE_FAILURE,
                            uiComponentType = UIComponentType.Toast(),
                            messageType = MessageType.Error()
                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                }
            }
        }.getResult()

        emit(response)
        updateNetwork(
            message = response.stateMessage?.response?.message,
            note = note
        )
    }

    private suspend fun updateNetwork(
        message: String?,
        note: Note
    ) {
        if (message == DELETE_NOTE_SUCCESS) {
            safeApiCall(IO) {
                noteNetworkDataSource.deleteNote(note.id)
            }
            safeApiCall(IO) {
                noteNetworkDataSource.insertDeletedNote(note)
            }
        }
    }

    companion object {
        const val DELETE_NOTE_SUCCESS = "Successfully deleted note"
        const val DELETE_NOTE_FAILURE = "Failed to delet note"
    }
}