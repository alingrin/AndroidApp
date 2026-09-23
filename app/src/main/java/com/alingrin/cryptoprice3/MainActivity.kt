package com.alingrin.cryptoprice3

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var marketChartApiClient: MarketChartApiClient
    private lateinit var notificationService: PriceNotificationService
    private var isBTCNameClicked = true
    private var lastBitcoinPrice = 0.0
    private var lastEthereumPrice = 0.0
    private var secondsUntilRefresh = REFRESH_SECONDS

    private val runnable = object : Runnable {
        override fun run() {
            startRefreshCountdown()
            fetchDataWithMyApiClient()
            handler.postDelayed(this, INTERVAL)
        }
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            secondsUntilRefresh = (secondsUntilRefresh - 1).coerceAtLeast(0)
            updateRefreshCountdown()
            handler.postDelayed(this, SECOND)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateRefreshCountdown()
        selectButton(R.id.MonthButton)
        requestNotificationPermission()
        sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        showCachedPrices()
        marketChartApiClient = MarketChartApiClient(this)
        notificationService = PriceNotificationService(this)
        fetchDataForBitcoin(30, "daily")
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener { onSettingsButtonClick(it) }
        handler.post(runnable)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) Log.d(TAG, "FCM Registration Token: ${task.result}")
        }
    }

    private fun showCachedPrices() {
        findViewById<TextView>(R.id.BitconPriceName).text = sharedPreferences.getString("btcName", "Bitcoin")
        findViewById<TextView>(R.id.EthPriceName).text = sharedPreferences.getString("ethName", "Ethereum")
        findViewById<TextView>(R.id.BTCPrice).text = "$$${sharedPreferences.getFloat("btcPrice", 0f)}".replace("$$", "$")
        findViewById<TextView>(R.id.ETHPrice).text = "$$${sharedPreferences.getFloat("ethPrice", 0f)}".replace("$$", "$")
        setChangeDisplay(R.id.BTC24PercentChange, sharedPreferences.getFloat("btc24Change", 0f).toDouble())
        setChangeDisplay(R.id.ETH24PercentChange, sharedPreferences.getFloat("eth24Change", 0f).toDouble())
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        handler.removeCallbacks(countdownRunnable)
        super.onDestroy()
    }

    private fun startRefreshCountdown() {
        secondsUntilRefresh = REFRESH_SECONDS
        updateRefreshCountdown()
        handler.removeCallbacks(countdownRunnable)
        handler.postDelayed(countdownRunnable, SECOND)
    }

    private fun updateRefreshCountdown() {
        findViewById<TextView>(R.id.priceRefreshCountdown).text =
            getString(R.string.price_refresh_countdown, secondsUntilRefresh)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && !::notificationService.isInitialized) {
            notificationService = PriceNotificationService(this)
        }
    }

    fun onBTCNameBtnClick(view: View) { isBTCNameClicked = true; selectRange(30, "daily"); fetchDataForBitcoin(30, "daily") }
    fun onETHNameBtnClick(view: View) { isBTCNameClicked = false; selectRange(30, "daily"); fetchDataForEthereum(30, "daily") }
    fun onDayButtonClick(view: View) { selectButton(R.id.DayButton); fetchSelectedRange(1, "") }
    fun onWeekButtonClick(view: View) { selectButton(R.id.WeekButton); fetchSelectedRange(7, "daily") }
    fun onMonthButtonClick(view: View) { selectButton(R.id.MonthButton); fetchSelectedRange(30, "daily") }
    fun onYearButtonClick(view: View) { selectButton(R.id.YearButton); fetchSelectedRange(365, "daily") }

    private fun selectRange(days: Int, interval: String) { selectButton(R.id.MonthButton) }
    private fun selectButton(selectedId: Int) {
        listOf(R.id.DayButton, R.id.WeekButton, R.id.MonthButton, R.id.YearButton).forEach { id ->
            findViewById<Button>(id).setBackgroundColor(if (id == selectedId) Color.RED else getResources().getColor(R.color.purple))
        }
    }
    private fun fetchSelectedRange(days: Int, interval: String) {
        if (isBTCNameClicked) fetchDataForBitcoin(days, interval) else fetchDataForEthereum(days, interval)
    }

    private fun fetchDataForBitcoin(days: Int, interval: String) = fetchChart(days, interval, true)
    private fun fetchDataForEthereum(days: Int, interval: String) = fetchChart(days, interval, false)

    private fun fetchChart(days: Int, interval: String, bitcoin: Boolean) {
        val callback = object : ChartCallback {
            override fun onDataReceived(data: MarketChartApiResponseModel, code: Int) {
                val points = data.prices.orEmpty().mapNotNull { values ->
                    if (values.size < 2) null else LineGraphView.DataPoint(
                        Instant.ofEpochMilli(round(values[0]).toLong()).atZone(ZoneId.of("EST")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy")), values[1]
                    )
                }
                findViewById<LineGraphView>(R.id.lineGraphView).apply {
                    setDataPoints(points)
                    setCoinName(if (bitcoin) "Bitcoin" else "Ethereum")
                }
            }
            override fun onFailure(t: Throwable) { Log.e(TAG, "Chart request failed", t) }
        }
        if (bitcoin) marketChartApiClient.fetchDataAsyncBTC(callback, days, interval)
        else marketChartApiClient.fetchDataAsyncEthereum(callback, days, interval)
    }

    fun fetchDataWithMyApiClient() {
        MyApiClient().fetchDataAsync(object : MyCallback {
            override fun onDataReceived(data: QuoteLatestResponseModel, code: Int) {
                runOnUiThread { findViewById<TextView>(R.id.priceErrorText).visibility = View.GONE }
                val bitcoin = data.data?.get("1") ?: return
                val ethereum = data.data?.get("1027") ?: return
                updateCoin(bitcoin, R.id.BitconPriceName, R.id.BTCPrice, R.id.BTC24PercentChange, "btc")
                updateCoin(ethereum, R.id.EthPriceName, R.id.ETHPrice, R.id.ETH24PercentChange, "eth")
            }
            override fun onFailure(t: Throwable) {
                Log.e(TAG, "Price request failed", t)
                runOnUiThread {
                    findViewById<TextView>(R.id.priceErrorText).apply {
                        text = t.message ?: getString(R.string.price_update_error)
                        visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun updateCoin(coin: QuoteLatestResponseModel.CryptoCurrency, nameId: Int, priceId: Int, changeId: Int, key: String) {
        val quote = coin.quote?.usd ?: return
        val price = (round(quote.price * 100) / 100).toDouble()
        val change = (round(quote.percentChange24h * 100) / 100).toDouble()
        findViewById<TextView>(nameId).text = coin.name
        animatePriceChange(price.toFloat(), sharedPreferences.getFloat("${key}Price", price.toFloat()), findViewById(priceId))
        setChangeDisplay(changeId, change)
        sharedPreferences.edit().putString("${key}Name", coin.name).putFloat("${key}Price", price.toFloat()).putFloat("${key}24Change", change.toFloat()).apply()
        if (key == "btc") { notificationService.checkPriceChange("bitcoin", price, lastBitcoinPrice); lastBitcoinPrice = price }
        else { notificationService.checkPriceChange("ethereum", price, lastEthereumPrice); lastEthereumPrice = price }
    }

    private fun animatePriceChange(finalPrice: Float, initialPrice: Float, view: TextView) {
        ValueAnimator.ofFloat(initialPrice, finalPrice).apply {
            duration = 2000
            addUpdateListener {
                view.text = String.format(Locale.US, "$%.2f", it.animatedValue as Float)
                view.setTextColor(
                    when {
                        finalPrice > initialPrice -> Color.GREEN
                        finalPrice < initialPrice -> Color.RED
                        else -> view.currentTextColor
                    }
                )
            }
            start()
        }
    }

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.2f%%", value)

    private fun setChangeDisplay(viewId: Int, value: Double) {
        findViewById<TextView>(viewId).apply {
            text = formatPercent(value)
            if (value > 0) setTextColor(Color.GREEN)
            else if (value < 0) setTextColor(Color.RED)
        }
    }

    fun onSettingsButtonClick(view: View) { startActivity(Intent(this, SettingsActivity::class.java)) }

    companion object {
        private const val TAG = "MainActivity"
        private const val INTERVAL = 60_000L
        private const val REFRESH_SECONDS = 60
        private const val SECOND = 1_000L
    }
}