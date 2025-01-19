package com.babsnet.accounting.data.entity

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation

data class JournalWithDetails(
    @Embedded
    val journal: Journal,

    @Relation(
        parentColumn = "journalId",
        entityColumn = "journal_id"
    )
    val ledgers: List<LedgerWithAccount>
)

@DatabaseView(
    viewName = "ledger_with_account",
    value = """
        SELECT 
            ledger.ledger_id AS ledgerId,
            ledger.journal_id AS journal_id, -- Sesuaikan nama kolom menjadi journal_id
            ledger.account_id AS accountId,
            ledger.debit AS ledgerDebit,
            ledger.credit AS ledgerCredit,
            ledger.created_at AS ledgerCreatedAt,
            ledger.updated_at AS ledgerUpdatedAt,
            account.account_name AS accountName,
            account.account_type AS accountType
        FROM ledger
        INNER JOIN account ON ledger.account_id = account.accountId
    """
)
data class LedgerWithAccount(
    val ledgerId: Int,
    val journal_id: Int,
    val accountId: Int,
    val ledgerDebit: Double,
    val ledgerCredit: Double,
    val ledgerCreatedAt: String?,
    val ledgerUpdatedAt: String?,
    val accountName: String,
    val accountType: String
)

