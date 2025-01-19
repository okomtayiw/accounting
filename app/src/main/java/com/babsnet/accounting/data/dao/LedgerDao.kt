package com.babsnet.accounting.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.babsnet.accounting.data.entity.Ledger


@Dao
interface LedgerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: Ledger)

    @Query("SELECT * FROM ledger WHERE journal_id = :journalId AND account_id = :accountId")
    suspend fun getLedgerByJournalIdAndAccountId(journalId: Int, accountId: Int) : Ledger

    @Update
    suspend fun updateLedger(ledger: Ledger): Int

    @Query("DELETE FROM ledger WHERE journal_id = :journalId")
    suspend fun deleteAllLedgersByJournal(journalId: Int): Int

    @Query("SELECT * FROM ledger WHERE account_id = :accountId")
    suspend fun getLedgerByAccountId(accountId: Int): List<Ledger>

}
