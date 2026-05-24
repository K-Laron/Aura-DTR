package com.auradtr.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradtr.app.data.TimeLog
import com.auradtr.app.ui.DtrViewModel
import com.auradtr.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DtrViewModel) {
    val profile by viewModel.profile.collectAsState(initial = null)
    val allLogs by viewModel.allLogs.collectAsState(initial = emptyList())

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Computations
    val totalHoursCompleted = allLogs.sumOf { it.totalWorkedMinutes } / 60f
    val targetHours = profile?.targetHours ?: 486
    val completionPercentage = if (targetHours > 0) (totalHoursCompleted / targetHours * 100).coerceAtMost(100f) else 0f

    // Pacing calculations
    val totalWorkedDays = allLogs.size
    val averageHoursPerDay = if (totalWorkedDays > 0) totalHoursCompleted / totalWorkedDays else 0f
    
    // Streak (consecutive logged working days)
    val streakCount = calculateStreak(allLogs)

    // Forecast completion date (business days only — Mon through Fri)
    val remainingHours = targetHours - totalHoursCompleted
    val estimatedWorkDaysLeft = if (averageHoursPerDay > 0) (remainingHours / averageHoursPerDay).toLong() else 0L
    val forecastDate = run {
        var date = LocalDate.now()
        var workDaysRemaining = maxOf(0L, estimatedWorkDaysLeft)
        while (workDaysRemaining > 0) {
            date = date.plusDays(1)
            if (date.dayOfWeek != java.time.DayOfWeek.SATURDAY && date.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
                workDaysRemaining--
            }
        }
        date
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome and Student Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome Back,",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Text(
                    text = profile?.studentName ?: "Student Intern",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.W900
                )
                Text(
                    text = "${profile?.course ?: "OJT"} • ${profile?.companyName ?: "Aura Tech"}",
                    color = Color(0xFF14B8A6), // Neon Teal
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Custom Paint hours ring card (Apple Glassmorphism Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL OJT PROGRESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 3D Curved Glass Donut Progress
                    Box(contentAlignment = Alignment.Center) {
                        val animateProgress by animateFloatAsState(
                            targetValue = completionPercentage,
                            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                        )

                        Canvas(modifier = Modifier.size(175.dp)) {
                            // Ambient Neon Drop Glow (Blurred shadow duplicate)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFF3B82F6))
                                ),
                                startAngle = -220f,
                                sweepAngle = (animateProgress / 100f * 260f).coerceAtLeast(0.1f),
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.25f
                            )
                            
                            // Indented Pocket Pocket Track
                            drawArc(
                                color = Color.Black.copy(alpha = 0.3f),
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            // Reflective Base Track border
                            drawArc(
                                color = Color.White.copy(alpha = 0.05f),
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            // Rich Premium Active Arc (Cobalt -> Magenta -> Cyan)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFF3B82F6))
                                ),
                                startAngle = -220f,
                                sweepAngle = (animateProgress / 100f * 260f).coerceAtLeast(0.1f),
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            // 3D Bevel Dome Sheen Overlay
                            drawArc(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.15f))
                                ),
                                startAngle = -220f,
                                sweepAngle = (animateProgress / 100f * 260f).coerceAtLeast(0.1f),
                                useCenter = false,
                                style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Text indicators in center
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f%%", completionPercentage),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.W900,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = String.format("%.1fh / %dh", totalHoursCompleted, targetHours),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Gamified OJT Achievement Milestones Shelf
        item {
            val totalMins = allLogs.sumOf { it.totalWorkedMinutes }
            val currentHrs = totalMins / 60f
            val context = LocalContext.current

            val milestoneItems = listOf(
                Milestone("Bronze Cadet", 50f, Color(0xFFD97706), "Start of OJT journey"),
                Milestone("Silver Builder", 150f, Color(0xFF94A3B8), "Core system integrations"),
                Milestone("Gold Developer", 300f, Color(0xFFF59E0B), "Advanced architecture"),
                Milestone("OJT Champion", 450f, Color(0xFF3B82F6), "Enterprise features"),
                Milestone("Master Graduate", 486f, Color(0xFF8B5CF6), "Completed target hours")
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "OJT MILESTONE ACHIEVEMENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTeal,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Horizontal scrollable badges shelf
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(milestoneItems) { milestone ->
                            val isUnlocked = currentHrs >= milestone.targetHours
                            val progress = (currentHrs / milestone.targetHours).coerceIn(0f, 1f)
                            
                            Box(
                                modifier = Modifier
                                    .width(115.dp)
                                    .glassCard(cornerRadius = 16, isDark = true)
                                    .border(
                                        1.dp,
                                        if (isUnlocked) milestone.color.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        triggerHaptic(context)
                                        val message = if (isUnlocked) {
                                            "Unlocked: ${milestone.name}! Earned at ${milestone.targetHours.toInt()} hours."
                                        } else {
                                            "Locked: Log ${(milestone.targetHours - currentHrs).toInt()} more hours to earn ${milestone.name} badge."
                                        }
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Circular Medal Badge Icon
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isUnlocked) milestone.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                2.dp,
                                                if (isUnlocked) milestone.color else Color.White.copy(alpha = 0.1f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isUnlocked) {
                                            Text(
                                                text = "🔥",
                                                fontSize = 18.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = milestone.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${milestone.targetHours.toInt()} hrs",
                                        fontSize = 9.sp,
                                        color = if (isUnlocked) milestone.color else Color.White.copy(alpha = 0.3f),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    
                                    // Progress bar inside badge
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (isUnlocked) milestone.color else LiquidTeal.copy(alpha = 0.4f),
                                        trackColor = Color.White.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Glassmorphic Interactive Analytics Bar Chart
        item {
            val context = LocalContext.current
            var selectedBarIndex by remember { mutableIntStateOf(-1) }
            var tooltipOffset by remember { mutableStateOf(Offset.Zero) }

            // Get last 7 days of logs sorted chronologically
            val last7DaysLogs = remember(allLogs) {
                allLogs.filter { it.totalWorkedMinutes > 0 }
                    .take(7)
                    .reversed()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "WEEKLY ATTENDANCE ANALYTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTeal,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (last7DaysLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No analytics records found.\nLog shifts to populate worked hours graph.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(last7DaysLogs) {
                                        detectTapGestures { offset ->
                                            // Dynamic hitbox calculation for 7 bars
                                            val paddingLeft = 32.dp.toPx()
                                            val paddingRight = 16.dp.toPx()
                                            val paddingTop = 16.dp.toPx()
                                            val paddingBottom = 24.dp.toPx()
                                            
                                            val graphWidth = size.width - paddingLeft - paddingRight
                                            val graphHeight = size.height - paddingTop - paddingBottom
                                            val barWidth = graphWidth / (last7DaysLogs.size * 2 - 1)

                                            var foundIndex = -1
                                            for (i in last7DaysLogs.indices) {
                                                val barLeft = paddingLeft + i * 2 * barWidth
                                                val barRight = barLeft + barWidth
                                                if (offset.x >= barLeft && offset.x <= barRight && offset.y >= paddingTop && offset.y <= size.height - paddingBottom) {
                                                    foundIndex = i
                                                    tooltipOffset = Offset(barLeft + barWidth / 2, offset.y - 12.dp.toPx())
                                                    triggerHaptic(context)
                                                    break
                                                }
                                            }
                                            selectedBarIndex = if (selectedBarIndex == foundIndex) -1 else foundIndex
                                        }
                                    }
                            ) {
                                val paddingLeft = 32.dp.toPx()
                                val paddingRight = 16.dp.toPx()
                                val paddingTop = 16.dp.toPx()
                                val paddingBottom = 24.dp.toPx()
                                
                                val graphWidth = size.width - paddingLeft - paddingRight
                                val graphHeight = size.height - paddingTop - paddingBottom
                                
                                // Draw horizontal guides (0h, 4h, 8h, 12h)
                                val maxHours = 12f
                                val guides = listOf(0f, 4f, 8f, 12f)
                                guides.forEach { hours ->
                                    val y = size.height - paddingBottom - (hours / maxHours * graphHeight)
                                    
                                    // Draw grid line
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.05f),
                                        start = Offset(paddingLeft, y),
                                        end = Offset(size.width - paddingRight, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Draw 7 vertical bars
                                if (last7DaysLogs.isNotEmpty()) {
                                    val barWidth = graphWidth / (last7DaysLogs.size * 2 - 1)

                                    last7DaysLogs.forEachIndexed { i, log ->
                                        val workedHours = log.totalWorkedMinutes / 60f
                                        val barLeft = paddingLeft + i * 2 * barWidth
                                        val barHeight = (workedHours / maxHours * graphHeight).coerceAtMost(graphHeight)
                                        val barTop = size.height - paddingBottom - barHeight

                                        // Neon active colored bar brush
                                        val isSelected = selectedBarIndex == i
                                        val barBrush = Brush.verticalGradient(
                                            colors = if (isSelected) {
                                                listOf(LiquidMagenta, LiquidPurple)
                                            } else {
                                                listOf(LiquidCobalt, LiquidTeal)
                                            }
                                        )

                                        // Draw Neon Drop Glow underneath (blurred copy)
                                        drawRoundRect(
                                            brush = barBrush,
                                            topLeft = Offset(barLeft, barTop),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                                            alpha = if (isSelected) 0.35f else 0.15f
                                        )

                                        // Draw Glass Bar
                                        drawRoundRect(
                                            brush = barBrush,
                                            topLeft = Offset(barLeft, barTop),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                                        )

                                        // Draw white highlight sheen reflection on bar
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.25f),
                                            topLeft = Offset(barLeft + 1.dp.toPx(), barTop + 1.dp.toPx()),
                                            size = Size(barWidth - 2.dp.toPx(), (barHeight * 0.15f).coerceAtMost(8.dp.toPx())),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                    }
                                }
                            }

                            // Render Hover tooltip popup inside graph box
                            if (selectedBarIndex != -1 && selectedBarIndex < last7DaysLogs.size) {
                                val log = last7DaysLogs[selectedBarIndex]
                                val hours = log.totalWorkedMinutes / 60f
                                val parsedDate = remember(log.date) {
                                    try {
                                        LocalDate.parse(log.date).format(DateTimeFormatter.ofPattern("EEE dd"))
                                    } catch (e: Exception) {
                                        log.date
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = (tooltipOffset.x / LocalContext.current.resources.displayMetrics.density).dp - 60.dp,
                                            y = (tooltipOffset.y / LocalContext.current.resources.displayMetrics.density).dp - 40.dp
                                        )
                                        .width(120.dp)
                                        .glassCard(cornerRadius = 10, isDark = true)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = parsedDate,
                                            fontSize = 8.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = String.format("%.1fh worked", hours),
                                            fontSize = 10.sp,
                                            color = LiquidTeal,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pacing & Metrics Analytics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .glassCard(cornerRadius = 20, isDark = true),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("STREAK", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (streakCount > 0) "$streakCount Days 🔥" else "0 Days 💤",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BreakYellow
                        )
                        Text("Active log streak", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
                
                // Forecast Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .glassCard(cornerRadius = 20, isDark = true),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMPLETION", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (totalWorkedDays > 0) forecastDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) else "TBD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClockInGreen
                        )
                        Text("Forecasted date", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // Daily Pacing / Average Metrics Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Pacing: " + String.format("%.2f hours / day", averageHoursPerDay),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "At this pace, you have ${remainingHours.toInt() / 8} full shifts left.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // University Portal Sync Console
        item {
            val unsyncedLogs by viewModel.unsyncedLogs.collectAsState(initial = emptyList())
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UNIVERSITY PORTAL SYNC STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTeal, // Neon Teal
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSyncing) BreakYellow
                                            else if (unsyncedLogs.isNotEmpty()) BreakYellow
                                            else ClockInGreen
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSyncing) syncStatus
                                           else if (unsyncedLogs.isNotEmpty()) "Pending upload: ${unsyncedLogs.size} logs queued"
                                           else "All records synced securely",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSyncing) BreakYellow
                                            else if (unsyncedLogs.isNotEmpty()) BreakYellow
                                            else ClockInGreen
                                )
                            }
                        }
                        
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                triggerHaptic(context)
                                viewModel.syncLogsWithPortal()
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isSyncing) {
                        LinearProgressIndicator(
                            progress = { syncProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LiquidTeal,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // Recent Logs Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT TIME LOGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            }
        }

        // Empty logs placeholder or latest list items
        if (allLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No time logs recorded yet.\nTap 'Clock In' in the time clock to start!",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(allLogs.take(5)) { log ->
                RecentLogItem(log)
            }
        }
    }
}

@Composable
fun RecentLogItem(log: TimeLog) {
    val hrs = log.totalWorkedMinutes / 60
    val mins = log.totalWorkedMinutes % 60
    val durationText = String.format("%dh %02dm", hrs, mins)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16, isDark = true),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = log.accomplishments.take(65) + (if (log.accomplishments.length > 65) "..." else ""),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                // Competencies tags
                if (log.competencyTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        log.competencyTags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF14B8A6).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(tag, color = Color(0xFF14B8A6), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationText,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF3B82F6) // Apple-style Cobalt
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Verification status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (log.verificationStatus) {
                            "APPROVED" -> Icons.Default.CheckCircle
                            "REJECTED" -> Icons.Default.Warning
                            else -> Icons.Default.Refresh
                        },
                        contentDescription = null,
                        tint = when (log.verificationStatus) {
                            "APPROVED" -> ClockInGreen
                            "REJECTED" -> ClockOutRed
                            else -> BreakYellow
                        },
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.verificationStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (log.verificationStatus) {
                            "APPROVED" -> ClockInGreen
                            "REJECTED" -> ClockOutRed
                            else -> BreakYellow
                        }
                    )
                }
            }
        }
    }
}

