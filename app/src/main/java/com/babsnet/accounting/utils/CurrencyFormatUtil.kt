package com.babsnet.accounting.utils

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatUtil {
    /** Formatter with "#,###.##" & rounding DOWN */
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