package com.babsnet.accounting.utils

import android.content.Context
import java.util.Currency
import java.util.Locale

object CurrencyPreference {

    private const val PREFS_NAME = "app_preferences"
    private const val KEY_CURRENCY_CODE = "currency_code"

    fun getSavedCurrencyCode(context: Context): String {
        val fallback = runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("USD")

        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENCY_CODE, fallback)
            ?: fallback
    }

    fun saveCurrencyCode(context: Context, currencyCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENCY_CODE, currencyCode)
            .apply()
    }
}
