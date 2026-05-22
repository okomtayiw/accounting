package com.babsnet.accounting.utils

import android.content.Context
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.IconItem

object IconProvider {

    fun getIcons(context: Context): List<IconItem> {
        return listOf(
            IconItem(R.drawable.ic_house, context.getString(R.string.icon_house), context.getString(R.string.icon_category_general), "ic_house"),
            IconItem(R.drawable.ic_food, context.getString(R.string.icon_eat_drink), context.getString(R.string.icon_category_food), "ic_food"),
            IconItem(R.drawable.ic_shopping, context.getString(R.string.icon_shopping), context.getString(R.string.icon_category_shopping), "ic_shopping"),
            IconItem(R.drawable.ic_gas, context.getString(R.string.icon_gasoline), context.getString(R.string.icon_category_transport), "ic_gas"),
            IconItem(R.drawable.ic_store, context.getString(R.string.icon_market), context.getString(R.string.icon_category_general), "ic_market"),
            IconItem(R.drawable.ic_electricity, context.getString(R.string.icon_electricity), context.getString(R.string.icon_category_bills), "ic_electricity"),
            IconItem(R.drawable.ic_phone_call, context.getString(R.string.icon_phone_load), context.getString(R.string.icon_category_bills), "ic_phone"),
            IconItem(R.drawable.ic_school, context.getString(R.string.icon_school), context.getString(R.string.icon_category_education), "ic_school"),
            IconItem(R.drawable.ic_credit_card, context.getString(R.string.icon_credit_card), context.getString(R.string.icon_category_finance), "ic_credit_card"),
            IconItem(R.drawable.ic_income, context.getString(R.string.icon_revenue), context.getString(R.string.icon_category_finance), "ic_income"),
            IconItem(R.drawable.ic_assurance, context.getString(R.string.icon_insurance_name), context.getString(R.string.icon_category_finance), "ic_assurance"),
            IconItem(R.drawable.ic_bank, context.getString(R.string.icon_bank_name), context.getString(R.string.icon_category_finance), "ic_bank"),
            IconItem(R.drawable.ic_stars, context.getString(R.string.icon_entertainment_name), context.getString(R.string.icon_category_entertainment), "ic_stars"),
            IconItem(R.drawable.ic_wallet, context.getString(R.string.icon_wallet_name), context.getString(R.string.icon_category_wallet), "ic_wallet"),
            IconItem(R.drawable.ic_credit, context.getString(R.string.icon_credit_name), context.getString(R.string.icon_category_credit), "ic_credit"),
            IconItem(R.drawable.ic_invoice, context.getString(R.string.icon_invoice_name), context.getString(R.string.icon_category_invoice), "ic_invoice")
        )
    }
}
