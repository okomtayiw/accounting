package com.babsnet.accounting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.databinding.ActivityMainBinding
import com.babsnet.accounting.repository.AccountRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var accountRepository: AccountRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }
}
