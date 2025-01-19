@file:Suppress("PackageName")

package com.babsnet.accounting.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.Ledger

import com.babsnet.accounting.repository.AccountRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    val allAccount = repository.allAccount.asLiveData()

    suspend fun getListAccount(accountType: String): List<Account> {
        return repository.getAccountsByTypeDesc(accountType).firstOrNull() ?: emptyList()
    }

    fun getAccountById(accountId: Int) = repository.getAccountById(accountId).asLiveData()


    fun insert(account: Account) {
        viewModelScope.launch {
            repository.insert(account)
        }
    }

    fun delete(account: Account) = viewModelScope.launch {
        repository.delete(account)
    }

    suspend fun checkLedgerByAccountId(account: Account): List<Ledger> {
        return repository.getLedgerByAccountId(account.accountId)
    }

    fun update(account: Account) {
        viewModelScope.launch {
            repository.update(account)
        }
    }

    suspend fun getAccountByIdAsync(accountId: Int): Account? {
        return repository.getAccountById(accountId).firstOrNull()
    }


}