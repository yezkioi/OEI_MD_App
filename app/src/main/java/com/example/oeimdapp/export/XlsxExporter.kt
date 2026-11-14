package com.example.oeimdapp.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.oeimdapp.data.model.ListDetailDto
import com.example.oeimdapp.data.model.StoreDto
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportListStatus {
    BELUM_DIBUKA,
    SEMUA_NOL,
    TERISI
}

data class ExportListSummary(
    val listId: String,
    val listName: String,
    val status: ExportListStatus,
    val filledCount: Int,
    val totalCount: Int
)

data class ExportRowData(
    val label: String,
    val targetText: String,
    val targetNum: Int?,
    val counted: Int,
    val neededText: String,
    val neededNum: Int?,
    val bestSellerText: String,
    val isZeroRow: Boolean,
    val isHabis: Boolean,
    val isKurang: Boolean
)

data class ExportTableSection(
    val title: String,
    val rows: List<ExportRowData>
)

data class ExportPreviewData(
    val fileName: String,
    val reportId: String,
    val storeName: String,
    val mdName: String,
    val dateSlash: String,
    val listSummaries: List<ExportListSummary>,
    val tables: List<ExportTableSection>
)

object XlsxExporter {

    fun buildPreviewData(
        store: StoreDto,
        storeLists: List<ListDetailDto>,
        countsByList: Map<String, Map<String, Int>>,
        openedListIds: Set<String>,
        mdName: String,
        reportId: String,
        now: Date = Date()
    ): ExportPreviewData {
        val dateFileStr = SimpleDateFormat("ddMMyyyy", Locale.US).format(now)
        val dateSlashStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(now)

        val safeMd = sanitizeFilePart(mdName.ifBlank { "MD" })
        val safeStore = sanitizeFilePart(store.nama)
        val fileName = "$reportId-$dateFileStr-$safeMd-$safeStore.xlsx"

        val summaries = mutableListOf<ExportListSummary>()
        val tables = mutableListOf<ExportTableSection>()

        for (listDetail in storeLists) {
            val listCounts = countsByList[listDetail.id].orEmpty()
            val isOpened = openedListIds.contains(listDetail.id)

            val filledRegular = listDetail.items.count { (listCounts[it.kode] ?: 0) > 0 }
            val hasAnyCount = listCounts.values.any { it > 0 }

            val status = when {
                !isOpened && !hasAnyCount -> ExportListStatus.BELUM_DIBUKA
                !hasAnyCount -> ExportListStatus.SEMUA_NOL
                else -> ExportListStatus.TERISI
            }

            summaries.add(
                ExportListSummary(
                    listId = listDetail.id,
                    listName = listDetail.nama,
                    status = status,
                    filledCount = filledRegular,
                    totalCount = listDetail.items.size
                )
            )

            if (listDetail.pisahOrientasi) {
                val verticalItems = listDetail.items.filter {
                    it.orientasi.equals("Vertical", ignoreCase = true)
                }
                val horizontalItems = listDetail.items.filter {
                    it.orientasi.equals("Horizontal", ignoreCase = true)
                }
                val zeroTotal = (listCounts["000-V"] ?: 0) + (listCounts["000-H"] ?: 0)

                when {
                    verticalItems.isNotEmpty() && horizontalItems.isNotEmpty() -> {
                        val vRows = verticalItems.map { item ->
                            toRegularRow(item.label, item.target, listCounts[item.kode] ?: 0, item.bestSeller)
                        }.toMutableList()
                        vRows.add(toZeroRow("${listDetail.jenisAlias}-000", zeroTotal))

                        tables.add(
                            ExportTableSection(
                                title = "${listDetail.nama} · Vertical",
                                rows = vRows
                            )
                        )

                        val hRows = horizontalItems.map { item ->
                            toRegularRow(item.label, item.target, listCounts[item.kode] ?: 0, item.bestSeller)
                        }
                        tables.add(
                            ExportTableSection(
                                title = "${listDetail.nama} · Horizontal",
                                rows = hRows
                            )
                        )
                    }
                    verticalItems.isNotEmpty() -> {
                        val vRows = verticalItems.map { item ->
                            toRegularRow(item.label, item.target, listCounts[item.kode] ?: 0, item.bestSeller)
                        }.toMutableList()
                        vRows.add(toZeroRow("${listDetail.jenisAlias}-000", zeroTotal))

                        tables.add(
                            ExportTableSection(
                                title = "${listDetail.nama} · Vertical",
                                rows = vRows
                            )
                        )
                    }
                    horizontalItems.isNotEmpty() -> {
                        val hRows = horizontalItems.map { item ->
                            toRegularRow(item.label, item.target, listCounts[item.kode] ?: 0, item.bestSeller)
                        }.toMutableList()
                        hRows.add(toZeroRow("${listDetail.jenisAlias}-000", zeroTotal))

                        tables.add(
                            ExportTableSection(
                                title = "${listDetail.nama} · Horizontal",
                                rows = hRows
                            )
                        )
                    }
                    else -> {
                        tables.add(
                            ExportTableSection(
                                title = listDetail.nama,
                                rows = listOf(toZeroRow("${listDetail.jenisAlias}-000", zeroTotal))
                            )
                        )
                    }
                }
            } else {
                val zeroCount = listCounts["000"] ?: 0
                val rows = listDetail.items.map { item ->
                    toRegularRow(item.label, item.target, listCounts[item.kode] ?: 0, item.bestSeller)
                }.toMutableList()
                rows.add(toZeroRow("${listDetail.jenisAlias}-000", zeroCount))

                tables.add(
                    ExportTableSection(
                        title = listDetail.nama,
                        rows = rows
                    )
                )
            }
        }

        return ExportPreviewData(
            fileName = fileName,
            reportId = reportId,
            storeName = store.nama,
            mdName = mdName.ifBlank { "MD" },
            dateSlash = dateSlashStr,
            listSummaries = summaries,
            tables = tables
        )
    }

