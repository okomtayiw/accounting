package com.babsnet.accounting.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.babsnet.accounting.data.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: Account)

    @Query("SELECT * FROM account ORDER BY accountId DESC")
    fun getAllAccounts(): Flow<List<Account>>


    @Query("SELECT * FROM account WHERE accountId = :accountId")
    fun getAccountById(accountId: Int): Flow<Account?>

    @Delete
    suspend fun deleteAccount(account: Account)

    @Update
    suspend fun update(account: Account)


    @Query("SELECT * FROM account WHERE account_type = :accountType ORDER BY accountId DESC")
    fun getAccountsByTypeDesc(accountType: String): Flow<List<Account>>


    @Query("SELECT COUNT(*) FROM account")
    suspend fun countAccounts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>)

    @Query("SELECT * FROM account")
    suspend fun getAllAccountsSnapshot(): List<Account>


}
