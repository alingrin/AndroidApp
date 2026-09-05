package com.alingrin.cryptoprice3

import com.google.gson.annotations.SerializedName

data class MarketChartApiResponseModel(
    @SerializedName("prices") val prices: List<List<Double>>? = null,
    @SerializedName("market_caps") val marketCaps: List<List<Double>>? = null,
    @SerializedName("total_volumes") val totalVolumes: List<List<Double>>? = null
)