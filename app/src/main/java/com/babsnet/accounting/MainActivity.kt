package com.babsnet.accounting

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.databinding.ActivityMainBinding
import com.babsnet.accounting.repository.AccountRepository
import com.babsnet.accounting.utils.CurrencyFormatUtil
import com.babsnet.accounting.utils.CurrencyPreference
import com.babsnet.accounting.utils.LanguagePreference
import com.babsnet.accounting.utils.ThemePreference
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var accountRepository: AccountRepository

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguagePreference.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguagePreference.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        val db = AppDatabase.getDatabase(applicationContext)
        val accountDao = db.accountDao()
        val ledgerDao: LedgerDao = db.ledgerDao()
        accountRepository = AccountRepository(accountDao, ledgerDao)

        lifecycleScope.launch {
            accountRepository.insertDefaultAccounts(applicationContext)
        }

        // --- Navigation host + bottom nav ---
        val navHostFragment =
            supportFragmentManager.findFragmentById(
                R.id.nav_host_fragment_activity_main
            ) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)
        setupDrawerNavigation(navController)
        binding.tvDrawerVersion.text = getString(R.string.drawer_version_format, appVersionName())

        // --- Back press: konfirmasi keluar ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.exit_app_title))
            .setMessage(getString(R.string.exit_app_message))
            .setPositiveButton(getString(R.string.yes), null)
            .setNegativeButton(getString(R.string.no), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val primaryColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                Color.GRAY
            )
            positiveButton.setTextColor(primaryColor)
            negativeButton.setTextColor(primaryColor)

            positiveButton.setOnClickListener {
                finish()
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupDrawerNavigation(navController: NavController) {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_theme -> {
                    showThemeSelectionDialog()
                }
                R.id.nav_currency -> {
                    showCurrencySelectionDialog()
                }
                R.id.nav_language -> {
                    showLanguageSelectionDialog()
                }
                R.id.nav_logout -> {
                    supportFragmentManager.popBackStack(
                        null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    finishAffinity()
//                    navController.navigate(R.id.nav_login_fragment)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showThemeSelectionDialog() {
        val themeOptions = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )

        val currentSelection = when (ThemePreference.getSavedThemeMode(this)) {
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 0
        }

        var selectedIndex = currentSelection

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.choose_theme))
            .setSingleChoiceItems(themeOptions, currentSelection) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.apply_selection)) { _, _ ->
                val selectedMode = if (selectedIndex == 1) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                ThemePreference.saveThemeMode(this, selectedMode)
            }
            .show()
    }

    private fun showCurrencySelectionDialog() {
        val currencies = CurrencyFormatUtil.availableCurrencies(this)
        val currencyLabels = currencies.map { it.label }.toTypedArray()
        val currentCode = CurrencyPreference.getSavedCurrencyCode(this)
        val currentSelection = currencies.indexOfFirst { it.code == currentCode }.coerceAtLeast(0)
        var selectedIndex = currentSelection

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.choose_currency))
            .setSingleChoiceItems(currencyLabels, currentSelection) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.apply_selection)) { _, _ ->
                val selected = currencies.getOrNull(selectedIndex) ?: return@setPositiveButton
                CurrencyPreference.saveCurrencyCode(this, selected.code)
                recreate()
            }
            .show()
    }

    private fun showLanguageSelectionDialog() {
        val languageOptions = arrayOf(
            getString(R.string.language_system_default),
            getString(R.string.language_english),
            getString(R.string.language_indonesian)
        )
        val languageTags = listOf("", "en", "in")
        val currentLanguageTag = LanguagePreference.getSavedLanguageTag(this)
        val normalizedCurrentTag = if (currentLanguageTag == "id") "in" else currentLanguageTag
        val currentSelection = languageTags.indexOf(normalizedCurrentTag).coerceAtLeast(0)
        var selectedIndex = currentSelection

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.choose_language))
            .setSingleChoiceItems(languageOptions, currentSelection) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.apply_selection)) { _, _ ->
                val selectedTag = languageTags.getOrElse(selectedIndex) { "" }
                LanguagePreference.saveLanguageTag(this, selectedTag)
                restartForLanguageChange()
            }
            .show()
    }

    private fun restartForLanguageChange() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
    }

    private fun appVersionName(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName ?: "1.0.0"
    }
}
