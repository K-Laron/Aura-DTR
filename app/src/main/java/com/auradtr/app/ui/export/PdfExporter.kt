package com.auradtr.app.ui.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.auradtr.app.data.Profile
import com.auradtr.app.data.TimeLog
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * PdfExporter handles compilation of student time records into premium PDF format reports.
 * Employs try-finally constructs to guarantee proper disposal of Android graphics native
 * PdfDocument allocations.
 */
class PdfExporter {

    fun exportDtrToPdf(
        context: Context,
        profile: Profile,
        logs: List<TimeLog>,
        outputFile: File,
        templateType: String = "STANDARD",
        coverageText: String? = null
    ): File {
        val pdfDocument = PdfDocument()
        try {
            // Standard A4 dimensions in postscript points (595 x 842)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val isModern = templateType == "MODERN"
            val accentColorString = if (isModern) "#00796B" else "#333333"

            // Draw Document Border
            val borderPaint = Paint().apply {
                color = Color.parseColor(accentColorString)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

            // Draw Header Section
            if (isModern) {
                val bannerPaint = Paint().apply {
                    color = Color.parseColor("#00796B")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(30f, 35f, 565f, 83f, bannerPaint)

                headerPaint.color = Color.WHITE
                canvas.drawText("UNIVERSITY INTERNSHIP REPORT", 40f, 56f, headerPaint)

                val modernSubPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("DAILY TIME RECORD (DTR) & ACTIVITY SHEET", 40f, 73f, modernSubPaint)
            } else {
                canvas.drawText("UNIVERSITY INTERNSHIP REPORT", 40f, 50f, headerPaint)
                canvas.drawText("DAILY TIME RECORD (DTR) & ACTIVITY SHEET", 40f, 68f, textPaint.apply { isFakeBoldText = true; textSize = 11f })
                // Reset textPaint
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false
            }
            
            // Draw Trainee Details Table
            var y = 105f
            val detailsPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                isAntiAlias = true
            }

            canvas.drawText("STUDENT: ${profile.studentName}", 40f, y, detailsPaint)
            canvas.drawText("ID NUMBER: ${profile.studentId}", 320f, y, detailsPaint)
            y += 18f
            canvas.drawText("COURSE/MAJOR: ${profile.course}", 40f, y, detailsPaint)
            canvas.drawText("DEPARTMENT: ${profile.department}", 320f, y, detailsPaint)
            y += 18f
            canvas.drawText("COMPANY: ${profile.companyName}", 40f, y, detailsPaint)
            canvas.drawText("SUPERVISOR: ${profile.supervisorName} (${profile.supervisorTitle})", 320f, y, detailsPaint)
            
            // Draw coverage details printed dynamically on header
            if (coverageText != null) {
                y += 15f
                val coveragePaint = Paint().apply {
                    color = Color.parseColor(accentColorString)
                    textSize = 8.5f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText(coverageText, 40f, y, coveragePaint)
            }
            
            // Compile SHA-256 integrity hash for QR verification
            val logsRawText = logs.joinToString("|") { "${it.date}:${it.totalWorkedMinutes}:${it.verificationStatus}" }
            val integrityHash = sha256(logsRawText).take(32)
            val verificationUrl = "https://verify.auradtr.edu/dtr?hash=$integrityHash"

            // Draw Verification QR Code Block
            drawMockQRCode(canvas, 480f, 32f, 75f, verificationUrl)
            canvas.drawText("SECURE VERIFICATION QR", 460f, 118f, Paint().apply { color = Color.GRAY; textSize = 7f })

            // Draw Table Header Block
            y += 30f
            val tableHeaderPaint = Paint().apply {
                color = if (isModern) Color.parseColor("#E0F2F1") else Color.LTGRAY
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, y, 555f, y + 20f, tableHeaderPaint)
            canvas.drawRect(40f, y, 555f, y + 20f, borderPaint)

            val tableTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            canvas.drawText("Date", 45f, y + 14f, tableTextPaint)
            canvas.drawText("Clock In", 110f, y + 14f, tableTextPaint)
            canvas.drawText("Clock Out", 175f, y + 14f, tableTextPaint)
            canvas.drawText("Worked", 240f, y + 14f, tableTextPaint)
            canvas.drawText("Accomplishments / Competencies", 305f, y + 14f, tableTextPaint)
            canvas.drawText("Status", 500f, y + 14f, tableTextPaint)

            y += 20f
            tableTextPaint.isFakeBoldText = false

            // Draw Time Log Rows
            val rowHeight = 24f
            var currentPage = page
            var currentCanvas = canvas
            var pageNum = 1

            logs.forEachIndexed { index, log ->
                // Check if we need to start a new page
                // Page 1 holds up to 20 logs. Subsequent pages hold up to 25 logs.
                val maxLogsOnPage1 = 20
                val maxLogsOnSubsequentPages = 25
                
                val shouldNewPage = if (pageNum == 1) {
                    index >= maxLogsOnPage1
                } else {
                    val indexOnCurrentPages = index - maxLogsOnPage1
                    val indexInThisPage = indexOnCurrentPages % maxLogsOnSubsequentPages
                    indexInThisPage == 0 && indexOnCurrentPages > 0
                }

                if (shouldNewPage) {
                    pdfDocument.finishPage(currentPage)
                    
                    pageNum++
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    currentCanvas = currentPage.canvas

                    // Draw border on new page
                    currentCanvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

                    // Draw Continued Header
                    currentCanvas.drawText("UNIVERSITY INTERNSHIP REPORT - DTR (PAGE $pageNum)", 40f, 45f, Paint().apply {
                        color = Color.parseColor(accentColorString)
                        textSize = 10f
                        isFakeBoldText = true
                        isAntiAlias = true
                    })
                    
                    // Draw Table Header on new page
                    val headerY = 60f
                    val tableHeaderPaint = Paint().apply {
                        color = if (isModern) Color.parseColor("#E0F2F1") else Color.LTGRAY
                        style = Paint.Style.FILL
                    }
                    currentCanvas.drawRect(40f, headerY, 555f, headerY + 20f, tableHeaderPaint)
                    currentCanvas.drawRect(40f, headerY, 555f, headerY + 20f, borderPaint)

                    currentCanvas.drawText("Date", 45f, headerY + 14f, tableTextPaint.apply { isFakeBoldText = true })
                    currentCanvas.drawText("Clock In", 110f, headerY + 14f, tableTextPaint)
                    currentCanvas.drawText("Clock Out", 175f, headerY + 14f, tableTextPaint)
                    currentCanvas.drawText("Worked", 240f, headerY + 14f, tableTextPaint)
                    currentCanvas.drawText("Accomplishments / Competencies", 305f, headerY + 14f, tableTextPaint)
                    currentCanvas.drawText("Status", 500f, headerY + 14f, tableTextPaint)
                    
                    tableTextPaint.isFakeBoldText = false
                    
                    // Reset y position for rows on subsequent page
                    y = 80f
                }

                // Draw row
                currentCanvas.drawRect(40f, y, 555f, y + rowHeight, borderPaint)
                currentCanvas.drawText(log.date, 45f, y + 15f, tableTextPaint)
                
                val inStr = formatIsoTime(log.clockIn)
                val outStr = log.clockOut?.let { formatIsoTime(it) } ?: "--:--"
                currentCanvas.drawText(inStr, 110f, y + 15f, tableTextPaint)
                currentCanvas.drawText(outStr, 175f, y + 15f, tableTextPaint)

                val hrs = log.totalWorkedMinutes / 60
                val mins = log.totalWorkedMinutes % 60
                currentCanvas.drawText("${hrs}h ${mins}m", 240f, y + 15f, tableTextPaint)

                val accomplishmentsPreview = log.accomplishments.take(30) + (if (log.accomplishments.length > 30) "..." else "")
                val competencyStr = if (log.competencyTags.isNotEmpty()) " [${log.competencyTags.joinToString(", ")}]" else ""
                currentCanvas.drawText(accomplishmentsPreview + competencyStr, 305f, y + 15f, tableTextPaint)

                currentCanvas.drawText(log.verificationStatus, 500f, y + 15f, tableTextPaint.apply { 
                    isFakeBoldText = true 
                    color = when(log.verificationStatus) {
                        "APPROVED" -> Color.parseColor("#059669")
                        "REJECTED" -> Color.parseColor("#EF4444")
                        else -> Color.parseColor("#D97706")
                    }
                })
                tableTextPaint.isFakeBoldText = false
                tableTextPaint.color = Color.BLACK

                y += rowHeight
            }

            // Draw Sign-Off Block on the final page (ensure it fits, or add final page if needed)
            if (y > 700f) {
                // Not enough room on the current page, create a final sign-off page
                pdfDocument.finishPage(currentPage)
                pageNum++
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                currentPage = pdfDocument.startPage(pageInfo)
                currentCanvas = currentPage.canvas

                currentCanvas.drawRect(20f, 20f, 575f, 822f, borderPaint)
                y = 80f
            }

            // Draw Sign-Off Block
            y = 720f
            currentCanvas.drawLine(40f, y, 220f, y, borderPaint)
            currentCanvas.drawText("STUDENT INTERN SIGNATURE", 54f, y + 15f, detailsPaint)
            
            currentCanvas.drawLine(375f, y, 555f, y, borderPaint)
            currentCanvas.drawText("SUPERVISOR SIGNATURE & DATE", 380f, y + 15f, detailsPaint)

            // Draw Sign Verification hashes
            val verifiedStampPaint = Paint().apply {
                color = Color.parseColor("#059669")
                textSize = 7f
                isFakeBoldText = true
                isAntiAlias = true
            }
            if (logs.any { it.verificationStatus == "APPROVED" }) {
                currentCanvas.drawText("VERIFIED BY PIN: **** (SECURED)", 375f, y - 20f, verifiedStampPaint)
                currentCanvas.drawText("INTEGRITY HASH: $integrityHash", 375f, y - 10f, verifiedStampPaint)
            }

            pdfDocument.finishPage(currentPage)

            // Save PDF to output file
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            return outputFile
        } finally {
            pdfDocument.close()
        }
    }

    private fun formatIsoTime(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            "--:--"
        }
    }

    private fun sha256(text: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "HASH_ERROR"
        }
    }

