package com.babsnet.accounting.repository

import com.babsnet.accounting.data.entity.Journal
import com.babsnet.accounting.data.dao.JournalDao
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {

    // gatAllData
    val allJournals: Flow<List<Journal>> = journalDao.getAllJournals()

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
        journalDao.delete(journal)
    }

    suspend fun deleteAll() {
        journalDao.deleteAll()
    }
}
