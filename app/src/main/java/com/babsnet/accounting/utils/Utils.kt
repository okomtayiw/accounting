package com.babsnet.accounting.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.itextpdf.kernel.colors.DeviceGray
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.BorderRadius
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
import java.math.RoundingMode
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

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }


    fun hideLoading(progressBar: ProgressBar) {
        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.GONE
        }, 3000)
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


            val primaryColor = MaterialColors.getColor(context, android.R.attr.color, Color.GRAY)
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
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        btnClose.setOnClickListener { dialog.dismiss() }

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

            //save file
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

    fun createLedgerPdf(context: Context, transactionData: List<TransactionData>) {
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
            val decimalFormat = DecimalFormat("#,###.##").apply {
                roundingMode = RoundingMode.DOWN
            }

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

    fun createPdfProfitAndLoss(
        context: Context,
        transactionData: List<TransactionData>
    ) {
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Profit_and_Loss_$dateTime.pdf"
        )

        try {
            val pdf = PdfDocument(PdfWriter(file))
            val doc = Document(pdf)
            doc.setMargins(24f, 20f, 24f, 20f)

            val lightBorder = DeviceRgb(200, 200, 200)
            val grayText = DeviceRgb(120, 120, 120)

            val df = DecimalFormat("#,###.##").apply { roundingMode = RoundingMode.DOWN }

            // ===== Filter data =====
            val incomes = transactionData.filter { it.accountType == "Income" }
            val expenses = transactionData.filter { it.accountType == "Expenses" }

            // ===== Total summary =====
            val totalIncome = incomes.sumOf { it.credit - it.debit }
            val totalExpense = expenses.sumOf { it.debit - it.credit }
            val netProfit = totalIncome - totalExpense

            // ===== Group per accountName (jadi tidak per transaksi) =====
            val incomeGrouped: List<Pair<String, Double>> = incomes
                .groupBy { it.accountName }
                .map { (name, list) -> name to list.sumOf { it.credit - it.debit } }
                .sortedBy { it.first.lowercase() }

            val expenseGrouped: List<Pair<String, Double>> = expenses
                .groupBy { it.accountName }
                .map { (name, list) -> name to list.sumOf { it.debit - it.credit } }
                .sortedBy { it.first.lowercase() }

            // ===== Title =====
            doc.add(
                Paragraph("Profit & Loss Preview")
                    .setBold()
                    .setFontSize(14f)
                    .setTextAlignment(TextAlignment.LEFT)
            )

            doc.add(Paragraph("\n").setFontSize(6f))

            // ===== Summary Box =====
            val summary = Table(UnitValue.createPercentArray(floatArrayOf(1f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setBorder(SolidBorder(lightBorder, 1f))

            fun summaryLine(text: String, bold: Boolean = false) {
                val p = Paragraph(text).setFontSize(11f)
                if (bold) p.setBold()
                summary.addCell(
                    Cell()
                        .add(p)
                        .setBorder(Border.NO_BORDER)
                        .setPaddingLeft(12f)
                        .setPaddingRight(12f)
                        .setPaddingTop(6f)
                        .setPaddingBottom(6f)
                )
            }

            summaryLine("Total Revenue: ${df.format(totalIncome)}")
            summaryLine("Total Expenses: ${df.format(totalExpense)}")
            summaryLine("Net Profit / Net Loss: ${df.format(netProfit)}", bold = true)

            doc.add(summary)
            doc.add(Paragraph("\n").setFontSize(10f))

            // ===== Helper: buat list table (2 kolom) =====
            fun addSection(title: String, rows: List<Pair<String, Double>>) {
                doc.add(
                    Paragraph(title)
                        .setBold()
                        .setFontSize(13f)
                )

                val table = Table(UnitValue.createPercentArray(floatArrayOf(7f, 3f)))
                    .setWidth(UnitValue.createPercentValue(100f))

                if (rows.isEmpty()) {
                    val emptyCell = Cell(1, 2)
                        .add(Paragraph("No data").setFontColor(grayText).setFontSize(10f))
                        .setBorder(Border.NO_BORDER)
                        .setPaddingTop(8f)
                        .setPaddingBottom(8f)
                    table.addCell(emptyCell)
                } else {
                    rows.forEach { (name, amount) ->
                        val left = Cell()
                            .add(Paragraph(name).setFontSize(12f))
                            .setBorder(Border.NO_BORDER)
                            .setPaddingTop(10f)
                            .setPaddingBottom(10f)
                            .setBorderBottom(SolidBorder(lightBorder, 0.5f))

                        val right = Cell()
                            .add(
                                Paragraph(df.format(amount))
                                    .setBold()
                                    .setFontSize(12f)
                                    .setTextAlignment(TextAlignment.RIGHT)
                            )
                            .setBorder(Border.NO_BORDER)
                            .setPaddingTop(10f)
                            .setPaddingBottom(10f)
                            .setBorderBottom(SolidBorder(lightBorder, 0.5f))

                        table.addCell(left)
                        table.addCell(right)
                    }
                }

                doc.add(table)
                doc.add(Paragraph("\n").setFontSize(8f))
            }

            // ===== Sections =====
            addSection("Income", incomeGrouped)
            addSection("Expenses", expenseGrouped)

            doc.close()

            Toast.makeText(
                context,
                "Profit & Loss PDF saved → ${file.path}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }




    fun createPdfBalanceSheet(context: Context, transactionData: List<TransactionData>) {

        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Balance_Sheet_$dateTime.pdf"
        )

        try {
            val pdf = PdfDocument(PdfWriter(file))
            val doc = Document(pdf)
            doc.setMargins(24f, 20f, 24f, 20f)

            val grayText = DeviceRgb(120, 120, 120)
            val lightBorder = DeviceRgb(200, 200, 200)

            val df = DecimalFormat("#,###.##").apply { roundingMode = RoundingMode.DOWN }


            val assets = transactionData.filter { it.accountType == "Assets" }.sumOf { it.debit - it.credit }
            val income = transactionData.filter { it.accountType == "Income" }.sumOf { it.credit - it.debit }
            val expenses = transactionData.filter { it.accountType == "Expenses" }.sumOf { it.debit - it.credit }
            val netIncomeLoss = income - expenses
            val totalBalanceAssets = assets


            doc.add(
                Paragraph("Balance Sheet Preview")
                    .setBold()
                    .setFontSize(14f)
                    .setTextAlignment(TextAlignment.LEFT)
            )

            doc.add(Paragraph("\n").setFontSize(6f))

            // ====== Summary Box ======
            val summary = Table(UnitValue.createPercentArray(floatArrayOf(1f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setBorder(SolidBorder(lightBorder, 1f))

            fun summaryLine(text: String, bold: Boolean = false) {
                val p = Paragraph(text).setFontSize(11f)
                if (bold) p.setBold()
                summary.addCell(
                    Cell()
                        .add(p)
                        .setBorder(Border.NO_BORDER)
                        .setPaddingLeft(12f)
                        .setPaddingRight(12f)
                        .setPaddingTop(6f)
                        .setPaddingBottom(6f)
                )
            }

            summaryLine("Assets: ${df.format(assets)}")
            summaryLine("Net Income / Loss: ${df.format(netIncomeLoss)}")
            summaryLine("Total Expenses: ${df.format(expenses)}")
            summaryLine("Total Balance (Assets): ${df.format(totalBalanceAssets)}", bold = true)

            doc.add(summary)

            doc.add(Paragraph("\n").setFontSize(10f))


            data class Row(val name: String, val type: String, val amount: Double)

            fun calcAmount(type: String, debit: Double, credit: Double): Double {
                return when (type) {
                    "Income" -> (credit - debit)
                    "Expenses", "Assets" -> (debit - credit)
                    else -> (debit - credit)
                }
            }

            val rows = transactionData
                .groupBy { it.accountName to it.accountType }
                .map { (k, list) ->
                    val (name, type) = k
                    val amt = list.sumOf { calcAmount(type, it.debit, it.credit) }
                    Row(name, type, amt)
                }

            val typeOrder = mapOf("Assets" to 0, "Expenses" to 1, "Income" to 2)
            val sortedRows = rows.sortedWith(
                compareBy<Row> { typeOrder[it.type] ?: 99 }.thenBy { it.name.lowercase() }
            )

            val listTable = Table(UnitValue.createPercentArray(floatArrayOf(7f, 3f)))
                .setWidth(UnitValue.createPercentValue(100f))

            sortedRows.forEach { r ->
                val left = Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPaddingTop(10f)
                    .setPaddingBottom(10f)

                left.add(
                    Paragraph(r.name)
                        .setBold()
                        .setFontSize(12f)
                )
                left.add(
                    Paragraph(r.type)
                        .setFontSize(9f)
                        .setFontColor(grayText)
                        .setMarginTop(2f)
                )

                val right = Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPaddingTop(10f)
                    .setPaddingBottom(10f)

                right.add(
                    Paragraph(df.format(r.amount))
                        .setBold()
                        .setFontSize(12f)
                        .setTextAlignment(TextAlignment.RIGHT)
                )

                left.setBorderBottom(SolidBorder(lightBorder, 0.5f))
                right.setBorderBottom(SolidBorder(lightBorder, 0.5f))

                listTable.addCell(left)
                listTable.addCell(right)
            }

            doc.add(listTable)

            doc.close()

            Toast.makeText(context, "Balance Sheet PDF saved → ${file.path}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error → ${e.message}", Toast.LENGTH_LONG).show()
        }
    }




    fun createLedgerPdfOneCategory(
        context: Context,
        transactionData: List<TransactionData>
    ) {


        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "journal_category_$dateTime.pdf"

        try {
            val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Gagal membuat file di Downloads")
                resolver.openOutputStream(uri) ?: throw IllegalStateException("Gagal membuka OutputStream")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                java.io.FileOutputStream(file)
            }

            val pdfWriter = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // ====== TITLE ======
            val title = Paragraph("Journal Report")
                .setBold()
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)

            val subTitle = Paragraph("Category: category")
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)

            document.add(title)
            document.add(subTitle)
            document.add(Paragraph(" ").setFontSize(6f))

            // ====== TABLE (No | Account | Description | Total) ======
            val columnWidths = floatArrayOf(1f, 3f, 4f, 2f)
            val table = Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100f))

            val headerBg = DeviceRgb(230, 230, 230)
            val accountBg = DeviceRgb(204, 255, 204)
            val descBg = DeviceRgb(255, 255, 204)
            val totalBg = DeviceRgb(204, 238, 255)

            fun header(text: String) =
                Cell().setBackgroundColor(headerBg).add(Paragraph(text).setBold())

            table.addHeaderCell(header("No"))
            table.addHeaderCell(header("Account"))
            table.addHeaderCell(header("Description"))
            table.addHeaderCell(header("Total"))

            val decimalFormat = DecimalFormat("#,###.##").apply { roundingMode = RoundingMode.DOWN }

            var totalDebit = 0.0

            transactionData.forEachIndexed { index, row ->

                val amount = row.debit

                table.addCell(Cell().add(Paragraph((index + 1).toString())))
                table.addCell(Cell().setBackgroundColor(accountBg).add(Paragraph(row.accountName)))
                table.addCell(Cell().setBackgroundColor(descBg).add(Paragraph(row.description)))
                table.addCell(
                    Cell()
                        .setBackgroundColor(totalBg)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .add(Paragraph(decimalFormat.format(amount)))
                )

                totalDebit += amount
            }

            document.add(table)
            document.add(Paragraph(" ").setFontSize(6f))

            // ====== TOTAL ======
            val totalText = Paragraph("Total: ${decimalFormat.format(totalDebit)}")
                .setBold()
                .setTextAlignment(TextAlignment.LEFT)

            document.add(totalText)

            document.close()
            outputStream.close()

            Toast.makeText(context, "PDF berhasil disimpan di Downloads: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun formatAmount(value: Double): String {
        return if (value % 1 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }




}