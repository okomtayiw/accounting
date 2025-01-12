package com.babsnet.accounting.data.dao

import com.babsnet.accounting.data.entity.Journal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal")
    fun getAllJournals(): Flow<List<Journal>>


    @Query("SELECT * FROM journal WHERE journalId = :journalId")
    fun getJournalById(journalId: Int): Flow<Journal?>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: Journal)

    @Transaction
    suspend fun insertJournalWithLedger(journal: Journal) {
        val journalId = insert(journal)
    }


    @Update
    suspend fun update(journal: Journal)


    @Delete
    suspend fun delete(journal: Journal)


    @Query("DELETE FROM journal")
    suspend fun deleteAll()
}

