package com.babsnet.accounting.utils

import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*

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
    fun showDatePicker(context: Context, editText: EditText, dateFormat: String = "yyyy-MM-dd") {
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
                editText.setText(formatter.format(selectedDate.time))
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }
}
