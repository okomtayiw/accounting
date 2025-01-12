package com.babsnet.accounting.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "ledger",
    foreignKeys = [
        ForeignKey(
            entity = Journal::class,
            parentColumns = ["journalId"],
            childColumns = ["journal_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["accountId"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["journal_id"]),
        Index(value = ["account_id"])
    ]
)
data class Ledger(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ledger_id")
    var ledgerId: Int = 0,

    @ColumnInfo(name = "journal_id")
    var journalId: Int,

    @ColumnInfo(name = "account_id")
    var accountId: Int,

    @ColumnInfo(name = "debit")
    var debit: Double = 0.0,

    @ColumnInfo(name = "credit")
    var credit: Double = 0.0,

    @ColumnInfo(name = "created_at")
    var createdAt: Date,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Date? = null,

    @ColumnInfo(name = "created_by")
    var createdBy: String? = null,

    @ColumnInfo(name = "updated_by")
    var updatedBy: String? = null
)
