package com.alingrin.cryptoprice3

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface CoinGeckoApiEthereum {
    @Headers("Authorization: Bearer CG-YVbRN4Sir4qJNtEbLZnHcb8n")
    @GET("coins/ethereum/market_chart")
    fun getMarketChart(
        @Query("vs_currency") currency: String,
        @Query("days") days: Int,
        @Query("interval") interval: String,
        @Query("precision") precision: Int
    ): Call<MarketChartApiResponseModel>
}