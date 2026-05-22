package com.babsnet.accounting

import android.app.Application
import com.babsnet.accounting.utils.LanguagePreference
import com.babsnet.accounting.utils.ThemePreference

class AccountingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreference.applySavedTheme(this)
        LanguagePreference.applySavedLanguage(this)
    }
}
