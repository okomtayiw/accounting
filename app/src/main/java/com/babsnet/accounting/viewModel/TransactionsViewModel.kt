package com.babsnet.accounting.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.repository.JournalRepository

class TransactionsViewModel(private val repository: JournalRepository) : ViewModel() {

    private var _transactions: LiveData<List<TransactionData>> =
        repository.getAllTransactions().asLiveData()
    val transactions: LiveData<List<TransactionData>> get() = _transactions

    private var lastStart: Long? = null
    private var lastEnd: Long? = null
    private var lastAccountId: Int? = null

    fun getTransactionData(startDate: Long, endDate: Long, accountId: Int?): LiveData<List<TransactionData>> {
        if (startDate != lastStart || endDate != lastEnd || accountId != lastAccountId) {
            _transactions = repository.getTransactionsByAccount(startDate, endDate, accountId).asLiveData()
            lastStart = startDate
            lastEnd = endDate
            lastAccountId = accountId
        }
        return _transactions
    }
}