private fun calculateStreak(logs: List<TimeLog>): Int {
    if (logs.isEmpty()) return 0
    val dates = logs.map { LocalDate.parse(it.date) }.distinct().sortedDescending()
    var streak = 0
    var expectedDate = LocalDate.now()

    // Skip back over weekends to find the last expected working day
    while (expectedDate.dayOfWeek == java.time.DayOfWeek.SATURDAY || expectedDate.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
        expectedDate = expectedDate.minusDays(1)
    }

    // Check if the most recent log is on or after the last expected working day
    if (dates.firstOrNull()?.let { it != expectedDate && it != run {
        var prev = expectedDate.minusDays(1)
        while (prev.dayOfWeek == java.time.DayOfWeek.SATURDAY || prev.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            prev = prev.minusDays(1)
        }
        prev
    }} != false) {
        return 0
    }

    expectedDate = dates.first()

    for (date in dates) {
        if (date == expectedDate) {
            streak++
            // Move to previous business day (skip weekends)
            expectedDate = expectedDate.minusDays(1)
            while (expectedDate.dayOfWeek == java.time.DayOfWeek.SATURDAY || expectedDate.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                expectedDate = expectedDate.minusDays(1)
            }
        } else {
            break
        }
    }
    return streak
}

private fun triggerHaptic(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
}

data class Milestone(
    val name: String,
    val targetHours: Float,
    val color: Color,
    val desc: String
)
