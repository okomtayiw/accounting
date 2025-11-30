package com.babsnet.accounting.utils

import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.IconItem

object IconProvider {

    fun getIcons(): List<IconItem> {
        return listOf(
            IconItem(R.drawable.ic_house, "House", "General","ic_house"),
            IconItem(R.drawable.ic_food, "Eat & Drink", "Food","ic_food"),
            IconItem(R.drawable.ic_shopping, "Shopping", "Shopping", "ic_shopping"),
            IconItem(R.drawable.ic_gas, "Gasoline", "Transport","ic_gas"),
            IconItem(R.drawable.ic_store, "Market", "General","ic_market"),
            IconItem(R.drawable.ic_electricity, "Electricity", "Bills", "ic_electricity"),
            IconItem(R.drawable.ic_phone_call, "Phone Load", "Bills", "ic_phone"),
            IconItem(R.drawable.ic_school, "School", "Education", "ic_school"),
            IconItem(R.drawable.ic_credit_card, "Credit Card", "Finance", "ic_credit_card"),
            IconItem(R.drawable.ic_income, "Revenue", "Finance", "ic_income"),
            IconItem(R.drawable.ic_assurance, "Assurance", "Finance", "ic_assurance"),
            IconItem(R.drawable.ic_bank, "Bank", "Finance", "ic_bank"),
            IconItem(R.drawable.ic_stars, "Entertainment", "Entertainment", "ic_stars"),
            IconItem(R.drawable.ic_wallet, "Wallet", "Wallet", "ic_wallet"),
            IconItem(R.drawable.ic_credit, "Credit", "Credit", "ic_credit"),
            IconItem(R.drawable.ic_invoice, "Invoice", "Invoice", "ic_invoice")
        )
    }

}
