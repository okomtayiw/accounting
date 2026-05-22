package com.babsnet.accounting.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguagePreference {

    private const val PREFS_NAME = "app_preferences"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    private fun normalizeLanguageTag(languageTag: String): String {
        return when (languageTag) {
            "id" -> "in"
            else -> languageTag
        }
    }

    private fun applyLanguageTag(languageTag: String) {
        val normalizedTag = normalizeLanguageTag(languageTag)
        val locales = if (normalizedTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            Locale.setDefault(Locale.forLanguageTag(normalizedTag))
            LocaleListCompat.forLanguageTags(normalizedTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun wrapContext(context: Context): Context {
        val languageTag = normalizeLanguageTag(getSavedLanguageTag(context))
        if (languageTag.isBlank()) return context

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(android.os.LocaleList(locale))
        }

        return context.createConfigurationContext(configuration)
    }

    fun applySavedLanguage(context: Context) {
        applyLanguageTag(getSavedLanguageTag(context))
    }

    fun saveLanguageTag(context: Context, languageTag: String) {
        val normalizedTag = normalizeLanguageTag(languageTag)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, normalizedTag)
            .commit()
        applyLanguageTag(normalizedTag)
    }

    fun getSavedLanguageTag(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, "")
            .orEmpty()
    }
}
