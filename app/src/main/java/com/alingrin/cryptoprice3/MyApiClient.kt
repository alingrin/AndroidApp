package com.alingrin.cryptoprice3

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyApiClient {
    fun fetchDataAsync(callback: MyCallback) {
        val apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        apiService.getCryptoData("bitcoin,ethereum", "USD", API_KEY, "*/*")
            .enqueue(object : Callback<QuoteLatestResponseModel> {
                override fun onResponse(
                    call: Call<QuoteLatestResponseModel>,
                    response: Response<QuoteLatestResponseModel>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        callback.onDataReceived(response.body()!!, response.code())
                    } else {
                        callback.onFailure(Exception("API call unsuccessful"))
                    }
                }

                override fun onFailure(call: Call<QuoteLatestResponseModel>, t: Throwable) {
                    callback.onFailure(t)
                }
            })
    }

    companion object {
        private const val BASE_URL = "https://pro-api.coinmarketcap.com/v2/"
        private const val API_KEY = "ffb8840c-b444-41ef-b260-cfd1544312a6"
    }
}