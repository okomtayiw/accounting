package com.babsnet.accounting.repository

import android.content.Context
import com.babsnet.accounting.data.dao.AccountDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.Ledger
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.io.IOException
import java.util.Date

data class AccountRepository(private val accountDao:AccountDao,
                             private val ledgerDao: LedgerDao) {
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

    fun getAccountsByTypeDesc(accountType: String): Flow<List<Account>> {
        return accountDao.getAccountsByTypeDesc(accountType)
    }

    suspend fun getLedgerByAccountId(accountId: Int): List<Ledger> {
        return ledgerDao.getLedgerByAccountId(accountId)
    }


    suspend fun insertDefaultAccounts(context: Context) {
        if (accountDao.countAccounts() == 0) {
            val jsonString = loadJSONFromAsset(context, "accounts.json")
            val jsonArray = JSONArray(jsonString)
            val accounts = mutableListOf<Account>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val account = Account(
                    accountId = jsonObject.getInt("accountId"),
                    accountName = jsonObject.getString("accountName"),
                    accountType = jsonObject.getString("accountType"),
                    color = jsonObject.getString("icon_color"),
                    iconResName = jsonObject.getString("icon_res_name"),
                    balance = 0.0,
                    createdAt = Date(),
                    createdBy = "System"
                )
                accounts.add(account)
            }
            accountDao.insertAll(accounts)
        }
    }


    private fun loadJSONFromAsset(context: Context, fileName: String): String {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            ""
        }
    }

}
