package com.babsnet.accounting.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.babsnet.accounting.data.entity.Ledger
import com.babsnet.accounting.data.entity.TransactionData
import kotlinx.coroutines.flow.Flow


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

    @Query("""
        SELECT 
            l.ledger_id AS ledgerId,
            j.date AS journalDate, 
            a.account_type AS accountType, 
            a.account_name AS accountName, 
            j.description AS description, 
            l.debit AS debit, 
            l.credit AS credit
        FROM ledger l
        INNER JOIN account a ON l.account_id = a.accountId
        INNER JOIN journal j ON l.journal_id = j.journalId
        ORDER BY j.date DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionData>>


}
