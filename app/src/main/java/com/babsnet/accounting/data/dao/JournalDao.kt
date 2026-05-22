package com.babsnet.accounting.data.dao

import com.babsnet.accounting.data.entity.Journal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Transaction
import com.babsnet.accounting.data.entity.JournalWithDetails
import com.babsnet.accounting.data.entity.Ledger
import com.babsnet.accounting.data.entity.LedgerWithAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: Journal): Long

    @Transaction
    suspend fun insertJournalWithLedger(
        journal: Journal,
        ledgers: List<Ledger>
    ): Journal {
        val journalId: Int = if (journal.journalId != 0) {
            update(journal)
            journal.journalId
        } else {
            insert(journal).toInt()
        }

        if (ledgers.isNotEmpty() && journalId > 0) {
            for (ledger in ledgers) {
                ledger.journalId = journalId
                insertLedger(ledger)
            }
        }

        return getJournalByIdSync(journalId) ?: throw IllegalStateException("Journal not found")
    }

    @Query("SELECT * FROM journal WHERE journalId = :journalId LIMIT 1")
    suspend fun getJournalByIdSync(journalId: Int): Journal?

    @Query("DELETE FROM ledger WHERE journal_id = :journalId")
    suspend fun deleteLedgersByJournalId(journalId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: Ledger)

    @Transaction
    @Query("""
        SELECT * FROM journal
        ORDER BY created_at DESC
    """)
    fun getAllJournals(): Flow<List<Journal>>

    @Transaction
    @Query("""
        SELECT * FROM journal
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY created_at DESC
    """)
    fun getJournalsWithinDateRange(startDate: Long, endDate: Long): Flow<List<Journal>>

    @Transaction
    @Query("""
    SELECT * FROM journal
    WHERE date BETWEEN :year AND :month
    ORDER BY created_at DESC
    """)
    fun getJournalsWithMonthly(year: Long, month: Long): Flow<List<Journal>>

    @Transaction
    @Query("""
    SELECT * FROM journal
    WHERE date BETWEEN :startDateYear AND :endDateYear
    ORDER BY created_at DESC
    """)
    fun getJournalsWithinYear(startDateYear: Long, endDateYear: Long): Flow<List<Journal>>


    @Query("SELECT * FROM ledger_with_account WHERE journal_id = :journalId")
    suspend fun getLedgersWithAccountByJournalId(journalId: Int): List<LedgerWithAccount>

    @Query("SELECT * FROM journal WHERE journalId = :journalId")
    fun getJournalById(journalId: Int): Flow<Journal?>

    @Update
    suspend fun update(journal: Journal) // Tidak boleh nullable!

    @Delete
    suspend fun delete(journal: Journal)

    @Query("DELETE FROM journal")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgers(ledgers: List<Ledger>)


    @Query("SELECT * FROM ledger WHERE journal_id = :journalId")
    suspend fun getLedgersByJournalId(journalId: Int): List<Ledger> // Tidak boleh nullable!


    @Transaction
    @Query("""
    SELECT * FROM journal j
    WHERE j.date BETWEEN :startDate AND :endDate
      AND (
        :query = ''
        OR CAST(j.journalId AS TEXT) LIKE '%' || :query || '%'
        OR j.description LIKE '%' || :query || '%' COLLATE NOCASE
        OR EXISTS (
            SELECT 1 FROM ledger_with_account lwa
            WHERE lwa.journal_id = j.journalId
              AND (
                lwa.accountName LIKE '%' || :query || '%' COLLATE NOCASE
                OR lwa.accountType LIKE '%' || :query || '%' COLLATE NOCASE
                OR CAST(lwa.ledgerDebit AS TEXT) LIKE '%' || :query || '%'
                OR CAST(lwa.ledgerCredit AS TEXT) LIKE '%' || :query || '%'
              )
        )
      )
    ORDER BY j.created_at DESC
    """)
    fun searchJournalsWithDetailsWithinDateRange(
        startDate: Long,
        endDate: Long,
        query: String
    ): Flow<List<JournalWithDetails>>

}



