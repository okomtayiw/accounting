package com.babsnet.accounting.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date


@Entity(tableName = "journal")
data class Journal(
    @PrimaryKey(autoGenerate = true)
    var journalId: Int = 0,

    @ColumnInfo(name = "date")
    var date: java.util.Date? = null,

    @ColumnInfo(name = "description")
    var description: String? = null,

    @ColumnInfo(name = "created_at")
    var createdAt: Date? = null,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Date? = null,

    @ColumnInfo(name = "created_by")
    var createdBy: String? = null,

    @ColumnInfo(name = "updated_by")
    var updatedBy: String? = null
)


