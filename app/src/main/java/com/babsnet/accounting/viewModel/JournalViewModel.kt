package com.babsnet.accounting.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.data.entity.JournalWithDetails
import com.babsnet.accounting.repository.JournalRepository
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {


    val allJournalsWithDetails = liveData {
        emitSource(repository.getAllJournalsWithDetails().asLiveData())
    }

    fun getJournalsForWeek(startDate: Long, endDate: Long): LiveData<List<JournalWithDetails>> {
        return repository.getAllJournalsWeekly(startDate, endDate).asLiveData()
    }

    fun getJournalsForMonth(year: Long, month: Long): LiveData<List<JournalWithDetails>> {
        return repository.getAllJournalsMonthly(year, month).asLiveData()
    }

    fun getJournalsForYear(startDateYear: Long, endDateYear: Long): LiveData<List<JournalWithDetails>> {
        return repository.getAllJournalsYearly(startDateYear, endDateYear).asLiveData()
    }

    fun getJournalWithDetails(journalId: Int): LiveData<JournalWithDetails?> {
        return repository.getJournalByIdWithDetails(journalId).asLiveData()
    }


    suspend fun saveJournalLedger(
        journal: Journal,
        debit: Double,
        credit: Double,
        account: Account
    ): Journal? {
        return repository.saveJournalWithLedger(journal, debit, credit, account)
    }


    fun delete(journal: Journal) = viewModelScope.launch {
        repository.delete(journal)
    }


    suspend fun updateJournalLedger(
        existJournal: Journal,
        debit: Double,
        credit: Double,
        account: Account,
        existAccountId: Int?
    ): Journal {
        return repository.updateJournalLedger(
            existJournal,
            debit,
            credit,
            account,
            existAccountId)
    }
}
