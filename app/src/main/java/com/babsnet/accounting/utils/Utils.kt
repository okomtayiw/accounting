package com.babsnet.accounting.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
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
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
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
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {

    private fun showDownloadSuccessDialog(
        context: Context,
        fileLabel: String,
        fileName: String,
        mimeType: String,
        file: File? = null,
        fileUri: Uri? = null,
        locationLabel: String = context.getString(R.string.download_location_downloads)
    ) {
        val safeUri = fileUri ?: file?.let {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                it
            )
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.download_success_title))
            .setMessage(
                context.getString(
                    R.string.download_success_message,
                    fileLabel,
                    fileName,
                    locationLabel
                )
            )
            .setNegativeButton(context.getString(R.string.close), null)
            .setNeutralButton(context.getString(R.string.share)) { _, _ ->
                if (safeUri == null) {
                    Toast.makeText(context, context.getString(R.string.error_file_not_found_share), Toast.LENGTH_SHORT).show()
                    return@setNeutralButton
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, safeUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_file_chooser)))
            }
            .setPositiveButton(context.getString(R.string.view_file)) { _, _ ->
                if (safeUri == null) {
                    Toast.makeText(context, context.getString(R.string.error_file_not_found_open), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(safeUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(viewIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.error_no_viewer_app), Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }


    fun showLoading(progressBar: ProgressBar) {
        (progressBar.parent as? View)?.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }


    fun hideLoading(progressBar: ProgressBar) {
        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.GONE
            (progressBar.parent as? View)?.visibility = View.GONE
        }, 3000)
    }

    private fun currentLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    fun showDeleteConfirmationDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.delete_confirmation))
            .setMessage(context.getString(R.string.delete_confirmation_message))
            .setPositiveButton(context.getString(R.string.yes), null)
            .setNegativeButton(context.getString(R.string.no), null)
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
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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
            val sheet = workbook.createSheet(context.getString(R.string.export_sheet_transactions), 0)

            // Header
            val headers = listOf(
                context.getString(R.string.number_short),
                context.getString(R.string.date),
                context.getString(R.string.account_name),
                context.getString(R.string.description),
                context.getString(R.string.debit),
                context.getString(R.string.credit)
            )
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
            sheet.addCell(Label(3, totalRow, context.getString(R.string.total)))
            sheet.addCell(Label(4, totalRow, totalDebit.toString()))
            sheet.addCell(Label(5, totalRow, totalCredit.toString()))

            //save file
            workbook.write()
            workbook.close()

            showDownloadSuccessDialog(
                context = context,
                fileLabel = context.getString(R.string.file_label_excel),
                fileName = file.name,
                mimeType = "application/vnd.ms-excel",
                file = file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.failed_create_excel, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun downloadCSV(context: Context, transactionData: List<List<String>>) {
        val fileName = "transaction_ledger.csv"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            val writer = BufferedWriter(FileWriter(file))

            // Header
            writer.append(
                "${context.getString(R.string.number_short)}," +
                    "${context.getString(R.string.date)}," +
                    "${context.getString(R.string.account_name)}," +
                    "${context.getString(R.string.description)}," +
                    "${context.getString(R.string.debit)}," +
                    "${context.getString(R.string.credit)}\n"
            )

            // Data
            var totalDebit = 0.0
            var totalCredit = 0.0
            transactionData.forEachIndexed { index, row ->
                writer.append("${index + 1},${row[0]},${row[1]},${row[2]},${row[3]},${row[4]}\n")

                totalDebit += row[3].toDouble()
                totalCredit += row[4].toDouble()
            }

            writer.append(",,${context.getString(R.string.total)},,${totalDebit},${totalCredit}\n")

            writer.flush()
            writer.close()

            showDownloadSuccessDialog(
                context = context,
                fileLabel = context.getString(R.string.file_label_csv),
                fileName = file.name,
                mimeType = "text/csv",
                file = file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.failed_create_csv, e.message),
                Toast.LENGTH_LONG
            ).show()
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
            val title = Paragraph(context.getString(R.string.transaction_ledger_preview))
                .setBold()
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(title)

            val columnWidths = floatArrayOf(1f, 2f, 3f, 3f, 2f, 2f)
            val table = Table(UnitValue.createPercentArray(columnWidths))
            table.setWidth(UnitValue.createPercentValue(100f))

            val headers = listOf(
                context.getString(R.string.number_short),
                context.getString(R.string.date),
                context.getString(R.string.account_name),
                context.getString(R.string.description),
                context.getString(R.string.debit),
                context.getString(R.string.credit)
            )
            headers.forEach {
                table.addHeaderCell(Cell().add(Paragraph(it).setBold()))
            }

            var totalDebit = 0.0
            var totalCredit = 0.0

            transactionData.forEachIndexed { index, row ->
                table.addCell(Cell().add(Paragraph((index + 1).toString()))) // Nomor urut
                table.addCell(Cell().add(Paragraph(DateUtil.dateToString(row.journalDate)))) // Date
                table.addCell(Cell().add(Paragraph(AccountLocalizationUtil.localizeAccountName(context, row.accountName)))) // Account Name
                table.addCell(Cell().add(Paragraph(row.description))) // Description
                table.addCell(Cell().add(Paragraph(CurrencyFormatUtil.formatCurrency(context, row.debit)))) // Debit
                table.addCell(Cell().add(Paragraph(CurrencyFormatUtil.formatCurrency(context, row.credit)))) // Credit

                totalDebit += row.debit
                totalCredit += row.credit
            }

            table.addCell(Cell(1, 4).add(Paragraph(context.getString(R.string.total)).setBold())) // Gabungkan 3 kolom pertama
            table.addCell(Cell().add(Paragraph(CurrencyFormatUtil.formatCurrency(context, totalDebit)).setBold())) // Total Debit
            table.addCell(Cell().add(Paragraph(CurrencyFormatUtil.formatCurrency(context, totalCredit)).setBold())) // Total Credit

            document.add(table)

            document.close()

            showDownloadSuccessDialog(
                context = context,
                fileLabel = context.getString(R.string.transaction_ledger_preview),
                fileName = file.name,
                mimeType = "application/pdf",
                file = file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.failed_create_pdf, e.message), Toast.LENGTH_LONG).show()
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
            doc.setMargins(32f, 28f, 32f, 28f)

            val lightBorder = DeviceRgb(200, 200, 200)
            val grayText = DeviceRgb(120, 120, 120)

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
                Paragraph(context.getString(R.string.preview_laba_rugi))
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

            summaryLine(
                context.getString(
                    R.string.label_total_type,
                    AccountLocalizationUtil.localizeAccountType(context, "Income"),
                    CurrencyFormatUtil.formatCurrency(context, totalIncome)
                )
            )
            summaryLine(
                context.getString(
                    R.string.label_total_type,
                    AccountLocalizationUtil.localizeAccountType(context, "Expenses"),
                    CurrencyFormatUtil.formatCurrency(context, totalExpense)
                )
            )
            summaryLine(
                context.getString(
                    R.string.label_net_profit_loss,
                    CurrencyFormatUtil.formatSignedCurrency(context, netProfit)
                ),
                bold = true
            )

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
                        .add(Paragraph(context.getString(R.string.report_spotlight_default_title)).setFontColor(grayText).setFontSize(10f))
                        .setBorder(Border.NO_BORDER)
                        .setPaddingTop(8f)
                        .setPaddingBottom(8f)
                    table.addCell(emptyCell)
                } else {
                    rows.forEach { (name, amount) ->
                        val left = Cell()
                            .add(Paragraph(AccountLocalizationUtil.localizeAccountName(context, name)).setFontSize(12f))
                            .setBorder(Border.NO_BORDER)
                            .setPaddingTop(10f)
                            .setPaddingBottom(10f)
                            .setBorderBottom(SolidBorder(lightBorder, 0.5f))

                        val right = Cell()
                            .add(
                                Paragraph(CurrencyFormatUtil.formatCurrency(context, amount))
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
            addSection(context.getString(R.string.income), incomeGrouped)
            addSection(context.getString(R.string.expenses), expenseGrouped)

            doc.close()
            showDownloadSuccessDialog(
                context = context,
                fileLabel = context.getString(R.string.file_label_profit_loss_pdf),
                fileName = file.name,
                mimeType = "application/pdf",
                file = file
            )
            return


        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_prefix, e.message), Toast.LENGTH_LONG).show()
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

            val primaryText = DeviceRgb(34, 40, 49)
            val mutedText = DeviceRgb(110, 118, 129)
            val lightBorder = DeviceRgb(222, 226, 235)
            val sectionBackground = DeviceRgb(247, 248, 252)
            val totalBackground = DeviceRgb(232, 238, 255)
            val accentText = DeviceRgb(42, 91, 215)

            val locale = currentLocale(context)
            val periodFormatter = SimpleDateFormat("dd MMM yyyy", locale)
            val generatedFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", locale)

            fun formatAmount(value: Double): String =
                CurrencyFormatUtil.formatSignedCurrency(context, value)

            fun accountTypeLabel(type: String): String {
                return AccountLocalizationUtil.localizeAccountType(context, type)
            }


            val assets = transactionData.filter { it.accountType == "Assets" }.sumOf { it.debit - it.credit }
            val income = transactionData.filter { it.accountType == "Income" }.sumOf { it.credit - it.debit }
            val expenses = transactionData.filter { it.accountType == "Expenses" }.sumOf { it.debit - it.credit }
            val netIncomeLoss = income - expenses
            val totalBalanceAssets = assets
            val startDate = transactionData.minOfOrNull { it.journalDate }
            val endDate = transactionData.maxOfOrNull { it.journalDate }
            val periodText = if (startDate != null && endDate != null) {
                "${periodFormatter.format(startDate)} - ${periodFormatter.format(endDate)}"
            } else {
                context.getString(R.string.all_periods)
            }


            doc.add(
                Paragraph(context.getString(R.string.balance_sheet_title_pdf))
                    .setBold()
                    .setFontSize(18f)
                    .setFontColor(primaryText)
            )
            doc.add(
                Paragraph(context.getString(R.string.balance_sheet_period, periodText))
                    .setFontSize(10f)
                    .setFontColor(mutedText)
                    .setMarginTop(2f)
            )
            doc.add(
                Paragraph(context.getString(R.string.generated_at, generatedFormatter.format(Date())))
                    .setFontSize(10f)
                    .setFontColor(mutedText)
                    .setMarginTop(0f)
                    .setMarginBottom(16f)
            )

            doc.add(
                Paragraph(context.getString(R.string.main_summary))
                    .setBold()
                    .setFontSize(12f)
                    .setFontColor(primaryText)
                    .setMarginBottom(8f)
            )

            val summary = Table(UnitValue.createPercentArray(floatArrayOf(6f, 4f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setBorder(SolidBorder(lightBorder, 1f))

            fun addSummaryRow(label: String, value: String, highlight: Boolean = false) {
                val background = if (highlight) totalBackground else null

                val left = Cell()
                    .add(
                        Paragraph(label)
                            .setFontSize(11f)
                            .setFontColor(primaryText)
                            .setBold()
                    )
                    .setPadding(10f)
                    .setBorderBottom(SolidBorder(lightBorder, 0.75f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)

                val right = Cell()
                    .add(
                        Paragraph(value)
                            .setFontSize(11f)
                            .setFontColor(if (highlight) accentText else primaryText)
                            .setBold()
                            .setTextAlignment(TextAlignment.RIGHT)
                    )
                    .setPadding(10f)
                    .setBorderBottom(SolidBorder(lightBorder, 0.75f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)

                if (background != null) {
                    left.setBackgroundColor(background)
                    right.setBackgroundColor(background)
                }

                summary.addCell(left)
                summary.addCell(right)
            }

            addSummaryRow(context.getString(R.string.total_assets), formatAmount(assets))
            addSummaryRow(context.getString(R.string.net_profit_loss), formatAmount(netIncomeLoss))
            addSummaryRow(context.getString(R.string.total_expense), formatAmount(expenses))
            addSummaryRow(context.getString(R.string.balance_total), formatAmount(totalBalanceAssets), highlight = true)

            doc.add(summary)
            doc.add(
                Paragraph(context.getString(R.string.account_breakdown))
                    .setBold()
                    .setFontSize(12f)
                    .setFontColor(primaryText)
                    .setMarginTop(18f)
                    .setMarginBottom(10f)
            )


            data class Row(val name: String, val type: String, val amount: Double)

            fun calcAmount(type: String, debit: Double, credit: Double): Double {
                return when (type) {
                    "Income" -> credit - debit
                    "Expenses", "Assets" -> debit - credit
                    else -> debit - credit
                }
            }

            val rowsByType = transactionData
                .groupBy { it.accountName to it.accountType }
                .map { (key, list) ->
                    val (name, type) = key
                    Row(name, type, list.sumOf { calcAmount(type, it.debit, it.credit) })
                }
                .groupBy { it.type }

            val orderedTypes = listOf("Assets", "Expenses", "Income")

            orderedTypes.forEach { type ->
                val rows = rowsByType[type].orEmpty().sortedBy { it.name.lowercase() }
                if (rows.isEmpty()) return@forEach

                doc.add(
                    Paragraph(accountTypeLabel(type))
                        .setBold()
                        .setFontSize(11f)
                        .setFontColor(accentText)
                        .setBackgroundColor(sectionBackground)
                        .setPadding(8f)
                        .setMarginTop(8f)
                        .setMarginBottom(0f)
                )

                val table = Table(UnitValue.createPercentArray(floatArrayOf(7f, 3f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(10f)

                rows.forEachIndexed { index, row ->
                    val isLast = index == rows.lastIndex
                    val bottomBorder = if (isLast) Border.NO_BORDER else SolidBorder(lightBorder, 0.5f)

                    val left = Cell()
                        .add(
                            Paragraph(AccountLocalizationUtil.localizeAccountName(context, row.name))
                                .setFontSize(11f)
                                .setFontColor(primaryText)
                                .setBold()
                        )
                        .add(
                            Paragraph(context.getString(R.string.account_category_label, accountTypeLabel(row.type)))
                                .setFontSize(9f)
                                .setFontColor(mutedText)
                                .setMarginTop(2f)
                        )
                        .setPaddingTop(10f)
                        .setPaddingBottom(10f)
                        .setPaddingLeft(8f)
                        .setPaddingRight(8f)
                        .setBorderTop(Border.NO_BORDER)
                        .setBorderLeft(Border.NO_BORDER)
                        .setBorderRight(Border.NO_BORDER)
                        .setBorderBottom(bottomBorder)

                    val right = Cell()
                        .add(
                            Paragraph(formatAmount(row.amount))
                                .setFontSize(11f)
                                .setFontColor(primaryText)
                                .setBold()
                                .setTextAlignment(TextAlignment.RIGHT)
                        )
                        .setPaddingTop(10f)
                        .setPaddingBottom(10f)
                        .setPaddingLeft(8f)
                        .setPaddingRight(8f)
                        .setBorderTop(Border.NO_BORDER)
                        .setBorderLeft(Border.NO_BORDER)
                        .setBorderRight(Border.NO_BORDER)
                        .setBorderBottom(bottomBorder)

                    table.addCell(left)
                    table.addCell(right)
                }

                doc.add(table)
            }

            doc.add(
                Paragraph(context.getString(R.string.negative_balance_note))
                    .setFontSize(9f)
                    .setFontColor(mutedText)
                    .setMarginTop(8f)
            )

            doc.close()
            showDownloadSuccessDialog(
                context = context,
                fileLabel = context.getString(R.string.file_label_balance_sheet_pdf),
                fileName = file.name,
                mimeType = "application/pdf",
                file = file
            )
            return


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
        var savedUri: Uri? = null
        var savedFile: File? = null

        try {
            val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException(context.getString(R.string.error_create_download_file))
                savedUri = uri
                resolver.openOutputStream(uri)
                    ?: throw IllegalStateException(context.getString(R.string.error_open_output_stream))
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                savedFile = file
                java.io.FileOutputStream(file)
            }

            val pdfWriter = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // ====== TITLE ======
            val title = Paragraph(context.getString(R.string.journal_report_title))
                .setBold()
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)

            val subTitle = Paragraph(context.getString(R.string.journal_report_category, context.getString(R.string.category)))
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

            table.addHeaderCell(header(context.getString(R.string.number_short)))
            table.addHeaderCell(header(context.getString(R.string.account)))
            table.addHeaderCell(header(context.getString(R.string.description)))
            table.addHeaderCell(header(context.getString(R.string.total)))

            var totalDebit = 0.0

            transactionData.forEachIndexed { index, row ->

                val amount = row.debit

                table.addCell(Cell().add(Paragraph((index + 1).toString())))
                table.addCell(Cell().setBackgroundColor(accountBg).add(Paragraph(AccountLocalizationUtil.localizeAccountName(context, row.accountName))))
                table.addCell(Cell().setBackgroundColor(descBg).add(Paragraph(row.description)))
                table.addCell(
                    Cell()
                        .setBackgroundColor(totalBg)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .add(Paragraph(CurrencyFormatUtil.formatCurrency(context, amount)))
                )

                totalDebit += amount
            }

            document.add(table)
            document.add(Paragraph(" ").setFontSize(6f))

            // ====== TOTAL ======
            val totalText = Paragraph(
                context.getString(
                    R.string.label_total_amount,
                    CurrencyFormatUtil.formatCurrency(context, totalDebit)
                )
            )
                .setBold()
                .setTextAlignment(TextAlignment.LEFT)

            document.add(totalText)

            document.close()
            outputStream.close()

            showDownloadSuccessDialog(
                context = context,
                fileLabel = context.getString(R.string.category_preview),
                fileName = fileName,
                mimeType = "application/pdf",
                file = savedFile,
                fileUri = savedUri
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.failed_create_pdf, e.message), Toast.LENGTH_LONG).show()
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
