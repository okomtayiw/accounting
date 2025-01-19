package com.babsnet.accounting.data.entity

import java.util.Date

data class TransactionData(
    val ledgerId: Int,
    val journalDate: Date,
    val accountType: String,
    val accountName: String,
    val description: String,
    val debit: Double,
    val credit: Double
)
