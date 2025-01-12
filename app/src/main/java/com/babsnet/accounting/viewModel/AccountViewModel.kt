@file:Suppress("PackageName")

package com.babsnet.accounting.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.babsnet.accounting.data.entity.Account

import com.babsnet.accounting.repository.AccountRepository
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    val allAccount = repository.allAccount.asLiveData()


    fun getAccountById(accountId: Int) = repository.getAccountById(accountId).asLiveData()


    fun insert(account: Account) {
        viewModelScope.launch {
            repository.insert(account)
        }
    }

    fun delete(account: Account) = viewModelScope.launch {
        repository.delete(account)
    }

    fun update(account: Account) {
        viewModelScope.launch {
            repository.update(account)
        }
    }


}