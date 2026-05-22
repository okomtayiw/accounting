package com.babsnet.accounting.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreference {

    private const val PREFS_NAME = "app_preferences"
    private const val KEY_THEME_MODE = "theme_mode"

    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getSavedThemeMode(context))
    }

    fun saveThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getSavedThemeMode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
