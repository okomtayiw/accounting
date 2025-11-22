package com.babsnet.accounting

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.databinding.ActivityMainBinding
import com.babsnet.accounting.repository.AccountRepository
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var accountRepository: AccountRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
        }

        val accountDao = AppDatabase.getDatabase(applicationContext).accountDao()
        val ledgerDao: LedgerDao = AppDatabase.getDatabase(applicationContext).ledgerDao()
        accountRepository = AccountRepository(accountDao, ledgerDao)

        lifecycleScope.launch {
            accountRepository.insertDefaultAccounts(applicationContext)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)
        setupDrawerNavigation(navController)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })


    }


    private fun showExitConfirmationDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Keluar Aplikasi")
            .setMessage("Apakah Anda ingin keluar dari aplikasi?")
            .setPositiveButton("YA", null)
            .setNegativeButton("TIDAK", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.GRAY)
            positiveButton.setTextColor(primaryColor)
            negativeButton.setTextColor(primaryColor)

            // Callback
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
                R.id.nav_logout-> {
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    finishAffinity()
                    navController.navigate(R.id.nav_login_fragment)
                }

            }
            binding.drawerLayout.closeDrawer(GravityCompat.START) // Close drawer after selection
            true
        }
    }

}
