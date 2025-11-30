package com.babsnet.accounting.utils

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import java.time.ZoneId


object DateUtil {

    /**
     * Function to format a date string from one format to another.
     *
     * @param dateString The date string to format.
     * @return Formatted date string in "dd/MM/yyyy" format, or the original string if parsing fails.
     */
    fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {

            dateString
        }
    }

    fun formatDateFromDateObject(date: Date): String {
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return outputFormat.format(date)
    }

    /**
     * Converts a string to a Date object.
     * @param dateString The string to be converted, e.g., "2024-12-22".
     * @param format The expected format of the date string, e.g., "yyyy-MM-dd".
     * @return The corresponding Date object or null if parsing fails.
     */
    fun stringToDate(dateString: String, format: String = "yyyy-MM-dd"): Date? {
        return try {
            val formatter = SimpleDateFormat(format, Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if the parsing fails
        }
    }

    /**
     * Converts a Date object to a formatted string.
     * @param date The Date object to be formatted.
     * @param format The desired format for the date string (default: "yyyy-MM-dd").
     * @return The formatted date string.
     */
    fun dateToString(date: Date, format: String = "yyyy-MM-dd"): String {
        return try {
            val formatter = SimpleDateFormat(format, Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Shows a DatePickerDialog and sets the selected date to the provided EditText.
     * @param context The context where the DatePickerDialog will be shown.
     * @param editText The EditText where the selected date will be set.
     * @param dateFormat The desired date format (default: yyyy-MM-dd).
     */
    fun showDatePicker(context: Context, text: TextView, dateFormat: String = "yyyy-MM-dd") {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Set the selected date in the desired format
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
                text.text = formatter.format(selectedDate.time)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    @SuppressLint("NewApi")
    fun getStartAndEndOfWeek(): Pair<Long, Long> {
        val today = LocalDate.now()
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val endOfWeek = today.with(DayOfWeek.SUNDAY)

        val startOfWeekMillis = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfWeekMillis = endOfWeek.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return Pair(startOfWeekMillis, endOfWeekMillis)
    }

    @SuppressLint("NewApi")
    fun getStartAndEndOfMonth(year: Int, month: Int): Pair<Long, Long> {
        val startOfMonth = LocalDate.of(year, month, 1)
        val endOfMonth = startOfMonth.plusMonths(1).minusDays(1)

        val startOfMonthMillis = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonthMillis = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return Pair(startOfMonthMillis, endOfMonthMillis)
    }

    @SuppressLint("NewApi")
    fun getStartAndEndOfYear(year: Int): Pair<Long, Long> {
        val startOfYear = LocalDate.of(year, 1, 1)
        val endOfYear = LocalDate.of(year, 12, 31)

        val startOfYearMillis = startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfYearMillis = endOfYear.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return Pair(startOfYearMillis, endOfYearMillis)
    }
}
