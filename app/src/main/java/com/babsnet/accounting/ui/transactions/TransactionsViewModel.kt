package com.babsnet.accounting.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.repository.JournalRepository

class TransactionsViewModel(repository: JournalRepository) : ViewModel() {
    val allTransactions: LiveData<List<TransactionData>> = repository.getAllTransactions().asLiveData()
}