package com.alingrin.cryptoprice3

import android.content.Context
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MarketChartApiClient(private var context: Context) {
    fun setContext(context: Context) {
        this.context = context
    }

    fun fetchDataAsyncBTC(callback: ChartCallback, days: Int, interval: String) {
        val api = createRetrofit().create(CoinGeckoApiBitcoin::class.java)
        enqueue(api.getMarketChart("USD", days, interval, 2), callback)
    }

    fun fetchDataAsyncEthereum(callback: ChartCallback, days: Int, interval: String) {
        val api = createRetrofit().create(CoinGeckoApiEthereum::class.java)
        enqueue(api.getMarketChart("USD", days, interval, 2), callback)
    }

    private fun createRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun enqueue(call: Call<MarketChartApiResponseModel>, callback: ChartCallback) {
        call.enqueue(object : Callback<MarketChartApiResponseModel> {
            override fun onResponse(
                call: Call<MarketChartApiResponseModel>,
                response: Response<MarketChartApiResponseModel>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onDataReceived(response.body()!!, response.code())
                } else {
                    showToast(if (response.code() == 429) {
                        "Too many requests. Please try again later."
                    } else {
                        "An error occurred. Please try again."
                    })
                    callback.onFailure(Exception("API call unsuccessful"))
                }
            }

            override fun onFailure(call: Call<MarketChartApiResponseModel>, t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/api/v3/"
    }
}