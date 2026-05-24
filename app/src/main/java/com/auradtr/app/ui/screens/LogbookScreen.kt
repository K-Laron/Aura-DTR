package com.auradtr.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradtr.app.data.TimeLog
import com.auradtr.app.ui.DtrViewModel
import com.auradtr.app.ui.theme.*
import java.time.LocalDate

@Composable
fun LogbookScreen(viewModel: DtrViewModel) {
    val allLogs by viewModel.allLogs.collectAsState(initial = emptyList())
    var showManualDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualDialog = true },
                containerColor = Color(0xFF14B8A6), // Neon Teal
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Manual Log")
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "DTR LOGBOOK",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DtrHeatmap(allLogs = allLogs)
            Spacer(modifier = Modifier.height(16.dp))

            if (allLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history recorded.\nClick the '+' button below to manually insert historical records.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allLogs) { log ->
                        LogbookItem(log, onDelete = { viewModel.deleteLog(log) })
                    }
                }
            }
        }

        if (showManualDialog) {
            AddManualLogDialog(
                onDismiss = { showManualDialog = false },
                onSubmit = { date, timeIn, timeOut, accomplishments, tags ->
                    viewModel.addManualLog(date, timeIn, timeOut, accomplishments, tags)
                    showManualDialog = false
                }
            )
        }
    }
}

