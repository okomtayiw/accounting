package com.babsnet.accounting.repository

import com.babsnet.accounting.data.dao.AccountDao
import com.babsnet.accounting.data.entity.Account
import kotlinx.coroutines.flow.Flow

data class AccountRepository(private val accountDao:AccountDao) {
    // gatAllData
    val allAccount: Flow<List<Account>> = accountDao.getAllAccounts()

    // Insert data
    suspend fun insert(account: Account) {
        accountDao.insert(account)
    }

    // gateDate based id
    fun getAccountById(accountId: Int): Flow<Account?> {
        return accountDao.getAccountById(accountId)
    }

    suspend fun delete(account: Account) {
        return accountDao.deleteAccount(account);
    }

    suspend fun update(account: Account) {
        return accountDao.update(account)
    }

}
