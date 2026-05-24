package com.auradtr.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.auradtr.app.ui.DtrViewModel
import com.auradtr.app.ui.screens.MainScreen
import com.auradtr.app.ui.theme.AuraDTRTheme
import com.auradtr.app.worker.DtrReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize View Model using default Factory
        val viewModel = ViewModelProvider(this)[DtrViewModel::class.java]

        // Schedule periodic attendance reminder tasks in background
        schedulePeriodicReminders()

        // Clean up orphaned check-in biometric JPEGs to prevent app storage bloat
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delay sweeper startup to avoid race condition with camera captures
                kotlinx.coroutines.delay(10_000)
                val db = com.auradtr.app.data.DtrDatabase.getDatabase(applicationContext)
                val activeSelfiePaths = db.dtrDao().getAllLogsSync().mapNotNull { it.selfiePath }.toSet()
                
                val selfiesDir = java.io.File(filesDir, "selfies")
                if (selfiesDir.exists() && selfiesDir.isDirectory) {
                    selfiesDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.lowercase().endsWith(".jpg")) {
                            if (!activeSelfiePaths.contains(file.absolutePath)) {
                                file.delete()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val appLockPrefs = getSharedPreferences("aura_app_prefs", android.content.Context.MODE_PRIVATE)
        val isLockEnabled = appLockPrefs.getBoolean("app_lock_enabled", false)

        setContent {
            AuraDTRTheme {
                var isAppUnlocked by remember { mutableStateOf(!isLockEnabled) }

                if (isAppUnlocked) {
                    MainScreen(viewModel = viewModel)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)), // Deep Slate Blue Background
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "App Locked",
                                tint = Color(0xFF14B8A6), // Liquid Teal
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "AURA DTR SECURED",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "App Launch Lock is Active. Verify your identity to resume the active Trainee session.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    com.auradtr.app.security.BiometricPromptHelper(
                                        activity = this@MainActivity,
                                        onSuccess = { isAppUnlocked = true },
                                        onError = { err ->
                                            android.widget.Toast.makeText(applicationContext, "Authentication failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ).showBiometricPrompt("Unlock Aura DTR", "Verify identity to access your DTR logs")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF14B8A6),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Unlock with Biometrics", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Auto-trigger biometric verification on app startup
                    LaunchedEffect(Unit) {
                        com.auradtr.app.security.BiometricPromptHelper(
                            activity = this@MainActivity,
                            onSuccess = { isAppUnlocked = true },
                            onError = { err ->
                                // Silent fallback to allow button click trigger
                            }
                        ).showBiometricPrompt("Unlock Aura DTR", "Verify identity to access your DTR logs")
                    }
                }
            }
        }
    }

    private fun schedulePeriodicReminders() {
        val reminderRequest = PeriodicWorkRequestBuilder<DtrReminderWorker>(
            3, TimeUnit.HOURS // Trigger a validation check every 3 hours
        )
        .addTag("dtr_attendance_reminders")
        .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "dtr_reminders_unique",
            ExistingPeriodicWorkPolicy.KEEP, // Maintain existing schedules
            reminderRequest
        )
    }
}
