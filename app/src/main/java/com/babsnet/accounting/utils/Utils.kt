package com.babsnet.accounting.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babsnet.accounting.R
import com.babsnet.accounting.adapter.AccountAdapterDialog
import com.babsnet.accounting.data.AppDatabase
import com.babsnet.accounting.data.entity.Account
import com.babsnet.accounting.data.entity.TransactionData
import com.babsnet.accounting.viewModel.AccountViewModel
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import jxl.Workbook
import jxl.write.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {


    fun showLoading(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE

    }


    fun hideLoading(progressBar: ProgressBar) {
        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.GONE
        }, 30000)
    }

    fun showDeleteConfirmationDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Delete Confirmation")
            .setMessage("Are you sure you want to delete this data?")
            .setPositiveButton("Yes", null)
            .setNegativeButton("No", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)


            val primaryColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.GRAY)
            positiveButton.setTextColor(primaryColor)
            negativeButton.setTextColor(primaryColor)

            // Callback
            positiveButton.setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }



    fun exportRoomDatabaseWithCheckpoint(context: Context): Boolean {
        return try {
            // Flush WAL
            val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
            db.close()

            // Path
            val dbFile = context.getDatabasePath("babs_system_accounting")
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFile = File(downloads, "babs_system_accounting-backup.db")

            dbFile.copyTo(backupFile, overwrite = true)
            Log.d("Backup", "Backup success to ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("Backup", "Error: ${e.message}", e)
            false
        }
    }


    fun showAccountSelectionDialog(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        accountViewModel: AccountViewModel,
        accountType: String,
        preloadedAccounts: List<Account>? = null,
        onAccountSelected: (Account) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_account, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewAccounts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            try {
                val accounts = preloadedAccounts ?: withContext(Dispatchers.IO) {
                    accountViewModel.getListAccount(accountType)
                }

                withContext(Dispatchers.Main) {
                    recyclerView.adapter = AccountAdapterDialog(accounts) { selectedAccount ->
                        onAccountSelected(selectedAccount)
                        dialog.dismiss()
                    }
                    dialog.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getStartAndEndOfCurrentWeek(): Pair<String, String> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = formatter.format(calendar.time)

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.add(Calendar.DATE, 7)
        val endOfWeek = formatter.format(calendar.time)

        return Pair(startOfWeek, endOfWeek)
    }

    @SuppressLint("NewApi")
    fun getStartAndEndOfCurrentWeekMillis(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startOfWeek = LocalDate.now()
            .with(DayOfWeek.MONDAY)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val endOfWeek = LocalDate.now()
            .with(DayOfWeek.SUNDAY)
            .atTime(LocalTime.MAX)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        return Pair(startOfWeek, endOfWeek)
    }

    @SuppressLint("NewApi")
    fun getStartAndEndOfCurrentMonth(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()

        val startOfMonth = now.withDayOfMonth(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
            .atTime(LocalTime.MAX)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        return Pair(startOfMonth, endOfMonth)
    }

    @SuppressLint("NewApi")
    fun getStartAndEndOfCurrentYear(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()

        val startOfYear = now.withDayOfYear(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val endOfYear = now.withDayOfYear(now.lengthOfYear())
            .atTime(LocalTime.MAX)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        return Pair(startOfYear, endOfYear)
    }

    private fun downloadExcelWithJExcel(context: Context, transactionData: List<List<String>>) {
        val fileName = "transaction_ledger.xls"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            val workbook = Workbook.createWorkbook(file)
            val sheet = workbook.createSheet("Transactions", 0)

            // Header
            val headers = listOf("No", "Date", "Account Name", "Description", "Debit", "Credit")
            headers.forEachIndexed { index, value ->
                sheet.addCell(Label(index, 0, value))
            }

            // Data
            var totalDebit = 0.0
            var totalCredit = 0.0
            transactionData.forEachIndexed { index, row ->
                sheet.addCell(Label(0, index + 1, (index + 1).toString())) // No
                sheet.addCell(Label(1, index + 1, row[0])) // Date
                sheet.addCell(Label(2, index + 1, row[1])) // Account Name
                sheet.addCell(Label(3, index + 1, row[2])) // Description
                sheet.addCell(Label(4, index + 1, row[3])) // Debit
                sheet.addCell(Label(5, index + 1, row[4])) // Credit

                totalDebit += row[3].toDouble()
                totalCredit += row[4].toDouble()
            }

            val totalRow = transactionData.size + 1
            sheet.addCell(Label(3, totalRow, "Total"))
            sheet.addCell(Label(4, totalRow, totalDebit.toString()))
            sheet.addCell(Label(5, totalRow, totalCredit.toString()))

            // Simpan File
            workbook.write()
            workbook.close()

            Toast.makeText(context, "Excel (XLS) saved in Download folder: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun downloadCSV(context: Context, transactionData: List<List<String>>) {
        val fileName = "transaction_ledger.csv"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            val writer = BufferedWriter(FileWriter(file))

            // Header
            writer.append("No,Date,Account Name,Description,Debit,Credit\n")

            // Data
            var totalDebit = 0.0
            var totalCredit = 0.0
            transactionData.forEachIndexed { index, row ->
                writer.append("${index + 1},${row[0]},${row[1]},${row[2]},${row[3]},${row[4]}\n")

                totalDebit += row[3].toDouble()
                totalCredit += row[4].toDouble()
            }

            writer.append(",,Total,,${totalDebit},${totalCredit}\n")

            writer.flush()
            writer.close()

            Toast.makeText(context, "CSV saved in Download folder: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun createPdf(context: Context, transactionData: List<TransactionData>) {
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "transaction_ledger_$dateTime.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            val title = Paragraph("Transaction Ledger")
                .setBold()
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(title)

            val columnWidths = floatArrayOf(1f, 2f, 3f, 3f, 2f, 2f)
            val table = Table(UnitValue.createPercentArray(columnWidths))
            table.setWidth(UnitValue.createPercentValue(100f))

            val headers = listOf("No", "Date", "Account Name", "Description", "Debit", "Credit")
            headers.forEach {
                table.addHeaderCell(Cell().add(Paragraph(it).setBold()))
            }

            var totalDebit = 0.0
            var totalCredit = 0.0
            val decimalFormat = DecimalFormat("#,###.00")

            transactionData.forEachIndexed { index, row ->
                table.addCell(Cell().add(Paragraph((index + 1).toString()))) // Nomor urut
                table.addCell(Cell().add(Paragraph(DateUtil.dateToString(row.journalDate)))) // Date
                table.addCell(Cell().add(Paragraph(row.accountName))) // Account Name
                table.addCell(Cell().add(Paragraph(row.description))) // Description
                table.addCell(Cell().add(Paragraph(decimalFormat.format(row.debit)))) // Debit
                table.addCell(Cell().add(Paragraph(decimalFormat.format(row.credit)))) // Credit

                totalDebit += row.debit
                totalCredit += row.credit
            }

            table.addCell(Cell(1, 4).add(Paragraph("Total").setBold())) // Gabungkan 3 kolom pertama
            table.addCell(Cell().add(Paragraph(decimalFormat.format(totalDebit)).setBold())) // Total Debit
            table.addCell(Cell().add(Paragraph(decimalFormat.format(totalCredit)).setBold())) // Total Credit

            document.add(table)

            document.close()

            Toast.makeText(context, "PDF saved in Download folder: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



}