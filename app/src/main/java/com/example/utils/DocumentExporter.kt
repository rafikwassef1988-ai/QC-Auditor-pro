package com.example.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AuditReportWithDetails
import com.example.data.Finding
import com.example.data.PreviousAuditRow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object DocumentExporter {

    private fun getUnifiedTrade(trade: String): String {
        val trim = trade.trim()
        return when {
            trim.equals("Civil", ignoreCase = true) || trim.equals("Structure", ignoreCase = true) || trim.equals("Structural", ignoreCase = true) -> "Structure"
            trim.equals("Architecture", ignoreCase = true) || trim.equals("Architectural", ignoreCase = true) -> "Architectural"
            trim.equals("MEP", ignoreCase = true) || trim.equals("Electrical", ignoreCase = true) -> "Electrical"
            trim.equals("Mechanical", ignoreCase = true) -> "Mechanical"
            trim.equals("Infrastructure", ignoreCase = true) -> "Infrastructure"
            else -> "Structure"
        }
    }

    private data class PDFTradeStats(val tradeName: String, val open: Int, val closed: Int, val total: Int, val closureRate: Float)

    private fun getDaysLate(dueDateStr: String): Int {
        val trimmed = dueDateStr.trim()
        if (trimmed.isEmpty()) return 0
        try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            val dueDate = sdf.parse(trimmed) ?: return 0
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse("2026-06-16") ?: return 0
            if (dueDate.before(today)) {
                val diffMs = today.time - dueDate.time
                return (diffMs / (1000 * 60 * 60 * 24)).toInt()
            }
        } catch (e: Exception) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val dueDate = sdf.parse(trimmed) ?: return 0
                val today = sdf.parse("2026-06-16") ?: return 0
                if (dueDate.before(today)) {
                    val diffMs = today.time - dueDate.time
                    return (diffMs / (1000 * 60 * 60 * 24)).toInt()
                }
            } catch (ex: Exception) { }
        }
        return 0
    }

    private fun getMonthYearSymbol(dateStr: String): String {
        val cleaned = dateStr.trim()
        if (cleaned.isEmpty()) return ""
        val parts = cleaned.split('/', '-')
        if (parts.size >= 3) {
            val p0 = parts[0]
            val p1 = parts[1]
            val p2 = parts[2]
            val year = if (p2.length == 4) p2 else if (p0.length == 4) p0 else "2026"
            val monthPart = if (p2.length == 4) p1 else p1
            val mInt = monthPart.toIntOrNull() ?: 1
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val mStr = if (mInt in 1..12) months[mInt] else "Jun"
            return "$mStr $year"
        }
        return ""
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun drawQcLogo(context: Context, canvas: Canvas, x: Float, y: Float, targetSize: Float) {
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_qc_audit_logo_1781912328532)
            if (bitmap != null) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val maxHeight = targetSize
                val maxWidth = targetSize * 2.2f
                
                var drawW = maxWidth
                var drawH = maxWidth / aspect
                if (drawH > maxHeight) {
                    drawH = maxHeight
                    drawW = maxHeight * aspect
                }
                
                val cx = x + targetSize / 2f
                val cy = y + targetSize / 2f
                
                val drawX = cx - drawW / 2f
                val drawY = cy - drawH / 2f
                
                val destRect = RectF(drawX, drawY, drawX + drawW, drawY + drawH)
                canvas.drawBitmap(bitmap, null, destRect, paint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decodeBase64ToBitmap(context: Context?, base64Str: String?): Bitmap? {
        if (base64Str.isNullOrEmpty()) return null
        val maxDimension = 3072
        
        // Helper to calculate power-of-two inSampleSize
        fun getPowerOfTwoSampleSize(outWidth: Int, outHeight: Int, targetSize: Int): Int {
            val largest = Math.max(outWidth, outHeight)
            var inSampleSize = 1
            while (largest / (inSampleSize * 2) >= targetSize) {
                inSampleSize *= 2
            }
            return inSampleSize
        }

        return try {
            if (base64Str.startsWith("file:") && context != null) {
                val filename = base64Str.substring(5)
                val dir = File(context.filesDir, "audit_photos")
                val file = File(dir, filename)
                if (!file.exists()) return null
                
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                
                val inSampleSize = getPowerOfTwoSampleSize(options.outWidth, options.outHeight, maxDimension)
                
                // Decode with high quality first
                var decodedBmp: Bitmap? = null
                try {
                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inJustDecodeBounds = false
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    decodedBmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    try {
                        // Fallback 1: RGB_565 (saves 50% RAM)
                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                            inJustDecodeBounds = false
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        decodedBmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    } catch (oom2: OutOfMemoryError) {
                        System.gc()
                        // Fallback 2: Downsample more
                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize * 2
                            inJustDecodeBounds = false
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        decodedBmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    }
                }
                
                if (decodedBmp != null && Math.max(decodedBmp.width, decodedBmp.height) > maxDimension) {
                    val scale = maxDimension.toFloat() / Math.max(decodedBmp.width, decodedBmp.height)
                    val newWidth = (decodedBmp.width * scale).toInt()
                    val newHeight = (decodedBmp.height * scale).toInt()
                    try {
                        val scaledBmp = Bitmap.createScaledBitmap(decodedBmp, newWidth, newHeight, true)
                        if (scaledBmp != decodedBmp) {
                            decodedBmp.recycle()
                            decodedBmp = scaledBmp
                        }
                    } catch (oom: OutOfMemoryError) {
                        System.gc()
                        // Keep the non-scaled bitmap as is if creation fails
                    }
                }
                return decodedBmp
            } else {
                val cleanStr = if (base64Str.contains(",")) base64Str.split(",")[1] else base64Str
                val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
                if (decodedBytes.isEmpty()) return null
                
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
                
                val inSampleSize = getPowerOfTwoSampleSize(options.outWidth, options.outHeight, maxDimension)
                
                var decodedBmp: Bitmap? = null
                try {
                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inJustDecodeBounds = false
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    decodedBmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOptions)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    try {
                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                            inJustDecodeBounds = false
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        decodedBmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOptions)
                    } catch (oom2: OutOfMemoryError) {
                        System.gc()
                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize * 2
                            inJustDecodeBounds = false
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        decodedBmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOptions)
                    }
                }
                
                if (decodedBmp != null && Math.max(decodedBmp.width, decodedBmp.height) > maxDimension) {
                    val scale = maxDimension.toFloat() / Math.max(decodedBmp.width, decodedBmp.height)
                    val newWidth = (decodedBmp.width * scale).toInt()
                    val newHeight = (decodedBmp.height * scale).toInt()
                    try {
                        val scaledBmp = Bitmap.createScaledBitmap(decodedBmp, newWidth, newHeight, true)
                        if (scaledBmp != decodedBmp) {
                            decodedBmp.recycle()
                            decodedBmp = scaledBmp
                        }
                    } catch (oom: OutOfMemoryError) {
                        System.gc()
                    }
                }
                return decodedBmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun wrapText(text: String, cellW: Float, paint: Paint): List<String> {
        val paragraphs = text.split("\n")
        val allLines = mutableListOf<String>()
        paragraphs.forEach { paragraph ->
            val words = paragraph.split(" ")
            var currentLine = ""
            words.forEach { word ->
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) < cellW) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) allLines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) allLines.add(currentLine)
        }
        return allLines
    }

    private fun drawTextUnified(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        if (text.isEmpty()) return
        val hasArabic = text.any { it.code in 0x0600..0x06FF || it.code in 0x0750..0x077F || it.code in 0x08A0..0x08FF || it.code in 0xFB50..0xFDFF || it.code in 0xFE70..0xFEFF }
        if (hasArabic) {
            try {
                val textPaint = android.text.TextPaint(paint)
                textPaint.color = paint.color
                textPaint.textSize = paint.textSize
                textPaint.typeface = paint.typeface
                
                val textWidth = textPaint.measureText(text)
                val layoutWidth = Math.ceil(textWidth.toDouble()).toInt().coerceAtLeast(1)
                
                val alignment = when (paint.textAlign) {
                    Paint.Align.CENTER -> android.text.Layout.Alignment.ALIGN_CENTER
                    Paint.Align.RIGHT -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                    else -> android.text.Layout.Alignment.ALIGN_NORMAL
                }
                
                val staticLayout = android.text.StaticLayout(
                    text,
                    textPaint,
                    layoutWidth,
                    alignment,
                    1.0f,
                    0.0f,
                    false
                )
                
                canvas.save()
                val baseline = if (staticLayout.lineCount > 0) staticLayout.getLineBaseline(0) else 0
                
                val startX = when (paint.textAlign) {
                    Paint.Align.CENTER -> x - layoutWidth / 2f
                    Paint.Align.RIGHT -> x - layoutWidth
                    else -> x
                }
                
                canvas.translate(startX, y - baseline)
                staticLayout.draw(canvas)
                canvas.restore()
            } catch (e: Exception) {
                e.printStackTrace()
                canvas.drawText(text, x, y, paint)
            }
        } else {
            canvas.drawText(text, x, y, paint)
        }
    }

    private fun Canvas.drawTextShaped(text: String, x: Float, y: Float, paint: Paint) {
        drawTextUnified(this, text, x, y, paint)
    }

    private fun drawCenteredMultilineText(text: String, x: Float, y: Float, cellW: Float, cellH: Float, canvas: Canvas, paint: Paint) {
        val hasArabic = text.any { it.code in 0x0600..0x06FF || it.code in 0x0750..0x077F || it.code in 0x08A0..0x08FF || it.code in 0xFB50..0xFDFF || it.code in 0xFE70..0xFEFF }
        if (hasArabic) {
            try {
                val textPaint = android.text.TextPaint(paint)
                textPaint.color = paint.color
                textPaint.textSize = paint.textSize
                textPaint.typeface = paint.typeface
                
                val staticLayout = android.text.StaticLayout(
                    text,
                    textPaint,
                    (cellW - 8f).toInt().coerceAtLeast(1),
                    android.text.Layout.Alignment.ALIGN_CENTER,
                    1.0f,
                    0.0f,
                    false
                )
                
                val totalH = staticLayout.height
                val startY = y + (cellH - totalH) / 2f
                
                canvas.save()
                canvas.translate(x + 4f, startY)
                staticLayout.draw(canvas)
                canvas.restore()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to simple multiline drawing
                val words = text.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                words.forEach { word ->
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) < cellW - 8f) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                
                val lineHeight = paint.textSize + 2.5f
                val totalH = lines.size * lineHeight
                var startY = y + (cellH - totalH) / 2f + paint.textSize - 1.5f
                lines.forEach { line ->
                    val lineW = paint.measureText(line)
                    canvas.drawText(line, x + (cellW - lineW) / 2f, startY, paint)
                    startY += lineHeight
                }
            }
        } else {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            words.forEach { word ->
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) < cellW - 8f) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
            
            val lineHeight = paint.textSize + 2.5f
            val totalH = lines.size * lineHeight
            var startY = y + (cellH - totalH) / 2f + paint.textSize - 1.5f
            lines.forEach { line ->
                val lineW = paint.measureText(line)
                canvas.drawTextShaped(line, x + (cellW - lineW) / 2f, startY, paint)
                startY += lineHeight
            }
        }
    }

    private fun drawMultilineText(
        text: String,
        x: Float,
        y: Float,
        width: Float,
        lineHeight: Float,
        canvas: Canvas,
        paint: Paint,
        maxLines: Int = 10
    ) {
        if (text.isEmpty()) return
        val hasArabic = text.any { it.code in 0x0600..0x06FF || it.code in 0x0750..0x077F || it.code in 0x08A0..0x08FF || it.code in 0xFB50..0xFDFF || it.code in 0xFE70..0xFEFF }
        if (hasArabic) {
            try {
                val textPaint = android.text.TextPaint(paint)
                textPaint.color = paint.color
                textPaint.textSize = paint.textSize
                textPaint.typeface = paint.typeface
                
                val alignment = android.text.Layout.Alignment.ALIGN_OPPOSITE
                
                val staticLayout = android.text.StaticLayout(
                    text,
                    textPaint,
                    width.toInt().coerceAtLeast(1),
                    alignment,
                    1.0f,
                    0.0f,
                    false
                )
                
                canvas.save()
                canvas.translate(x, y - paint.textSize)
                
                val countToDraw = Math.min(staticLayout.lineCount, maxLines)
                val clipHeight = countToDraw * lineHeight
                canvas.clipRect(0f, 0f, width, clipHeight)
                
                staticLayout.draw(canvas)
                canvas.restore()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to simple multiline drawing
                val words = text.split(" ")
                var line = ""
                var currentY = y
                var linesCount = 0
                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    val measure = paint.measureText(testLine)
                    if (measure > width) {
                        canvas.drawText(line, x, currentY, paint)
                        line = word
                        currentY += lineHeight
                        linesCount++
                        if (linesCount >= maxLines) break
                    } else {
                        line = testLine
                    }
                }
                if (linesCount < maxLines && line.isNotEmpty()) {
                    canvas.drawText(line, x, currentY, paint)
                }
            }
        } else {
            val words = text.split(" ")
            var line = ""
            var currentY = y
            var linesCount = 0
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val measure = paint.measureText(testLine)
                if (measure > width) {
                    canvas.drawTextShaped(line, x, currentY, paint)
                    line = word
                    currentY += lineHeight
                    linesCount++
                    if (linesCount >= maxLines) break
                } else {
                    line = testLine
                }
            }
            if (linesCount < maxLines && line.isNotEmpty()) {
                canvas.drawTextShaped(line, x, currentY, paint)
            }
        }
    }

    // PDF EXPORT
    fun exportToPdf(context: Context, details: AuditReportWithDetails, file: File, selectedTrade: String = "All Trades") {
        val findings = if (selectedTrade == "All Trades") {
            details.findings
        } else {
            details.findings.filter { getUnifiedTrade(it.trade) == selectedTrade }
        }
        val ncrFindings = findings.filter { it.type.trim().uppercase() == "NCR" }
        val page1Ncrs = ncrFindings.take(10)
        val continuedNcrs = ncrFindings.drop(10)
        val continuedNcrPages = continuedNcrs.chunked(12)
        val totalP = 4 + continuedNcrPages.size + findings.size
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { 
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        
        // High resolution scale factor
        val scaleFactor = 4.0f
        val basePageWidth = 595
        val basePageHeight = 842
        
        val pageWidth = (basePageWidth * scaleFactor).toInt()
        val pageHeight = (basePageHeight * scaleFactor).toInt()
        var pageNumber = 1
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        canvas.scale(scaleFactor, scaleFactor)
        
        val marginX = 36f
        var currentY = 40f
        
        // DRAW PAGE HEADER (LETTERHEAD)
        fun drawHeader() {
            // Draw dark blue letterhead bar
            paint.color = Color.parseColor("#0D253F")
            canvas.drawRect(marginX, currentY, basePageWidth - marginX, currentY + 70f, paint)
            
            // Draw custom logo inside bar (Vector direct drawing for crystal clear quality)
            try {
                drawQcLogo(context, canvas, marginX + 10f, currentY + 7f, 56f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Title Text inside bar
            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("QUALITY CONTROL DEPARTMENT", marginX + 110f, currentY + 28f, paint)
            
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped("Internal Audit Report   |   Issuance: ${details.report.reportIssuanceDate}", marginX + 110f, currentY + 45f, paint)
            
            // Draw a subtle line underneath
            paint.color = Color.parseColor("#00BFA5")
            canvas.drawRect(marginX, currentY + 70f, basePageWidth - marginX, currentY + 74f, paint)
            
            currentY += 92f
        }
        
        // ----------------- PAGE 1: COVER/PROJECT AUDIT REPORT COVER -----------------
        canvas.drawColor(Color.WHITE)
        
        // 1. DRAW HEADER BOX (X: 36f -> 559f, Y: 40f -> 100f)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawRect(36f, 40f, 559f, 96f, paint)
        canvas.drawLine(146f, 40f, 146f, 96f, paint)
        canvas.drawLine(439f, 40f, 439f, 96f, paint)
        
        // Custom logo inside left box (Vector direct drawing for crystal clear quality)
        try {
            drawQcLogo(context, canvas, 36f + 29f, 40f + 6f, 44f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Centered Bold Title Box (X: 146f -> 439f)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val centeredHeaderTitle = "QC Internal Audit Report # ${details.report.auditNumber.ifEmpty { "255" }}"
        val centeredHeaderTitleWidth = paint.measureText(centeredHeaderTitle)
        // Underline the header title text nicely
        canvas.drawTextShaped(centeredHeaderTitle, 146f + (293f - centeredHeaderTitleWidth)/2f, 73f, paint)
        // Draw underline
        paint.strokeWidth = 1.2f
        canvas.drawLine(
            146f + (293f - centeredHeaderTitleWidth)/2f, 
            76f, 
            146f + (293f - centeredHeaderTitleWidth)/2f + centeredHeaderTitleWidth, 
            76f, 
            paint
        )
        
        // Right Side Department texts (X: 439f -> 559f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        val depText1 = "Quality Control Department"
        val depText2 = "Internal Audit"
        canvas.drawTextShaped(depText1, 553f - paint.measureText(depText1), 60f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawTextShaped(depText2, 553f - paint.measureText(depText2), 76f, paint)
        
        // Centered Big Title above table
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        val coverTitle = "QC Internal Audit Report # ${details.report.auditNumber.ifEmpty { "255" }}"
        val coverTitleW = paint.measureText(coverTitle)
        canvas.drawTextShaped(coverTitle, 36f + (523f - coverTitleW)/2f, 116f, paint)
        canvas.drawLine(
            36f + (523f - coverTitleW)/2f,
            120f,
            36f + (523f - coverTitleW)/2f + coverTitleW,
            120f,
            paint
        )
        
        // 2. PROJECT INFORMATION TABLE (Y starts at 128f)
        var py = 128f
        fun drawProjectInfoRow(y: Float, lbl1: String, val1: String, lbl2: String, val2: String) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawRect(36f, y, 559f, y + 16f, paint)
            canvas.drawLine(146f, y, 146f, y + 16f, paint)
            canvas.drawLine(291f, y, 291f, y + 16f, paint)
            canvas.drawLine(401f, y, 401f, y + 16f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7.5f
            canvas.drawTextShaped(lbl1, 42f, y + 11f, paint)
            canvas.drawTextShaped(lbl2, 296f, y + 11f, paint)
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(val1, 151f, y + 11f, paint)
            canvas.drawTextShaped(val2, 406f, y + 11f, paint)
        }
        
        drawProjectInfoRow(py, "Project name/ Number:", "${details.report.projectName} / ${details.report.projectNumber}", "Location:", details.report.location)
        py += 16f
        drawProjectInfoRow(py, "Project Manager:", details.report.projectManager, "Acting QC Manager:", details.report.qcManager)
        py += 16f
        drawProjectInfoRow(py, "Audit Date/duration:", details.report.auditDate, "Auditor:", details.report.auditorName)
        py += 16f
        drawProjectInfoRow(py, "Report issuance date:", details.report.reportIssuanceDate, "Audit follow up date:", details.report.followupDueDate)
        
        // 3. AUDIT FINDINGS SUMMARY (Y starts at 208f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.style = Paint.Style.FILL
        canvas.drawTextShaped("01- Audit findings summary:", 36f, 206f, paint)
        canvas.drawLine(36f, 208f, 150f, 208f, paint)
        
        val ncrCount = findings.count { it.type == "NCR" }
        val obsCount = findings.count { it.type == "OBS" }
        
        paint.style = Paint.Style.STROKE
        canvas.drawRect(36f, 214f, 559f, 246f, paint)
        canvas.drawLine(36f, 230f, 559f, 230f, paint)
        canvas.drawLine(210f, 214f, 210f, 246f, paint)
        canvas.drawLine(384f, 214f, 384f, 246f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        
        val h1 = "Total number of issued NCRs"
        canvas.drawTextShaped(h1, 210f - 174f/2f - paint.measureText(h1)/2f, 225f, paint)
        val h2 = "Total number of issued Observations"
        canvas.drawTextShaped(h2, 384f - 174f/2f - paint.measureText(h2)/2f, 225f, paint)
        val h3 = "Due date"
        canvas.drawTextShaped(h3, 559f - (559f - 384f)/2f - paint.measureText(h3)/2f, 225f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val v1 = String.format("%02d", ncrCount)
        canvas.drawTextShaped(v1, 210f - 174f/2f - paint.measureText(v1)/2f, 241f, paint)
        val v2 = String.format("%02d", obsCount)
        canvas.drawTextShaped(v2, 384f - 174f/2f - paint.measureText(v2)/2f, 241f, paint)
        val v3 = details.report.followupDueDate
        canvas.drawTextShaped(v3, 559f - (559f - 384f)/2f - paint.measureText(v3)/2f, 241f, paint)
        
        // 4. AUDIT NCR FINDINGS (Y starts at 262f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawTextShaped("02- Audit NCR findings:", 36f, 264f, paint)
        canvas.drawLine(36f, 266f, 130f, 266f, paint)
        
        // Draw Table Headers (Y: 272f -> 296f, height: 24f)
        paint.style = Paint.Style.STROKE
        canvas.drawRect(36f, 272f, 559f, 296f, paint)
        canvas.drawLine(61f, 272f, 61f, 296f, paint)
        canvas.drawLine(275f, 272f, 275f, 296f, paint)
        canvas.drawLine(417f, 272f, 417f, 296f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawTextShaped("Sr.", 43f, 286f, paint)
        
        drawCenteredMultilineText("Finding Description", 61f, 272f, 214f, 24f, canvas, paint)
        drawCenteredMultilineText("Negative Impact on the quality of the project for the same item and/or other elements.", 275f, 272f, 142f, 24f, canvas, paint)
        drawCenteredMultilineText("Losses in material and manpower.", 417f, 272f, 142f, 24f, canvas, paint)
        
        // Draw Rows
        var rowY = 296f
        val rowH = 40f
        if (page1Ncrs.isEmpty()) {
            paint.style = Paint.Style.STROKE
            canvas.drawRect(36f, rowY, 559f, rowY + rowH, paint)
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 8f
            paint.color = Color.GRAY
            canvas.drawTextShaped("No NCR findings recorded.", 45f, rowY + rowH/2f + 3f, paint)
            paint.color = Color.BLACK
        } else {
            page1Ncrs.forEachIndexed { idx, fi ->
                paint.style = Paint.Style.STROKE
                canvas.drawRect(36f, rowY, 559f, rowY + rowH, paint)
                canvas.drawLine(61f, rowY, 61f, rowY + rowH, paint)
                canvas.drawLine(275f, rowY, 275f, rowY + rowH, paint)
                canvas.drawLine(417f, rowY, 417f, rowY + rowH, paint)
                
                paint.style = Paint.Style.FILL
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 7f
                val srText = (idx + 1).toString()
                canvas.drawTextShaped(srText, 61f - 25f/2f - paint.measureText(srText)/2f, rowY + rowH/2f + 3f, paint)
                
                // Description wrapping
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 5.2f
                val descLines = wrapText(fi.description, 202f, paint)
                var descY = rowY + 9f
                descLines.forEachIndexed { lineIdx, line ->
                    if (lineIdx < 3) {
                        canvas.drawTextShaped(line, 67f, descY, paint)
                        descY += 6.5f
                    }
                }
                // Reference ID
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 5.5f
                canvas.drawTextShaped(fi.referenceId, 67f, rowY + rowH - 4f, paint)
                canvas.drawLine(67f, rowY + rowH - 3f, 67f + paint.measureText(fi.referenceId), rowY + rowH - 3f, paint)
                
                // Negative Impact wrapping
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 5.2f
                val impactLines = wrapText(fi.negativeImpact, 134f, paint)
                var impY = rowY + 9f
                impactLines.forEachIndexed { lineIdx, line ->
                    if (lineIdx < 4) {
                        canvas.drawTextShaped(line, 281f, impY, paint)
                        impY += 6.5f
                    }
                }
                
                // Losses wrapping
                val lossesLines = wrapText(fi.materialLosses, 134f, paint)
                var losY = rowY + 9f
                lossesLines.forEachIndexed { lineIdx, line ->
                    if (lineIdx < 4) {
                        canvas.drawTextShaped(line, 423f, losY, paint)
                        losY += 6.5f
                    }
                }
                
                rowY += rowH
            }
        }
        
        // 5. SIGNATURES TABLE (Docked near the bottom at fixed 708f)
        val sigY = 708f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(36f, sigY, 559f, sigY + 74f, paint)
        canvas.drawLine(297.5f, sigY, 297.5f, sigY + 74f, paint)
        canvas.drawLine(36f, sigY + 14f, 559f, sigY + 14f, paint)
        canvas.drawLine(36f, sigY + 38f, 559f, sigY + 38f, paint)
        canvas.drawLine(36f, sigY + 50f, 559f, sigY + 50f, paint)
        canvas.drawLine(36f, sigY + 62f, 559f, sigY + 62f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        
        val sr1 = "Audit report issued by"
        canvas.drawTextShaped(sr1, 297.5f - 261.5f/2f - paint.measureText(sr1)/2f, sigY + 10f, paint)
        val sr2 = "Audit report reviewed by"
        canvas.drawTextShaped(sr2, 559f - 261.5f/2f - paint.measureText(sr2)/2f, sigY + 10f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawTextShaped("Signature:", 42f, sigY + 28f, paint)
        canvas.drawTextShaped("Signature:", 303.5f, sigY + 28f, paint)
        
        // Elegant visual script fonts for real signatures signature simulation
        paint.typeface = Typeface.create("serif", Typeface.ITALIC or Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#1B5E20") // clean green ink signature
        canvas.drawTextShaped(details.report.sigAuditorName.ifEmpty { details.report.auditorName }.ifEmpty { "Rafik Hisham" }, 95f, sigY + 29f, paint)
        canvas.drawTextShaped(details.report.sigReviewerName.ifEmpty { "Moamen Othman" }, 356.5f, sigY + 29f, paint)
        
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawTextShaped("Name: ${details.report.sigAuditorName.ifEmpty { details.report.auditorName }.ifEmpty { "Rafik Hisham" }}", 42f, sigY + 47f, paint)
        canvas.drawTextShaped("Name: ${details.report.sigReviewerName.ifEmpty { "Moamen Othman" }}", 303.5f, sigY + 47f, paint)
        
        canvas.drawTextShaped("Designation: ${details.report.sigAuditorDesignation.ifEmpty { "Quality Auditor" }}", 42f, sigY + 59f, paint)
        canvas.drawTextShaped("Designation: ${details.report.sigReviewerDesignation.ifEmpty { "Quality Director" }}", 303.5f, sigY + 59f, paint)
        
        canvas.drawTextShaped("Date: ${details.report.sigAuditorDate.ifEmpty { details.report.reportIssuanceDate }.ifEmpty { "14/06/2026" }}", 42f, sigY + 71f, paint)
        canvas.drawTextShaped("Date: ${details.report.sigReviewerDate.ifEmpty { "15/06/2026" }}", 303.5f, sigY + 71f, paint)
        
        // 6. BOTTOM FORM FOOTER BOX (Y: 808f -> 822f)
        val fY = 808f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(36f, fY, 559f, fY + 14f, paint)
        canvas.drawLine(156f, fY, 156f, fY + 14f, paint)
        canvas.drawLine(306f, fY, 306f, fY + 14f, paint)
        canvas.drawLine(366f, fY, 366f, fY + 14f, paint)
        canvas.drawLine(446f, fY, 446f, fY + 14f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Internal Quality Audit Report", 42f, fY + 9.5f, paint)
        canvas.drawTextShaped(details.report.formReference.ifEmpty { "innovo/QAQC/FRM - 1.14/05" }, 162f, fY + 9.5f, paint)
        canvas.drawTextShaped("REV 02", 312f, fY + 9.5f, paint)
        canvas.drawTextShaped("1-Mar-2023", 372f, fY + 9.5f, paint)
        
        canvas.drawTextShaped("Page 1 of $totalP", 452f, fY + 9.5f, paint)
        
        // Finish page 1
        pdfDocument.finishPage(page)
        
        // --- DRAW CONTINUED NCR TABLE PAGES IF NECESSARY ---
        continuedNcrPages.forEachIndexed { pageIdx, pageItems ->
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.scale(scaleFactor, scaleFactor)
            canvas.drawColor(Color.WHITE)
            
            // Elegant Header box
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawRect(36f, 40f, 559f, 96f, paint)
            canvas.drawLine(146f, 40f, 146f, 96f, paint)
            canvas.drawLine(439f, 40f, 439f, 96f, paint)
            
            try {
                drawQcLogo(context, canvas, 36f + 29f, 40f + 6f, 44f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            val headerTitleCont = "QC Internal Audit Report # ${details.report.auditNumber.ifEmpty { "255" }}"
            val headerTitleContW = paint.measureText(headerTitleCont)
            canvas.drawTextShaped(headerTitleCont, 146f + (293f - headerTitleContW)/2f, 73f, paint)
            paint.strokeWidth = 1.2f
            canvas.drawLine(
                146f + (293f - headerTitleContW)/2f, 
                76f, 
                146f + (293f - headerTitleContW)/2f + headerTitleContW, 
                76f, 
                paint
            )
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7f
            val depText1 = "Quality Control Department"
            val depText2 = "Internal Audit"
            canvas.drawTextShaped(depText1, 553f - paint.measureText(depText1), 60f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(depText2, 553f - paint.measureText(depText2), 76f, paint)
            
            // Continued Section Title
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            val titleContText = "02- Audit NCR findings (Continued):"
            canvas.drawTextShaped(titleContText, 36f, 116f, paint)
            paint.strokeWidth = 1f
            canvas.drawLine(36f, 118f, 36f + paint.measureText(titleContText), 118f, paint)
            
            // Draw Table Headers (Y: 124f -> 148f, height: 24f)
            var contRowY = 124f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(36f, contRowY, 559f, contRowY + 24f, paint)
            canvas.drawLine(61f, contRowY, 61f, contRowY + 24f, paint)
            canvas.drawLine(275f, contRowY, 275f, contRowY + 24f, paint)
            canvas.drawLine(417f, contRowY, 417f, contRowY + 24f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7f
            canvas.drawTextShaped("Sr.", 43f, contRowY + 14f, paint)
            
            drawCenteredMultilineText("Finding Description", 61f, contRowY, 214f, 24f, canvas, paint)
            drawCenteredMultilineText("Negative Impact on the quality of the project for the same item and/or other elements.", 275f, contRowY, 142f, 24f, canvas, paint)
            drawCenteredMultilineText("Losses in material and manpower.", 417f, contRowY, 142f, 24f, canvas, paint)
            
            contRowY += 24f
            
            // Draw continued rows
            pageItems.forEachIndexed { itemIdx, fi ->
                val overallIdx = 10 + pageIdx * 12 + itemIdx
                paint.style = Paint.Style.STROKE
                canvas.drawRect(36f, contRowY, 559f, contRowY + rowH, paint)
                canvas.drawLine(61f, contRowY, 61f, contRowY + rowH, paint)
                canvas.drawLine(275f, contRowY, 275f, contRowY + rowH, paint)
                canvas.drawLine(417f, contRowY, 417f, contRowY + rowH, paint)
                
                paint.style = Paint.Style.FILL
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 7f
                val srText = (overallIdx + 1).toString()
                canvas.drawTextShaped(srText, 61f - 25f/2f - paint.measureText(srText)/2f, contRowY + rowH/2f + 3f, paint)
                
                // Description wrapping
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 5.2f
                val descLines = wrapText(fi.description, 202f, paint)
                var descY = contRowY + 9f
                descLines.forEachIndexed { lineIdx, line ->
                    if (lineIdx < 3) {
                        canvas.drawTextShaped(line, 67f, descY, paint)
                        descY += 6.5f
                    }
                }
                // Reference ID
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 5.5f
                canvas.drawTextShaped(fi.referenceId, 67f, contRowY + rowH - 4f, paint)
                canvas.drawLine(67f, contRowY + rowH - 3f, 67f + paint.measureText(fi.referenceId), contRowY + rowH - 3f, paint)
                
                // Negative Impact wrapping
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 5.2f
                val impactLines = wrapText(fi.negativeImpact, 134f, paint)
                var impY = contRowY + 9f
                impactLines.forEachIndexed { lineIdx, line ->
                    if (lineIdx < 4) {
                        canvas.drawTextShaped(line, 281f, impY, paint)
                        impY += 6.5f
                    }
                }
                
                // Losses wrapping
                val lossesLines = wrapText(fi.materialLosses, 134f, paint)
                var losY = contRowY + 9f
                lossesLines.forEachIndexed { lineIdx, line ->
                    if (lineIdx < 4) {
                        canvas.drawTextShaped(line, 423f, losY, paint)
                        losY += 6.5f
                    }
                }
                
                contRowY += rowH
            }
            
            // Footer Box
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(36f, fY, 559f, fY + 14f, paint)
            canvas.drawLine(156f, fY, 156f, fY + 14f, paint)
            canvas.drawLine(306f, fY, 306f, fY + 14f, paint)
            canvas.drawLine(366f, fY, 366f, fY + 14f, paint)
            canvas.drawLine(446f, fY, 446f, fY + 14f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6f
            canvas.drawTextShaped("Internal Quality Audit Report", 42f, fY + 9.5f, paint)
            canvas.drawTextShaped(details.report.formReference.ifEmpty { "innovo/QAQC/FRM - 1.14/05" }, 162f, fY + 9.5f, paint)
            canvas.drawTextShaped("REV 02", 312f, fY + 9.5f, paint)
            canvas.drawTextShaped("1-Mar-2023", 372f, fY + 9.5f, paint)
            canvas.drawTextShaped("Page $pageNumber of $totalP", 452f, fY + 9.5f, paint)
            
            pdfDocument.finishPage(page)
        }
        
        // ================= PAGE 2: EXECUTIVE ANALYTICS KPI'S & GRAPHICAL CHARTS =================
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        canvas.scale(scaleFactor, scaleFactor)
        canvas.drawColor(Color.WHITE)
        
        // Elegant header box
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawRect(36f, 40f, 559f, 96f, paint)
        canvas.drawLine(146f, 40f, 146f, 96f, paint)
        canvas.drawLine(439f, 40f, 439f, 96f, paint)
        
        try {
            drawQcLogo(context, canvas, 36f + 29f, 40f + 6f, 44f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val centeredHeaderTitle2 = "QC Internal Audit Report # ${details.report.auditNumber.ifEmpty { "255" }}"
        val centeredHeaderTitleWidth2 = paint.measureText(centeredHeaderTitle2)
        canvas.drawTextShaped(centeredHeaderTitle2, 146f + (293f - centeredHeaderTitleWidth2)/2f, 73f, paint)
        paint.strokeWidth = 1.2f
        canvas.drawLine(
            146f + (293f - centeredHeaderTitleWidth2)/2f, 
            76f, 
            146f + (293f - centeredHeaderTitleWidth2)/2f + centeredHeaderTitleWidth2, 
            76f, 
            paint
        )
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawTextShaped(depText1, 553f - paint.measureText(depText1), 60f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawTextShaped(depText2, 553f - paint.measureText(depText2), 76f, paint)
        
        // Page 2 Section Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val p2Title = "NCR / OBS Executive Analytics Dashboard"
        canvas.drawTextShaped(p2Title, 36f, 116f, paint)
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 119f, 36f + paint.measureText(p2Title), 119f, paint)
        
        // Calculate the exact variables matching MainActivity.kt
        val totalNcrs = findings.count { it.type == "NCR" }
        val openNcrs = findings.count { it.type == "NCR" && it.status == "Open" }
        val closedNcrs = findings.count { it.type == "NCR" && it.status == "Closed" }
        val ncrClosureRate = if (totalNcrs > 0) (closedNcrs.toFloat() / totalNcrs * 100f) else 0f
        
        val totalObs = findings.count { it.type == "OBS" }
        val openObs = findings.count { it.type == "OBS" && it.status == "Open" }
        val closedObs = findings.count { it.type == "OBS" && it.status == "Closed" }
        val obsClosureRate = if (totalObs > 0) (closedObs.toFloat() / totalObs * 100f) else 0f
        
        val overdueNcrs = findings.count { it.type == "NCR" && it.status == "Open" && getDaysLate(it.dueDate) > 0 }
        
        // ------------------ KPI CARD GRID ------------------
        // Row 1 (NCR KPIs): Y = 132f to 164f
        val r1CardY = 132f
        val cardH = 32f
        val cardW = 120f
        val cardSpacing = 11f
        
        val kpiData1 = listOf(
            Pair("Total NCRs", totalNcrs.toString() to "#2563EB"),
            Pair("Open NCRs", openNcrs.toString() to "#EF4444"),
            Pair("Closed NCRs", closedNcrs.toString() to "#10B981"),
            Pair("Closure (NCR)", "${String.format(java.util.Locale.US, "%.1f", ncrClosureRate)}%" to "#06B6D4")
        )
        
        kpiData1.forEachIndexed { idx, item ->
            val cardX = 36f + idx * (cardW + cardSpacing)
            // Background
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(cardX, r1CardY, cardX + cardW, r1CardY + cardH, 4f, 4f, paint)
            // Left border accent
            paint.color = Color.parseColor(item.second.second)
            canvas.drawRect(cardX, r1CardY, cardX + 3f, r1CardY + cardH, paint)
            // Title
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            paint.color = Color.parseColor("#475569")
            canvas.drawTextShaped(item.first, cardX + 8f, r1CardY + 12f, paint)
            // Value
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = Color.BLACK
            canvas.drawTextShaped(item.second.first, cardX + 8f, r1CardY + 24f, paint)
        }
        
        // Row 2 (OBS & Overdue KPIs): Y = 171f to 203f
        val r2CardY = 171f
        val kpiData2 = listOf(
            Pair("Total OBS", totalObs.toString() to "#FFB020"),
            Pair("Open OBS", openObs.toString() to "#F97316"),
            Pair("Closed OBS", closedObs.toString() to "#10B981"),
            Pair("Overdue NCRs", overdueNcrs.toString() to "#DC2626")
        )
        
        kpiData2.forEachIndexed { idx, item ->
            val cardX = 36f + idx * (cardW + cardSpacing)
            // Background
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(cardX, r2CardY, cardX + cardW, r2CardY + cardH, 4f, 4f, paint)
            // Left border accent
            paint.color = Color.parseColor(item.second.second)
            canvas.drawRect(cardX, r2CardY, cardX + 3f, r2CardY + cardH, paint)
            // Title
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            paint.color = Color.parseColor("#475569")
            canvas.drawTextShaped(item.first, cardX + 8f, r2CardY + 12f, paint)
            // Value
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = Color.BLACK
            canvas.drawTextShaped(item.second.first, cardX + 8f, r2CardY + 24f, paint)
        }
        
        // ------------------ CHARTS SECTION ------------------
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawTextShaped("02- Graphical Performance and Resolution Metrics:", 36f, 222f, paint)
        canvas.drawLine(36f, 224f, 238f, 224f, paint)
        
        // Left Column Chart Box (Findings by Trade Stacked Bars)
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(36f, 233f, 280f, 443f, 6f, 6f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(36f, 233f, 280f, 443f, 6f, 6f, paint)
        
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawTextShaped("Trade-wise Findings Distribution", 46f, 248f, paint)
        
        val standardTrades = listOf("Structure", "Architectural", "Electrical", "Mechanical", "Infrastructure")
        val ncrOpenCounts2 = mutableListOf<Int>()
        val ncrClosedCounts2 = mutableListOf<Int>()
        val obsOpenCounts2 = mutableListOf<Int>()
        val obsClosedCounts2 = mutableListOf<Int>()
        
        standardTrades.forEach { tr ->
            val matching = findings.filter { getUnifiedTrade(it.trade) == tr }
            val ncrOp = matching.count { it.type == "NCR" && it.status == "Open" }
            val ncrCl = matching.count { it.type == "NCR" && it.status == "Closed" }
            val obsOp = matching.count { it.type == "OBS" && it.status == "Open" }
            val obsCl = matching.count { it.type == "OBS" && it.status == "Closed" }
            ncrOpenCounts2.add(ncrOp)
            ncrClosedCounts2.add(ncrCl)
            obsOpenCounts2.add(obsOp)
            obsClosedCounts2.add(obsCl)
        }
        
        val maxTradeVal2 = standardTrades.indices.map { sIdx ->
            ncrOpenCounts2[sIdx] + ncrClosedCounts2[sIdx] + obsOpenCounts2[sIdx] + obsClosedCounts2[sIdx]
        }.maxOrNull()?.coerceAtLeast(1) ?: 1
        
        var barY = 264f
        val barH = 10f
        val barGap = 26f
        standardTrades.forEachIndexed { sIdx, tr ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 6.5f
            paint.color = Color.BLACK
            canvas.drawTextShaped(tr, 46f, barY + 8f, paint)
            
            val ncrTotalRow = ncrOpenCounts2[sIdx] + ncrClosedCounts2[sIdx]
            val obsTotalRow = obsOpenCounts2[sIdx] + obsClosedCounts2[sIdx]
            val rowSum = ncrTotalRow + obsTotalRow
            
            val scaleFactor = 110f / maxTradeVal2.toFloat()
            val ncrBarW = ncrTotalRow * scaleFactor
            val obsBarW = obsTotalRow * scaleFactor
            
            if (ncrTotalRow > 0) {
                paint.color = Color.parseColor("#3B82F6") // Blue for NCR (matching chart on screen color)
                paint.style = Paint.Style.FILL
                canvas.drawRect(120f, barY, 120f + ncrBarW, barY + barH, paint)
            }
            if (obsTotalRow > 0) {
                paint.color = Color.parseColor("#FFB020") // Amber for OBS (matching chart on screen color)
                paint.style = Paint.Style.FILL
                canvas.drawRect(120f + ncrBarW, barY, 120f + ncrBarW + obsBarW, barY + barH, paint)
            }
            if (rowSum == 0) {
                paint.color = Color.parseColor("#E2E8F0")
                paint.style = Paint.Style.FILL
                canvas.drawRect(120f, barY + 4f, 230f, barY + 5f, paint)
            } else {
                paint.color = Color.BLACK
                paint.textSize = 6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawTextShaped(rowSum.toString(), 125f + ncrBarW + obsBarW, barY + 8f, paint)
            }
            barY += barGap
        }
        
        // Left Column Chart Legend
        paint.color = Color.parseColor("#3B82F6")
        paint.style = Paint.Style.FILL
        canvas.drawRect(46f, 423f, 54f, 431f, paint)
        paint.color = Color.BLACK
        paint.textSize = 6f
        canvas.drawTextShaped("NCRs", 58f, 429f, paint)
        
        paint.color = Color.parseColor("#FFB020")
        paint.style = Paint.Style.FILL
        canvas.drawRect(100f, 423f, 108f, 431f, paint)
        paint.color = Color.BLACK
        paint.textSize = 6f
        canvas.drawTextShaped("Observations", 112f, 429f, paint)
        
        // Right Column Chart Box (Doughnut circular progress chart)
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(310f, 233f, 556f, 443f, 6f, 6f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(310f, 233f, 556f, 443f, 6f, 6f, paint)
        
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawTextShaped("Audit Resolution Progress Index", 320f, 248f, paint)
        
        val totalOpen = findings.count { it.status.trim().equals("Open", ignoreCase = true) }
        val totalClosed = findings.count { it.status.trim().equals("Closed", ignoreCase = true) }
        val grandTotal = (totalOpen + totalClosed).coerceAtLeast(1)
        
        val openRatio = totalOpen.toFloat() / grandTotal.toFloat()
        val closedRatio = totalClosed.toFloat() / grandTotal.toFloat()
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 14f
        paint.strokeCap = Paint.Cap.ROUND
        
        val radialOval = RectF(433f - 42f, 335f - 42f, 433f + 42f, 335f + 42f)
        
        val angleClosedStart = -90f
        val angleClosedSweep = closedRatio * 360f
        val angleOpenStart = angleClosedStart + angleClosedSweep
        val angleOpenSweep = openRatio * 360f
        
        if (closedRatio > 0f) {
            paint.color = Color.parseColor("#10B981")
            canvas.drawArc(radialOval, angleClosedStart, angleClosedSweep, false, paint)
        }
        if (openRatio > 0f) {
            paint.color = Color.parseColor("#EF4444")
            canvas.drawArc(radialOval, angleOpenStart, angleOpenSweep, false, paint)
        }
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val closedPercentString = "${(closedRatio * 100).toInt()}%"
        canvas.drawTextShaped(closedPercentString, 433f - paint.measureText(closedPercentString)/2f, 335f + 4f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 5.5f
        canvas.drawTextShaped("RESOLVED", 433f - paint.measureText("RESOLVED")/2f, 345f, paint)
        
        // Right Column Chart Legend
        paint.color = Color.parseColor("#10B981")
        paint.style = Paint.Style.FILL
        canvas.drawRect(320f, 423f, 328f, 431f, paint)
        paint.color = Color.BLACK
        paint.textSize = 6f
        canvas.drawTextShaped("Closed ($totalClosed)", 332f, 429f, paint)
        
        paint.color = Color.parseColor("#EF4444")
        paint.style = Paint.Style.FILL
        canvas.drawRect(436f, 423f, 444f, 431f, paint)
        paint.color = Color.BLACK
        canvas.drawTextShaped("Open ($totalOpen)", 448f, 429f, paint)
        
        // ------------------ SEVERITY & RISK HIGHLIGHTS ------------------
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawTextShaped("03- Issue Severity Analysis & Root Causes:", 36f, 461f, paint)
        canvas.drawLine(36f, 463f, 210f, 463f, paint)
        
        val majorNcrCount = findings.count { it.type == "NCR" && it.severity == "Major" }
        val minorNcrCount = findings.count { it.type == "NCR" && it.severity == "Minor" }
        val repeatedFlagged = findings.count { it.repeated == "Yes" }
        val lossesFlagged = findings.count { it.materialLosses.trim().isNotEmpty() && !it.materialLosses.trim().equals("None", true) }
        
        // Row 1 Severity Cards: Y_Start = 472f, Y_End = 501f
        val r1SevY = 472f
        val sevH = 29f
        val sevW = 237f
        
        // Card 1: Major Severities
        paint.color = Color.parseColor("#FEF2F2")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(36f, r1SevY, 36f + sevW, r1SevY + sevH, 4f, 4f, paint)
        paint.color = Color.parseColor("#FCA5A5")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(36f, r1SevY, 36f + sevW, r1SevY + sevH, 4f, 4f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#991B1B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawTextShaped(majorNcrCount.toString() + "  Major Severities Risk Flags", 46f, r1SevY + 11f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("High-impact safety or structural defects requiring priority remedy.", 46f, r1SevY + 22f, paint)
        
        // Card 2: Minor Severities
        paint.color = Color.parseColor("#FFFBEB")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(310f, r1SevY, 310f + sevW, r1SevY + sevH, 4f, 4f, paint)
        paint.color = Color.parseColor("#FDE68A")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(310f, r1SevY, 310f + sevW, r1SevY + sevH, 4f, 4f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#92400E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawTextShaped(minorNcrCount.toString() + "  Minor Severity Issues Captured", 320f, r1SevY + 11f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Routine work quality deviations or minor specifications non-conformances.", 320f, r1SevY + 22f, paint)
        
        // Row 2 Severity Cards: Y_Start = 508f, Y_End = 537f
        val r2SevY = 508f
        
        // Card 3: Repeated
        paint.color = Color.parseColor("#FFF7ED")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(36f, r2SevY, 36f + sevW, r2SevY + sevH, 4f, 4f, paint)
        paint.color = Color.parseColor("#FED7AA")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(36f, r2SevY, 36f + sevW, r2SevY + sevH, 4f, 4f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#C2410C")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawTextShaped(repeatedFlagged.toString() + "  Recurring Findings Highlighted", 46f, r2SevY + 11f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Systemic or chronic gaps that repeated across previous inspection cycles.", 46f, r2SevY + 22f, paint)
        
        // Card 4: Financial Losses
        paint.color = Color.parseColor("#EFF6FF")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(310f, r2SevY, 310f + sevW, r2SevY + sevH, 4f, 4f, paint)
        paint.color = Color.parseColor("#BFDBFE")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(310f, r2SevY, 310f + sevW, r2SevY + sevH, 4f, 4f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E40AF")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawTextShaped(lossesFlagged.toString() + "  Findings Reporting Material Losses", 320f, r2SevY + 11f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Deviations resulting in resource, financial wastage, or rework man-hour losses.", 320f, r2SevY + 22f, paint)
        
        // Page 2 Footer Box
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(36f, fY, 559f, fY + 14f, paint)
        canvas.drawLine(156f, fY, 156f, fY + 14f, paint)
        canvas.drawLine(306f, fY, 306f, fY + 14f, paint)
        canvas.drawLine(366f, fY, 366f, fY + 14f, paint)
        canvas.drawLine(446f, fY, 446f, fY + 14f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Internal Quality Audit Report", 42f, fY + 9.5f, paint)
        canvas.drawTextShaped(details.report.formReference.ifEmpty { "innovo/QAQC/FRM - 1.14/05" }, 162f, fY + 9.5f, paint)
        canvas.drawTextShaped("REV 02", 312f, fY + 9.5f, paint)
        canvas.drawTextShaped("1-Mar-2023", 372f, fY + 9.5f, paint)
        canvas.drawTextShaped("Page $pageNumber of $totalP", 452f, fY + 9.5f, paint)
        
        pdfDocument.finishPage(page)
        
        // ================= PAGE 3: NCR & OBS ANALYTICS DATA TABLES (MATCHING MOBILE APP PERFECTLY) =================
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        canvas.scale(scaleFactor, scaleFactor)
        canvas.drawColor(Color.WHITE)
        
        // Elegant header box
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawRect(36f, 40f, 559f, 96f, paint)
        canvas.drawLine(146f, 40f, 146f, 96f, paint)
        canvas.drawLine(439f, 40f, 439f, 96f, paint)
        
        try {
            drawQcLogo(context, canvas, 36f + 29f, 40f + 6f, 44f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawTextShaped(centeredHeaderTitle2, 146f + (293f - centeredHeaderTitleWidth2)/2f, 73f, paint)
        paint.strokeWidth = 1.2f
        canvas.drawLine(
            146f + (293f - centeredHeaderTitleWidth2)/2f, 
            76f, 
            146f + (293f - centeredHeaderTitleWidth2)/2f + centeredHeaderTitleWidth2, 
            76f, 
            paint
        )
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawTextShaped(depText1, 553f - paint.measureText(depText1), 60f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawTextShaped(depText2, 553f - paint.measureText(depText2), 76f, paint)
        
        // Page 3 Section Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val p3Title = "NCR / OBS Detailed Analytics and Tracking Tables"
        canvas.drawTextShaped(p3Title, 36f, 116f, paint)
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 119f, 36f + paint.measureText(p3Title), 119f, paint)
        
        // Prepare rows for tables exactly like MainActivity
        val ncrTradeRows = standardTrades.map { tr ->
            val matching = findings.filter { getUnifiedTrade(it.trade) == tr && it.type == "NCR" }
            val o = matching.count { it.status == "Open" }
            val c = matching.count { it.status == "Closed" }
            val tot = o + c
            val rate = if (tot > 0) (c.toFloat() / tot * 100f) else 0f
            PDFTradeStats(tr, o, c, tot, rate)
        }
        
        val obsTradeRows = standardTrades.map { tr ->
            val matching = findings.filter { getUnifiedTrade(it.trade) == tr && it.type == "OBS" }
            val o = matching.count { it.status == "Open" }
            val c = matching.count { it.status == "Closed" }
            val tot = o + c
            val rate = if (tot > 0) (c.toFloat() / tot * 100f) else 0f
            PDFTradeStats(tr, o, c, tot, rate)
        }
        
        // Table 1: NCR Analysis by Trade
        var tableY = 132f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawTextShaped("Table 1. NCR Analysis by Trade Domain", 36f, tableY, paint)
        tableY += 6f
        
        // Draw Header row and border
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 0.5f
        canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
        canvas.drawLine(196f, tableY, 196f, tableY + 12f, paint)
        canvas.drawLine(286f, tableY, 286f, tableY + 12f, paint)
        canvas.drawLine(376f, tableY, 376f, tableY + 12f, paint)
        canvas.drawLine(466f, tableY, 466f, tableY + 12f, paint)
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        canvas.drawTextShaped("Trade Name", 42f, tableY + 9f, paint)
        canvas.drawTextShaped("Open", 202f, tableY + 9f, paint)
        canvas.drawTextShaped("Closed", 292f, tableY + 9f, paint)
        canvas.drawTextShaped("Total", 382f, tableY + 9f, paint)
        canvas.drawTextShaped("Closure %", 472f, tableY + 9f, paint)
        
        tableY += 12f
        
        ncrTradeRows.forEach { r ->
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(36f, tableY, 556f, tableY + 11f, paint)
            canvas.drawLine(196f, tableY, 196f, tableY + 11f, paint)
            canvas.drawLine(286f, tableY, 286f, tableY + 11f, paint)
            canvas.drawLine(376f, tableY, 376f, tableY + 11f, paint)
            canvas.drawLine(466f, tableY, 466f, tableY + 11f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            canvas.drawTextShaped(r.tradeName, 42f, tableY + 8f, paint)
            canvas.drawTextShaped(r.open.toString(), 202f, tableY + 8f, paint)
            canvas.drawTextShaped(r.closed.toString(), 292f, tableY + 8f, paint)
            canvas.drawTextShaped(r.total.toString(), 382f, tableY + 8f, paint)
            canvas.drawTextShaped("${String.format(java.util.Locale.US, "%.1f", r.closureRate)}%", 472f, tableY + 8f, paint)
            tableY += 11f
        }
        // Table 1 Total Row
        val ncrTotalOpen = ncrTradeRows.sumOf { it.open }
        val ncrTotalClosed = ncrTradeRows.sumOf { it.closed }
        val ncrTotalSum = ncrTotalOpen + ncrTotalClosed
        val ncrOverallRate = if (ncrTotalSum > 0) (ncrTotalClosed.toFloat() / ncrTotalSum * 100f) else 0f
        
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(36f, tableY, 556f, tableY + 11f, paint)
        canvas.drawLine(196f, tableY, 196f, tableY + 11f, paint)
        canvas.drawLine(286f, tableY, 286f, tableY + 11f, paint)
        canvas.drawLine(376f, tableY, 376f, tableY + 11f, paint)
        canvas.drawLine(466f, tableY, 466f, tableY + 11f, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawTextShaped("Total NCRs", 42f, tableY + 8f, paint)
        canvas.drawTextShaped(ncrTotalOpen.toString(), 202f, tableY + 8f, paint)
        canvas.drawTextShaped(ncrTotalClosed.toString(), 292f, tableY + 8f, paint)
        canvas.drawTextShaped(ncrTotalSum.toString(), 382f, tableY + 8f, paint)
        canvas.drawTextShaped("${String.format(java.util.Locale.US, "%.1f", ncrOverallRate)}%", 472f, tableY + 8f, paint)
        
        
        // Table 2: OBS Analysis by Trade
        tableY += 21f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawTextShaped("Table 2. OBS Analysis by Trade Domain", 36f, tableY, paint)
        tableY += 6f
        
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
        canvas.drawLine(196f, tableY, 196f, tableY + 12f, paint)
        canvas.drawLine(286f, tableY, 286f, tableY + 12f, paint)
        canvas.drawLine(376f, tableY, 376f, tableY + 12f, paint)
        canvas.drawLine(466f, tableY, 466f, tableY + 12f, paint)
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        canvas.drawTextShaped("Trade Name", 42f, tableY + 9f, paint)
        canvas.drawTextShaped("Open", 202f, tableY + 9f, paint)
        canvas.drawTextShaped("Closed", 292f, tableY + 9f, paint)
        canvas.drawTextShaped("Total", 382f, tableY + 9f, paint)
        canvas.drawTextShaped("Closure %", 472f, tableY + 9f, paint)
        
        tableY += 12f
        obsTradeRows.forEach { r ->
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(36f, tableY, 556f, tableY + 11f, paint)
            canvas.drawLine(196f, tableY, 196f, tableY + 11f, paint)
            canvas.drawLine(286f, tableY, 286f, tableY + 11f, paint)
            canvas.drawLine(376f, tableY, 376f, tableY + 11f, paint)
            canvas.drawLine(466f, tableY, 466f, tableY + 11f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            canvas.drawTextShaped(r.tradeName, 42f, tableY + 8f, paint)
            canvas.drawTextShaped(r.open.toString(), 202f, tableY + 8f, paint)
            canvas.drawTextShaped(r.closed.toString(), 292f, tableY + 8f, paint)
            canvas.drawTextShaped(r.total.toString(), 382f, tableY + 8f, paint)
            canvas.drawTextShaped("${String.format(java.util.Locale.US, "%.1f", r.closureRate)}%", 472f, tableY + 8f, paint)
            tableY += 11f
        }
        // Table 2 Total Row
        val obsTotalOpen = obsTradeRows.sumOf { it.open }
        val obsTotalClosed = obsTradeRows.sumOf { it.closed }
        val obsTotalSum = obsTotalOpen + obsTotalClosed
        val obsOverallRate = if (obsTotalSum > 0) (obsTotalClosed.toFloat() / obsTotalSum * 100f) else 0f
        
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(36f, tableY, 556f, tableY + 11f, paint)
        canvas.drawLine(196f, tableY, 196f, tableY + 11f, paint)
        canvas.drawLine(286f, tableY, 286f, tableY + 11f, paint)
        canvas.drawLine(376f, tableY, 376f, tableY + 11f, paint)
        canvas.drawLine(466f, tableY, 466f, tableY + 11f, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawTextShaped("Total OBS", 42f, tableY + 8f, paint)
        canvas.drawTextShaped(obsTotalOpen.toString(), 202f, tableY + 8f, paint)
        canvas.drawTextShaped(obsTotalClosed.toString(), 292f, tableY + 8f, paint)
        canvas.drawTextShaped(obsTotalSum.toString(), 382f, tableY + 8f, paint)
        canvas.drawTextShaped("${String.format(java.util.Locale.US, "%.1f", obsOverallRate)}%", 472f, tableY + 8f, paint)
        
        
        // Table 3: Cumulative Analysis Table (Monthly)
        tableY += 21f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawTextShaped("Table 3. Monthly Cumulative Gaps Progression Table", 36f, tableY, paint)
        tableY += 6f
        
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 0.5f
        canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
        canvas.drawLine(156f, tableY, 156f, tableY + 12f, paint)
        canvas.drawLine(256f, tableY, 256f, tableY + 12f, paint)
        canvas.drawLine(356f, tableY, 356f, tableY + 12f, paint)
        canvas.drawLine(456f, tableY, 456f, tableY + 12f, paint)
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        canvas.drawTextShaped("Month / Cycle", 42f, tableY + 9f, paint)
        canvas.drawTextShaped("NCR (Monthly)", 162f, tableY + 9f, paint)
        canvas.drawTextShaped("NCR (Cumulative)", 262f, tableY + 9f, paint)
        canvas.drawTextShaped("OBS (Monthly)", 362f, tableY + 9f, paint)
        canvas.drawTextShaped("OBS (Cumulative)", 462f, tableY + 9f, paint)
        
        tableY += 12f
        
        val monthsList = listOf("Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026")
        var pNcrCount = findings.count { it.type == "NCR" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
        var pObsCount = findings.count { it.type == "OBS" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
        
        monthsList.forEach { m ->
            val nM = findings.count { it.type == "NCR" && getMonthYearSymbol(it.issueDate) == m }
            val oM = findings.count { it.type == "OBS" && getMonthYearSymbol(it.issueDate) == m }
            pNcrCount += nM
            pObsCount += oM
            
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(36f, tableY, 556f, tableY + 11f, paint)
            canvas.drawLine(156f, tableY, 156f, tableY + 11f, paint)
            canvas.drawLine(256f, tableY, 256f, tableY + 11f, paint)
            canvas.drawLine(356f, tableY, 356f, tableY + 11f, paint)
            canvas.drawLine(456f, tableY, 456f, tableY + 11f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            canvas.drawTextShaped(m, 42f, tableY + 8f, paint)
            canvas.drawTextShaped(nM.toString(), 162f, tableY + 8f, paint)
            canvas.drawTextShaped(pNcrCount.toString(), 262f, tableY + 8f, paint)
            canvas.drawTextShaped(oM.toString(), 362f, tableY + 8f, paint)
            canvas.drawTextShaped(pObsCount.toString(), 462f, tableY + 8f, paint)
            tableY += 11f
        }
        
        // Table 4: Overdue NCR Details
        tableY += 21f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawTextShaped("Table 4. Critical Overdue NCRs Logs (Top 5 Items)", 36f, tableY, paint)
        tableY += 6f
        
        val overdueDetailsList = findings.filter { it.type == "NCR" && it.status == "Open" && getDaysLate(it.dueDate) > 0 }
            .sortedByDescending { getDaysLate(it.dueDate) }
            .take(5)
            
        if (overdueDetailsList.isEmpty()) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(36f, tableY, 556f, tableY + 16f, paint)
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.color = Color.GRAY
            canvas.drawTextShaped("No Overdue NCR items found.", 42f, tableY + 11f, paint)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(36f, tableY, 556f, tableY + 12f, paint)
            canvas.drawLine(176f, tableY, 176f, tableY + 12f, paint)
            canvas.drawLine(306f, tableY, 306f, tableY + 12f, paint)
            canvas.drawLine(436f, tableY, 436f, tableY + 12f, paint)
            
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 6.5f
            canvas.drawTextShaped("NCR No.", 42f, tableY + 9f, paint)
            canvas.drawTextShaped("Trade Domain", 182f, tableY + 9f, paint)
            canvas.drawTextShaped("Due Date", 312f, tableY + 9f, paint)
            canvas.drawTextShaped("Days Late", 442f, tableY + 9f, paint)
            
            tableY += 12f
            
            overdueDetailsList.forEach { r ->
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
                canvas.drawRect(36f, tableY, 556f, tableY + 11f, paint)
                canvas.drawLine(176f, tableY, 176f, tableY + 11f, paint)
                canvas.drawLine(306f, tableY, 306f, tableY + 11f, paint)
                canvas.drawLine(436f, tableY, 436f, tableY + 11f, paint)
                
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#990000")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 6.5f
                canvas.drawTextShaped(r.referenceId, 42f, tableY + 8f, paint)
                
                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawTextShaped(r.trade, 182f, tableY + 8f, paint)
                canvas.drawTextShaped(r.dueDate, 312f, tableY + 8f, paint)
                
                paint.color = Color.parseColor("#DC2626")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawTextShaped("${getDaysLate(r.dueDate)} Days", 442f, tableY + 8f, paint)
                
                tableY += 11f
            }
        }
        
        // Page 3 Footer Box
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(36f, fY, 559f, fY + 14f, paint)
        canvas.drawLine(156f, fY, 156f, fY + 14f, paint)
        canvas.drawLine(306f, fY, 306f, fY + 14f, paint)
        canvas.drawLine(366f, fY, 366f, fY + 14f, paint)
        canvas.drawLine(446f, fY, 446f, fY + 14f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Internal Quality Audit Report", 42f, fY + 9.5f, paint)
        canvas.drawTextShaped(details.report.formReference.ifEmpty { "innovo/QAQC/FRM - 1.14/05" }, 162f, fY + 9.5f, paint)
        canvas.drawTextShaped("REV 02", 312f, fY + 9.5f, paint)
        canvas.drawTextShaped("1-Mar-2023", 372f, fY + 9.5f, paint)
        canvas.drawTextShaped("Page $pageNumber of $totalP", 452f, fY + 9.5f, paint)
        
        pdfDocument.finishPage(page)
        
        // ================= PAGE 4: EXECUTIVE DIAGRAMS (S-CURVE TREND LINE & ROOT CAUSE PARETO DIAGRAMS) =================
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        canvas.scale(scaleFactor, scaleFactor)
        canvas.drawColor(Color.WHITE)
        
        // Elegant header box
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawRect(36f, 40f, 559f, 96f, paint)
        canvas.drawLine(146f, 40f, 146f, 96f, paint)
        canvas.drawLine(439f, 40f, 439f, 96f, paint)
        
        try {
            drawQcLogo(context, canvas, 36f + 29f, 40f + 6f, 44f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawTextShaped(centeredHeaderTitle2, 146f + (293f - centeredHeaderTitleWidth2)/2f, 73f, paint)
        paint.strokeWidth = 1.2f
        canvas.drawLine(
            146f + (293f - centeredHeaderTitleWidth2)/2f, 
            76f, 
            146f + (293f - centeredHeaderTitleWidth2)/2f + centeredHeaderTitleWidth2, 
            76f, 
            paint
        )
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawTextShaped(depText1, 553f - paint.measureText(depText1), 60f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawTextShaped(depText2, 553f - paint.measureText(depText2), 76f, paint)
        
        // Page 4 Section Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val p4Title = "NCR / OBS Executive Trends & Root Cause Diagrams"
        canvas.drawTextShaped(p4Title, 36f, 116f, paint)
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 119f, 36f + paint.measureText(p4Title), 119f, paint)
        
        // 1. TOP DIAGRAM: S-CURVE TREND LINE (Y: 132f to 412f)
        val sBoxL = 36f
        val sBoxT = 132f
        val sBoxR = 556f
        val sBoxB = 412f
        
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(sBoxL, sBoxT, sBoxR, sBoxB, 6f, 6f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(sBoxL, sBoxT, sBoxR, sBoxB, 6f, 6f, paint)
        
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawTextShaped("Monthly Gaps S-Curve Progression (Cumulative)", sBoxL + 12f, sBoxT + 16f, paint)
        
        // Calculate S-curve data
        val sMonths = listOf("Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026")
        var sNcrTotal = findings.count { it.type == "NCR" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
        var sObsTotal = findings.count { it.type == "OBS" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
        val sNcrCum = mutableListOf<Float>()
        val sObsCum = mutableListOf<Float>()
        sMonths.forEach { m ->
            sNcrTotal += findings.count { it.type == "NCR" && getMonthYearSymbol(it.issueDate) == m }
            sObsTotal += findings.count { it.type == "OBS" && getMonthYearSymbol(it.issueDate) == m }
            sNcrCum.add(sNcrTotal.toFloat())
            sObsCum.add(sObsTotal.toFloat())
        }
        
        val sMaxVal = maxOf(sNcrCum.maxOrNull() ?: 1f, sObsCum.maxOrNull() ?: 1f, 8f)
        val sScaleMax = (Math.ceil(sMaxVal / 10.0) * 10).toFloat().coerceAtLeast(10f)
        
        // S-curve Grid Area
        val sGridL = sBoxL + 45f
        val sGridT = sBoxT + 30f
        val sGridR = sBoxR - 20f
        val sGridB = sBoxB - 35f
        val sGridW = sGridR - sGridL
        val sGridH = sGridB - sGridT
        
        // Horizontal Grid Lines & Y-Axis Labels
        val sSteps = 5
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6.5f
        for (i in 0..sSteps) {
            val y = sGridB - (i * sGridH / sSteps)
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(sGridL, y, sGridR, y, paint)
            
            val labelVal = (sScaleMax * i / sSteps).toInt().toString()
            paint.color = Color.parseColor("#64748B")
            canvas.drawTextShaped(labelVal, sGridL - paint.measureText(labelVal) - 6f, y + 2.5f, paint)
        }
        
        // X-Axis month labels & Vertical Grid Lines
        val sPointX = FloatArray(sMonths.size)
        val sStepX = sGridW / (sMonths.size - 1).coerceAtLeast(1)
        sMonths.forEachIndexed { idx, m ->
            val x = sGridL + idx * sStepX
            sPointX[idx] = x
            
            // Vertical line
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawLine(x, sGridT, x, sGridB, paint)
            
            // Month abbreviation
            paint.color = Color.parseColor("#64748B")
            val monthLabel = m.split(" ")[0]
            canvas.drawTextShaped(monthLabel, x - paint.measureText(monthLabel) / 2f, sGridB + 11f, paint)
        }
        
        // Plot lines and data points
        val sNcrPoints = FloatArray(sMonths.size * 2)
        val sObsPoints = FloatArray(sMonths.size * 2)
        sMonths.indices.forEach { idx ->
            sNcrPoints[idx * 2] = sPointX[idx]
            sNcrPoints[idx * 2 + 1] = sGridB - (sNcrCum[idx] / sScaleMax * sGridH)
            
            sObsPoints[idx * 2] = sPointX[idx]
            sObsPoints[idx * 2 + 1] = sGridB - (sObsCum[idx] / sScaleMax * sGridH)
        }
        
        // Draw S-curves
        val pathNcr = Path()
        pathNcr.moveTo(sNcrPoints[0], sNcrPoints[1])
        for (i in 1 until sMonths.size) {
            pathNcr.lineTo(sNcrPoints[i * 2], sNcrPoints[i * 2 + 1])
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#00BFA5") // Teal
        canvas.drawPath(pathNcr, paint)
        
        val pathObs = Path()
        pathObs.moveTo(sObsPoints[0], sObsPoints[1])
        for (i in 1 until sMonths.size) {
            pathObs.lineTo(sObsPoints[i * 2], sObsPoints[i * 2 + 1])
        }
        paint.color = Color.parseColor("#ED7D31") // Orange
        canvas.drawPath(pathObs, paint)
        
        // Draw circles & values over points
        paint.style = Paint.Style.FILL
        sMonths.indices.forEach { idx ->
            // NCR circles
            paint.color = Color.parseColor("#00BFA5")
            canvas.drawCircle(sNcrPoints[idx * 2], sNcrPoints[idx * 2 + 1], 3.5f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(sNcrPoints[idx * 2], sNcrPoints[idx * 2 + 1], 1.8f, paint)
            // Value
            paint.color = Color.parseColor("#0F2644")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 6f
            val nValStr = sNcrCum[idx].toInt().toString()
            canvas.drawTextShaped(nValStr, sNcrPoints[idx * 2] - paint.measureText(nValStr)/2f, sNcrPoints[idx * 2 + 1] - 5f, paint)
            
            // OBS circles
            paint.color = Color.parseColor("#ED7D31")
            canvas.drawCircle(sObsPoints[idx * 2], sObsPoints[idx * 2 + 1], 3.5f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(sObsPoints[idx * 2], sObsPoints[idx * 2 + 1], 1.8f, paint)
            // Value
            paint.color = Color.parseColor("#0F2644")
            val oValStr = sObsCum[idx].toInt().toString()
            canvas.drawTextShaped(oValStr, sObsPoints[idx * 2] - paint.measureText(oValStr)/2f, sObsPoints[idx * 2 + 1] - 5f, paint)
        }
        
        // Draw Legend for S-curve
        paint.style = Paint.Style.FILL
        val sLegendY = sGridB + 23f
        
        paint.color = Color.parseColor("#00BFA5")
        canvas.drawRoundRect(sGridL + 50f, sLegendY - 3f, sGridL + 62f, sLegendY + 3f, 1f, 1f, paint)
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawTextShaped("Cumulative NCR Gaps Progression", sGridL + 67f, sLegendY + 2.5f, paint)
        
        paint.color = Color.parseColor("#ED7D31")
        canvas.drawRoundRect(sGridL + 210f, sLegendY - 3f, sGridL + 222f, sLegendY + 3f, 1f, 1f, paint)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawTextShaped("Cumulative OBS Gaps Progression", sGridL + 227f, sLegendY + 2.5f, paint)
        
        
        // 2. BOTTOM DIAGRAM: PARETO ROOT CAUSE ANALYSIS (Y: 432f to 712f)
        val pBoxL = 36f
        val pBoxT = 432f
        val pBoxR = 556f
        val pBoxB = 712f
        
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(pBoxL, pBoxT, pBoxR, pBoxB, 6f, 6f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(pBoxL, pBoxT, pBoxR, pBoxB, 6f, 6f, paint)
        
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawTextShaped("Root Cause Pareto Analysis (80/20 Rule Diagram)", pBoxL + 12f, pBoxT + 16f, paint)
        
        // Calculate Pareto data
        val pCategories = listOf("Workmanship", "Material Quality", "Lack of Supervision", "Equipment Failure", "Design Gaps", "Other")
        val pMap = mutableMapOf<String, Int>().apply {
            pCategories.forEach { put(it, 0) }
        }
        findings.forEach { f ->
            val cause = f.rootCause.lowercase()
            val cat = when {
                cause.contains("workman") || cause.contains("craft") || cause.contains("skill") -> "Workmanship"
                cause.contains("material") || cause.contains("spec") || cause.contains("quality") -> "Material Quality"
                cause.contains("supervis") || cause.contains("manage") || cause.contains("follow") -> "Lack of Supervision"
                cause.contains("equip") || cause.contains("tool") || cause.contains("machine") -> "Equipment Failure"
                cause.contains("design") || cause.contains("draw") || cause.contains("plan") -> "Design Gaps"
                else -> "Other"
            }
            pMap[cat] = pMap.getOrDefault(cat, 0) + 1
        }
        val pSortedList = pMap.toList().sortedByDescending { it.second }
        val pTotalCount = findings.size.coerceAtLeast(1)
        val pMaxCount = pSortedList.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
        val pScaleLeftMax = (Math.ceil(pMaxCount / 5.0) * 5).toInt().coerceAtLeast(5)
        
        val pGridL = pBoxL + 45f
        val pGridT = pBoxT + 30f
        val pGridR = pBoxR - 45f // Space for right scale
        val pGridB = pBoxB - 35f
        val pGridW = pGridR - pGridL
        val pGridH = pGridB - pGridT
        
        // Draw grid lines and left labels (frequencies) and right labels (cumulative %)
        val pSteps = 5
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6.5f
        for (i in 0..pSteps) {
            val y = pGridB - (i * pGridH / pSteps)
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(pGridL, y, pGridR, y, paint)
            
            // Left frequency label
            val leftLabel = (pScaleLeftMax * i / pSteps).toString()
            paint.color = Color.parseColor("#64748B")
            canvas.drawTextShaped(leftLabel, pGridL - paint.measureText(leftLabel) - 6f, y + 2.5f, paint)
            
            // Right cumulative % label
            val rightLabel = "${i * 20}%"
            canvas.drawTextShaped(rightLabel, pGridR + 6f, y + 2.5f, paint)
        }
        
        // Draw category bars and compute cumulative lines
        val pBarStep = pGridW / 6f
        val pBarW = pBarStep * 0.55f
        var pCumSum = 0
        val pCumPcts = FloatArray(6)
        val pBarCentersX = FloatArray(6)
        
        pSortedList.forEachIndexed { idx, item ->
            val catName = item.first
            val catCount = item.second
            pCumSum += catCount
            pCumPcts[idx] = pCumSum.toFloat() / pTotalCount.toFloat() * 100f
            
            val centerX = pGridL + (idx + 0.5f) * pBarStep
            pBarCentersX[idx] = centerX
            
            // Draw bar (frequency)
            val barH = (catCount.toFloat() / pScaleLeftMax.toFloat() * pGridH)
            val barL = centerX - pBarW / 2f
            val barR = centerX + pBarW / 2f
            val barT = pGridB - barH
            
            paint.color = Color.parseColor("#3B82F6") // modern blue bar
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(barL, barT, barR, pGridB, 2f, 2f, paint)
            
            // Show count value on top of bar if count > 0
            if (catCount > 0) {
                paint.color = Color.parseColor("#1E3A8A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 6.5f
                val countStr = catCount.toString()
                canvas.drawTextShaped(countStr, centerX - paint.measureText(countStr)/2f, barT - 4f, paint)
            }
            
            // Category label underneath (split or shortened to fit)
            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 5.5f
            val labelParts = if (catName.contains(" ")) catName.split(" ") else listOf(catName)
            if (labelParts.size == 1) {
                canvas.drawTextShaped(catName, centerX - paint.measureText(catName)/2f, pGridB + 10f, paint)
            } else {
                canvas.drawTextShaped(labelParts[0], centerX - paint.measureText(labelParts[0])/2f, pGridB + 10f, paint)
                if (labelParts.size > 1) {
                    canvas.drawTextShaped(labelParts[1], centerX - paint.measureText(labelParts[1])/2f, pGridB + 16f, paint)
                }
            }
        }
        
        // Draw Cumulative percentage Pareto S-Line
        val pLinePath = Path()
        val pLinePoints = FloatArray(12)
        pSortedList.indices.forEach { idx ->
            pLinePoints[idx * 2] = pBarCentersX[idx]
            pLinePoints[idx * 2 + 1] = pGridB - (pCumPcts[idx] / 100f * pGridH)
        }
        pLinePath.moveTo(pLinePoints[0], pLinePoints[1])
        for (i in 1..5) {
            pLinePath.lineTo(pLinePoints[i * 2], pLinePoints[i * 2 + 1])
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = Color.parseColor("#EF4444") // Coral red line
        canvas.drawPath(pLinePath, paint)
        
        // Draw nodes on line
        paint.style = Paint.Style.FILL
        pSortedList.indices.forEach { idx ->
            paint.color = Color.parseColor("#EF4444")
            canvas.drawCircle(pLinePoints[idx * 2], pLinePoints[idx * 2 + 1], 3f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(pLinePoints[idx * 2], pLinePoints[idx * 2 + 1], 1.5f, paint)
            
            // Draw percentage label over node
            paint.color = Color.parseColor("#991B1B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 5.5f
            val pctStr = "${pCumPcts[idx].toInt()}%"
            canvas.drawTextShaped(pctStr, pLinePoints[idx * 2] - paint.measureText(pctStr)/2f, pLinePoints[idx * 2 + 1] - 4f, paint)
        }
        
        // Draw Pareto Legend
        val pLegendY = pGridB + 23f
        paint.style = Paint.Style.FILL
        
        paint.color = Color.parseColor("#3B82F6")
        canvas.drawRoundRect(pGridL + 50f, pLegendY - 3f, pGridL + 62f, pLegendY + 3f, 1f, 1f, paint)
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawTextShaped("Root Cause Gaps Frequency (Count)", pGridL + 67f, pLegendY + 2.5f, paint)
        
        paint.color = Color.parseColor("#EF4444")
        canvas.drawRoundRect(pGridL + 210f, pLegendY - 3f, pGridL + 222f, pLegendY + 3f, 1f, 1f, paint)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawTextShaped("Cumulative Pareto Impact Line (80/20 Rule)", pGridL + 227f, pLegendY + 2.5f, paint)
        
        
        // Page 4 Footer Box
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(36f, fY, 559f, fY + 14f, paint)
        canvas.drawLine(156f, fY, 156f, fY + 14f, paint)
        canvas.drawLine(306f, fY, 306f, fY + 14f, paint)
        canvas.drawLine(366f, fY, 366f, fY + 14f, paint)
        canvas.drawLine(446f, fY, 446f, fY + 14f, paint)
        
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
        canvas.drawTextShaped("Internal Quality Audit Report", 42f, fY + 9.5f, paint)
        canvas.drawTextShaped(details.report.formReference.ifEmpty { "innovo/QAQC/FRM - 1.14/05" }, 162f, fY + 9.5f, paint)
        canvas.drawTextShaped("REV 02", 312f, fY + 9.5f, paint)
        canvas.drawTextShaped("1-Mar-2023", 372f, fY + 9.5f, paint)
        canvas.drawTextShaped("Page $pageNumber of $totalP", 452f, fY + 9.5f, paint)
        
        pdfDocument.finishPage(page)
        
        // 5. FINDINGS DETAILS (PAGINATED ON SUCCESSIVE PAGES FROM PAGE 4 OUTWARDS)
        findings.forEachIndexed { fIdx, fi ->
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.scale(scaleFactor, scaleFactor)
            
            canvas.drawColor(Color.WHITE)
            
            val tableX = marginX
            val tableW = 520f
            val tableYStart = 45f
            
            // --- HEADER BOX DESIGN ---
            val headerH = 65f
            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 1.0f
            }
            val fillPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            
            // Draw the outer rectangle of the header
            canvas.drawRect(tableX, tableYStart, tableX + tableW, tableYStart + headerH, borderPaint)
            
            // Vertical split line 1: separating logo at X = tableX + 130f
            val splitX1 = tableX + 130f
            canvas.drawLine(splitX1, tableYStart, splitX1, tableYStart + headerH, borderPaint)
            
            // Vertical split line 2: separating metadata at X = tableX + 390f
            val splitX2 = tableX + 390f
            canvas.drawLine(splitX2, tableYStart, splitX2, tableYStart + headerH, borderPaint)
            
            // --- 1. LOGO SECTION (Left side, from tableX to splitX1 - Vector direct drawing) ---
            try {
                val targetH = 45f
                val logoX = tableX + (130f - targetH) / 2f
                val logoY = tableYStart + (headerH - targetH) / 2f
                drawQcLogo(context, canvas, logoX, logoY, targetH)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // --- 2. FINDING REFERENCE SECTION (Middle side, from splitX1 to splitX2) ---
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            
            val refText = fi.referenceId.ifEmpty { "Finding #${fi.id}" }
            val textWidth = paint.measureText(refText)
            
            val refX = splitX1 + (260f - textWidth) / 2f
            val refY = tableYStart + 38f
            canvas.drawTextShaped(refText, refX, refY, paint)
            
            // --- 3. METADATA SECTION (Right side, from splitX2 to tableX + tableW) ---
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            
            val metaLines = listOf(
                "Quality Control Department",
                "Internal Audit",
                "NCR / OBS Form",
                "Audit #${details.report.auditNumber.ifEmpty { "N/A" }}"
            )
            
            var metaY = tableYStart + 14f
            metaLines.forEach { line ->
                val lineWidth = paint.measureText(line)
                val lineX = (tableX + tableW) - lineWidth - 8f
                canvas.drawTextShaped(line, lineX, metaY, paint)
                metaY += 11f
            }
            
            // --- MAIN GRID TABLE ---
            val contentYStart = tableYStart + headerH + 10f
            val gridYStart = contentYStart
            val headerHeight = 22f
            
            // Draw backgrounds for headers
            val headerBgColor = Color.parseColor("#EEF4FB")
            paint.color = headerBgColor
            paint.style = Paint.Style.FILL
            canvas.drawRect(tableX, gridYStart, tableX + tableW, gridYStart + headerHeight, paint)
            
            // Draw border outlines for headers
            canvas.drawRect(tableX, gridYStart, tableX + tableW, gridYStart + headerHeight, borderPaint)
            canvas.drawLine(tableX + 210f, gridYStart, tableX + 210f, gridYStart + headerHeight, borderPaint)
            canvas.drawLine(tableX + 420f, gridYStart, tableX + 420f, gridYStart + headerHeight, borderPaint)
            
            paint.color = Color.BLACK
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            // h1
            val isObsType = fi.type.trim().uppercase() == "OBS"
            val h1Text = if (isObsType) "OBS Photo (By auditor)" else "NCR Photo (By auditor)"
            val h1W = paint.measureText(h1Text)
            canvas.drawTextShaped(h1Text, tableX + (210f - h1W) / 2f, gridYStart + 14f, paint)
            
            // h2
            val h2Text = "Rectification Photo (By auditee)"
            val h2W = paint.measureText(h2Text)
            canvas.drawTextShaped(h2Text, tableX + 210f + (210f - h2W) / 2f, gridYStart + 14f, paint)
            
            // h3
            val h3Text = if (isObsType) "OBS Status (By Auditor)" else "NCR Status (By Auditor)"
            val h3W = paint.measureText(h3Text)
            canvas.drawTextShaped(h3Text, tableX + 420f + (100f - h3W) / 2f, gridYStart + 14f, paint)
            
            // --- PHOTOS & COMMENTS ROW ---
            val photosYStart = gridYStart + headerHeight
            val photosH = 165f
            
            canvas.drawRect(tableX, photosYStart, tableX + tableW, photosYStart + photosH, borderPaint)
            canvas.drawLine(tableX + 210f, photosYStart, tableX + 210f, photosYStart + photosH, borderPaint)
            canvas.drawLine(tableX + 420f, photosYStart, tableX + 420f, photosYStart + photosH, borderPaint)
            
            // Auditor photos: 2x2 grid or single large collage/photo
            val hasMultiplePhotos = !fi.ph2Base64.isNullOrEmpty() || !fi.ph3Base64.isNullOrEmpty() || !fi.ph4Base64.isNullOrEmpty()
            
            val photoSlots = if (!hasMultiplePhotos) {
                listOf(
                    Pair(fi.ph1Base64, RectF(tableX + 4f, photosYStart + 4f, tableX + 206f, photosYStart + 161f))
                )
            } else {
                listOf(
                    Pair(fi.ph1Base64, RectF(tableX + 4f, photosYStart + 4f, tableX + 103f, photosYStart + 80f)),
                    Pair(fi.ph2Base64, RectF(tableX + 107f, photosYStart + 4f, tableX + 206f, photosYStart + 80f)),
                    Pair(fi.ph3Base64, RectF(tableX + 4f, photosYStart + 84f, tableX + 103f, photosYStart + 160f)),
                    Pair(fi.ph4Base64, RectF(tableX + 107f, photosYStart + 84f, tableX + 206f, photosYStart + 160f))
                )
            }
            
            photoSlots.forEachIndexed { sIdx, pair ->
                val base64 = pair.first
                val rect = pair.second
                paint.color = Color.parseColor("#F5F5F7")
                paint.style = Paint.Style.FILL
                canvas.drawRect(rect, paint)
                
                paint.color = Color.parseColor("#E0E0E0")
                paint.style = Paint.Style.STROKE
                canvas.drawRect(rect, paint)
                paint.style = Paint.Style.FILL
                
                val pBmp = decodeBase64ToBitmap(context, base64)
                if (pBmp != null) {
                    try {
                        // Draw full resolution bitmap into the bounding rect directly to preserve quality
                        canvas.drawBitmap(pBmp, null, rect, paint)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pBmp.recycle()
                    }
                } else {
                    paint.color = Color.parseColor("#999999")
                    paint.textSize = 6f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val label = if (!hasMultiplePhotos) {
                        if (isObsType) "OBS Photo / Collage" else "NCR Photo / Collage"
                    } else {
                        "Photo ${sIdx + 1}"
                    }
                    val lw = paint.measureText(label)
                    canvas.drawTextShaped(label, rect.left + (rect.width() - lw) / 2f, rect.top + rect.height() / 2f + 2f, paint)
                }
            }
            
            // Auditee Rectification/Closure Photo inside Col 2
            val auditeeRect = RectF(tableX + 216f, photosYStart + 6f, tableX + 414f, photosYStart + 159f)
            paint.color = Color.parseColor("#F5F5F7")
            paint.style = Paint.Style.FILL
            canvas.drawRect(auditeeRect, paint)
            
            paint.color = Color.parseColor("#E0E0E0")
            paint.style = Paint.Style.STROKE
            canvas.drawRect(auditeeRect, paint)
            paint.style = Paint.Style.FILL
            
            val closureBmp = decodeBase64ToBitmap(context, fi.auditeeClosurePhoto)
            if (closureBmp != null) {
                try {
                    // Draw full resolution bitmap into the bounding rect directly to preserve quality
                    canvas.drawBitmap(closureBmp, null, auditeeRect, paint)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    closureBmp.recycle()
                }
            } else {
                paint.color = Color.parseColor("#999999")
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val label = "No Rectification Photo"
                val lw = paint.measureText(label)
                canvas.drawTextShaped(label, auditeeRect.left + (auditeeRect.width() - lw) / 2f, auditeeRect.top + auditeeRect.height() / 2f + 3f, paint)
            }
            
            // Auditor review comments in Col 3
            val col3X = tableX + 420f
            val col3W = 100f
            
            paint.color = Color.BLACK
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Comments:", col3X + 6f, photosYStart + 12f, paint)
            
            val commentsText = if (fi.auditeeResponse.isNotEmpty()) {
                fi.auditeeResponse
            } else if (details.report.reviewerRemarks.isNotEmpty()) {
                details.report.reviewerRemarks
            } else {
                "No comments added yet."
            }
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            drawMultilineText(
                text = commentsText,
                x = col3X + 6f,
                y = photosYStart + 22f,
                width = col3W - 12f,
                lineHeight = 9f,
                canvas = canvas,
                paint = paint,
                maxLines = 15
            )
            
            // --- DETAILS SECTIONS GRID ---
            val detailsYStart = photosYStart + photosH
            val tableYEnd = detailsYStart + 246f
            
            // Outer box
            canvas.drawRect(tableX, detailsYStart, tableX + tableW, tableYEnd, borderPaint)
            canvas.drawLine(tableX + 210f, detailsYStart, tableX + 210f, tableYEnd, borderPaint)
            canvas.drawLine(tableX + 420f, detailsYStart, tableX + 420f, tableYEnd, borderPaint)
            
            // --- AUDITOR COLUMN (Left Column) ---
            var rY = detailsYStart
            val labelW = 48f
            
            // Row 1: Trade (18f)
            paint.color = Color.BLACK
            canvas.drawLine(tableX, rY + 18f, tableX + 210f, rY + 18f, borderPaint)
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Trade:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(fi.trade, tableX + labelW, rY + 12f, paint)
            rY += 18f
            
            // Row 2: Activity (18f)
            canvas.drawLine(tableX, rY + 18f, tableX + 210f, rY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Activity:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(fi.activity, tableX + labelW, rY + 12f, paint)
            rY += 18f
            
            // Row 3: Location (18f)
            canvas.drawLine(tableX, rY + 18f, tableX + 210f, rY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Location:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(fi.locationZone, tableX + labelW, rY + 12f, paint)
            rY += 18f
            
            // Row 4: Description (76f)
            canvas.drawLine(tableX, rY + 76f, tableX + 210f, rY + 76f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Description:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            drawMultilineText(
                text = fi.description,
                x = tableX + 4f,
                y = rY + 22f,
                width = 202f,
                lineHeight = 9f,
                canvas = canvas,
                paint = paint,
                maxLines = 6
            )
            rY += 76f
            
            // Row 5: Type (20f) with simulated checkboxes
            canvas.drawLine(tableX, rY + 20f, tableX + 210f, rY + 20f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Type:", tableX + 4f, rY + 13f, paint)
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val ncrBoxChecked = fi.type == "NCR"
            paint.color = if (ncrBoxChecked) Color.BLACK else Color.LTGRAY
            paint.style = Paint.Style.STROKE
            canvas.drawRect(tableX + 48f, rY + 5f, tableX + 58f, rY + 15f, paint)
            if (ncrBoxChecked) {
                paint.style = Paint.Style.FILL
                canvas.drawRect(tableX + 50f, rY + 7f, tableX + 56f, rY + 13f, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            canvas.drawTextShaped("NCR", tableX + 64f, rY + 13f, paint)
            
            val obsBoxChecked = fi.type == "OBS"
            paint.color = if (obsBoxChecked) Color.BLACK else Color.LTGRAY
            paint.style = Paint.Style.STROKE
            canvas.drawRect(tableX + 104f, rY + 5f, tableX + 114f, rY + 15f, paint)
            if (obsBoxChecked) {
                paint.style = Paint.Style.FILL
                canvas.drawRect(tableX + 106f, rY + 7f, tableX + 112f, rY + 13f, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            canvas.drawTextShaped("OBS", tableX + 120f, rY + 13f, paint)
            rY += 20f
            
            // Row 6: Issuing Date (18f)
            canvas.drawLine(tableX, rY + 18f, tableX + 210f, rY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Issuing Date:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(fi.issueDate.ifEmpty { details.report.auditDate }, tableX + labelW + 16f, rY + 12f, paint)
            rY += 18f
            
            // Row 7: Due Date (18f)
            canvas.drawLine(tableX, rY + 18f, tableX + 210f, rY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Due Date:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(fi.dueDate, tableX + labelW + 16f, rY + 12f, paint)
            rY += 18f
            
            // Row 8: Auditor Name (18f)
            canvas.drawLine(tableX, rY + 18f, tableX + 210f, rY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Auditor Name:", tableX + 4f, rY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(details.report.auditorName, tableX + labelW + 20f, rY + 12f, paint)
            rY += 18f
            
            // Row 9: Auditor Signature (42f)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Auditor Signature:", tableX + 4f, rY + 12f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.LTGRAY
            canvas.drawLine(tableX + 4f, rY + 32f, tableX + 200f, rY + 32f, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.typeface = Typeface.create("serif", Typeface.ITALIC)
            paint.textSize = 9.5f
            canvas.drawTextShaped(details.report.sigAuditorName, tableX + 50f, rY + 28f, paint)
            paint.textSize = 7f
            
            // --- AUDITEE COLUMN (Middle Column) ---
            var midY = detailsYStart
            val col2X = tableX + 210f
            
            // Row 1: Corrective Action Box (130f)
            paint.color = Color.BLACK
            canvas.drawLine(col2X, midY + 130f, col2X + 210f, midY + 130f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f
            canvas.drawTextShaped("Corrective Action Required:", col2X + 6f, midY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            val correctiveActionText = if (fi.correctiveAction.isNotEmpty()) fi.correctiveAction else fi.auditeeCorrectiveAction
            drawMultilineText(
                text = correctiveActionText,
                x = col2X + 6f,
                y = midY + 22f,
                width = 202f,
                lineHeight = 9f,
                canvas = canvas,
                paint = paint,
                maxLines = 10
            )
            midY += 130f
            
            // Row 2: Root Cause (20f)
            canvas.drawLine(col2X, midY + 20f, col2X + 210f, midY + 20f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7f
            canvas.drawTextShaped("Root Cause:", col2X + 6f, midY + 13f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val rcaText = if (fi.rootCause.isNotEmpty()) fi.rootCause else fi.auditeeRca
            canvas.drawTextShaped(rcaText, col2X + 60f, midY + 13f, paint)
            midY += 20f
            
            // Row 3: Reply Date (18f)
            canvas.drawLine(col2X, midY + 18f, col2X + 210f, midY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Reply Date:", col2X + 6f, midY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val replyDateStr = if (fi.replyDate.isNotEmpty()) fi.replyDate else if (fi.auditeeTargetDate.isNotEmpty()) fi.auditeeTargetDate else "N/A"
            canvas.drawTextShaped(replyDateStr, col2X + 60f, midY + 12f, paint)
            midY += 18f
            
            // Row 4: QC Manager Name (18f)
            canvas.drawLine(col2X, midY + 18f, col2X + 210f, midY + 18f, borderPaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("QC Manager name:", col2X + 6f, midY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawTextShaped(details.report.qcManager, col2X + 86f, midY + 12f, paint)
            midY += 18f
            
            // Row 5: QC Manager Signature (60f)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("QC Manager Signature:", col2X + 6f, midY + 12f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.LTGRAY
            canvas.drawLine(col2X + 6f, midY + 45f, col2X + 200f, midY + 45f, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.typeface = Typeface.create("serif", Typeface.ITALIC)
            paint.textSize = 9.5f
            canvas.drawTextShaped(details.report.sigReviewerName, col2X + 50f, midY + 40f, paint)
            paint.textSize = 7f
            
            // --- STATUS COLUMN (Right Column) ---
            val c3X = tableX + 420f
            
            // Box 1: Verified area
            paint.color = Color.parseColor("#FAFAFA")
            paint.style = Paint.Style.FILL
            canvas.drawRect(c3X, detailsYStart, c3X + 100f, detailsYStart + 130f, paint)
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawRect(c3X, detailsYStart, c3X + 100f, detailsYStart + 130f, borderPaint)
            paint.style = Paint.Style.FILL
            
            // Box 2: Yellow Status Highlight Box
            val yellowBg = Color.parseColor("#FFF59D")
            paint.color = yellowBg
            canvas.drawRect(c3X, detailsYStart + 130f, c3X + 100f, detailsYStart + 202f, paint)
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawRect(c3X, detailsYStart + 130f, c3X + 100f, detailsYStart + 202f, borderPaint)
            paint.style = Paint.Style.FILL
            
            paint.color = Color.BLACK
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val isObsTypeLower = fi.type.trim().uppercase() == "OBS"
            val statusLabel = if (isObsTypeLower) "OBS Status:" else "NCR Status:"
            canvas.drawTextShaped(statusLabel, c3X + 6f, detailsYStart + 144f, paint)
            
            val statVal = fi.status.uppercase()
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statColor = if (statVal == "OPEN") Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
            paint.color = statColor
            val statW = paint.measureText(statVal)
            canvas.drawTextShaped(statVal, c3X + (100f - statW) / 2f, detailsYStart + 185f, paint)
            paint.color = Color.BLACK
            
            // Box 3: Auditor Verification signature
            paint.color = Color.WHITE
            canvas.drawRect(c3X, detailsYStart + 202f, c3X + 100f, tableYEnd, paint)
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawRect(c3X, detailsYStart + 202f, c3X + 100f, tableYEnd, borderPaint)
            paint.style = Paint.Style.FILL
            
            paint.textSize = 6.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawTextShaped("Auditor Signature:", c3X + 6f, detailsYStart + 214f, paint)
            
            paint.typeface = Typeface.create("serif", Typeface.ITALIC)
            paint.textSize = 7f
            canvas.drawTextShaped(details.report.sigAuditorName, c3X + 12f, detailsYStart + 232f, paint)
            
            // Findings page Footer Box
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(36f, fY, 559f, fY + 14f, paint)
            canvas.drawLine(156f, fY, 156f, fY + 14f, paint)
            canvas.drawLine(306f, fY, 306f, fY + 14f, paint)
            canvas.drawLine(366f, fY, 366f, fY + 14f, paint)
            canvas.drawLine(446f, fY, 446f, fY + 14f, paint)
            
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6f
            canvas.drawTextShaped("Internal Quality Audit Report", 42f, fY + 9.5f, paint)
            canvas.drawTextShaped(details.report.formReference.ifEmpty { "innovo/QAQC/FRM - 1.14/05" }, 162f, fY + 9.5f, paint)
            canvas.drawTextShaped("REV 02", 312f, fY + 9.5f, paint)
            canvas.drawTextShaped("1-Mar-2023", 372f, fY + 9.5f, paint)
            canvas.drawTextShaped("Page $pageNumber of $totalP", 452f, fY + 9.5f, paint)
            
            pdfDocument.finishPage(page)
        }
        
        // Write out PDF to storage
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    private fun getLogoBase64(context: Context): String {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_qc_audit_logo_1781912328532) ?: return ""
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // DOCX (COMPLIANT OOXML ZIP FORMAT EXPORT WITH ALTCHUNK AND EMBEDDED IMAGES / MS OFFICE COMPATIBLE)
    fun exportToDoc(context: Context, details: AuditReportWithDetails, file: File) {
        val h = StringBuilder()
        
        val logoBase64 = getLogoBase64(context)
        
        h.append("<!DOCTYPE html>")
        h.append("<html>")
        h.append("<head><meta charset='utf-8'/>")
        h.append("<style>")
        h.append("body { font-family: 'Arial', sans-serif; font-size: 11pt; color: #1a1a18; line-height: 1.4; }")
        h.append(".header-table { width: 100%; border-collapse: collapse; border: 2pt solid #0D253F; margin-bottom: 20px; }")
        h.append(".header-left { padding: 12px; background: #0D253F; color: white; vertical-align: middle; }")
        h.append(".header-title { font-size: 16pt; font-weight: bold; font-family: 'Arial', sans-serif; margin-bottom: 3px; }")
        h.append(".h-sub { font-size: 9.5pt; opacity: 0.85; }")
        h.append(".title { text-align: center; font-size: 20pt; font-weight: bold; color: #0D253F; margin-top: 15px; margin-bottom: 5px; }")
        h.append(".subtitle { text-align: center; font-size: 11pt; color: #666666; margin-bottom: 25px; }")
        h.append("h3 { color: #0D253F; font-size: 13pt; font-weight: bold; border-bottom: 1.5pt solid #0D253F; padding-bottom: 4px; margin-top: 25px; margin-bottom: 10px; text-transform: uppercase; }")
        h.append("table.data-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 10pt; }")
        h.append("table.data-table th, table.data-table td { border: 1px solid #cccccc; padding: 7px 10px; text-align: left; vertical-align: middle; }")
        h.append("table.data-table th { background-color: #EEF4FB; color: #0D253F; font-weight: bold; }")
        h.append(".summary-card { padding: 12px; text-align: center; background: #F5F5F5; border: 1px solid #E0E0E0; border-radius: 6px; }")
        h.append(".summary-num { font-size: 18pt; font-weight: bold; line-height: 1; margin-bottom: 4px; }")
        h.append(".sig-box { border: 1px solid #cccccc; background: #FAFAFA; padding: 10px; width: 48%; display: inline-block; box-sizing: border-box; vertical-align: top; }")
        h.append(".finding-header { background: #0D253F; color: white; padding: 8px 12px; font-size: 11pt; font-weight: bold; border-radius: 4px 4px 0 0; margin-top: 20px; }")
        h.append(".photo-grid { margin-top: 10px; margin-bottom: 20px; width: 100%; border-collapse: collapse; }")
        h.append(".photo-cell { border: 1px solid #E0E0E0; background: #FAFAFA; padding: 10px; width: 20%; text-align: center; vertical-align: middle; font-size: 8pt; }")
        h.append("</style></head><body>")
        
        // Letterhead
        h.append("<table class='header-table'><tr>")
        if (logoBase64.isNotEmpty()) {
            h.append("<td style='width: 85px; background: #ffffff; padding: 10px; text-align: center; vertical-align: middle;'>")
            h.append("<img src='$logoBase64' style='max-width: 85px; max-height: 75px; object-fit: contain;' />")
            h.append("</td>")
        }
        h.append("<td class='header-left'>")
        h.append("<div class='header-title'>QUALITY CONTROL DEPARTMENT</div>")
        h.append("<div class='h-sub'>Internal Audit Report &nbsp;•&nbsp; Issuance Date: <b>${details.report.reportIssuanceDate}</b></div>")
        h.append("</td></tr></table>")
        
        // Title
        h.append("<div class='title'>QC Internal Audit Report</div>")
        h.append("<div class='subtitle'>Report #${details.report.auditNumber} &nbsp;|&nbsp; Project: ${details.report.projectName} &nbsp;|&nbsp; Date: ${details.report.auditDate}</div>")
        
        // 1. Project Information
        h.append("<h3>1. Project Information</h3>")
        h.append("<table class='data-table'>")
        h.append("<tr><td style='width:20%;font-weight:bold;background:#F5F5F5'>Project Name</td><td style='width:30%'>${details.report.projectName}</td>")
        h.append("<td style='width:20%;font-weight:bold;background:#F5F5F5'>Project Number</td><td style='width:30%'>${details.report.projectNumber}</td></tr>")
        h.append("<tr><td style='font-weight:bold;background:#F5F5F5'>Location / Site</td><td>${details.report.location}</td>")
        h.append("<td style='font-weight:bold;background:#F5F5F5'>Project Manager</td><td>${details.report.projectManager}</td></tr>")
        h.append("<tr><td style='font-weight:bold;background:#F5F5F5'>QC Manager</td><td>${details.report.qcManager}</td>")
        h.append("<td style='font-weight:bold;background:#F5F5F5'>Contractor / Sub</td><td>${details.report.contractor}</td></tr>")
        h.append("<tr><td style='font-weight:bold;background:#F5F5F5'>Stage / Phase</td><td>${details.report.phase}</td>")
        h.append("<td style='font-weight:bold;background:#F5F5F5'>Project Type</td><td>${details.report.projectType}</td></tr>")
        h.append("<tr><td style='font-weight:bold;background:#F5F5F5'>Audit Date</td><td>${details.report.auditDate}</td>")
        h.append("<td style='font-weight:bold;background:#F5F5F5'>Form Reference</td><td>${details.report.formReference}</td></tr>")
        h.append("<tr><td style='font-weight:bold;background:#F5F5F5'>Auditor Name</td><td>${details.report.auditorName}</td>")
        h.append("<td style='font-weight:bold;background:#F5F5F5'>Audit Scope / Zone</td><td>${details.report.auditScope}</td></tr>")
        h.append("</table>")
        
        // Counts
        val ncrCount = details.findings.count { it.type == "NCR" }
        val obsCount = details.findings.count { it.type == "OBS" }
        val majorCount = details.findings.count { it.severity == "Major" }
        val minorCount = details.findings.count { it.severity == "Minor" }
        val openCount = details.findings.count { it.status == "Open" }
        val closedCount = details.findings.count { it.status == "Closed" }
        
        // 2. Findings Summary
        h.append("<h3>2. Findings Summary</h3>")
        h.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px'>")
        h.append("<tr><td>")
        h.append("<div class='summary-card'><div class='summary-num' style='color:#A32D2D'>$ncrCount</div><div style='font-size:9pt'>NCRs</div></div></td><td>")
        h.append("<div class='summary-card'><div class='summary-num' style='color:#854F0B'>$obsCount</div><div style='font-size:9pt'>Observations</div></div></td><td>")
        h.append("<div class='summary-card'><div class='summary-num' style='color:#A32D2D'>$majorCount</div><div style='font-size:9pt'>Major</div></div></td><td>")
        h.append("<div class='summary-card'><div class='summary-num' style='color:#854F0B'>$minorCount</div><div style='font-size:9pt'>Minor</div></div></td><td>")
        h.append("<div class='summary-card'><div class='summary-num' style='color:#854F0B'>$openCount</div><div style='font-size:9pt'>Open</div></div></td><td>")
        h.append("<div class='summary-card'><div class='summary-num' style='color:#3B6D11'>$closedCount</div><div style='font-size:9pt'>Closed</div></div>")
        h.append("</td></tr></table>")
        
        // Tracking sheet and Previous history
        h.append("<h4 style='color:#0D253F;font-size:11.5pt;margin-top:15px;margin-bottom:10px;text-transform:uppercase;border-bottom:1px solid #D9E1F2;padding-bottom:3px;'>2.5 NCR & OBS Tracking Sheet</h4>")
        h.append("<table class='data-table'>")
        h.append("<thead>")
        h.append("<tr style='background:#1F4E79;color:white;'>")
        h.append("<th style='background:#1F4E79;color:white;'>Trade</th>")
        h.append("<th style='background:#2F5597;color:white;text-align:center;'>NCR - Open</th>")
        h.append("<th style='background:#2F5597;color:white;text-align:center;'>Major - Closed</th>")
        h.append("<th style='background:#2F5597;color:white;text-align:center;'>NCR Total Cumulative</th>")
        h.append("<th style='background:#2F5597;color:white;text-align:center;'>OBS - Open</th>")
        h.append("<th style='background:#2F5597;color:white;text-align:center;'>Minor - Closed</th>")
        h.append("<th style='background:#2F5597;color:white;text-align:center;'>OBS Total Cumulative</th>")
        h.append("</tr>")
        h.append("</thead>")
        h.append("<tbody>")
        
        val standardTrades = listOf("Civil", "Structural", "Architectural", "MEP", "Infrastructure", "Safety")
        var lastNcrCumulative = 0
        var lastObsCumulative = 0
        
        standardTrades.forEach { tr ->
            val tFindings = details.findings.filter { f -> f.trade.trim().equals(tr, ignoreCase = true) || f.trade.lowercase().startsWith(tr.lowercase()) || tr.lowercase().startsWith(f.trade.lowercase()) }
            val ncrOpen = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) && f.status.trim().equals("Open", ignoreCase = true) }
            val ncrClosed = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) && f.status.trim().equals("Closed", ignoreCase = true) }
            val obsOpen = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) && f.status.trim().equals("Open", ignoreCase = true) }
            val obsClosed = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) && f.status.trim().equals("Closed", ignoreCase = true) }
            val ncrCumulative = lastNcrCumulative + ncrOpen - ncrClosed
            val obsCumulative = lastObsCumulative + obsOpen - obsClosed
            lastNcrCumulative = ncrCumulative
            lastObsCumulative = obsCumulative
            
            h.append("<tr>")
            h.append("<td style='font-weight:bold;background:#ffffff;'>$tr</td>")
            h.append("<td style='background:#D9E1F2;text-align:center;font-weight:bold;color:#1F4E79;'>$ncrOpen</td>")
            h.append("<td style='background:#D9E1F2;text-align:center;font-weight:bold;color:#1F4E79;'>$ncrClosed</td>")
            h.append("<td style='background:#ffffff;text-align:center;font-weight:bold;'>$ncrCumulative</td>")
            h.append("<td style='background:#D9E1F2;text-align:center;font-weight:bold;color:#1F4E79;'>$obsOpen</td>")
            h.append("<td style='background:#D9E1F2;text-align:center;font-weight:bold;color:#1F4E79;'>$obsClosed</td>")
            h.append("<td style='background:#ffffff;text-align:center;font-weight:bold;'>$obsCumulative</td>")
            h.append("</tr>")
        }
        h.append("</tbody>")
        h.append("</table>")
        
        // 2.7 Visual Data Analytics Dashboard with Charts
        h.append("<h4 style='color:#0D253F;font-size:11.5pt;margin-top:24px;margin-bottom:12px;text-transform:uppercase;border-bottom:1px solid #D9E1F2;padding-bottom:3px;'>2.7 Executive Analytics Dashboard</h4>")
        
        h.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;'>")
        h.append("<tr>")
        
        h.append("<td style='width:50%;padding:5px;'>")
        h.append("<div style='background:#FEF2F2;border:1px solid #FCA5A5;padding:12px;border-radius:4px;'>")
        h.append("<div style='font-size:18pt;font-weight:bold;color:#991B1B;'>$majorCount</div>")
        h.append("<div style='font-size:9pt;color:#7F1D1D;font-weight:bold;'>Major Severity Red Flags</div>")
        h.append("<div style='font-size:7.5pt;color:#B91C1C;margin-top:4px;'>High-impact safety / structural defects require immediate mitigation.</div>")
        h.append("</div></td>")
        
        h.append("<td style='width:50%;padding:5px;'>")
        h.append("<div style='background:#FFFBEB;border:1px solid #FDE68A;padding:12px;border-radius:4px;'>")
        h.append("<div style='font-size:18pt;font-weight:bold;color:#92400E;'>$minorCount</div>")
        h.append("<div style='font-size:9pt;color:#78350F;font-weight:bold;'>Minor Severity Issues</div>")
        h.append("<div style='font-size:7.5pt;color:#D97706;margin-top:4px;'>Routine work quality / compliance gaps on audited trades.</div>")
        h.append("</div></td>")
        
        h.append("</tr><tr>")
        
        val repeatedIssues = details.findings.count { it.repeated == "Yes" }
        h.append("<td style='padding:5px;'>")
        h.append("<div style='background:#FFF7ED;border:1px solid #FED7AA;padding:12px;border-radius:4px;'>")
        h.append("<div style='font-size:18pt;font-weight:bold;color:#C2410C;'>$repeatedIssues</div>")
        h.append("<div style='font-size:9pt;color:#7C2D12;font-weight:bold;'>Recurring (Repeated) Gaps</div>")
        h.append("<div style='font-size:7.5pt;color:#EA580C;margin-top:4px;'>Chronic issues repeating from historical inspections.</div>")
        h.append("</div></td>")
        
        val lossIssues = details.findings.count { it.materialLosses.trim().isNotEmpty() && !it.materialLosses.trim().equals("None", true) }
        h.append("<td style='padding:5px;'>")
        h.append("<div style='background:#EFF6FF;border:1px solid #BFDBFE;padding:12px;border-radius:4px;'>")
        h.append("<div style='font-size:18pt;font-weight:bold;color:#1E40AF;'>$lossIssues</div>")
        h.append("<div style='font-size:9pt;color:#1E3A8A;font-weight:bold;'>Material / Man-hour Losses</div>")
        h.append("<div style='font-size:7.5pt;color:#2563EB;margin-top:4px;'>Findings with financial or asset wastage impacts flagged.</div>")
        h.append("</div></td>")
        
        h.append("</tr>")
        h.append("</table>")
        
        // Trade-wise progress bars visual representation chart
        h.append("<div style='font-weight:bold;color:#0D253F;margin-top:15px;margin-bottom:8px;font-size:10pt;'>Findings Distribution by Trades (Graphical Progress Bars)</div>")
        h.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;font-size:8.5pt;'>")
        h.append("<tr style='background:#F1F5F9;'>")
        h.append("<th style='padding:6px;width:25%;border:1px solid #CBD5E1;text-align:left;'>Trade Domain</th>")
        h.append("<th style='padding:6px;width:75%;border:1px solid #CBD5E1;text-align:left;'>Visual Volume Bar (Red = NCRs, Amber = OBSs)</th>")
        h.append("</tr>")
        
        val maxTotalFindings = details.findings.size.coerceAtLeast(1)
        standardTrades.forEach { tr ->
            val tFindings = details.findings.filter { f -> f.trade.trim().equals(tr, ignoreCase = true) || f.trade.lowercase().startsWith(tr.lowercase()) }
            val ncrSub = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) }
            val obsSub = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) }
            val rowSum = ncrSub + obsSub
            
            val ncrPercent = if (maxTotalFindings > 0) (ncrSub * 100 / maxTotalFindings).coerceAtMost(100) else 0
            val obsPercent = if (maxTotalFindings > 0) (obsSub * 100 / maxTotalFindings).coerceAtMost(100) else 0
            val remainingPercent = 100 - ncrPercent - obsPercent
            
            h.append("<tr>")
            h.append("<td style='padding:6px;font-weight:bold;border:1px solid #E2E8F0;background:#F8FAFC;'>$tr ($rowSum)</td>")
            h.append("<td style='padding:6px;border:1px solid #E2E8F0;'>")
            if (rowSum > 0) {
                h.append("<table style='width:100%;border-collapse:collapse;'><tr>")
                if (ncrPercent > 0) {
                    h.append("<td style='background:#EF4444;width:$ncrPercent%;height:14px;color:white;font-size:7pt;text-align:center;'>$ncrSub NCR</td>")
                }
                if (obsPercent > 0) {
                    h.append("<td style='background:#F59E0B;width:$obsPercent%;height:14px;color:white;font-size:7pt;text-align:center;'>$obsSub OBS</td>")
                }
                if (remainingPercent > 0) {
                    h.append("<td style='background:#F1F5F9;width:$remainingPercent%;height:14px;'></td>")
                }
                h.append("</tr></table>")
            } else {
                h.append("<div style='color:#94A3B8;font-size:7.5pt;'>No deviations flagged in current audit cycle</div>")
            }
            h.append("</td></tr>")
        }
        h.append("</table>")
        
        // Circular resolution rate dial gauge (SVG representation)
        val grandTotal = (openCount + closedCount).coerceAtLeast(1)
        val closedRatePerc = (closedCount * 100 / grandTotal)
        val openRatePerc = 100 - closedRatePerc
        
        h.append("<div style='font-weight:bold;color:#0D253F;margin-top:15px;margin-bottom:8px;font-size:10pt;'>Overall Issue Closure Quality Index</div>")
        h.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;font-size:9pt;'><tr>")
        
        h.append("<td style='width:35%;text-align:center;padding:10px;border:1px solid #E2E8F0;background:#F8FAFC;border-radius:4px;'>")
        h.append("<svg width='110' height='110' style='display:inline-block;'>")
        h.append("<circle cx='55' cy='55' r='42' fill='none' stroke='#E2E8F0' stroke-width='8'></circle>")
        val ringCircumference = 264
        val dashOffsetValue = (ringCircumference * (100 - closedRatePerc)) / 100
        h.append("<circle cx='55' cy='55' r='42' fill='none' stroke='#10B981' stroke-width='8' stroke-dasharray='$ringCircumference' stroke-dashoffset='$dashOffsetValue' transform='rotate(-90 55 55)' stroke-linecap='round'></circle>")
        h.append("<text x='55' y='59' font-family='Arial, sans-serif' font-size='15' font-weight='bold' text-anchor='middle' fill='#0D253F'>$closedRatePerc%</text>")
        h.append("<text x='55' y='72' font-family='Arial, sans-serif' font-size='7' text-anchor='middle' fill='#64748B'>RESOLVED</text>")
        h.append("</svg>")
        h.append("</td>")
        
        h.append("<td style='width:65%;padding:12px;vertical-align:middle;border:1px solid #E2E8F0;border-left:none;'>")
        h.append("<div style='font-weight:bold;font-size:9.5pt;margin-bottom:6px;color:#1E293B;'>Audit Issue Closing Performance Rate:</div>")
        h.append("<div style='margin-bottom:4px;'><span style='display:inline-block;width:10px;height:10px;background:#10B981;margin-right:6px;'></span>")
        h.append("<b>Closed, Rectified, and Verified:</b> $closedCount out of $grandTotal ($closedRatePerc%)</div>")
        h.append("<div><span style='display:inline-block;width:10px;height:10px;background:#EF4444;margin-right:6px;'></span>")
        h.append("<b>Outstanding, Needing Attention:</b> $openCount out of $grandTotal ($openRatePerc%)</div>")
        h.append("</td>")
        
        h.append("</tr></table>")
        
        // Side-by-side Charts Table: S-Curve & Pareto Diagrams
        h.append("<div style='font-weight:bold; color:#0D253F; margin-top:24px; margin-bottom:10px; font-size:11pt; text-transform:uppercase; border-bottom:1px solid #D9E1F2; padding-bottom:3px;'>2.8 Trend Analytics & Root Cause Analysis</div>")
        h.append("<table style='width:100%; border-collapse:collapse; margin-bottom:20px;'><tr>")
        h.append("<td style='width:50%; padding-right:10px; text-align:center;'>")
        h.append(generateSCurveSvg(details.findings))
        h.append("</td>")
        h.append("<td style='width:50%; padding-left:10px; text-align:center;'>")
        h.append(generateParetoSvg(details.findings))
        h.append("</td>")
        h.append("</tr></table>")
        
        if (details.historyRows.isNotEmpty()) {
            h.append("<h3>3. History of Previous Audits</h3>")
            h.append("<table class='data-table'>")
            h.append("<tr><th>Audit #</th><th>Audit Date</th><th>NCRs Issued</th><th>NCRs Closed</th><th>OBS Issued</th><th>OBS Closed</th><th>Auditor Name</th></tr>")
            details.historyRows.forEach { r ->
                h.append("<tr><td>${r.auditNumber}</td>")
                h.append("<td>${r.auditDate}</td>")
                h.append("<td>${r.ncrsIssued}</td>")
                h.append("<td>${r.ncrsClosed}</td>")
                h.append("<td>${r.obsIssued}</td>")
                h.append("<td>${r.obsClosed}</td>")
                h.append("<td>${r.auditorName}</td></tr>")
            }
            h.append("</table>")
        }
        
        // 4. Detailed Findings
        if (details.findings.isNotEmpty()) {
            h.append("<h3>4. Audit Findings Details</h3>")
            details.findings.forEachIndexed { idx, fi ->
                h.append("<div class='finding-header'>Finding ${idx+1}: ${fi.referenceId} [${fi.type} / ${fi.severity} / ${fi.status}]</div>")
                h.append("<table class='data-table' style='margin-bottom:0'>")
                h.append("<tr><td style='font-weight:bold;width:20%;background:#F9F9F9'>Trade</td><td style='width:30%'>${fi.trade}</td>")
                h.append("<td style='font-weight:bold;width:20%;background:#F9F9F9'>Activity</td><td style='width:30%'>${fi.activity}</td></tr>")
                h.append("<tr><td style='font-weight:bold;background:#F9F9F9'>Location / Zone</td><td colspan='3'>${fi.locationZone}</td></tr>")
                h.append("<tr><td style='font-weight:bold;background:#F9F9F9'>Description</td><td colspan='3'>${fi.description}</td></tr>")
                h.append("<tr><td style='font-weight:bold;background:#F9F9F9'>Negative Impact</td><td>${fi.negativeImpact}</td>")
                h.append("<td style='font-weight:bold;background:#F9F9F9'>Losses (Estimated)</td><td>${fi.materialLosses}</td></tr>")
                h.append("<tr><td style='font-weight:bold;background:#F9F9F9'>Root Cause Reason</td><td>${fi.rootCause}</td>")
                h.append("<td style='font-weight:bold;background:#F9F9F9'>Repeated?</td><td>${fi.repeated}</td></tr>")
                h.append("<tr><td style='font-weight:bold;background:#F9F9F9'>Corrective Action</td><td colspan='3'>${fi.correctiveAction}</td></tr>")
                h.append("<tr><td style='font-weight:bold;background:#F9F9F9'>Proposed Due Date</td><td colspan='3'>${fi.dueDate}</td></tr>")
                h.append("</table>")
                
                // Embedded Base64 photos - Make them display super cleanly
                h.append("<table class='photo-grid'><tr>")
                val pLabels = listOf("Auditor finding photo 1", "Auditor finding photo 2", "Extra photo 1", "Extra photo 2", "Auditee closure/rectification")
                val pData = listOf(fi.ph1Base64, fi.ph2Base64, fi.ph3Base64, fi.ph4Base64, fi.auditeeClosurePhoto)
                pData.forEachIndexed { pIdx, base64 ->
                    h.append("<td class='photo-cell'>")
                    h.append("<b>${pLabels[pIdx]}</b><br/><br/>")
                    
                    val imageSrc = if (base64 != null && base64.startsWith("file:")) {
                        try {
                            val filename = base64.substring(5)
                            val dir = java.io.File(context.filesDir, "audit_photos")
                            val file = java.io.File(dir, filename)
                            if (file.exists()) {
                                val options = android.graphics.BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                                var inSampleSize = 1
                                val maxDim = 2048
                                if (options.outWidth > maxDim || options.outHeight > maxDim) {
                                    var tempSize = 1
                                    val originalMaxSide = Math.max(options.outWidth, options.outHeight)
                                    while (originalMaxSide / (tempSize * 2) >= maxDim) {
                                        tempSize *= 2
                                    }
                                    inSampleSize = tempSize
                                }
                                var bmp: android.graphics.Bitmap? = null
                                try {
                                    val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                                        this.inSampleSize = inSampleSize
                                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                    }
                                    bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                                } catch (oom: OutOfMemoryError) {
                                    System.gc()
                                    try {
                                        val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                                            this.inSampleSize = inSampleSize
                                            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                                        }
                                        bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                                    } catch (oom2: OutOfMemoryError) {
                                        System.gc()
                                    }
                                }
                                if (bmp != null) {
                                    val baos = java.io.ByteArrayOutputStream()
                                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, baos)
                                    val bytes = baos.toByteArray()
                                    bmp.recycle()
                                    baos.close()
                                    "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    } else if (base64 != null && base64.trim().isNotEmpty()) {
                        if (base64.startsWith("data:image")) base64 else "data:image/jpeg;base64,${base64.trim()}"
                    } else {
                        null
                    }

                    if (imageSrc != null) {
                        h.append("<img src='$imageSrc' style='max-width:180px;max-height:150px;border-radius:4px' />")
                    } else {
                        h.append("<span style='color:#aaaaaa'>(No Photo)</span>")
                    }
                    h.append("</td>")
                }
                h.append("</tr></table>")
            }
        }
        
        // 5. Signatures
        h.append("<h3 style='margin-top:30px'>5. Project Signatures</h3>")
        h.append("<div style='width:100%'>")
        h.append("<div class='sig-box'>")
        h.append("<b>Issued by (Auditor)</b><br/><br/>")
        h.append("Name: ${details.report.sigAuditorName}<br/>")
        h.append("Designation: ${details.report.sigAuditorDesignation}<br/>")
        h.append("Date: ${details.report.sigAuditorDate}<br/><br/>")
        h.append("<span style='color:#aaaaaa'>Signature: __________________________</span>")
        h.append("</div>")
        
        h.append("<div class='sig-box' style='float:right'>")
        h.append("<b>Reviewed by (QA/QC Director)</b><br/><br/>")
        h.append("Name: ${details.report.sigReviewerName}<br/>")
        h.append("Designation: ${details.report.sigReviewerDesignation}<br/>")
        h.append("Date: ${details.report.sigReviewerDate}<br/><br/>")
        h.append("<span style='color:#aaaaaa'>Signature: __________________________</span>")
        h.append("</div>")
        h.append("</div>")
        h.append("<div style='clear:both'></div>")
        
        h.append("<br/><br/><div style='text-align:center;font-size:8.5pt;color:#888888;margin-top:20px'>")
        if (details.report.formReference.isNotEmpty()) {
            h.append("${details.report.formReference}<br/>")
        }
        h.append("QC Internal Audit Form   |   نموذج المراجعة الداخلية لضبط الجودة")
        h.append("</div>")
        h.append("</body></html>")
        
        val htmlDocString = h.toString()
        
        // Package inside ZIP to create a completely valid, uncorruptible .docx OpenXML Document
        try {
            val zos = java.util.zip.ZipOutputStream(java.io.FileOutputStream(file))
            
            // 1. Write [Content_Types].xml
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="html" ContentType="text/html"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/htmlDoc.html" ContentType="text/html"/>
</Types>"""
            zos.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
            zos.write(contentTypes.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 2. Write _rels/.rels
            val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            zos.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
            zos.write(rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 3. Write word/_rels/document.xml.rels
            val docXmlRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId100" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/aFChunk" Target="htmlDoc.html"/>
</Relationships>"""
            zos.putNextEntry(java.util.zip.ZipEntry("word/_rels/document.xml.rels"))
            zos.write(docXmlRels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 4. Write word/document.xml
            val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
    <w:altChunk r:id="rId100"/>
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
    </w:sectPr>
  </w:body>
</w:document>"""
            zos.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
            zos.write(documentXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 5. Write word/htmlDoc.html
            zos.putNextEntry(java.util.zip.ZipEntry("word/htmlDoc.html"))
            zos.write(htmlDocString.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            zos.flush()
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun generateSCurveSvg(findings: List<com.example.data.Finding>): String {
        val months = listOf("Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026")
        var ncrTotal = findings.count { it.type == "NCR" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
        var obsTotal = findings.count { it.type == "OBS" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
        val ncrCum = mutableListOf<Float>()
        val obsCum = mutableListOf<Float>()
        months.forEach { m ->
            ncrTotal += findings.count { it.type == "NCR" && getMonthYearSymbol(it.issueDate) == m }
            obsTotal += findings.count { it.type == "OBS" && getMonthYearSymbol(it.issueDate) == m }
            ncrCum.add(ncrTotal.toFloat())
            obsCum.add(obsTotal.toFloat())
        }
        
        val maxVal = maxOf(ncrCum.maxOrNull() ?: 1f, obsCum.maxOrNull() ?: 1f, 8f)
        val scaleMax = (Math.ceil(maxVal / 10.0) * 10).toFloat().coerceAtLeast(10f)
        
        val width = 500f
        val height = 240f
        val padL = 45f
        val padT = 35f
        val padR = 25f
        val padB = 40f
        val plotW = width - padL - padR
        val plotH = height - padT - padB
        
        val sb = java.lang.StringBuilder()
        sb.append("<svg width='$width' height='$height' style='background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; font-family:Arial,sans-serif;'>")
        
        // Title
        sb.append("<text x='15' y='18' font-size='9.5' font-weight='bold' fill='#0f172a'>Monthly Gaps S-Curve Progression (Cumulative)</text>")
        
        // Grid lines & Y-axis labels
        val steps = 5
        for (i in 0..steps) {
            val y = padT + plotH - (i * plotH / steps)
            sb.append("<line x1='$padL' y1='$y' x2='${width - padR}' y2='$y' stroke='#e2e8f0' stroke-width='1' />")
            val lblVal = (scaleMax * i / steps).toInt().toString()
            sb.append("<text x='${padL - 6}' y='${y + 3}' font-size='8' fill='#64748b' text-anchor='end'>$lblVal</text>")
        }
        
        // X-axis labels
        val pointsX = FloatArray(months.size)
        val stepX = plotW / (months.size - 1).coerceAtLeast(1)
        months.forEachIndexed { idx, m ->
            val x = padL + idx * stepX
            pointsX[idx] = x
            sb.append("<line x1='$x' y1='$padT' x2='$x' y2='${height - padB}' stroke='#f1f5f9' stroke-width='1' />")
            val shortMonth = m.split(" ")[0]
            sb.append("<text x='$x' y='${height - padB + 14}' font-size='8' fill='#64748b' text-anchor='middle'>$shortMonth</text>")
        }
        
        // Draw NCR Line
        sb.append("<path d='")
        months.indices.forEach { idx ->
            val x = pointsX[idx]
            val y = padT + plotH - (ncrCum[idx] / scaleMax * plotH)
            if (idx == 0) sb.append("M $x $y") else sb.append(" L $x $y")
        }
        sb.append("' fill='none' stroke='#00BFA5' stroke-width='2' />")
        
        // Draw OBS Line
        sb.append("<path d='")
        months.indices.forEach { idx ->
            val x = pointsX[idx]
            val y = padT + plotH - (obsCum[idx] / scaleMax * plotH)
            if (idx == 0) sb.append("M $x $y") else sb.append(" L $x $y")
        }
        sb.append("' fill='none' stroke='#ED7D31' stroke-width='2' />")
        
        // Circles & text over points
        months.indices.forEach { idx ->
            val x = pointsX[idx]
            val yN = padT + plotH - (ncrCum[idx] / scaleMax * plotH)
            val yO = padT + plotH - (obsCum[idx] / scaleMax * plotH)
            
            // NCR marker
            sb.append("<circle cx='$x' cy='$yN' r='4' fill='#00BFA5' stroke='#ffffff' stroke-width='1.5' />")
            sb.append("<text x='$x' y='${yN - 6}' font-size='7.5' font-weight='bold' fill='#0f172a' text-anchor='middle'>${ncrCum[idx].toInt()}</text>")
            
            // OBS marker
            sb.append("<circle cx='$x' cy='$yO' r='4' fill='#ED7D31' stroke='#ffffff' stroke-width='1.5' />")
            sb.append("<text x='$x' y='${yO - 6}' font-size='7.5' font-weight='bold' fill='#0f172a' text-anchor='middle'>${obsCum[idx].toInt()}</text>")
        }
        
        // Legend at bottom
        val legY = height - 12
        sb.append("<rect x='${padL + 20}' y='${legY - 6}' width='12' height='6' fill='#00BFA5' rx='1' />")
        sb.append("<text x='${padL + 38}' y='${legY - 1}' font-size='8' fill='#1e293b'>Cumulative NCR Gaps</text>")
        
        sb.append("<rect x='${padL + 180}' y='${legY - 6}' width='12' height='6' fill='#ED7D31' rx='1' />")
        sb.append("<text x='${padL + 198}' y='${legY - 1}' font-size='8' fill='#1e293b'>Cumulative OBS Gaps</text>")
        
        sb.append("</svg>")
        return sb.toString()
    }

    private fun generateParetoSvg(findings: List<com.example.data.Finding>): String {
        val pCategories = listOf("Workmanship", "Material Quality", "Lack of Supervision", "Equipment Failure", "Design Gaps", "Other")
        val pMap = mutableMapOf<String, Int>().apply {
            pCategories.forEach { put(it, 0) }
        }
        findings.forEach { f ->
            val cause = f.rootCause.lowercase()
            val cat = when {
                cause.contains("workman") || cause.contains("craft") || cause.contains("skill") -> "Workmanship"
                cause.contains("material") || cause.contains("spec") || cause.contains("quality") -> "Material Quality"
                cause.contains("supervis") || cause.contains("manage") || cause.contains("follow") -> "Lack of Supervision"
                cause.contains("equip") || cause.contains("tool") || cause.contains("machine") -> "Equipment Failure"
                cause.contains("design") || cause.contains("draw") || cause.contains("plan") -> "Design Gaps"
                else -> "Other"
            }
            pMap[cat] = pMap.getOrDefault(cat, 0) + 1
        }
        val pSortedList = pMap.toList().sortedByDescending { it.second }
        val pTotalCount = findings.size.coerceAtLeast(1)
        val pMaxCount = pSortedList.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
        val pScaleLeftMax = (Math.ceil(pMaxCount / 5.0) * 5).toInt().coerceAtLeast(5)
        
        val width = 500f
        val height = 240f
        val padL = 45f
        val padT = 35f
        val padR = 45f
        val padB = 40f
        val plotW = width - padL - padR
        val plotH = height - padT - padB
        
        val sb = java.lang.StringBuilder()
        sb.append("<svg width='$width' height='$height' style='background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; font-family:Arial,sans-serif;'>")
        
        // Title
        sb.append("<text x='15' y='18' font-size='9.5' font-weight='bold' fill='#0f172a'>Root Cause Pareto Analysis Chart (80/20 Rule)</text>")
        
        // Grid lines & Labels (left and right)
        val steps = 5
        for (i in 0..steps) {
            val y = padT + plotH - (i * plotH / steps)
            sb.append("<line x1='$padL' y1='$y' x2='${width - padR}' y2='$y' stroke='#e2e8f0' stroke-width='1' />")
            
            // Left
            val leftVal = (pScaleLeftMax * i / steps).toString()
            sb.append("<text x='${padL - 6}' y='${y + 3}' font-size='8' fill='#64748b' text-anchor='end'>$leftVal</text>")
            
            // Right
            val rightVal = "${i * 20}%"
            sb.append("<text x='${width - padR + 6}' y='${y + 3}' font-size='8' fill='#64748b' text-anchor='start'>$rightVal</text>")
        }
        
        // Columns & Cumulative line coordinates
        val barStep = plotW / 6f
        val barW = barStep * 0.55f
        var cumSum = 0
        val cumPcts = FloatArray(6)
        val barCentersX = FloatArray(6)
        
        pSortedList.forEachIndexed { idx, item ->
            val catName = item.first
            val catCount = item.second
            cumSum += catCount
            cumPcts[idx] = cumSum.toFloat() / pTotalCount.toFloat() * 100f
            
            val centerX = padL + (idx + 0.5f) * barStep
            barCentersX[idx] = centerX
            
            // Bar
            val barH = catCount.toFloat() / pScaleLeftMax.toFloat() * plotH
            val barL = centerX - barW / 2f
            val barT = padT + plotH - barH
            sb.append("<rect x='$barL' y='$barT' width='$barW' height='$barH' fill='#3B82F6' rx='2' />")
            
            // Count text on top of bar
            if (catCount > 0) {
                sb.append("<text x='$centerX' y='${barT - 4}' font-size='8' font-weight='bold' fill='#1e3a8a' text-anchor='middle'>$catCount</text>")
            }
            
            // X label
            val labelParts = if (catName.contains(" ")) catName.split(" ") else listOf(catName)
            if (labelParts.size == 1) {
                sb.append("<text x='$centerX' y='${height - padB + 13}' font-size='7' fill='#475569' text-anchor='middle'>$catName</text>")
            } else {
                sb.append("<text x='$centerX' y='${height - padB + 11}' font-size='7' fill='#475569' text-anchor='middle'>${labelParts[0]}</text>")
                sb.append("<text x='$centerX' y='${height - padB + 18}' font-size='7' fill='#475569' text-anchor='middle'>${labelParts[1]}</text>")
            }
        }
        
        // Draw Cumulative percentage Pareto Line
        sb.append("<path d='")
        pSortedList.indices.forEach { idx ->
            val x = barCentersX[idx]
            val y = padT + plotH - (cumPcts[idx] / 100f * plotH)
            if (idx == 0) sb.append("M $x $y") else sb.append(" L $x $y")
        }
        sb.append("' fill='none' stroke='#EF4444' stroke-width='2' />")
        
        // Draw circles over cumulative percentage line
        pSortedList.indices.forEach { idx ->
            val x = barCentersX[idx]
            val y = padT + plotH - (cumPcts[idx] / 100f * plotH)
            sb.append("<circle cx='$x' cy='$y' r='3.5' fill='#EF4444' stroke='#ffffff' stroke-width='1.5' />")
            sb.append("<text x='$x' y='${y - 6}' font-size='7.5' font-weight='bold' fill='#991b1b' text-anchor='middle'>${cumPcts[idx].toInt()}%</text>")
        }
        
        // Pareto Legend
        val legY = height - 12
        sb.append("<rect x='${padL + 20}' y='${legY - 6}' width='12' height='6' fill='#3B82F6' rx='1' />")
        sb.append("<text x='${padL + 38}' y='${legY - 1}' font-size='8' fill='#1e293b'>Gaps Count</text>")
        
        sb.append("<rect x='${padL + 180}' y='${legY - 6}' width='12' height='6' fill='#EF4444' rx='1' />")
        sb.append("<text x='${padL + 198}' y='${legY - 1}' font-size='8' fill='#1e293b'>Cumulative Impact %</text>")
        
        sb.append("</svg>")
        return sb.toString()
    }

    // PPTX (COMPLIANT OOXML ZIP FORMAT PRESENTATION EXPORT / MS OFFICE COMPATIBLE)
    fun exportToPptx(context: Context, details: AuditReportWithDetails, file: File) {
        val findings = details.findings
        val totalSlides = 5 + findings.size
        
        fun makeRect(id: Int, x: Long, y: Long, cx: Long, cy: Long, fillColorHex: String): String {
            return """
              <p:sp>
                <p:nvSpPr>
                  <p:cNvPr id="$id" name="Rect_$id"/>
                  <p:cNvSpPr/>
                  <p:nvPr/>
                </p:nvSpPr>
                <p:spPr>
                  <a:xfrm>
                    <a:off x="$x" y="$y"/>
                    <a:ext cx="$cx" cy="$cy"/>
                  </a:xfrm>
                  <a:prstGeom prst="rect">
                    <a:avLst/>
                  </a:prstGeom>
                  <a:solidFill>
                    <a:srgbClr val="$fillColorHex"/>
                  </a:solidFill>
                  <a:ln>
                    <a:noFill/>
                  </a:ln>
                </p:spPr>
              </p:sp>
            """
        }

        fun makeOval(id: Int, x: Long, y: Long, cx: Long, cy: Long, fillColorHex: String): String {
            return """
              <p:sp>
                <p:nvSpPr>
                  <p:cNvPr id="$id" name="Oval_$id"/>
                  <p:cNvSpPr/>
                  <p:nvPr/>
                </p:nvSpPr>
                <p:spPr>
                  <a:xfrm>
                    <a:off x="$x" y="$y"/>
                    <a:ext cx="$cx" cy="$cy"/>
                  </a:xfrm>
                  <a:prstGeom prst="ellipse">
                    <a:avLst/>
                  </a:prstGeom>
                  <a:solidFill>
                    <a:srgbClr val="$fillColorHex"/>
                  </a:solidFill>
                  <a:ln>
                    <a:noFill/>
                  </a:ln>
                </p:spPr>
              </p:sp>
            """
        }

        fun makeLine(id: Int, x1: Long, y1: Long, x2: Long, y2: Long, strokeColorHex: String, thicknessEmu: Long = 20000L): String {
            val left = Math.min(x1, x2)
            val top = Math.min(y1, y2)
            val cx = Math.max(1L, Math.abs(x1 - x2))
            val cy = Math.max(1L, Math.abs(y1 - y2))
            
            val flipHAttr = if (x1 > x2) " flipH=\"1\"" else ""
            val flipVAttr = if (y1 > y2) " flipV=\"1\"" else ""
            
            return """
              <p:sp>
                <p:nvSpPr>
                  <p:cNvPr id="$id" name="Line_$id"/>
                  <p:cNvSpPr/>
                  <p:nvPr/>
                </p:nvSpPr>
                <p:spPr>
                  <a:xfrm$flipHAttr$flipVAttr>
                    <a:off x="$left" y="$top"/>
                    <a:ext cx="$cx" cy="$cy"/>
                  </a:xfrm>
                  <a:prstGeom prst="line">
                    <a:avLst/>
                  </a:prstGeom>
                  <a:ln w="$thicknessEmu">
                    <a:solidFill>
                      <a:srgbClr val="$strokeColorHex"/>
                    </a:solidFill>
                  </a:ln>
                </p:spPr>
              </p:sp>
            """
        }

        fun makeTextBox(id: Int, name: String, x: Long, y: Long, cx: Long, cy: Long, text: String, sizePt: Int = 18, colorHex: String = "000000", isBold: Boolean = false, align: String = "l"): String {
            val bTag = if (isBold) " b=\"1\"" else ""
            val szValue = sizePt * 100
            val alignAttr = when (align) {
                "ctr" -> " align=\"ctr\""
                "r" -> " align=\"r\""
                else -> ""
            }
            val escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
            return """
              <p:sp>
                <p:nvSpPr>
                  <p:cNvPr id="$id" name="$name"/>
                  <p:cNvSpPr>
                    <a:spLocks noGrp="1"/>
                  </p:cNvSpPr>
                  <p:nvPr/>
                </p:nvSpPr>
                <p:spPr>
                  <a:xfrm>
                    <a:off x="$x" y="$y"/>
                    <a:ext cx="$cx" cy="$cy"/>
                  </a:xfrm>
                  <a:prstGeom prst="rect">
                    <a:avLst/>
                  </a:prstGeom>
                </p:spPr>
                <p:txBody>
                  <a:bodyPr rtlCol="0" anchor="ctr"/>
                  <a:lstStyle/>
                  <a:p>
                    <a:pPr$alignAttr/>
                    <a:r>
                      <a:rPr lang="en-US" sz="$szValue" $bTag>
                        <a:solidFill>
                          <a:srgbClr val="$colorHex"/>
                        </a:solidFill>
                        <a:latin typeface="Arial"/>
                      </a:rPr>
                      <a:t>$escapedText</a:t>
                    </a:r>
                  </a:p>
                </p:txBody>
              </p:sp>
            """
        }

        fun buildSlide(shapesXml: String): String {
            return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
          <p:cSld>
            <p:spTree>
              <p:nvGrpSpPr>
                <p:cNvPr id="1" name=""/>
                <p:cNvGrpSpPr/>
                <p:nvPr/>
              </p:nvGrpSpPr>
              <p:grpSpPr/>
              $shapesXml
            </p:spTree>
          </p:cSld>
        </p:sld>
        """
        }

        try {
            val zos = java.util.zip.ZipOutputStream(java.io.FileOutputStream(file))
            
            // 1. Write [Content_Types].xml
            val contentTypes = StringBuilder()
            contentTypes.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
""")
            for (i in 1..totalSlides) {
                contentTypes.append("  <Override PartName=\"/ppt/slides/slide$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>\n")
            }
            contentTypes.append("</Types>")
            zos.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
            zos.write(contentTypes.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 2. Write _rels/.rels
            val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>"""
            zos.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
            zos.write(rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 3. Write ppt/_rels/presentation.xml.rels
            val presentationRels = StringBuilder()
            presentationRels.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdLayoutMaster" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
""")
            for (i in 1..totalSlides) {
                presentationRels.append("  <Relationship Id=\"rIdSlide$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide$i.xml\"/>\n")
            }
            presentationRels.append("</Relationships>")
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/_rels/presentation.xml.rels"))
            zos.write(presentationRels.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 4. Write ppt/presentation.xml
            val presentationXml = StringBuilder()
            presentationXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rIdLayoutMaster"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>
""")
            for (i in 1..totalSlides) {
                val id = 256 + i
                presentationXml.append("    <p:sldId id=\"$id\" r:id=\"rIdSlide$i\"/>\n")
            }
            presentationXml.append("""  </p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000" type="screen16x9"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>""")
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/presentation.xml"))
            zos.write(presentationXml.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 5. Write ppt/slideMasters/slideMaster1.xml
            val slideMaster1 = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg>
      <p:bgPr>
        <a:solidFill>
          <a:srgbClr val="FFFFFF"/>
        </a:solidFill>
      </p:bgPr>
    </p:bg>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr/>
    </p:spTree>
  </p:cSld>
  <p:sldLayoutIdLst>
    <p:sldLayoutId id="2147483649" r:id="rIdLayout1"/>
  </p:sldLayoutIdLst>
  <p:txStyles>
    <p:titleStyle/>
    <p:bodyStyle/>
    <p:otherStyle/>
  </p:txStyles>
</p:sldMaster>"""
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slideMasters/slideMaster1.xml"))
            zos.write(slideMaster1.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 6. Write ppt/slideMasters/_rels/slideMaster1.xml.rels
            val slideMaster1Rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdLayout1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>"""
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slideMasters/_rels/slideMaster1.xml.rels"))
            zos.write(slideMaster1Rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 7. Write ppt/slideLayouts/slideLayout1.xml
            val slideLayout1 = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr/>
    </p:spTree>
  </p:cSld>
</p:sldLayout>"""
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slideLayouts/slideLayout1.xml"))
            zos.write(slideLayout1.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 8. Write ppt/slideLayouts/_rels/slideLayout1.xml.rels
            val slideLayout1Rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdMaster" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>"""
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slideLayouts/_rels/slideLayout1.xml.rels"))
            zos.write(slideLayout1Rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 9. Write each slide relation
            for (i in 1..totalSlides) {
                val slideRel = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdLayout" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>"""
                zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/_rels/slide$i.xml.rels"))
                zos.write(slideRel.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            
            // 10. Write Slide 1 XML (Cover/Title Slide)
            val s1Shapes = StringBuilder()
            s1Shapes.append(makeRect(10, 0, 0, 4500000, 6858000, "0D253F"))
            s1Shapes.append(makeRect(11, 4500000, 0, 150000, 6858000, "00BFA5"))
            s1Shapes.append(makeTextBox(12, "QC_Logo", 500000, 1500000, 3500000, 800000, "QC INTERNAL AUDIT", 28, "FFFFFF", true))
            s1Shapes.append(makeTextBox(13, "QC_Tag", 500000, 2400000, 3500000, 500000, "QUALITY • COMPLIANCE • IMPROVEMENT", 9, "00BFA5", true))
            s1Shapes.append(makeTextBox(14, "QC_Sub", 500000, 5000000, 3500000, 600000, "AUDIT REPORT PRESENTATION", 12, "FFFFFF", false))
            
            val projNameUpper = details.report.projectName.ifEmpty { "QC Quality Audit" }.uppercase()
            s1Shapes.append(makeTextBox(15, "Proj_Label", 5200000, 1200000, 6500000, 300000, "PROJECT:", 11, "666666", true))
            s1Shapes.append(makeTextBox(16, "Proj_Val", 5200000, 1500000, 6500000, 1200000, projNameUpper, 24, "0D253F", true))
            s1Shapes.append(makeTextBox(17, "Ref_Val", 5200000, 3000000, 6500000, 400000, "Audit Reference: #${details.report.auditNumber}", 14, "333333", true))
            s1Shapes.append(makeTextBox(18, "Form_Val", 5200000, 3400000, 6500000, 400000, "Form Ref: ${details.report.formReference}", 10, "666666", false))
            
            s1Shapes.append(makeTextBox(19, "Date_Val", 5200000, 4200000, 6500000, 400000, "Date: ${details.report.auditDate}", 11, "0D253F", true))
            s1Shapes.append(makeTextBox(20, "Aud_Val", 5200000, 4700000, 6500000, 400000, "Auditor: ${details.report.auditorName}", 11, "333333", false))
            s1Shapes.append(makeTextBox(21, "Rev_Val", 5200000, 5200000, 6500000, 400000, "Reviewed by: ${details.report.sigReviewerName}", 11, "333333", false))
            
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/slide1.xml"))
            zos.write(buildSlide(s1Shapes.toString()).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 11. Write Slide 2 XML (Executive Summary)
            val s2Shapes = StringBuilder()
            s2Shapes.append(makeRect(30, 0, 0, 12192000, 1000000, "0D253F"))
            s2Shapes.append(makeRect(31, 0, 1000000, 12192000, 80000, "00BFA5"))
            s2Shapes.append(makeTextBox(32, "Title2", 500000, 200000, 11000000, 600000, "EXECUTIVE AUDIT SUMMARY", 24, "FFFFFF", true))
            
            val totalF = findings.size
            val ncrCount = findings.count { it.type.trim().equals("NCR", true) }
            val obsCount = findings.count { it.type.trim().equals("OBS", true) }
            val openCount = findings.count { it.status.trim().equals("Open", true) }
            val closedCount = findings.count { it.status.trim().equals("Closed", true) }
            
            // Total Card
            s2Shapes.append(makeRect(33, 600000, 1600000, 3300000, 2000000, "F1F5F9"))
            s2Shapes.append(makeRect(34, 600000, 1600000, 3300000, 100000, "0D253F"))
            s2Shapes.append(makeTextBox(35, "Tot_Lbl", 800000, 1800000, 2900000, 400000, "TOTAL FINDINGS", 10, "475569", true, "ctr"))
            s2Shapes.append(makeTextBox(36, "Tot_Num", 800000, 2200000, 2900000, 1200000, totalF.toString(), 44, "0D253F", true, "ctr"))
            
            // NCR Card
            s2Shapes.append(makeRect(37, 4400000, 1600000, 3300000, 2000000, "FDF2F2"))
            s2Shapes.append(makeRect(38, 4400000, 1600000, 3300000, 100000, "DC2626"))
            s2Shapes.append(makeTextBox(39, "Ncr_Lbl", 4600000, 1800000, 2900000, 400000, "NON-CONFORMANCES (NCR)", 10, "991B1B", true, "ctr"))
            s2Shapes.append(makeTextBox(40, "Ncr_Num", 4600000, 2200000, 2900000, 1200000, ncrCount.toString(), 44, "DC2626", true, "ctr"))
            
            // OBS Card
            s2Shapes.append(makeRect(41, 8200000, 1600000, 3300000, 2000000, "FFFBEB"))
            s2Shapes.append(makeRect(42, 8200000, 1600000, 3300000, 100000, "D97706"))
            s2Shapes.append(makeTextBox(43, "Obs_Lbl", 8400000, 1800000, 2900000, 400000, "OBSERVATIONS (OBS)", 10, "92400E", true, "ctr"))
            s2Shapes.append(makeTextBox(44, "Obs_Num", 8400000, 2200000, 2900000, 1200000, obsCount.toString(), 44, "D97706", true, "ctr"))
            
            // Bottom Cards: Open vs Closed
            s2Shapes.append(makeRect(45, 2500000, 4100000, 3300000, 2000000, "FFFDF2"))
            s2Shapes.append(makeRect(46, 2500000, 4100000, 3300000, 100000, "D97706"))
            s2Shapes.append(makeTextBox(47, "Opn_Lbl", 2700000, 4300000, 2900000, 400000, "OPEN FINDINGS", 10, "92400E", true, "ctr"))
            s2Shapes.append(makeTextBox(48, "Opn_Num", 2700000, 4700000, 2900000, 1200000, openCount.toString(), 44, "D97706", true, "ctr"))
            
            s2Shapes.append(makeRect(49, 6300000, 4100000, 3300000, 2000000, "F0FDF4"))
            s2Shapes.append(makeRect(50, 6300000, 4100000, 3300000, 100000, "16A34A"))
            s2Shapes.append(makeTextBox(51, "Cld_Lbl", 6500000, 4300000, 2900000, 400000, "CLOSED FINDINGS", 10, "166534", true, "ctr"))
            s2Shapes.append(makeTextBox(52, "Cld_Num", 6500000, 4700000, 2900000, 1200000, closedCount.toString(), 44, "16A34A", true, "ctr"))
            
            // Native graphical stacked bar chart representing overall resolution percentage
            val totalDenominator = (openCount + closedCount).coerceAtLeast(1)
            val closedBarW = (7100000L * closedCount) / totalDenominator
            val openBarW = 7100000L - closedBarW
            
            s2Shapes.append(makeRect(80, 2500000L, 6350000L, 7100000L, 160000L, "E2E8F0")) // tracker background
            if (closedBarW > 0) {
                s2Shapes.append(makeRect(81, 2500000L, 6350000L, closedBarW, 160000L, "10B981")) // Green part (Closed)
            }
            if (openBarW > 0) {
                s2Shapes.append(makeRect(82, 2500000L + closedBarW, 6350000L, openBarW, 160000L, "EF4444")) // Red part (Open)
            }
            
            s2Shapes.append(makeTextBox(83, "ResMeter_Txt", 2500000L, 6550000L, 7100000L, 300000L, "Resolution Progress Index: ${closedCount * 100 / totalDenominator}% Resolved (Green)  |  ${openCount * 100 / totalDenominator}% Outstanding (Red)", 10, "475569", true, "ctr"))
            
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/slide2.xml"))
            zos.write(buildSlide(s2Shapes.toString()).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 11b. Write Slide 3 XML (Executive Gaps Severity & Compliance Dashboard)
            val majorCount = findings.count { it.severity.trim().equals("Major", ignoreCase = true) }
            val minorCount = findings.count { it.severity.trim().equals("Minor", ignoreCase = true) }
            val repeatedFlagged = findings.count { it.repeated.trim().equals("Yes", ignoreCase = true) }
            val lossesFlagged = findings.count { it.materialLosses.trim().isNotEmpty() && !it.materialLosses.trim().equals("None", true) }

            val s2bShapes = StringBuilder()
            s2bShapes.append(makeRect(100, 0, 0, 12192000, 1000000, "0D253F"))
            s2bShapes.append(makeRect(101, 0, 1000000, 12192000, 80000, "00BFA5"))
            s2bShapes.append(makeTextBox(102, "Title2b", 500000, 200000, 11000000, 600000, "RISK SEVERITY & RESOURCES COMPLIANCE", 24, "FFFFFF", true))
            
            // Card 1: Major Severities (Red Flags)
            s2bShapes.append(makeRect(103, 600000, 1500000, 5200000, 2000000, "FEF2F2"))
            s2bShapes.append(makeRect(104, 600000, 1500000, 5200000, 100000, "DC2626"))
            s2bShapes.append(makeTextBox(105, "Maj_Lbl", 800000, 1700000, 4800000, 300000, "MAJOR AUDIT RED FLAGS", 10, "991B1B", true))
            s2bShapes.append(makeTextBox(106, "Maj_Num", 800000, 2000000, 4800000, 800000, majorCount.toString(), 32, "DC2626", true))
            s2bShapes.append(makeTextBox(107, "Maj_Desc", 800000, 2800000, 4800000, 500000, "High-impact safety / structural defects require immediate mitigation.", 9, "7F1D1D", false))
            
            // Card 2: Minor Gaps
            s2bShapes.append(makeRect(108, 6300000, 1500000, 5200000, 2000000, "FFFBEB"))
            s2bShapes.append(makeRect(109, 6300000, 1500000, 5200000, 100000, "D97706"))
            s2bShapes.append(makeTextBox(110, "Min_Lbl", 6500000, 1700000, 4800000, 300000, "MINOR QUALITY COMPLIANCE GAPS", 10, "78350F", true))
            s2bShapes.append(makeTextBox(111, "Min_Num", 6500000, 2000000, 4800000, 800000, minorCount.toString(), 32, "D97706", true))
            s2bShapes.append(makeTextBox(112, "Min_Desc", 6500000, 2800000, 4800000, 500000, "Routine work quality or procedural gaps observed on audited trades.", 9, "78350F", false))
            
            // Card 3: Recurring Gaps
            s2bShapes.append(makeRect(113, 600000, 3900000, 5200000, 2000000, "FFF7ED"))
            s2bShapes.append(makeRect(114, 600000, 3900000, 5200000, 100000, "EA580C"))
            s2bShapes.append(makeTextBox(115, "Rec_Lbl", 800000, 4100000, 4800000, 300000, "RECURRING SYSTEMIC FINDINGS", 10, "7C2D12", true))
            s2bShapes.append(makeTextBox(116, "Rec_Num", 800000, 4400000, 4800000, 800000, repeatedFlagged.toString(), 32, "EA580C", true))
            s2bShapes.append(makeTextBox(117, "Rec_Desc", 800000, 5200000, 4800000, 500000, "Chronic repeating issues from historical inspection cycles.", 9, "7C2D12", false))
            
            // Card 4: Material Losses
            s2bShapes.append(makeRect(118, 6300000, 3900000, 5200000, 2000000, "EFF6FF"))
            s2bShapes.append(makeRect(119, 6300000, 3900000, 5200000, 100000, "2563EB"))
            s2bShapes.append(makeTextBox(120, "Los_Lbl", 6500000, 4100000, 4800000, 300000, "RESOURCE RED FLAGS & WASTE IMPACTS", 10, "1E3A8A", true))
            s2bShapes.append(makeTextBox(121, "Los_Num", 6500000, 4400000, 4800000, 800000, lossesFlagged.toString(), 32, "2563EB", true))
            s2bShapes.append(makeTextBox(122, "Los_Desc", 6500000, 5200000, 4800000, 500000, "Findings indicating financial, asset wastage, or man-hour losses.", 9, "1E3A8A", false))
            
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/slide3.xml"))
            zos.write(buildSlide(s2bShapes.toString()).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 12. Write Slide 4 XML (Summary By Trade, shifted from slide 3)
            val standardTrades = listOf("Civil", "Structural", "Architectural", "MEP", "Infrastructure", "Safety")
            val s3Shapes = StringBuilder()
            s3Shapes.append(makeRect(60, 0, 0, 12192000, 1000000, "0D253F"))
            s3Shapes.append(makeRect(61, 0, 1000000, 12192000, 80000, "00BFA5"))
            s3Shapes.append(makeTextBox(62, "Title3", 500000, 200000, 11000000, 600000, "FINDINGS DISTRIBUTION BY TRADE", 24, "FFFFFF", true))
            
            var yOffset = 1400000L
            var shId = 150
            standardTrades.forEachIndexed { sIdx, tr ->
                val tFindings = findings.filter { f -> f.trade.trim().equals(tr, ignoreCase = true) || f.trade.lowercase().startsWith(tr.lowercase()) }
                val ncrOpen = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) && f.status.trim().equals("Open", ignoreCase = true) }
                val ncrClosed = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) && f.status.trim().equals("Closed", ignoreCase = true) }
                val obsOpen = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) && f.status.trim().equals("Open", ignoreCase = true) }
                val obsClosed = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) && f.status.trim().equals("Closed", ignoreCase = true) }
                
                val bgHex = if (sIdx % 2 == 0) "F8FAFC" else "FFFFFF"
                s3Shapes.append(makeRect(shId++, 500000, yOffset, 11192000, 700000, bgHex))
                s3Shapes.append(makeTextBox(shId++, "Tr_$sIdx", 700000, yOffset + 120000, 3000000, 400000, tr.uppercase(), 14, "0D253F", true))
                
                // Draw a native vector diagram block bar next to each trade
                var barXOffset = 4200000L
                val blockW = 140000L
                val blockH = 160000L
                val blockYOffset = yOffset + 240000L
                
                for (i in 0 until (ncrOpen + ncrClosed)) {
                    s3Shapes.append(makeRect(shId++, barXOffset, blockYOffset, blockW, blockH, "EF4444")) // Red blocks for NCRs
                    barXOffset += 160000L
                }
                for (i in 0 until (obsOpen + obsClosed)) {
                    s3Shapes.append(makeRect(shId++, barXOffset, blockYOffset, blockW, blockH, "F59E0B")) // Orange blocks for OBSs
                    barXOffset += 160000L
                }
                
                val summaryStr = "Total: ${tFindings.size}  |  NCR: $ncrOpen Open, $ncrClosed Closed  |  OBS: $obsOpen Open, $obsClosed Closed"
                s3Shapes.append(makeTextBox(shId++, "Sum_$sIdx", 7600000, yOffset + 120000, 4000000, 400000, summaryStr, 11, "475569", false))
                
                yOffset += 800000
            }
            
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/slide4.xml"))
            zos.write(buildSlide(s3Shapes.toString()).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 12b. Write Slide 5 XML (Trends progression S-curve & Root Cause Pareto Diagrams)
            val s5Shapes = StringBuilder()
            s5Shapes.append(makeRect(200, 0, 0, 12192000, 1000000, "0D253F"))
            s5Shapes.append(makeRect(201, 0, 1000000, 12192000, 80000, "00BFA5"))
            s5Shapes.append(makeTextBox(202, "Title5", 500000, 200000, 11000000, 600000, "QUALITY PROGRESSION TRENDS & ROOT CAUSES", 24, "FFFFFF", true))
            
            // Draw backgrounds
            s5Shapes.append(makeRect(203, 500000, 1300000, 5300000, 5000000, "F8FAFC"))
            s5Shapes.append(makeRect(204, 6392000, 1300000, 5300000, 5000000, "F8FAFC"))
            
            s5Shapes.append(makeTextBox(205, "S_Title", 700000, 1450000, 4900000, 350000, "Monthly Cumulative Gaps S-Curve Progression", 12, "0D253F", true))
            s5Shapes.append(makeTextBox(206, "P_Title", 6592000, 1450000, 4900000, 350000, "Root Cause Pareto Analysis Chart (80/20 Rule)", 12, "0D253F", true))
            
            var pptShId = 210
            
            // S-Curve calculations
            val sMonthsList = listOf("Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026")
            var pNcrVal = findings.count { it.type == "NCR" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
            var pObsVal = findings.count { it.type == "OBS" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
            val ncrCumVals = mutableListOf<Float>()
            val obsCumVals = mutableListOf<Float>()
            sMonthsList.forEach { m ->
                pNcrVal += findings.count { it.type == "NCR" && getMonthYearSymbol(it.issueDate) == m }
                pObsVal += findings.count { it.type == "OBS" && getMonthYearSymbol(it.issueDate) == m }
                ncrCumVals.add(pNcrVal.toFloat())
                obsCumVals.add(pObsVal.toFloat())
            }
            
            val sMaxVal = maxOf(ncrCumVals.maxOrNull() ?: 1f, obsCumVals.maxOrNull() ?: 1f, 8f)
            val sScaleMax = (Math.ceil(sMaxVal / 10.0) * 10).toFloat().coerceAtLeast(10f)
            
            // S-Curve Plot inside panel
            val sL = 1000000L
            val sT = 2000000L
            val sW = 4400000L
            val sH = 3400000L
            val sB = sT + sH
            
            // Grid
            val gridSteps = 5
            for (i in 0..gridSteps) {
                val gridY = sB - (i * sH / gridSteps)
                s5Shapes.append(makeRect(pptShId++, sL, gridY, sW, 10000L, "E2E8F0"))
                val labelVal = (sScaleMax * i / gridSteps).toInt().toString()
                s5Shapes.append(makeTextBox(pptShId++, "SLbl_$i", sL - 450000, gridY - 150000, 400000, 300000, labelVal, 9, "64748B", false, "r"))
            }
            
            // X-Axis labels & columns
            val xPoints = LongArray(sMonthsList.size)
            val stepX = sW / (sMonthsList.size - 1).coerceAtLeast(1)
            sMonthsList.forEachIndexed { idx, m ->
                val x = sL + idx * stepX
                xPoints[idx] = x
                s5Shapes.append(makeRect(pptShId++, x, sT, 10000L, sH, "F1F5F9"))
                s5Shapes.append(makeTextBox(pptShId++, "SMonth_$idx", x - 500000, sB + 100000, 1000000, 300000, m.split(" ")[0], 9, "64748B", false, "ctr"))
            }
            
            // Plot trend lines and markers
            sMonthsList.indices.forEach { idx ->
                val x = xPoints[idx]
                val yN = sB - (ncrCumVals[idx] / sScaleMax * sH).toLong()
                val yO = sB - (obsCumVals[idx] / sScaleMax * sH).toLong()
                
                if (idx < sMonthsList.size - 1) {
                    val nextX = xPoints[idx + 1]
                    val nextYN = sB - (ncrCumVals[idx + 1] / sScaleMax * sH).toLong()
                    val nextYO = sB - (obsCumVals[idx + 1] / sScaleMax * sH).toLong()
                    
                    s5Shapes.append(makeLine(pptShId++, x, yN, nextX, nextYN, "00BFA5", 30000L)) // Teal
                    s5Shapes.append(makeLine(pptShId++, x, yO, nextX, nextYO, "ED7D31", 30000L)) // Orange
                }
                
                // Markers
                s5Shapes.append(makeOval(pptShId++, x - 60000L, yN - 60000L, 120000L, 120000L, "00BFA5"))
                s5Shapes.append(makeOval(pptShId++, x - 30000L, yN - 30000L, 60000L, 60000L, "FFFFFF"))
                s5Shapes.append(makeTextBox(pptShId++, "SValN_$idx", x - 400000, yN - 420000, 800000, 350000, ncrCumVals[idx].toInt().toString(), 8, "0F2644", true, "ctr"))
                
                s5Shapes.append(makeOval(pptShId++, x - 60000L, yO - 60000L, 120000L, 120000L, "ED7D31"))
                s5Shapes.append(makeOval(pptShId++, x - 30000L, yO - 30000L, 60000L, 60000L, "FFFFFF"))
                s5Shapes.append(makeTextBox(pptShId++, "SValO_$idx", x - 400000, yO - 420000, 800000, 350000, obsCumVals[idx].toInt().toString(), 8, "0F2644", true, "ctr"))
            }
            
            // Legend
            val sLegY = 5950000L
            s5Shapes.append(makeRect(pptShId++, 900000L, sLegY, 200000L, 100000L, "00BFA5"))
            s5Shapes.append(makeTextBox(pptShId++, "SLegN", 1150000L, sLegY - 120000, 2000000L, 350000L, "Cumulative NCR Gaps", 9, "475569", false, "l"))
            
            s5Shapes.append(makeRect(pptShId++, 3200000L, sLegY, 200000L, 100000L, "ED7D31"))
            s5Shapes.append(makeTextBox(pptShId++, "SLegO", 3450000L, sLegY - 120000, 2000000L, 350000L, "Cumulative OBS Gaps", 9, "475569", false, "l"))
            
            // Pareto Calculations
            val pCategories = listOf("Workmanship", "Material Quality", "Lack of Supervision", "Equipment Failure", "Design Gaps", "Other")
            val pMap = mutableMapOf<String, Int>().apply {
                pCategories.forEach { put(it, 0) }
            }
            findings.forEach { f ->
                val cause = f.rootCause.lowercase()
                val cat = when {
                    cause.contains("workman") || cause.contains("craft") || cause.contains("skill") -> "Workmanship"
                    cause.contains("material") || cause.contains("spec") || cause.contains("quality") -> "Material Quality"
                    cause.contains("supervis") || cause.contains("manage") || cause.contains("follow") -> "Lack of Supervision"
                    cause.contains("equip") || cause.contains("tool") || cause.contains("machine") -> "Equipment Failure"
                    cause.contains("design") || cause.contains("draw") || cause.contains("plan") -> "Design Gaps"
                    else -> "Other"
                }
                pMap[cat] = pMap.getOrDefault(cat, 0) + 1
            }
            val pSortedList = pMap.toList().sortedByDescending { it.second }
            val pTotalCount = findings.size.coerceAtLeast(1)
            val pMaxCount = pSortedList.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
            val pScaleLeftMax = (Math.ceil(pMaxCount / 5.0) * 5).toInt().coerceAtLeast(5)
            
            // Right Plot bounds
            val pL = 6892000L
            val pT = 2000000L
            val pW = 4400000L
            val pH = 3400000L
            val pB = pT + pH
            val pR = pL + pW
            
            // Grid
            for (i in 0..gridSteps) {
                val gridY = pB - (i * pH / gridSteps)
                s5Shapes.append(makeRect(pptShId++, pL, gridY, pW, 10000L, "E2E8F0"))
                
                // Left Scale
                val leftLabel = (pScaleLeftMax * i / gridSteps).toString()
                s5Shapes.append(makeTextBox(pptShId++, "PLblL_$i", pL - 450000, gridY - 150000, 400000, 300000, leftLabel, 9, "64748B", false, "r"))
                
                // Right Scale
                val rightLabel = "${i * 20}%"
                s5Shapes.append(makeTextBox(pptShId++, "PLblR_$i", pR + 50000, gridY - 150000, 500000, 300000, rightLabel, 9, "64748B", false, "l"))
            }
            
            // Bars & cumulative
            val pBarStep = pW / 6f
            val pBarW = pBarStep * 0.55f
            var pCumSum = 0
            val pCumPcts = FloatArray(6)
            val pCentersX = LongArray(6)
            
            pSortedList.forEachIndexed { idx, item ->
                val catName = item.first
                val catCount = item.second
                pCumSum += catCount
                pCumPcts[idx] = pCumSum.toFloat() / pTotalCount.toFloat() * 100f
                
                val centerX = pL + (idx + 0.5f) * pBarStep
                pCentersX[idx] = centerX.toLong()
                
                // Bar Rect
                val barH = (catCount.toFloat() / pScaleLeftMax.toFloat() * pH).toLong()
                val barL = (centerX - pBarW / 2).toLong()
                val barT = pB - barH
                
                s5Shapes.append(makeRect(pptShId++, barL, barT, pBarW.toLong(), barH, "3B82F6"))
                
                if (catCount > 0) {
                    s5Shapes.append(makeTextBox(pptShId++, "PCnt_$idx", centerX.toLong() - 400000, barT - 400000, 800000, 350000, catCount.toString(), 9, "1E3A8A", true, "ctr"))
                }
                
                val shortenedName = if (catName.length > 12) catName.substring(0, 10) + "." else catName
                s5Shapes.append(makeTextBox(pptShId++, "PCat_$idx", centerX.toLong() - 500000, pB + 100000, 1000000, 300000, shortenedName, 8, "475569", false, "ctr"))
            }
            
            // Draw Pareto Line & Circles
            pSortedList.indices.forEach { idx ->
                val x = pCentersX[idx]
                val y = pB - (pCumPcts[idx] / 100f * pH).toLong()
                
                if (idx < 5) {
                    val nextX = pCentersX[idx + 1]
                    val nextY = pB - (pCumPcts[idx + 1] / 100f * pH).toLong()
                    s5Shapes.append(makeLine(pptShId++, x, y, nextX, nextY, "EF4444", 30000L))
                }
                
                s5Shapes.append(makeOval(pptShId++, x - 60000L, y - 60000L, 120000L, 120000L, "EF4444"))
                s5Shapes.append(makeOval(pptShId++, x - 30000L, y - 30000L, 60000L, 60000L, "FFFFFF"))
                s5Shapes.append(makeTextBox(pptShId++, "PPct_$idx", x - 400000, y - 420000, 800000, 350000, "${pCumPcts[idx].toInt()}%", 8, "991B1B", true, "ctr"))
            }
            
            // Pareto Legend
            val pLegY = 5950000L
            s5Shapes.append(makeRect(pptShId++, 6892000L, pLegY, 200000L, 100000L, "3B82F6"))
            s5Shapes.append(makeTextBox(pptShId++, "PLegB", 7142000L, pLegY - 120000, 2000000L, 350000L, "Gaps Count (Bars)", 9, "475569", false, "l"))
            
            s5Shapes.append(makeRect(pptShId++, 9192000L, pLegY, 200000L, 100000L, "EF4444"))
            s5Shapes.append(makeTextBox(pptShId++, "PLegR", 9442000L, pLegY - 120000, 2000000L, 350000L, "Cumulative % Line", 9, "475569", false, "l"))
            
            zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/slide5.xml"))
            zos.write(buildSlide(s5Shapes.toString()).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 13. Write Finding Detail Slides (Slide 6..N)
            for (fIdx in findings.indices) {
                val fi = findings[fIdx]
                val sNum = 6 + fIdx
                val sfShapes = StringBuilder()
                
                sfShapes.append(makeRect(300, 0, 0, 12192000, 1000000, "0D253F"))
                sfShapes.append(makeRect(301, 0, 1000000, 12192000, 80000, "00BFA5"))
                sfShapes.append(makeTextBox(302, "TitleDetail", 500000, 200000, 11000000, 600000, "FINDING DETAIL: ${fi.referenceId.uppercase()}", 20, "FFFFFF", true))
                
                // Left Column Card (General Audit finding description)
                sfShapes.append(makeRect(303, 500000, 1300000, 5300000, 5000000, "F8FAFC"))
                sfShapes.append(makeRect(304, 500000, 1300000, 5300000, 80000, "0D253F"))
                
                sfShapes.append(makeTextBox(305, "T1", 700000, 1500000, 4900000, 300000, "FINDING METADATA", 12, "0D253F", true))
                sfShapes.append(makeTextBox(306, "T2", 700000, 1900000, 4900000, 300000, "Type / Severity: ${fi.type} - ${fi.severity}", 11, "475569", false))
                sfShapes.append(makeTextBox(307, "T3", 700000, 2300000, 4900000, 300000, "Trade / Activity: ${fi.trade} / ${fi.activity}", 11, "475569", false))
                sfShapes.append(makeTextBox(308, "T4", 700000, 2700000, 4900000, 300000, "Location Zone: ${fi.locationZone}", 11, "475569", false))
                sfShapes.append(makeTextBox(309, "T5", 700000, 3100000, 4900000, 300000, "Status: ${fi.status.uppercase()}", 12, if (fi.status.trim().equals("Open", true)) "DC2626" else "16A34A", true))
                
                val cleanedDesc = fi.description.replace("\n", " ").take(180) + (if (fi.description.length > 180) "..." else "")
                sfShapes.append(makeTextBox(310, "T6", 700000, 3500000, 4900000, 1500000, "Description:\n$cleanedDesc", 11, "000000", false))
                
                // Right Column Card (Root Cause and actions for remediation)
                val actCardColor = if (fi.status.trim().equals("Open", true)) "FFFBEB" else "F0FDF4"
                val actStripColor = if (fi.status.trim().equals("Open", true)) "D97706" else "16A34A"
                
                sfShapes.append(makeRect(311, 6300000, 1300000, 5300000, 5000000, actCardColor))
                sfShapes.append(makeRect(312, 6300000, 1300000, 5300000, 80000, actStripColor))
                
                sfShapes.append(makeTextBox(313, "T7", 6500000, 1500000, 4900000, 300000, "ROOT CAUSE & RESOLUTION PLAN", 12, actStripColor, true))
                sfShapes.append(makeTextBox(314, "T8", 6500000, 1900000, 4900000, 300000, "Proposed Due Date: ${fi.dueDate}", 11, "475569", false))
                
                val cleanedRoot = fi.rootCause.replace("\n", " ").take(120) + (if (fi.rootCause.length > 120) "..." else "")
                sfShapes.append(makeTextBox(315, "T9", 6500000, 2400000, 4900000, 1200000, "Root Cause:\n$cleanedRoot", 11, "000000", false))
                
                val cleanedAct = fi.correctiveAction.replace("\n", " ").take(140) + (if (fi.correctiveAction.length > 140) "..." else "")
                sfShapes.append(makeTextBox(316, "T10", 6500000, 3800000, 4900000, 1400000, "Corrective Action:\n$cleanedAct", 11, "000000", false))
                
                zos.putNextEntry(java.util.zip.ZipEntry("ppt/slides/slide$sNum.xml"))
                zos.write(buildSlide(sfShapes.toString()).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            
            zos.flush()
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // XLSX (OPTIMIZED COMPLIANT OPENXML SPREADSHEET ZIP EXPORT)
    fun exportToXlsx(context: Context, details: AuditReportWithDetails, file: File) {
        val findings = details.findings
        val report = details.report
        
        fun escapeXml(text: String?): String {
            if (text == null) return ""
            return text.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;")
                       .replace("'", "&apos;")
        }

        fun getColumnRef(colIndex: Int): String {
            var temp = colIndex
            val sb = StringBuilder()
            while (temp >= 0) {
                sb.insert(0, ('A'.code + (temp % 26)).toChar())
                temp = temp / 26 - 1
            }
            return sb.toString()
        }

        fun compileSheetXml(builderAction: (StringBuilder) -> Unit): String {
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
            sb.append("  <sheetData>\n")
            builderAction(sb)
            sb.append("  </sheetData>\n")
            sb.append("</worksheet>")
            return sb.toString()
        }

        // ================= SHEET 1: AUDIT COVER PAGE =================
        val sheet1Xml = compileSheetXml { sb ->
            var rowIndex = 1
            fun writeRow(cells: List<String>) {
                sb.append("    <row r=\"$rowIndex\">\n")
                for (cIdx in cells.indices) {
                    val cellVal = cells[cIdx]
                    val colRef = getColumnRef(cIdx)
                    sb.append("      <c r=\"$colRef$rowIndex\" t=\"inlineStr\"><is><t>${escapeXml(cellVal)}</t></is></c>\n")
                }
                sb.append("    </row>\n")
                rowIndex++
            }
            fun writeBlankRow() {
                rowIndex++
            }
            
            writeRow(listOf("QC INTERNAL AUDIT"))
            writeRow(listOf("QUALITY • COMPLIANCE • IMPROVEMENT"))
            writeRow(listOf("------------------------------------------------------------------------"))
            writeRow(listOf("QC INTERNAL QUALITY AUDIT REPORT - COVER PAGE"))
            writeBlankRow()
            writeRow(listOf("01- PRIMARY AUDIT & PROJECT INFORMATION"))
            writeBlankRow()
            writeRow(listOf("Audit Reference ID:", report.auditNumber))
            writeRow(listOf("Project Name:", report.projectName))
            writeRow(listOf("Project Number:", report.projectNumber))
            writeRow(listOf("Audit Scope Area:", report.auditScope))
            writeRow(listOf("Audit Conducted Date:", report.auditDate))
            writeRow(listOf("Project Build Stage/Phase:", report.phase))
            writeRow(listOf("Subject Location/Zones:", report.location))
            writeRow(listOf("Main Project Manager:", report.projectManager))
            writeRow(listOf("QA/QC Director of Record:", report.qcManager))
            writeRow(listOf("Lead Quality Auditor:", report.auditorName))
            writeRow(listOf("Auditee Coordinator Delegate:", report.auditeeName))
            writeRow(listOf("Report Issuance Date:", report.reportIssuanceDate))
            writeRow(listOf("Follow-Up Due Date:", report.followupDueDate))
            writeBlankRow()
            writeRow(listOf("02- DOCUMENT AUTHORIZATION SIGN OFF DETAILS"))
            writeBlankRow()
            writeRow(listOf("Lead Auditor of Record Name:", report.sigAuditorName.ifEmpty { report.auditorName }))
            writeRow(listOf("Lead Auditor Sign Date:", report.sigAuditorDate.ifEmpty { report.reportIssuanceDate }))
            writeRow(listOf("Reviewing Director Name:", report.sigReviewerName.ifEmpty { "Moamen Othman" }))
            writeRow(listOf("Reviewing Director Designation:", report.sigReviewerDesignation.ifEmpty { "Quality Director" }))
            writeRow(listOf("Reviewing Director Date:", report.sigReviewerDate.ifEmpty { "15/06/2026" }))
        }

        // ================= SHEET 2: NCR & OBS TRACKING & ANALYTICS =================
        val sheet2Xml = compileSheetXml { sb ->
            var rowIndex = 1
            fun writeRow(cells: List<String>) {
                sb.append("    <row r=\"$rowIndex\">\n")
                for (cIdx in cells.indices) {
                    val cellVal = cells[cIdx]
                    val colRef = getColumnRef(cIdx)
                    sb.append("      <c r=\"$colRef$rowIndex\" t=\"inlineStr\"><is><t>${escapeXml(cellVal)}</t></is></c>\n")
                }
                sb.append("    </row>\n")
                rowIndex++
            }
            fun writeBlankRow() {
                rowIndex++
            }
            
            val totalF = findings.size
            val ncrOpen = findings.count { it.type.trim().equals("NCR", true) && it.status.trim().equals("Open", true) }
            val ncrClosed = findings.count { it.type.trim().equals("NCR", true) && it.status.trim().equals("Closed", true) }
            val obsOpen = findings.count { it.type.trim().equals("OBS", true) && it.status.trim().equals("Open", true) }
            val obsClosed = findings.count { it.type.trim().equals("OBS", true) && it.status.trim().equals("Closed", true) }
            val grandResolved = ncrClosed + obsClosed
            val resolutionPercent = if (totalF > 0) (grandResolved * 100 / totalF) else 0
            
            writeRow(listOf("QC INTERNAL AUDIT"))
            writeRow(listOf("QUALITY • COMPLIANCE • IMPROVEMENT"))
            writeRow(listOf("------------------------------------------------------------------------"))
            writeRow(listOf("NCR & OBSERVATION SYSTEM TRACKING REGISTER & EXECUTIVE ANALYTICS"))
            writeBlankRow()
            
            writeRow(listOf("01- AUDIT METRICS PROGRESS KPI CARDS"))
            writeRow(listOf("Metric Type", "Aggregate Total Findings", "NCR (Open Status)", "NCR (Closed/Resolved)", "OBS (Open Status)", "OBS (Closed/Resolved)", "Overall Resolution Progress Indicator"))
            writeRow(listOf("Counts", totalF.toString(), ncrOpen.toString(), ncrClosed.toString(), obsOpen.toString(), obsClosed.toString(), "$resolutionPercent% Closed"))
            writeBlankRow()
            
            // Adding detailed Risk Severity & Resource Performance Analytics to Excel Sheet 2
            val majorCount = findings.count { it.severity.equals("Major", ignoreCase = true) }
            val minorCount = findings.count { it.severity.equals("Minor", ignoreCase = true) }
            val repeatedFlagged = findings.count { it.repeated.equals("Yes", ignoreCase = true) }
            val lossesFlagged = findings.count { it.materialLosses.trim().isNotEmpty() && !it.materialLosses.trim().equals("None", true) }
            
            writeRow(listOf("01b- RISK SEVERITY & RESOURCE PERFORMANCE ANALYTICS"))
            writeRow(listOf("Risk / Compliance Executive KPIs", "Aggregate Gaps Count", "Diagnostic Guidelines & Systemic Impact Criteria"))
            writeRow(listOf("Major Severity Red Flags", majorCount.toString(), "High-impact safety / structural defects require immediate mitigation."))
            writeRow(listOf("Minor Severity Issues Captured", minorCount.toString(), "Routine work quality / compliance gaps on audited trades."))
            writeRow(listOf("Recurring (Repeated) Gaps", repeatedFlagged.toString(), "Chronic issues repeating from historical inspections."))
            writeRow(listOf("Resource Material / Man-hour Losses", lossesFlagged.toString(), "Findings with financial or asset wastage impacts flagged."))
            writeBlankRow()
            
            writeRow(listOf("02- DEVIATION TRACKING SHEET BY TRADES (HISTORIC RUNNING TOTALS)"))
            writeRow(listOf("Trade Domain Segment", "NCR - Open", "NCR - Closed", "NCR Running Cumulative", "OBS - Open", "OBS - Closed", "OBS Running Cumulative"))
            
            val standardTrades = listOf("Civil", "Structural", "Architectural", "MEP", "Infrastructure", "Safety")
            var lastNcrCumulative = 0
            var lastObsCumulative = 0
            
            standardTrades.forEach { tr ->
                val tFindings = findings.filter { f -> f.trade.trim().equals(tr, ignoreCase = true) || f.trade.lowercase().startsWith(tr.lowercase()) }
                val nOpen = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) && f.status.trim().equals("Open", ignoreCase = true) }
                val nClosed = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) && f.status.trim().equals("Closed", ignoreCase = true) }
                val oOpen = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) && f.status.trim().equals("Open", ignoreCase = true) }
                val oClosed = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) && f.status.trim().equals("Closed", ignoreCase = true) }
                
                val nCumulative = lastNcrCumulative + nOpen - nClosed
                val oCumulative = lastObsCumulative + oOpen - oClosed
                lastNcrCumulative = nCumulative
                lastObsCumulative = oCumulative
                
                writeRow(listOf(
                    tr,
                    nOpen.toString(),
                    nClosed.toString(),
                    nCumulative.toString(),
                    oOpen.toString(),
                    oClosed.toString(),
                    oCumulative.toString()
                ))
            }
            writeBlankRow()
            
            writeRow(listOf("03- CELLULAR GRAPHICAL DIAGRAMS & COLUMN CHARTS"))
            writeBlankRow()
            writeRow(listOf("Audited Project Division", "Volume Column Bar Representation (Red = NCR, Gold = OBS)"))
            
            standardTrades.forEach { tr ->
                val tFindings = findings.filter { f -> f.trade.trim().equals(tr, ignoreCase = true) || f.trade.lowercase().startsWith(tr.lowercase()) }
                val ncrTotal = tFindings.count { f -> f.type.trim().equals("NCR", ignoreCase = true) }
                val obsTotal = tFindings.count { f -> f.type.trim().equals("OBS", ignoreCase = true) }
                val chartBar = StringBuilder()
                for (i in 0 until ncrTotal) chartBar.append("█ (NCR) ")
                for (i in 0 until obsTotal) chartBar.append("▒ (OBS) ")
                val outputBar = if (chartBar.isEmpty()) "No non-compliances flagged" else chartBar.toString()
                writeRow(listOf(tr, outputBar))
            }
            writeBlankRow()
            
            // Text-based diagram progress gauge
            val fullBlocks = (resolutionPercent / 10).coerceIn(0, 10)
            val progressGauge = StringBuilder()
            progressGauge.append("[")
            for (i in 0 until 10) {
                if (i < fullBlocks) progressGauge.append("■") else progressGauge.append("□")
            }
            progressGauge.append("] $resolutionPercent% Consolidated & Rectified")
            writeRow(listOf("Overall Audit Issue Closure Meter:", progressGauge.toString()))
        }

        // ================= SHEET 3: DETAILED FINDINGS REGISTER =================
        val sheet3Xml = compileSheetXml { sb ->
            var rowIndex = 1
            fun writeRow(cells: List<String>) {
                sb.append("    <row r=\"$rowIndex\">\n")
                for (cIdx in cells.indices) {
                    val cellVal = cells[cIdx]
                    val colRef = getColumnRef(cIdx)
                    sb.append("      <c r=\"$colRef$rowIndex\" t=\"inlineStr\"><is><t>${escapeXml(cellVal)}</t></is></c>\n")
                }
                sb.append("    </row>\n")
                rowIndex++
            }
            fun writeBlankRow() {
                rowIndex++
            }
            
            writeRow(listOf("03- DETAILED AUDIT FINDINGS RAW REGISTER DESCRIPTION DATA"))
            writeBlankRow()
            writeRow(listOf(
                "Reference ID", 
                "Finding Type", 
                "Risk Severity", 
                "Project Trade", 
                "Activity / Category Sector", 
                "Location / Zone Area", 
                "Current Status", 
                "Proposed Due Date", 
                "Brief Description Gaps", 
                "Root Cause Analysis Reasons", 
                "Corrective Action Work Plan"
            ))
            
            for (fi in findings) {
                writeRow(listOf(
                    fi.referenceId,
                    fi.type,
                    fi.severity,
                    fi.trade,
                    fi.activity,
                    fi.locationZone,
                    fi.status,
                    fi.dueDate,
                    fi.description,
                    fi.rootCause,
                    fi.correctiveAction
                ))
            }
        }
        
        // Let's bundle XLSX as zip archive with multiple worksheets (TABS!)
        try {
            val zos = java.util.zip.ZipOutputStream(java.io.FileOutputStream(file))
            
            // 1. [Content_Types].xml - updated overrides to include multiple sheets
            val contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                "  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
                "  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                "  <Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                "  <Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                "</Types>"
            zos.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
            zos.write(contentTypes.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 2. _rels/.rels
            val rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
                "</Relationships>"
            zos.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
            zos.write(rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 3. xl/workbook.xml - updated with 3 tab names
            val workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n" +
                "  <sheets>\n" +
                "    <sheet name=\"1. Audit Cover Page\" sheetId=\"1\" r:id=\"rId1\"/>\n" +
                "    <sheet name=\"2. NCR &amp; OBS Trackers\" sheetId=\"2\" r:id=\"rId2\"/>\n" +
                "    <sheet name=\"3. Detailed Findings\" sheetId=\"3\" r:id=\"rId3\"/>\n" +
                "  </sheets>\n" +
                "</workbook>"
            zos.putNextEntry(java.util.zip.ZipEntry("xl/workbook.xml"))
            zos.write(workbook.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 4. xl/_rels/workbook.xml.rels - updated references for worksheets
            val workbookRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n" +
                "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>\n" +
                "  <Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>\n" +
                "</Relationships>"
            zos.putNextEntry(java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write(workbookRels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 5. xl/worksheets/sheet1.xml
            zos.putNextEntry(java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheet1Xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 6. xl/worksheets/sheet2.xml
            zos.putNextEntry(java.util.zip.ZipEntry("xl/worksheets/sheet2.xml"))
            zos.write(sheet2Xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 7. xl/worksheets/sheet3.xml
            zos.putNextEntry(java.util.zip.ZipEntry("xl/worksheets/sheet3.xml"))
            zos.write(sheet3Xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            zos.flush()
            zos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

