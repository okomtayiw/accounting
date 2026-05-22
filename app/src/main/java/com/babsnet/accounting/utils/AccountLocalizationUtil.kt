package com.babsnet.accounting.utils

import android.content.Context
import com.babsnet.accounting.R
import com.babsnet.accounting.data.entity.Account

object AccountLocalizationUtil {

    private val accountNameMap = mapOf(
        "Kas" to R.string.account_cash,
        "Bank" to R.string.account_bank,
        "Belanja Harian" to R.string.account_daily_shopping,
        "Sewa" to R.string.account_rent,
        "Tagihan Listrik" to R.string.account_electricity_bill,
        "Tagihan Air" to R.string.account_water_bill,
        "Biaya Internet & Telepon" to R.string.account_internet_phone,
        "Asuransi" to R.string.account_insurance,
        "Cicilan Kendaraan" to R.string.account_vehicle_installment,
        "Hiburan" to R.string.account_entertainment,
        "Pendidikan" to R.string.account_education,
        "Pendapatan" to R.string.account_income,
        "Ledger" to R.string.report_option_ledger,
        "Laba Rugi" to R.string.report_option_profit_loss,
        "Neraca" to R.string.report_option_balance_sheet
    )

    fun localizeAccountName(context: Context, rawName: String?): String {
        if (rawName.isNullOrBlank()) return ""
        val resId = accountNameMap[rawName]
        return if (resId != null) context.getString(resId) else rawName
    }

    fun localizeAccountName(context: Context, account: Account): String {
        return localizeAccountName(context, account.accountName)
    }

    fun localizeAccountType(context: Context, rawType: String?): String {
        return when (rawType) {
            "Assets" -> context.getString(R.string.assets)
            "Expenses" -> context.getString(R.string.expenses)
            "Income" -> context.getString(R.string.income)
            "Report" -> context.getString(R.string.report)
            else -> rawType.orEmpty()
        }
    }

    fun matchesAccountQuery(context: Context, account: Account, query: String): Boolean {
        val keyword = query.trim()
        if (keyword.isEmpty()) return true

        return account.accountName.contains(keyword, ignoreCase = true) ||
            localizeAccountName(context, account).contains(keyword, ignoreCase = true) ||
            account.accountType.contains(keyword, ignoreCase = true) ||
            localizeAccountType(context, account.accountType).contains(keyword, ignoreCase = true)
    }
}
