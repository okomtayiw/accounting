package com.babsnet.accounting.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.repository.JournalRepository
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {


    val allJournals = repository.allJournals.asLiveData()


    fun getJournalById(journalId: Int) = repository.getJournalById(journalId).asLiveData()


    fun insert(journal: Journal) {
        viewModelScope.launch {
            repository.insert(journal)
        }
    }

    fun update(journal: Journal) = viewModelScope.launch {
        repository.update(journal)
    }

    fun delete(journal: Journal) = viewModelScope.launch {
        repository.delete(journal)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}
