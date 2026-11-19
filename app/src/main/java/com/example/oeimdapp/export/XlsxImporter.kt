package com.example.oeimdapp.export

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ImportedRow(
    val labelOrCode: String,
    val counted: Int
)

data class ImportedTable(
    val title: String,
    val rows: List<ImportedRow>
)

data class ImportedReport(
    val storeName: String,
    val mdName: String,
    val dateStr: String,
    val reportId: String,
    val tables: List<ImportedTable>
)

object XlsxImporter {

    fun parseXlsx(context: Context, uri: Uri): ImportedReport? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val sheetXmlBytes = extractSheet1Xml(inputStream) ?: return null

            return parseSheetXml(sheetXmlBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun extractSheet1Xml(inputStream: InputStream): ByteArray? {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true)) {
                    return zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun parseSheetXml(xmlBytes: ByteArray): ImportedReport? {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xmlBytes.inputStream(), "UTF-8")
        }

        var eventType = parser.eventType
        var currentCellRef = ""
        var isValueTag = false
        var isTTag = false

        val cellValues = mutableMapOf<String, String>()
        val currentRowCells = mutableMapOf<String, String>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowCells.clear()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r").orEmpty()
                        }
                        "v" -> {
                            isValueTag = true
                        }
                        "t" -> {
                            isTTag = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.orEmpty()
                    if (text.isNotBlank() && currentCellRef.isNotBlank()) {
                        if (isValueTag || isTTag) {
                            currentRowCells[currentCellRef] = text.trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> isValueTag = false
                        "t" -> isTTag = false
                        "row" -> {
                            if (currentRowCells.isNotEmpty()) {
                                cellValues.putAll(currentRowCells)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val rowMap = mutableMapOf<Int, MutableMap<String, String>>()
        for ((cellRef, value) in cellValues) {
            val colLetter = cellRef.takeWhile { it.isLetter() }
            val rowNumStr = cellRef.dropWhile { it.isLetter() }
            val rowNum = rowNumStr.toIntOrNull() ?: continue
            rowMap.getOrPut(rowNum) { mutableMapOf() }[colLetter] = value
        }

        val sortedRowNums = rowMap.keys.sorted()
        if (sortedRowNums.isEmpty()) return null

        var storeName = ""
        var mdName = ""
        var dateStr = ""
        var reportId = ""
        var hasValidHeader = false

        val tables = mutableListOf<ImportedTable>()
        var currentTableTitle = ""
        val currentTableRows = mutableListOf<ImportedRow>()
        var isParsingTableData = false

        for (rowNum in sortedRowNums) {
            val cols = rowMap[rowNum] ?: continue
            val colA = cols["A"].orEmpty()
            val colB = cols["B"].orEmpty()
            val colC = cols["C"].orEmpty()
            val colD = cols["D"].orEmpty()
            val colE = cols["E"].orEmpty()

            // 1. Metadata check (Rows 1 to 5)
            if (colA.contains("LAPORAN PENGECEKAN STOK", ignoreCase = true)) {
                hasValidHeader = true
                continue
            }
            if (colA.startsWith("Toko", ignoreCase = true)) {
                storeName = extractValueAfterColon(colA)
                continue
            }
            if (colA.startsWith("Nama MD", ignoreCase = true)) {
                mdName = extractValueAfterColon(colA)
                continue
            }
            if (colA.startsWith("Tanggal cek", ignoreCase = true)) {
                dateStr = extractValueAfterColon(colA)
                continue
            }
            if (colA.startsWith("ID Laporan", ignoreCase = true)) {
                reportId = extractValueAfterColon(colA)
                continue
            }

            // 2. Table Header check ("Kode Produk" / "Jumlah Target" / "Alias_Jenis_Produk")
            if (colA.equals("Kode Produk", ignoreCase = true) ||
                colB.equals("Jumlah Target", ignoreCase = true) ||
                colA.contains("Alias_Jenis_Produk", ignoreCase = true)) {
                isParsingTableData = true
                continue
            }

            // 3. Table Title check (colA has text, colB-colE are blank)
            if (colA.isNotBlank() && colB.isBlank() && colC.isBlank() && colD.isBlank() && colE.isBlank()) {
                if (currentTableTitle.isNotBlank() && currentTableRows.isNotEmpty()) {
                    tables.add(ImportedTable(currentTableTitle, currentTableRows.toList()))
                    currentTableRows.clear()
                }
                currentTableTitle = colA
                isParsingTableData = false
                continue
            }

            // 4. Data Row check
            if (isParsingTableData) {
                if (colA.isNotBlank()) {
                    val countedNum = colC.toIntOrNull() ?: 0
                    currentTableRows.add(ImportedRow(labelOrCode = colA, counted = countedNum))
                }
            }
        }

        if (currentTableTitle.isNotBlank() && currentTableRows.isNotEmpty()) {
            tables.add(ImportedTable(currentTableTitle, currentTableRows.toList()))
        }

        if (!hasValidHeader || storeName.isBlank() || tables.isEmpty()) {
            return null
        }

        return ImportedReport(
            storeName = storeName,
            mdName = mdName,
            dateStr = dateStr,
            reportId = reportId,
            tables = tables
        )
    }

    private fun extractValueAfterColon(text: String): String {
        val idx = text.indexOf(':')
        if (idx != -1 && idx < text.length - 1) {
            return text.substring(idx + 1).trim()
        }
        return text.trim()
    }
}
