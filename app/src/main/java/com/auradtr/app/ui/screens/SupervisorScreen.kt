package com.auradtr.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.auradtr.app.security.BiometricPromptHelper
import com.auradtr.app.security.findActivity
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradtr.app.data.TimeLog
import com.auradtr.app.ui.DtrViewModel
import com.auradtr.app.ui.theme.*

@Composable
fun SupervisorScreen(viewModel: DtrViewModel) {
    val profile by viewModel.profile.collectAsState(initial = null)
    val allLogs by viewModel.allLogs.collectAsState(initial = emptyList())
    val pendingLogs = allLogs.filter { it.verificationStatus == "PENDING" }

    var isUnlocked by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("supervisor_auth_prefs", android.content.Context.MODE_PRIVATE)
    }

    var failedAttempts by remember {
        mutableStateOf(sharedPrefs.getInt("failed_attempts", 0))
    }
    var lockoutUntil by remember {
        mutableStateOf(sharedPrefs.getLong("lockout_until", 0L))
    }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lockoutUntil) {
        if (lockoutUntil > 0) {
            while (System.currentTimeMillis() < lockoutUntil) {
                currentTime = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000)
            }
            // Reset failed attempts after lockout expiry to prevent immediate re-lockout
            currentTime = System.currentTimeMillis()
            failedAttempts = 0
            sharedPrefs.edit().putInt("failed_attempts", 0).putLong("lockout_until", 0L).apply()
            lockoutUntil = 0L
        }
    }

    var activeReviewLog by remember { mutableStateOf<TimeLog?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isUnlocked) {
            // PIN Entry Portal wrapped in beautiful glassCard
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .glassCard(cornerRadius = 24, isDark = true)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = LiquidTeal,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SUPERVISOR REVIEW PORTAL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Enter the 4-digit supervisor PIN from settings to unlock verification",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                val isLockedOut = currentTime < lockoutUntil
                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 4 && !isLockedOut && it.all { c -> c.isDigit() }) {
                            pinText = it
                            pinError = false
                        }
                    },
                    label = { Text("Supervisor PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLockedOut,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
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
                if (isLockedOut) {
                    val remainingSecs = ((lockoutUntil - currentTime) / 1000).coerceAtLeast(0)
                    val displayMins = remainingSecs / 60
                    val displaySecs = remainingSecs % 60
                    Text(
                        text = String.format("Too many failed attempts. Locked out for %02d:%02d", displayMins, displaySecs),
                        color = ClockOutRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (pinError) {
                    val remainingAttempts = 5 - failedAttempts
                    Text(
                        text = "Invalid PIN. $remainingAttempts attempts remaining.",
                        color = ClockOutRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isLockedOut) return@Button
                            val salt = profile?.supervisorPinSalt ?: ""
                            val hash = profile?.supervisorPinHash ?: ""
                            val inputHash = com.auradtr.app.security.SecurityUtils.hashPin(pinText, salt)
                            
                            if (inputHash == hash && hash.isNotEmpty()) {
                                isUnlocked = true
                                pinError = false
                                failedAttempts = 0
                                sharedPrefs.edit().putInt("failed_attempts", 0).putLong("lockout_until", 0L).apply()
                            } else {
                                val newFailedAttempts = failedAttempts + 1
                                failedAttempts = newFailedAttempts
                                sharedPrefs.edit().putInt("failed_attempts", newFailedAttempts).apply()
                                if (newFailedAttempts >= 5) {
                                    val lockoutTime = System.currentTimeMillis() + 900_000 // 15 minutes lockout
                                    lockoutUntil = lockoutTime
                                    sharedPrefs.edit().putLong("lockout_until", lockoutTime).apply()
                                    pinError = false
                                } else {
                                    pinError = true
                                }
                            }
                        },
                        enabled = !isLockedOut,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LiquidTeal,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Unlock with PIN", fontWeight = FontWeight.Bold)
                    }
                    
                    val context = LocalContext.current
                    FilledIconButton(
                        onClick = {
                            if (isLockedOut) return@FilledIconButton
                            val activity = context.findActivity()
                            if (activity != null) {
                                BiometricPromptHelper(
                                    activity = activity,
                                    onSuccess = {
                                        isUnlocked = true
                                        pinError = false
                                    },
                                    onError = { err ->
                                        if (err != "PIN_FALLBACK") {
                                            pinError = true
                                        }
                                    }
                                ).showBiometricPrompt()
                            } else {
                                pinError = true
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LiquidPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Verify Biometrics",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            // Supervisor Dashboard Review Panel
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SUPERVISOR VIEW",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = profile?.studentName ?: "Trainee Logs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    TextButton(onClick = { isUnlocked = false; pinText = "" }) {
                        Text("Lock Portal", color = ClockOutRed, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Pending Log Approvals (${pendingLogs.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LiquidTeal
                )

                if (pendingLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .glassCard(cornerRadius = 24, isDark = true)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All logs reviewed!\nNo pending verification items.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(pendingLogs) { log ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(cornerRadius = 16, isDark = true)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.date, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Text(
                                            text = log.accomplishments.take(50) + if (log.accomplishments.length > 50) "..." else "", 
                                            fontSize = 11.sp, 
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${log.totalWorkedMinutes / 60}h ${log.totalWorkedMinutes % 60}m worked",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LiquidTeal,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { activeReviewLog = log },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.15f),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    ) {
                                        Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Review Modal with Vector Signature Canvas Pad
        if (activeReviewLog != null) {
            ReviewLogDialog(
                log = activeReviewLog!!,
                onDismiss = { activeReviewLog = null },
                onSubmit = { id, rating, comment ->
                    viewModel.approveLog(id, rating, comment)
                    activeReviewLog = null
                },
                onReject = { id, comment ->
                    viewModel.rejectLog(id, comment)
                    activeReviewLog = null
                }
            )
        }
    }
}

@Composable
fun ReviewLogDialog(
    log: TimeLog,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    
    // Signature drawing path states
    val drawPaths = remember { mutableStateListOf<ComposePath>() }
    var currentPath by remember { mutableStateOf<ComposePath?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.glassCard(cornerRadius = 24, isDark = true),
        containerColor = Color.Transparent,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true),
        title = { Text("Log Review: ${log.date}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${log.totalWorkedMinutes / 60}h ${log.totalWorkedMinutes % 60}m logged (${log.workLocationType})",
                    fontWeight = FontWeight.Bold,
                    color = LiquidTeal,
                    fontSize = 13.sp
                )
                Text(
                    text = "Intern Accomplishments:",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = log.accomplishments,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillModifierBorder()
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Rate Performance:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        val isSelected = star <= rating
                        Text(
                            text = if (isSelected) "★" else "☆",
                            fontSize = 24.sp,
                            color = if (isSelected) BreakYellow else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.clickable { rating = star }
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Add comment or improvement notes...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                    label = { Text("Supervisor Comment", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
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

                // Interactive Drawing Signature Canvas
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Supervisor Signature:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (drawPaths.isNotEmpty()) {
                            Text(
                                text = "Undo",
                                fontSize = 11.sp,
                                color = LiquidTeal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { drawPaths.removeLastOrNull() }
                            )
                        }
                        Text(
                            text = "Clear Pad",
                            fontSize = 11.sp,
                            color = ClockOutRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { drawPaths.clear() }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newP = ComposePath()
                                    newP.moveTo(offset.x, offset.y)
                                    currentPath = newP
                                    drawPaths.add(newP)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentPath?.let { p ->
                                        p.lineTo(change.position.x, change.position.y)
                                        val idx = drawPaths.indexOf(p)
                                        if (idx >= 0) {
                                            drawPaths[idx] = ComposePath().apply { addPath(p) }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPaths.forEach { p ->
                            drawPath(
                                path = p,
                                color = LiquidTeal,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    if (drawPaths.isEmpty()) {
                        Text(
                            text = "Sign here with finger",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(log.id, rating, comment) },
                enabled = drawPaths.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LiquidTeal,
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text("Approve", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onReject(log.id, comment) },
                colors = ButtonDefaults.textButtonColors(contentColor = ClockOutRed)
            ) {
                Text("Reject", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// Custom Extension to draw simple bordered blocks inside dialogs
@Composable
fun Modifier.fillModifierBorder(): Modifier {
    return this
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White.copy(alpha = 0.05f))
        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
}
