package com.alingrin.cryptoprice3

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiService {
    @Headers("Content-Type: text/plain")
    @GET("cryptocurrency/quotes/latest")
    fun getCryptoData(
        @Query("slug") slug: String,
        @Query("convert") convert: String,
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Header("Accept") accept: String
    ): Call<QuoteLatestResponseModel>
}