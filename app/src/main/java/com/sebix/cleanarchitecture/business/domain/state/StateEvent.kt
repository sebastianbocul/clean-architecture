package com.sebix.cleanarchitecture.business.domain.state

interface StateEvent {

    fun errorInfo(): String

    fun eventName(): String

    fun shouldDisplayProgressBar(): Boolean
}