    private fun toRegularRow(
        label: String,
        target: Int,
        counted: Int,
        bestSeller: Boolean
    ): ExportRowData {
        val needed = maxOf(target - counted, 0)
        return ExportRowData(
            label = label,
            targetText = target.toString(),
            targetNum = target,
            counted = counted,
            neededText = needed.toString(),
            neededNum = needed,
            bestSellerText = if (bestSeller) "Ya" else "Tidak",
            isZeroRow = false,
            isHabis = counted <= 0,
            isKurang = counted in 1 until target
        )
    }

    private fun toZeroRow(label: String, counted: Int): ExportRowData {
        return ExportRowData(
            label = label,
            targetText = "-",
            targetNum = null,
            counted = counted,
            neededText = "-",
            neededNum = null,
            bestSellerText = "Tidak",
            isZeroRow = true,
            isHabis = false,
            isKurang = false
        )
    }

    private fun sanitizeFilePart(raw: String): String {
        return raw.trim()
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifEmpty { "OEI" }
    }

    // ==========================================
    // PENYIMPANAN KE FOLDER DOWNLOAD & BERBAGI FILE
    // ==========================================
    fun saveToDownloads(context: Context, preview: ExportPreviewData): Result<String> {
        return try {
            val xlsxBytes = generateXlsxBytes(preview)
            val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, preview.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Gagal membuat file di folder Download"))

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(xlsxBytes)
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Result.success("Tersimpan di Download/${preview.fileName}")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val outFile = File(downloadsDir, preview.fileName)
                FileOutputStream(outFile).use { it.write(xlsxBytes) }
                Result.success("Tersimpan di Download/${preview.fileName}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareXlsxFile(context: Context, preview: ExportPreviewData): Result<Unit> {
        return try {
            val xlsxBytes = generateXlsxBytes(preview)
            val exportCacheDir = File(context.cacheDir, "exports")
            if (!exportCacheDir.exists()) exportCacheDir.mkdirs()

            val targetFile = File(exportCacheDir, preview.fileName)
            FileOutputStream(targetFile).use { it.write(xlsxBytes) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Stok OEI - ${preview.storeName} (${preview.reportId})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Laporan XLSX").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // GENERATOR OPENXML (.XLSX) MURNI KOTLIN
    // ==========================================
    fun generateXlsxBytes(preview: ExportPreviewData): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            addZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            addZipEntry(zip, "_rels/.rels", rootRelsXml())
            addZipEntry(zip, "xl/workbook.xml", workbookXml())
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            addZipEntry(zip, "xl/styles.xml", stylesXml())
            addZipEntry(zip, "xl/worksheets/sheet1.xml", buildSheet1Xml(preview))
        }
        return baos.toByteArray()
    }

    private fun addZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildSheet1Xml(preview: ExportPreviewData): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<cols>")
        sb.append("""<col min="1" max="1" width="26" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="16" customWidth="1"/>""")
        sb.append("""<col min="3" max="3" width="18" customWidth="1"/>""")
        sb.append("""<col min="4" max="4" width="18" customWidth="1"/>""")
        sb.append("""<col min="5" max="5" width="14" customWidth="1"/>""")
        sb.append("</cols>")
        sb.append("<sheetData>")

        var rowIndex = 1

        // Header Informasi Laporan
        sb.append(singleStringRow(rowIndex++, "LAPORAN PENGECEKAN STOK", styleId = 1))
        sb.append(singleStringRow(rowIndex++, "Toko         : ${preview.storeName}", styleId = 0))
        sb.append(singleStringRow(rowIndex++, "Nama MD      : ${preview.mdName}", styleId = 0))
        sb.append(singleStringRow(rowIndex++, "Tanggal cek  : ${preview.dateSlash}", styleId = 0))
        sb.append(singleStringRow(rowIndex++, "ID Laporan   : ${preview.reportId}", styleId = 0))

        // 1 baris kosong sebelum tabel pertama
        sb.append("""<row r="${rowIndex++}"/>""")

        preview.tables.forEachIndexed { tableIdx, table ->
            if (tableIdx > 0) {
                // 4 baris kosong murni antartabel tanpa teks tambahan
                repeat(4) {
                    sb.append("""<row r="${rowIndex++}"/>""")
                }
            }

            // Judul tabel di atas header
            sb.append(singleStringRow(rowIndex++, table.title, styleId = 2))

            // Baris header kolom
            val headerRow = rowIndex++
            sb.append("""<row r="$headerRow">""")
            sb.append(inlineStringCell("A$headerRow", "Kode Produk", styleId = 3))
            sb.append(inlineStringCell("B$headerRow", "Jumlah Target", styleId = 3))
            sb.append(inlineStringCell("C$headerRow", "Jumlah Dihitung", styleId = 3))
            sb.append(inlineStringCell("D$headerRow", "Stok Dibutuhkan", styleId = 3))
            sb.append(inlineStringCell("E$headerRow", "Best Seller", styleId = 3))
            sb.append("</row>")

            // Baris item
            for (itemRow in table.rows) {
                val r = rowIndex++
                sb.append("""<row r="$r">""")

                when {
                    itemRow.isZeroRow -> {
                        sb.append(inlineStringCell("A$r", itemRow.label, styleId = 8))
                        sb.append(inlineStringCell("B$r", itemRow.targetText, styleId = 9))
                        sb.append(numberCell("C$r", itemRow.counted, styleId = 9))
                        sb.append(inlineStringCell("D$r", itemRow.neededText, styleId = 9))
                        sb.append(inlineStringCell("E$r", itemRow.bestSellerText, styleId = 9))
                    }
                    itemRow.isHabis -> {
                        // Seluruh baris latar merah muda + teks merah tebal
                        sb.append(inlineStringCell("A$r", itemRow.label, styleId = 6))
                        sb.append(numberCell("B$r", itemRow.targetNum ?: 0, styleId = 7))
                        sb.append(numberCell("C$r", itemRow.counted, styleId = 7))
                        sb.append(numberCell("D$r", itemRow.neededNum ?: 0, styleId = 7))
                        sb.append(inlineStringCell("E$r", itemRow.bestSellerText, styleId = 7))
                    }
                    itemRow.isKurang -> {
                        // Hanya sel Stok_Dibutuhkan yang diberi aksen merah
                        sb.append(inlineStringCell("A$r", itemRow.label, styleId = 4))
                        sb.append(numberCell("B$r", itemRow.targetNum ?: 0, styleId = 5))
                        sb.append(numberCell("C$r", itemRow.counted, styleId = 5))
                        sb.append(numberCell("D$r", itemRow.neededNum ?: 0, styleId = 7))
                        sb.append(inlineStringCell("E$r", itemRow.bestSellerText, styleId = 5))
                    }
                    else -> {
                        // Terpenuhi: tanpa warna
                        sb.append(inlineStringCell("A$r", itemRow.label, styleId = 4))
                        sb.append(numberCell("B$r", itemRow.targetNum ?: 0, styleId = 5))
                        sb.append(numberCell("C$r", itemRow.counted, styleId = 5))
                        sb.append(numberCell("D$r", itemRow.neededNum ?: 0, styleId = 5))
                        sb.append(inlineStringCell("E$r", itemRow.bestSellerText, styleId = 5))
                    }
                }

                sb.append("</row>")
            }
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun singleStringRow(rowIdx: Int, text: String, styleId: Int): String {
        return """<row r="$rowIdx">${inlineStringCell("A$rowIdx", text, styleId)}</row>"""
    }

    private fun inlineStringCell(ref: String, value: String, styleId: Int): String {
        return """<c r="$ref" s="$styleId" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>"""
    }

    private fun numberCell(ref: String, value: Int, styleId: Int): String {
        return """<c r="$ref" s="$styleId"><v>$value</v></c>"""
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Laporan Stok" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="4">
        <font>
            <sz val="11"/>
            <name val="Calibri"/>
        </font>
        <font>
            <b/>
            <sz val="11"/>
            <name val="Calibri"/>
        </font>
        <font>
            <b/>
            <sz val="11"/>
            <color rgb="FF991B1B"/>
            <name val="Calibri"/>
        </font>
        <font>
            <b/>
            <sz val="14"/>
            <name val="Calibri"/>
        </font>
    </fonts>
    <fills count="4">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill>
            <patternFill patternType="solid">
                <fgColor rgb="FFF1F5F9"/>
                <bgColor indexed="64"/>
            </patternFill>
        </fill>
        <fill>
            <patternFill patternType="solid">
                <fgColor rgb="FFFEE2E2"/>
                <bgColor indexed="64"/>
            </patternFill>
        </fill>
    </fills>
    <borders count="3">
        <border><left/><right/><top/><bottom/><diagonal/></border>
        <border>
            <left style="thin"><color rgb="FFCBD5E1"/></left>
            <right style="thin"><color rgb="FFCBD5E1"/></right>
            <top style="thin"><color rgb="FFCBD5E1"/></top>
            <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
            <diagonal/>
        </border>
        <border>
            <left style="thin"><color rgb="FFCBD5E1"/></left>
            <right style="thin"><color rgb="FFCBD5E1"/></right>
            <top style="medium"><color rgb="FF0F172A"/></top>
            <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
            <diagonal/>
        </border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="10">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
        <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="2" xfId="0" applyFont="1" applyBorder="1"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="2" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
            <alignment horizontal="center" vertical="center"/>
        </xf>
    </cellXfs>
    <cellStyles count="1">
        <cellStyle name="Normal" xfId="0" builtinId="0"/>
    </cellStyles>
    <dxfs count="0"/>
    <tableStyles count="0" defaultTableStyle="TableStyleMedium2" defaultPivotStyle="PivotStyleLight16"/>
</styleSheet>"""
}