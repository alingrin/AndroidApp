package com.alingrin.cryptoprice3

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

data class QuoteLatestResponseModel(
    @SerializedName("status") var status: Status? = null,
    @SerializedName("data") var data: Map<String, CryptoCurrency>? = null
) {
    data class Status(@SerializedName("timestamp") var timestamp: Timestamp? = null)

    data class CryptoCurrency(
        @SerializedName("id") var id: Int = 0,
        @SerializedName("name") var name: String? = null,
        @SerializedName("quote") var quote: Quote? = null
    )

    data class Quote(@SerializedName("USD") var usd: USD? = null)

    data class USD(
        @SerializedName("price") var price: Float = 0f,
        @SerializedName("percent_change_24h") var percentChange24h: Float = 0f
    )
}