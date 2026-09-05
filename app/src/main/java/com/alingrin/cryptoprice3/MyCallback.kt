package com.alingrin.cryptoprice3

interface MyCallback {
    fun onDataReceived(data: QuoteLatestResponseModel, code: Int)
    fun onFailure(t: Throwable)
}