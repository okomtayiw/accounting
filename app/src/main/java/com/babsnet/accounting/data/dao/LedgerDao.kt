package com.babsnet.accounting.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.babsnet.accounting.data.entity.Ledger


@Dao
interface LedgerDao {
    @Insert
    suspend fun insert(ledger: Ledger)

    @Query("SELECT * FROM ledger WHERE journal_id = :journalId")
    suspend fun getLedgerByJournal(journalId: Int): List<Ledger>
}
