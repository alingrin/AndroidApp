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
            .create(CoinGeckoPriceApi::class.java)

        apiService.getPrices(
            ids = "bitcoin,ethereum",
            currency = "usd",
            include24hChange = true,
            authorization = "Bearer $API_KEY"
        )
            .enqueue(object : Callback<CoinGeckoPriceResponse> {
                override fun onResponse(
                    call: Call<CoinGeckoPriceResponse>,
                    response: Response<CoinGeckoPriceResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val prices = response.body()!!
                        val bitcoin = prices.bitcoin
                        val ethereum = prices.ethereum
                        if (bitcoin == null || ethereum == null) {
                            callback.onFailure(Exception("CoinGecko response did not include both Bitcoin and Ethereum prices"))
                            return
                        }
                        callback.onDataReceived(
                            QuoteLatestResponseModel(
                                data = mapOf(
                                    "1" to bitcoin.toQuote("Bitcoin", 1),
                                    "1027" to ethereum.toQuote("Ethereum", 1027)
                                )
                            ),
                            response.code()
                        )
                    } else {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        callback.onFailure(
                            Exception("CoinGecko API call unsuccessful (HTTP ${response.code()}): $errorBody")
                        )
                    }
                }

                override fun onFailure(call: Call<CoinGeckoPriceResponse>, t: Throwable) {
                    callback.onFailure(t)
                }
            })
    }

    private fun CoinGeckoPrice.toQuote(name: String, id: Int) =
        QuoteLatestResponseModel.CryptoCurrency(
            id = id,
            name = name,
            quote = QuoteLatestResponseModel.Quote(
                usd = QuoteLatestResponseModel.USD(usd, usd24hChange)
            )
        )

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/api/v3/"
        private const val API_KEY = "CG-YVbRN4Sir4qJNtEbLZnHcb8n"
    }
}