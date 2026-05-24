package com.auradtr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradtr.app.data.Profile
import com.auradtr.app.ui.DtrViewModel
import com.auradtr.app.ui.theme.*

@Composable
fun ProfileScreen(viewModel: DtrViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileFlow = viewModel.profile.collectAsState(initial = null)
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var idNum by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var compName by remember { mutableStateOf("") }
    var superName by remember { mutableStateOf("") }
    var targetHrs by remember { mutableStateOf("486") }
    var superPin by remember { mutableStateOf("1234") }
    var geofence by remember { mutableStateOf(false) }
    var workLat by remember { mutableStateOf("14.5995") }
    var workLng by remember { mutableStateOf("120.9842") }
    var workRadius by remember { mutableStateOf("100") }
    var currentPinVerify by remember { mutableStateOf("") }

    LaunchedEffect(profileFlow.value) {
        val p = profileFlow.value
        if (p != null) {
            name = p.studentName
            idNum = p.studentId
            course = p.course
            compName = p.companyName
            superName = p.supervisorName
            targetHrs = p.targetHours.toString()
            superPin = "••••" // Safeguard credentials from cleartext visual leakage
            currentPinVerify = ""
            geofence = p.geofenceEnabled
            workLat = (p.workLatitude ?: 14.5995).toString()
            workLng = (p.workLongitude ?: 120.9842).toString()
            workRadius = p.workGeofenceRadius.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PROFILE SETTINGS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Intern Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Intern Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LiquidTeal)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Name") },
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
                    OutlinedTextField(
                        value = idNum,
                        onValueChange = { idNum = it },
                        label = { Text("Student ID No.") },
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
                    OutlinedTextField(
                        value = course,
                        onValueChange = { course = it },
                        label = { Text("Course / Major") },
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
                }
            }

            // Company Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("OJT Placement Info", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LiquidTeal)
                    OutlinedTextField(
                        value = compName,
                        onValueChange = { compName = it },
                        label = { Text("Host Training Company") },
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
                    OutlinedTextField(
                        value = superName,
                        onValueChange = { superName = it },
                        label = { Text("Supervisor Name") },
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
                    OutlinedTextField(
                        value = targetHrs,
                        onValueChange = { targetHrs = it },
                        label = { Text("Required OJT Target Hours") },
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
                }
            }

            // Security & Geofence Configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Security Configurations", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LiquidTeal)
                    OutlinedTextField(
                        value = superPin,
                        onValueChange = { if (it.length <= 4 && (it.all { c -> c.isDigit() } || it == "••••")) superPin = it },
                        label = { Text("4-digit Supervisor Review PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (superPin == "••••") androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        isError = superPin != "••••" && superPin.length in 1..3,
                        supportingText = if (superPin != "••••" && superPin.length in 1..3) { { Text("PIN must be exactly 4 digits", color = androidx.compose.ui.graphics.Color.Red, fontSize = 11.sp) } } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LiquidTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = LiquidTeal,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    if (superPin != "••••") {
                        OutlinedTextField(
                            value = currentPinVerify,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPinVerify = it },
                            label = { Text("Current Supervisor PIN (Required)") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(12.dp),
                            isError = currentPinVerify.length in 1..3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LiquidTeal,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = LiquidTeal,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce Geofence Verification", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Warn when clocking in outside designated boundaries", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = geofence, 
                            onCheckedChange = { geofence = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LiquidTeal,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    // App Launch Lock Toggle
                    var appLockEnabled by remember { mutableStateOf(false) }
                    val appLockPrefs = remember(context) {
                        context.getSharedPreferences("aura_app_prefs", android.content.Context.MODE_PRIVATE)
                    }
                    LaunchedEffect(Unit) {
                        appLockEnabled = appLockPrefs.getBoolean("app_lock_enabled", false)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce App Launch Lock", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Lock app on startup with biometric authentication", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = appLockEnabled, 
                            onCheckedChange = { value ->
                                appLockEnabled = value
                                appLockPrefs.edit().putBoolean("app_lock_enabled", value).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LiquidTeal,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                    if (geofence) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = workLat,
                                onValueChange = { workLat = it },
                                label = { Text("Latitude", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
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
                            OutlinedTextField(
                                value = workLng,
                                onValueChange = { workLng = it },
                                label = { Text("Longitude", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
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
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = workRadius,
                            onValueChange = { workRadius = it },
                            label = { Text("Geofence Radius (meters)", fontSize = 11.sp) },
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
                    }
                }
            }

            // Google Drive Cloud Backup Card
            val isBackingUp by viewModel.isBackingUp.collectAsState()
            val backupStatus by viewModel.backupStatus.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16, isDark = true),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DTR Local Archive Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LiquidTeal
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local ZIP Backup Exporter",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = backupStatus,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.backupLogsToGoogleDrive { message ->
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isBackingUp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBackingUp) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = if (isBackingUp) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(
                                1.dp, 
                                if (isBackingUp) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), 
                                RoundedCornerShape(12.dp)
                            )
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Cloud",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Save Action Button
            Button(
                onClick = {
                    val current = profileFlow.value
                    if (current != null) {
                        val updatedProfile = if (superPin != "••••" && superPin.length == 4 && superPin.all { it.isDigit() }) {
                            val salt = current.supervisorPinSalt
                            val hash = current.supervisorPinHash
                            val verifyHash = com.auradtr.app.security.SecurityUtils.hashPin(currentPinVerify, salt)
                            if (verifyHash != hash && hash.isNotEmpty()) {
                                android.widget.Toast.makeText(context, "Current Supervisor PIN verification failed. Settings not saved.", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            val newSalt = com.auradtr.app.security.SecurityUtils.generateSalt()
                            val newHash = com.auradtr.app.security.SecurityUtils.hashPin(superPin, newSalt)
                            current.copy(
                                studentName = name,
                                studentId = idNum,
                                course = course,
                                companyName = compName,
                                supervisorName = superName,
                                targetHours = targetHrs.toIntOrNull() ?: 486,
                                supervisorPinHash = newHash,
                                supervisorPinSalt = newSalt,
                                geofenceEnabled = geofence,
                                workLatitude = workLat.toDoubleOrNull() ?: 14.5995,
                                workLongitude = workLng.toDoubleOrNull() ?: 120.9842,
                                workGeofenceRadius = workRadius.toIntOrNull() ?: 100
                            )
                        } else {
                            current.copy(
                                studentName = name,
                                studentId = idNum,
                                course = course,
                                companyName = compName,
                                supervisorName = superName,
                                targetHours = targetHrs.toIntOrNull() ?: 486,
                                geofenceEnabled = geofence,
                                workLatitude = workLat.toDoubleOrNull() ?: 14.5995,
                                workLongitude = workLng.toDoubleOrNull() ?: 120.9842,
                                workGeofenceRadius = workRadius.toIntOrNull() ?: 100
                            )
                        }
                        viewModel.saveProfile(updatedProfile)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LiquidTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            // High-fidelity progress sheet for backup execution
            if (isBackingUp) {
                AlertDialog(
                    onDismissRequest = {},
                    title = null,
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = LiquidTeal,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "LOCAL VAULT INITIATED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTeal,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Compiling secure offline DTR database...\nArchiving captured check-in selfies...",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {},
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color(0xFF10151B).copy(alpha = 0.95f),
                    modifier = Modifier.border(1.3.dp, LiquidTeal.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                )
            }
        }
    }
}
