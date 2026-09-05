package com.alingrin.cryptoprice3

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var thresholdEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        thresholdEditText = findViewById(R.id.thresholdEditText)
        val saveButton: android.widget.Button = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        thresholdEditText.setText(getNotificationThreshold(sharedPreferences).toString())
        saveButton.setOnClickListener { saveThreshold() }
    }

    private fun saveThreshold() {
        val threshold = thresholdEditText.text.toString().toFloatOrNull()
        if (threshold == null) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
        } else if (threshold <= 0) {
            Toast.makeText(this, "Threshold must be greater than 0", Toast.LENGTH_SHORT).show()
        } else {
            sharedPreferences.edit().putFloat(THRESHOLD_KEY, threshold).apply()
            Toast.makeText(this, "Threshold saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val PREFS_NAME = "CryptoPricePrefs"
        private const val THRESHOLD_KEY = "notification_threshold"

        @JvmStatic
        fun getNotificationThreshold(prefs: SharedPreferences): Float =
            prefs.getFloat(THRESHOLD_KEY, 3.0f)
    }
}