package com.babsnet.accounting.repository

import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.data.dao.JournalDao
import com.babsnet.accounting.data.dao.LedgerDao
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.JournalWithDetails
import com.babsnet.accounting.data.entity.Ledger
import com.babsnet.accounting.data.entity.TransactionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Date

class JournalRepository(
    private val journalDao: JournalDao,
    private val ledgerDao: LedgerDao
)
{


    fun getAllJournalsWithDetails(): Flow<List<JournalWithDetails>> {
        return journalDao.getAllJournals().map { journals ->
            journals.map { journal ->
                val ledgers = runBlocking {
                    journalDao.getLedgersWithAccountByJournalId(journal.journalId)
                }
                JournalWithDetails(journal, ledgers)
            }
        }
    }

    fun getAllJournalsWeekly(startDate: Long, endDate: Long): Flow<List<JournalWithDetails>> {
        return journalDao.getJournalsWithinDateRange(startDate, endDate).map { journals ->
            journals.map { journal ->
                val ledgers = runBlocking {
                    journalDao.getLedgersWithAccountByJournalId(journal.journalId)
                }
                JournalWithDetails(journal, ledgers)
            }
        }
    }

    fun getAllJournalsMonthly(year: Long, month: Long): Flow<List<JournalWithDetails>> {
        return journalDao.getJournalsWithMonthly(year, month).map { journals ->
            journals.map { journal ->
                val ledgers = runBlocking {
                    journalDao.getLedgersWithAccountByJournalId(journal.journalId)
                }
                JournalWithDetails(journal, ledgers)
            }
        }
    }

    fun getAllJournalsYearly(starDateYear: Long, endDateYear: Long): Flow<List<JournalWithDetails>> {
        return journalDao.getJournalsWithinYear(starDateYear, endDateYear).map { journals ->
            journals.map { journal ->
                val ledgers = runBlocking {
                    journalDao.getLedgersWithAccountByJournalId(journal.journalId)
                }
                JournalWithDetails(journal, ledgers)
            }
        }
    }

    fun getJournalByIdWithDetails(journalId: Int): Flow<JournalWithDetails?> {
        return journalDao.getJournalById(journalId).map { journal ->
            if (journal != null) {
                val ledgers = runBlocking {
                    journalDao.getLedgersWithAccountByJournalId(journal.journalId)
                }
                JournalWithDetails(journal, ledgers)
            } else {
                null
            }
        }
    }


    // gateDate based id
    fun getJournalById(journalId: Int): Flow<Journal?> {
        return journalDao.getJournalById(journalId)
    }

    // Insert data
    suspend fun insert(journal: Journal) {
        journalDao.insert(journal)
    }

    suspend fun update(journal: Journal) {
        journalDao.update(journal)
    }

    suspend fun delete(journal: Journal) {
        val deleteLedger : Int = ledgerDao.deleteAllLedgersByJournal(journal.journalId)
        if (deleteLedger != 0 ){
            journalDao.delete(journal)
        }
    }

    suspend fun deleteAll() {
        journalDao.deleteAll()
    }

    suspend fun saveJournalWithLedger(
        journal: Journal,
        debit: Double,
        credit: Double,
        account: Account
    ): Journal? {
        val ledgers = mutableListOf<Ledger>()
        val ledger = Ledger(
            journalId = 0,
            accountId = account.accountId,
            debit = debit,
            credit = credit,
            createdAt = Date(),
            updatedAt = null,
            createdBy = "User",
            updatedBy = null
        )

        ledgers.add(ledger)

        return journalDao.insertJournalWithLedger(journal, ledgers)
    }


    suspend fun updateJournalWithLedger(
        journal: Journal,
        accountIds: List<Int?>,
        debit: Double,
        credit: Double
    ) {

        if (accountIds.isEmpty()) {
            throw IllegalArgumentException("Account IDs is empty")
        }


    }

    suspend fun updateJournalLedger(
        existJournal: Journal,
        debit: Double,
        credit: Double,
        account: Account,
        existAccountId: Int?
    ): Journal {
        journalDao.update(existJournal)
        val existingLedgers = journalDao.getLedgersByJournalId(existJournal.journalId)
        if(existingLedgers.isNotEmpty()) {
            val existingLedger = ledgerDao.getLedgerByJournalIdAndAccountId(existJournal.journalId, existAccountId!!)
            existingLedger.accountId = account.accountId
            existingLedger.updatedBy = "User"
            existingLedger.debit = debit
            existingLedger.credit = credit
            existingLedger.updatedAt = Date()
            ledgerDao.updateLedger(existingLedger)
        }
        return existJournal
    }

    fun getAllTransactions(): Flow<List<TransactionData>> {
        return ledgerDao.getAllTransactions()
    }

    fun getTransactionsByAccount(startDate: Long, endDate: Long, accountId: Int?): Flow<List<TransactionData>> {
        return if (accountId == null || accountId == 0 || accountId == -1 || accountId == -2 || accountId == -3) {
            ledgerDao.getTransactionsBetweenDates(startDate, endDate)
        } else {
            ledgerDao.getTransactionsBetweenDatesWitAccountId(startDate, endDate, accountId)
        }
    }

    fun searchJournalsByRange(start: Long, end: Long, query: String) =
        journalDao.searchJournalsWithDetailsWithinDateRange(start, end, query)
}
