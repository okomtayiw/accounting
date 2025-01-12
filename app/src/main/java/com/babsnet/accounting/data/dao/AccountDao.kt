package com.babsnet.accounting.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
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


}
