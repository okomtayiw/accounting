package com.babsnet.accounting.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "account")
data class Account(
    @PrimaryKey(autoGenerate = true)
    var accountId: Int = 0,

    @ColumnInfo(name = "account_name")
    var accountName: String,

    @ColumnInfo(name = "account_type")
    var accountType: String,

    @ColumnInfo(name = "balance")
    var balance: Double = 0.0,

    @ColumnInfo(name = "created_at")
    var createdAt: Date? = null,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Date? = null,

    @ColumnInfo(name = "created_by")
    var createdBy: String? = null,
    @ColumnInfo(name = "updated_by")
    var updatedBy: String? = null
)

