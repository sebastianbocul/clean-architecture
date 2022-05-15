package com.sebix.cleanarchitecture.business.data.network

import com.sebix.cleanarchitecture.business.data.cache.CacheErrors
import com.sebix.cleanarchitecture.business.data.network.NetworkErrors.NETWORK_ERROR
import com.sebix.cleanarchitecture.business.domain.state.*

abstract class ApiResponseHandler<ViewState, Data>(
    private val response: ApiResult<Data>,
    private val stateEvent: StateEvent?
) {
    suspend fun getResult(): DataState<ViewState> {
        return when (response) {
            is ApiResult.GenericError -> {
                DataState.error(
                    response = Response(
                        message = "${stateEvent?.errorInfo()}\n" +
                                "Reason: ${response.errorMessage}",
                        uiComponentType = UIComponentType.Dialog(),
                        messageType = MessageType.Error()
                    ),
                    stateEvent = stateEvent
                )
            }
            is ApiResult.NetworkError -> {
                DataState.error(
                    response = Response(
                        message = "${stateEvent?.errorInfo()}\n" +
                                "Reason: ${NETWORK_ERROR}",
                        uiComponentType = UIComponentType.Dialog(),
                        messageType = MessageType.Error()
                    ),
                    stateEvent = stateEvent
                )
            }
            is ApiResult.Success -> {
                if (response.value == null) {
                    DataState.error(
                        response = Response(
                            message = "${stateEvent?.errorInfo()}\n" +
                                    "Reason: ${CacheErrors.CACHE_ERROR_DATA_NULL}",
                            uiComponentType = UIComponentType.Dialog(),
                            messageType = MessageType.Error()
                        ),
                        stateEvent = stateEvent
                    )
                } else
                    handleSuccess(resultObj = response.value)
            }
        }
    }

    abstract suspend fun handleSuccess(resultObj: Data): DataState<ViewState>
}