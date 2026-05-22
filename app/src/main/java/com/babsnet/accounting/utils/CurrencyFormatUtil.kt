package com.babsnet.accounting.utils

import android.content.Context
import android.os.Build
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatUtil {

    data class CurrencyEntry(
        val code: String,
        val label: String
    )

    private fun currentLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            configuration.locale ?: Locale.getDefault()
        }
    }

    private fun resolveCurrency(context: Context): Currency {
        val code = CurrencyPreference.getSavedCurrencyCode(context)
        return runCatching { Currency.getInstance(code) }
            .getOrElse { Currency.getInstance(Locale.getDefault()) }
    }

    fun availableCurrencies(context: Context): List<CurrencyEntry> {
        val locale = currentLocale(context)
        return availableCurrencies(locale)
    }

    fun availableCurrencies(locale: Locale = Locale.getDefault()): List<CurrencyEntry> {
        return Currency.getAvailableCurrencies()
            .map { currency ->
                CurrencyEntry(
                    code = currency.currencyCode,
                    label = "${currency.currencyCode} - ${currency.getDisplayName(locale)} (${currency.symbol})"
                )
            }
            .sortedBy { it.code }
    }

    fun formatCurrency(
        context: Context,
        value: Number?,
        locale: Locale = currentLocale(context)
    ): String {
        if (value == null) return zeroCurrency(context, locale)

        val currency = resolveCurrency(context)
        val amount = value.toDouble()
        val numberFormatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = resolveFractionDigits(currency, amount)
            isGroupingUsed = true
        }
        val prefix = displayPrefix(currency, locale)
        return prefix + numberFormatter.format(amount)
    }

    fun formatSignedCurrency(
        context: Context,
        value: Double,
        locale: Locale = currentLocale(context)
    ): String {
        val formatted = formatCurrency(context, abs(value), locale)
        return if (value < 0) "-$formatted" else formatted
    }

    fun zeroCurrency(
        context: Context,
        locale: Locale = currentLocale(context)
    ): String = formatCurrency(context, 0, locale)

    private fun resolveFractionDigits(currency: Currency, amount: Double): Int {
        if (amount % 1.0 == 0.0) return 0
        val currencyDigits = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
        return currencyDigits.coerceIn(0, 2)
    }

    private fun displayPrefix(currency: Currency, locale: Locale): String {
        val symbol = currency.getSymbol(locale)
        return if (symbol.equals(currency.currencyCode, ignoreCase = true) || symbol.length > 4) {
            currency.currencyCode
        } else {
            symbol
        }
    }

    fun decimalDownFormatter(
        pattern: String = "#,###.##",
        locale: Locale = Locale("in", "ID")
    ): DecimalFormat {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        return DecimalFormat(pattern, symbols).apply {
            roundingMode = RoundingMode.DOWN
            isGroupingUsed = true
        }
    }

    /** Format Number to String */
    fun formatDecimalDown(
        value: Number?,
        pattern: String = "#,###.##",
        locale: Locale = Locale("in", "ID")
    ): String {
        if (value == null) return "0"
        return decimalDownFormatter(pattern, locale).format(value)
    }
}