@Composable
fun LogbookItem(log: TimeLog, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val hrs = log.totalWorkedMinutes / 60
    val mins = log.totalWorkedMinutes % 60
    val durationText = String.format("%dh %02dm", hrs, mins)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16, isDark = true)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = log.date,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (log.isManualEntry) "Retroactive Entry" else "GPS-Verified Entry",
                        fontSize = 10.sp,
                        color = if (log.isManualEntry) BreakYellow else ClockInGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = durationText,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF3B82F6) // Cobalt Blue
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show identity verification selfie if exists
                if (log.selfiePath != null) {
                    val bitmap = remember(log.selfiePath) {
                        try {
                            val options = android.graphics.BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                                android.graphics.BitmapFactory.decodeFile(log.selfiePath, this)
                                val targetSize = 200
                                var scale = 1
                                while (outWidth / scale / 2 >= targetSize && outHeight / scale / 2 >= targetSize) {
                                    scale *= 2
                                }
                                inSampleSize = scale
                                inJustDecodeBounds = false
                            }
                            android.graphics.BitmapFactory.decodeFile(log.selfiePath, options)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Text(
                            text = "IDENTITY VERIFICATION PHOTO",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap,
                                contentDescription = "Verification Selfie",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Text(
                    text = "ACCOMPLISHMENTS",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.accomplishments.ifBlank { "No accomplishment notes provided." },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (log.supervisorComment != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SUPERVISOR FEEDBACK (${log.supervisorRating ?: 0} Stars)",
                        fontSize = 10.sp,
                        color = ClockInGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = log.supervisorComment,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ClockOutRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddManualLogDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, List<String>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var clockIn by remember { mutableStateOf("08:00") }
    var clockOut by remember { mutableStateOf("17:00") }
    var accomplishments by remember { mutableStateOf("") }
    
    val competencies = listOf("Development", "Design", "Testing", "Documentation")
    val selectedCompetencies = remember { mutableStateListOf<String>() }

    // Helpers to launch native system pickers
    fun showDatePicker() {
        val current = try { LocalDate.parse(date) } catch(e: Exception) { LocalDate.now() }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                date = selectedDate.toString()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    fun showTimePicker(isClockIn: Boolean) {
        val currentStr = if (isClockIn) clockIn else clockOut
        val current = try { java.time.LocalTime.parse(currentStr) } catch(e: Exception) { java.time.LocalTime.of(8, 0) }
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                if (isClockIn) clockIn = selectedTime else clockOut = selectedTime
            },
            current.hour,
            current.minute,
            true // 24-hour format
        ).show()
    }

    // Dynamic date and time parsing validations
    val isDateValid = remember(date) {
        try {
            LocalDate.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }
    val isTimeInValid = remember(clockIn) {
        try {
            java.time.LocalTime.parse(clockIn)
            true
        } catch (e: Exception) {
            false
        }
    }
    val isTimeOutValid = remember(clockOut) {
        try {
            java.time.LocalTime.parse(clockOut)
            true
        } catch (e: Exception) {
            false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.glassCard(cornerRadius = 24, isDark = true),
        containerColor = Color.Transparent,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true),
        title = { Text("Add Retroactive Log", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive read-only Date field triggering native picker
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    readOnly = true,
                    label = { Text("Select Date", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = LiquidTeal,
                            modifier = Modifier.clickable { showDatePicker() }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LiquidTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = LiquidTeal,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Interactive read-only Time In field
                    OutlinedTextField(
                        value = clockIn,
                        onValueChange = { clockIn = it },
                        readOnly = true,
                        label = { Text("Time In", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker(true) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Select Time In",
                                tint = LiquidTeal,
                                modifier = Modifier.clickable { showTimePicker(true) }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LiquidTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = LiquidTeal,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    // Interactive read-only Time Out field
                    OutlinedTextField(
                        value = clockOut,
                        onValueChange = { clockOut = it },
                        readOnly = true,
                        label = { Text("Time Out", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker(false) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Select Time Out",
                                tint = LiquidTeal,
                                modifier = Modifier.clickable { showTimePicker(false) }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LiquidTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = LiquidTeal,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = accomplishments,
                    onValueChange = { accomplishments = it },
                    placeholder = { Text("What did you accomplish today?", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                    label = { Text("Accomplishments", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LiquidTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = LiquidTeal,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "Select Competencies:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    competencies.forEach { comp ->
                        val isSelected = selectedCompetencies.contains(comp)
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedCompetencies.remove(comp) else selectedCompetencies.add(comp) },
                            label = { Text(comp, fontSize = 9.sp, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LiquidTeal.copy(alpha = 0.2f),
                                selectedLabelColor = LiquidTeal
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = LiquidTeal
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(date, clockIn, clockOut, accomplishments, selectedCompetencies.toList()) },
                enabled = accomplishments.isNotBlank() && isDateValid && isTimeInValid && isTimeOutValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LiquidTeal,
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text("Insert", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DtrHeatmap(allLogs: List<TimeLog>) {
    val scrollState = rememberScrollState()
    val logsMap = remember(allLogs) { allLogs.associate { it.date to it.totalWorkedMinutes } }
    
    val today = LocalDate.now()
    // Align to Monday 25 weeks ago (total 26 weeks shown)
    val startDay = remember {
        today.minusWeeks(25).minusDays((today.dayOfWeek.value - 1).toLong())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16, isDark = true),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ATTENDANCE VISUALIZER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LiquidTeal,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weekday Labels on the left
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    listOf("M", "W", "F").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.height(11.dp) // align with squares
                        )
                    }
                }

                // Grid columns representing weeks
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0..25).forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            (0..6).forEach { dayOfWeek ->
                                val targetDate = startDay.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                                val workedMins = logsMap[targetDate.toString()] ?: 0
                                
                                val color = when {
                                    workedMins == 0 -> Color.White.copy(alpha = 0.08f)
                                    workedMins <= 240 -> ClockInGreen.copy(alpha = 0.25f) // 1-4 hrs
                                    workedMins <= 480 -> ClockInGreen.copy(alpha = 0.6f)  // 4-8 hrs
                                    else -> ClockInGreen // 8+ hrs
                                }

                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
            
            // Legend at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0, 1, 2, 3).forEach { level ->
                    val color = when (level) {
                        0 -> Color.White.copy(alpha = 0.08f)
                        1 -> ClockInGreen.copy(alpha = 0.25f)
                        2 -> ClockInGreen.copy(alpha = 0.6f)
                        else -> ClockInGreen
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "More",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
