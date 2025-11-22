package com.babsnet.accounting.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.repository.JournalRepository

class TransactionsViewModel(private val repository: JournalRepository) : ViewModel() {

    val allTransactions: LiveData<List<TransactionData>> = repository.getAllTransactions().asLiveData()


    fun getTransactionData(startDate: Long, endDate: Long, accountId: Int?): LiveData<List<TransactionData>> {
        return repository.getTransactionsByAccount(startDate, endDate, accountId).asLiveData()
    }
}