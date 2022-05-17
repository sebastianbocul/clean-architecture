package com.sebix.cleanarchitecture.util

import android.util.Log
import com.sebix.cleanarchitecture.util.Constants.DEBUG
import com.sebix.cleanarchitecture.util.Constants.TAG

var isUnitTest = false

fun printLogD(className: String?, message: String) {
    if (DEBUG && !isUnitTest) {
        Log.d(TAG, "$className: $message")
    } else if (DEBUG && isUnitTest) {
        println("$className: $message")
    }
}