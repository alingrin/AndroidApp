package com.alingrin.cryptoprice3

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class CoinGeckoPriceResponse(
    @SerializedName("bitcoin") val bitcoin: CoinGeckoPrice? = null,
    @SerializedName("ethereum") val ethereum: CoinGeckoPrice? = null
)

data class CoinGeckoPrice(
    @SerializedName("usd") val usd: Float = 0f,
    @SerializedName("usd_24h_change") val usd24hChange: Float = 0f
)

interface CoinGeckoPriceApi {
    @GET("simple/price")
    fun getPrices(
        @Query("ids") ids: String,
        @Query("vs_currencies") currency: String,
        @Query("include_24hr_change") include24hChange: Boolean,
        @Header("Authorization") authorization: String
    ): Call<CoinGeckoPriceResponse>
}