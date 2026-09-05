package com.alingrin.cryptoprice3

interface ChartCallback {
    fun onDataReceived(data: MarketChartApiResponseModel, code: Int)
    fun onFailure(t: Throwable)
}