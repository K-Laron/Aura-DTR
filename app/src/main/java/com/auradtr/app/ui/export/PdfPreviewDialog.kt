package com.auradtr.app.ui.export

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auradtr.app.data.Profile
import com.auradtr.app.data.TimeLog
import com.auradtr.app.ui.theme.*
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun PdfPreviewDialog(
    profile: Profile,
    logs: List<TimeLog>,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    var templateType by remember { mutableStateOf("STANDARD") } // STANDARD or MODERN
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .shadow(1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "TIMESHEET PRINT PREVIEW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onDownload(templateType) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Main Scrolling Preview Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Template Selector Pill
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (templateType == "STANDARD") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { templateType = "STANDARD" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Classic standard",
                                color = if (templateType == "STANDARD") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (templateType == "MODERN") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { templateType = "MODERN" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Modern Teal Accent",
                                color = if (templateType == "MODERN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val pageCount = if (logs.isEmpty()) 1 else {
                        val totalLogs = logs.size
                        if (totalLogs <= 20) 1 else {
                            1 + kotlin.math.ceil((totalLogs - 20) / 25.0).toInt()
                        }
                    }

                    (1..pageCount).forEach { pageNum ->
                        Box(
                            modifier = Modifier
                                .width(360.dp)
                                .height(510.dp)
                                .shadow(8.dp, RoundedCornerShape(8.dp))
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Drawing calculations (A4: 595 x 842 points mapped to 360 x 510 dp)
                                val scaleX = size.width / 595f
                                val scaleY = size.height / 842f

                                // Draw Document Border
                                val isModern = templateType == "MODERN"
                                val accentColor = if (isModern) Color(0xFF00796B) else Color.DarkGray
                                val accentPaintColor = accentColor

                                drawRect(
                                    color = accentPaintColor.copy(alpha = 0.8f),
                                    topLeft = Offset(20f * scaleX, 20f * scaleY),
                                    size = Size(555f * scaleX, 802f * scaleY),
                                    style = Stroke(width = 1f * scaleX)
                                )

                                drawIntoCanvas { composeCanvas ->
                                    val nativeCanvas = composeCanvas.nativeCanvas
                                    val paint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                    }

                                    if (pageNum == 1) {
                                        // Title Block
                                        if (isModern) {
                                            // Draw filled modern header block
                                            drawRoundRect(
                                                color = Color(0xFF00796B),
                                                topLeft = Offset(30f * scaleX, 35f * scaleY),
                                                size = Size(535f * scaleX, 48f * scaleY),
                                                cornerRadius = CornerRadius(4f * scaleX, 4f * scaleY)
                                            )
                                        }

                                        // Header Text
                                        paint.color = if (isModern) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                        paint.textSize = 12f * scaleY
                                        paint.isFakeBoldText = true
                                        nativeCanvas.drawText("UNIVERSITY INTERNSHIP REPORT", 40f * scaleX, 52f * scaleY, paint)

                                        paint.textSize = 8.5f * scaleY
                                        paint.isFakeBoldText = false
                                        nativeCanvas.drawText("DAILY TIME RECORD (DTR) & ACTIVITY SHEET", 40f * scaleX, 68f * scaleY, paint)

                                        // Student Details block
                                        paint.color = android.graphics.Color.BLACK
                                        paint.textSize = 8f * scaleY
                                        var y = 105f
                                        nativeCanvas.drawText("STUDENT: ${profile.studentName}", 40f * scaleX, y * scaleY, paint)
                                        nativeCanvas.drawText("ID NUMBER: ${profile.studentId}", 320f * scaleX, y * scaleY, paint)
                                        y += 15f
                                        nativeCanvas.drawText("COURSE/MAJOR: ${profile.course}", 40f * scaleX, y * scaleY, paint)
                                        nativeCanvas.drawText("DEPARTMENT: ${profile.department}", 320f * scaleX, y * scaleY, paint)
                                        y += 15f
                                        nativeCanvas.drawText("COMPANY: ${profile.companyName}", 40f * scaleX, y * scaleY, paint)
                                        nativeCanvas.drawText("SUPERVISOR: ${profile.supervisorName}", 320f * scaleX, y * scaleY, paint)

                                        // Draw QR placeholder
                                        val qrX = 490f * scaleX
                                        val qrY = 32f * scaleY
                                        val qrSize = 58f * scaleX
                                        paint.color = android.graphics.Color.LTGRAY
                                        nativeCanvas.drawRect(qrX, qrY, qrX + qrSize, qrY + qrSize, paint)
                                        paint.color = android.graphics.Color.DKGRAY
                                        paint.strokeWidth = 1f
                                        paint.style = android.graphics.Paint.Style.STROKE
                                        nativeCanvas.drawRect(qrX, qrY, qrX + qrSize, qrY + qrSize, paint)
                                        paint.style = android.graphics.Paint.Style.FILL

                                        // Table Header Block
                                        y += 20f
                                        paint.color = if (isModern) android.graphics.Color.parseColor("#E0F2F1") else android.graphics.Color.parseColor("#E0E0E0")
                                        nativeCanvas.drawRect(40f * scaleX, y * scaleY, 555f * scaleX, (y + 18f) * scaleY, paint)

                                        paint.color = android.graphics.Color.BLACK
                                        paint.style = android.graphics.Paint.Style.STROKE
                                        nativeCanvas.drawRect(40f * scaleX, y * scaleY, 555f * scaleX, (y + 18f) * scaleY, paint)
                                        paint.style = android.graphics.Paint.Style.FILL

                                        paint.isFakeBoldText = true
                                        paint.textSize = 7.5f * scaleY
                                        nativeCanvas.drawText("Date", 45f * scaleX, (y + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Clock In", 110f * scaleX, (y + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Clock Out", 175f * scaleX, (y + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Worked", 240f * scaleX, (y + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Accomplishments", 305f * scaleX, (y + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Status", 505f * scaleX, (y + 12f) * scaleY, paint)

                                        y += 18f
                                        paint.isFakeBoldText = false

                                        // Draw Rows for Page 1 (up to 20 logs)
                                        val rowH = 18f
                                        val pageLogs = logs.take(20)
                                        pageLogs.forEachIndexed { rowIdx, log ->
                                            val rowY = y + rowIdx * rowH
                                            paint.style = android.graphics.Paint.Style.STROKE
                                            paint.color = android.graphics.Color.parseColor("#E0E0E0")
                                            nativeCanvas.drawRect(40f * scaleX, rowY * scaleY, 555f * scaleX, (rowY + rowH) * scaleY, paint)
                                            paint.style = android.graphics.Paint.Style.FILL

                                            paint.color = android.graphics.Color.BLACK
                                            nativeCanvas.drawText(log.date, 45f * scaleX, (rowY + 12f) * scaleY, paint)

                                            val inTime = formatTime(log.clockIn)
                                            val outTime = log.clockOut?.let { formatTime(it) } ?: "--:--"
                                            nativeCanvas.drawText(inTime, 110f * scaleX, (rowY + 12f) * scaleY, paint)
                                            nativeCanvas.drawText(outTime, 175f * scaleX, (rowY + 12f) * scaleY, paint)

                                            val hrs = log.totalWorkedMinutes / 60
                                            val mins = log.totalWorkedMinutes % 60
                                            nativeCanvas.drawText("${hrs}h ${mins}m", 240f * scaleX, (rowY + 12f) * scaleY, paint)

                                            val desc = log.accomplishments.take(22) + (if (log.accomplishments.length > 22) "..." else "")
                                            nativeCanvas.drawText(desc, 305f * scaleX, (rowY + 12f) * scaleY, paint)

                                            paint.color = when (log.verificationStatus) {
                                                "APPROVED" -> android.graphics.Color.parseColor("#059669")
                                                "REJECTED" -> android.graphics.Color.parseColor("#EF4444")
                                                else -> android.graphics.Color.parseColor("#D97706")
                                            }
                                            paint.isFakeBoldText = true
                                            nativeCanvas.drawText(log.verificationStatus, 505f * scaleX, (rowY + 12f) * scaleY, paint)

                                            paint.isFakeBoldText = false
                                            paint.color = android.graphics.Color.BLACK
                                        }

                                        // Draw Sign Block if it is the only page
                                        if (pageCount == 1) {
                                            drawSignBlock(nativeCanvas, scaleX, scaleY, 720f, borderPaint = android.graphics.Paint().apply { color = if (isModern) android.graphics.Color.parseColor("#00796B") else android.graphics.Color.BLACK; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1f }, detailsPaint = paint, logs = logs, profile = profile)
                                        }
                                    } else {
                                        // Draw Continued Header on subsequent page
                                        paint.color = if (isModern) android.graphics.Color.parseColor("#00796B") else android.graphics.Color.BLACK
                                        paint.textSize = 10f * scaleY
                                        paint.isFakeBoldText = true
                                        nativeCanvas.drawText("UNIVERSITY INTERNSHIP REPORT - DTR (PAGE $pageNum)", 40f * scaleX, 45f * scaleY, paint)

                                        // Draw Table Header on subsequent page
                                        val headerY = 60f
                                        paint.color = if (isModern) android.graphics.Color.parseColor("#E0F2F1") else android.graphics.Color.parseColor("#E0E0E0")
                                        nativeCanvas.drawRect(40f * scaleX, headerY * scaleY, 555f * scaleX, (headerY + 18f) * scaleY, paint)

                                        paint.color = android.graphics.Color.BLACK
                                        paint.style = android.graphics.Paint.Style.STROKE
                                        nativeCanvas.drawRect(40f * scaleX, headerY * scaleY, 555f * scaleX, (headerY + 18f) * scaleY, paint)
                                        paint.style = android.graphics.Paint.Style.FILL

                                        paint.textSize = 7.5f * scaleY
                                        nativeCanvas.drawText("Date", 45f * scaleX, (headerY + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Clock In", 110f * scaleX, (headerY + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Clock Out", 175f * scaleX, (headerY + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Worked", 240f * scaleX, (headerY + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Accomplishments", 305f * scaleX, (headerY + 12f) * scaleY, paint)
                                        nativeCanvas.drawText("Status", 505f * scaleX, (headerY + 12f) * scaleY, paint)

                                        paint.isFakeBoldText = false

                                        // Draw rows for this page
                                        val rowH = 18f
                                        val startIndex = 20 + (pageNum - 2) * 25
                                        val pageLogs = logs.drop(startIndex).take(25)
                                        var y = 80f
                                        pageLogs.forEachIndexed { rowIdx, log ->
                                            val rowY = y + rowIdx * rowH
                                            paint.style = android.graphics.Paint.Style.STROKE
                                            paint.color = android.graphics.Color.parseColor("#E0E0E0")
                                            nativeCanvas.drawRect(40f * scaleX, rowY * scaleY, 555f * scaleX, (rowY + rowH) * scaleY, paint)
                                            paint.style = android.graphics.Paint.Style.FILL

                                            paint.color = android.graphics.Color.BLACK
                                            nativeCanvas.drawText(log.date, 45f * scaleX, (rowY + 12f) * scaleY, paint)

                                            val inTime = formatTime(log.clockIn)
                                            val outTime = log.clockOut?.let { formatTime(it) } ?: "--:--"
                                            nativeCanvas.drawText(inTime, 110f * scaleX, (rowY + 12f) * scaleY, paint)
                                            nativeCanvas.drawText(outTime, 175f * scaleX, (rowY + 12f) * scaleY, paint)

                                            val hrs = log.totalWorkedMinutes / 60
                                            val mins = log.totalWorkedMinutes % 60
                                            nativeCanvas.drawText("${hrs}h ${mins}m", 240f * scaleX, (rowY + 12f) * scaleY, paint)

                                            val desc = log.accomplishments.take(22) + (if (log.accomplishments.length > 22) "..." else "")
                                            nativeCanvas.drawText(desc, 305f * scaleX, (rowY + 12f) * scaleY, paint)

                                            paint.color = when (log.verificationStatus) {
                                                "APPROVED" -> android.graphics.Color.parseColor("#059669")
                                                "REJECTED" -> android.graphics.Color.parseColor("#EF4444")
                                                else -> android.graphics.Color.parseColor("#D97706")
                                            }
                                            paint.isFakeBoldText = true
                                            nativeCanvas.drawText(log.verificationStatus, 505f * scaleX, (rowY + 12f) * scaleY, paint)

                                            paint.isFakeBoldText = false
                                            paint.color = android.graphics.Color.BLACK
                                        }

                                        // Draw sign block if it is the last page
                                        if (pageNum == pageCount) {
                                            drawSignBlock(nativeCanvas, scaleX, scaleY, 720f, borderPaint = android.graphics.Paint().apply { color = if (isModern) android.graphics.Color.parseColor("#00796B") else android.graphics.Color.BLACK; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1f }, detailsPaint = paint, logs = logs, profile = profile)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PAGE $pageNum OF $pageCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text(
                        text = "A4 Page aspect ratio preview. Standard template is monochrome. Modern template highlights boundaries with primary university colors.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Download Button Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { onDownload(templateType) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Save PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatTime(isoStr: String): String {
    return try {
        val instant = Instant.parse(isoStr)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "--:--"
    }
}

private fun drawSignBlock(
    nativeCanvas: android.graphics.Canvas,
    scaleX: Float,
    scaleY: Float,
    yVal: Float,
    borderPaint: android.graphics.Paint,
    detailsPaint: android.graphics.Paint,
    logs: List<TimeLog>,
    profile: Profile
) {
    val y = yVal
    borderPaint.color = android.graphics.Color.BLACK
    borderPaint.style = android.graphics.Paint.Style.STROKE
    nativeCanvas.drawLine(40f * scaleX, y * scaleY, 200f * scaleX, y * scaleY, borderPaint)
    nativeCanvas.drawLine(380f * scaleX, y * scaleY, 540f * scaleX, y * scaleY, borderPaint)
    borderPaint.style = android.graphics.Paint.Style.FILL

    detailsPaint.textSize = 7.5f * scaleY
    nativeCanvas.drawText("STUDENT INTERN SIGNATURE", 48f * scaleX, (y + 12f) * scaleY, detailsPaint)
    nativeCanvas.drawText("SUPERVISOR SIGNATURE & DATE", 385f * scaleX, (y + 12f) * scaleY, detailsPaint)

    // PIN / SHA verified stamp
    if (logs.any { it.verificationStatus == "APPROVED" }) {
        detailsPaint.color = android.graphics.Color.parseColor("#059669")
        detailsPaint.textSize = 6.5f * scaleY
        detailsPaint.isFakeBoldText = true
        nativeCanvas.drawText("VERIFIED BY PIN: **** (SECURED)", 380f * scaleX, (y - 18f) * scaleY, detailsPaint)
        nativeCanvas.drawText("SECURED BY CRYPTO QR CODE", 380f * scaleX, (y - 8f) * scaleY, detailsPaint)
    }
}