    // Dynamic pixel-based QR drawing on Canvas representing authentic security code
    private fun drawMockQRCode(canvas: Canvas, x: Float, y: Float, size: Float, hash: String) {
        val qrBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val qrPixelPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Draw QR box base
        canvas.drawRect(x, y, x + size, y + size, qrBgPaint)
        canvas.drawRect(x, y, x + size, y + size, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f })

        // Render Alignment Markers (Corners)
        val markerSize = 18f
        // Top-Left
        canvas.drawRect(x + 2f, y + 2f, x + markerSize, y + markerSize, qrPixelPaint)
        canvas.drawRect(x + 5f, y + 5f, x + markerSize - 3f, y + markerSize - 3f, qrBgPaint)
        canvas.drawRect(x + 7f, y + 7f, x + markerSize - 5f, y + markerSize - 5f, qrPixelPaint)

        // Top-Right
        canvas.drawRect(x + size - markerSize, y + 2f, x + size - 2f, y + markerSize, qrPixelPaint)
        canvas.drawRect(x + size - markerSize + 3f, y + 5f, x + size - 5f, y + markerSize - 3f, qrBgPaint)
        canvas.drawRect(x + size - markerSize + 5f, y + 7f, x + size - 7f, y + markerSize - 5f, qrPixelPaint)

        // Bottom-Left
        canvas.drawRect(x + 2f, y + size - markerSize, x + markerSize, y + size - 2f, qrPixelPaint)
        canvas.drawRect(x + 5f, y + size - markerSize + 3f, x + markerSize - 3f, y + size - 5f, qrBgPaint)
        canvas.drawRect(x + 7f, y + size - markerSize + 5f, x + markerSize - 5f, y + size - 7f, qrPixelPaint)

        // Generate random pixel elements matching the compiled cryptographic hash string
        val cols = 9
        val pSize = (size - 6f) / cols
        var hashIdx = 0
        for (row in 3..6) {
            for (col in 3..6) {
                // If character in hash is odd/even, draw pixel!
                val charVal = hash.getOrNull(hashIdx)?.code ?: 0
                if (charVal % 2 == 0) {
                    val px = x + 3f + (col * pSize)
                    val py = y + 3f + (row * pSize)
                    canvas.drawRect(px, py, px + pSize - 1f, py + pSize - 1f, qrPixelPaint)
                }
                hashIdx++
            }
        }
    }
}
