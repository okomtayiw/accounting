package com.babsnet.accounting.utils

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog

object Utils {


    fun showLoading(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
    }


    fun hideLoading(progressBar: ProgressBar) {
        progressBar.visibility = View.GONE
    }

    fun showDeleteConfirmationDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Delete Confirmation")
            .setMessage("Are you sure you want to delete this data?")
            .setPositiveButton("Yes") { _, _ ->
                // Callback when user confirms
                onConfirm()
            }
            .setNegativeButton("No") { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
            .create()
            .show()
    }
}