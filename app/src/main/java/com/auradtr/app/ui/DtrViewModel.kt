package com.auradtr.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.auradtr.app.data.*
import com.auradtr.app.worker.SyncWorker
import com.auradtr.app.ui.export.CsvExporter
import com.auradtr.app.ui.export.ZipBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class ClockState {
    CLOCKED_OUT,
    CLOCKED_IN,
    ON_BREAK
}

@OptIn(ExperimentalCoroutinesApi::class)
class DtrViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DtrDatabase.getDatabase(application)
    private val dao = db.dtrDao()

    val profile: Flow<Profile?> = dao.getProfile()
    val allLogs: Flow<List<TimeLog>> = dao.getAllLogs()
    val unsyncedLogs: Flow<List<TimeLog>> = dao.getUnsyncedLogs()

    private val _currentDate = MutableStateFlow(LocalDate.now().toString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _syncStatus = MutableStateFlow("Idle")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _backupStatus = MutableStateFlow("Last backup: Never")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    // Manual log validation error feedback
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    fun clearValidationError() { _validationError.value = null }

    // Active log for today
    val activeLog: Flow<TimeLog?> = currentDate.flatMapLatest { date ->
        dao.getLogForDate(date)
    }

    // Current State inferred from active log
    val clockState: Flow<ClockState> = activeLog.map { log ->
        when {
            log == null -> ClockState.CLOCKED_OUT
            log.clockOut != null -> ClockState.CLOCKED_OUT
            log.lunchStart != null && log.lunchEnd == null -> ClockState.ON_BREAK
            else -> ClockState.CLOCKED_IN
        }
    }

    // Initialize mock profile on startup if none exists
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getProfileSync()
            if (existing == null) {
                val salt = com.auradtr.app.security.SecurityUtils.generateSalt()
                val hash = com.auradtr.app.security.SecurityUtils.hashPin(DEFAULT_SUPERVISOR_PIN, salt)
                dao.insertProfile(
                    Profile(
                        studentName = DEFAULT_STUDENT_NAME,
                        studentId = DEFAULT_STUDENT_ID,
                        course = DEFAULT_COURSE,
                        department = DEFAULT_DEPARTMENT,
                        companyName = DEFAULT_COMPANY,
                        supervisorName = DEFAULT_SUPERVISOR,
                        supervisorTitle = DEFAULT_SUPERVISOR_TITLE,
                        targetHours = DEFAULT_TARGET_HOURS,
                        supervisorPinHash = hash,
                        supervisorPinSalt = salt,
                        startDate = LocalDate.now().toString(),
                        endDate = LocalDate.now().plusMonths(3).toString()
                    )
                )
            }
        }
    }

    fun clockIn(locationType: String = "OFFICE", selfiePath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = LocalDate.now().toString()
            val now = Instant.now().toString()
            
            // Check if log already exists for today, else create new
            val existing = dao.getLogForDateSync(date)
            if (existing == null || existing.clockOut != null) {
                val newLog = TimeLog(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    clockIn = now,
                    workLocationType = locationType,
                    verificationStatus = "PENDING",
                    selfiePath = selfiePath,
                    isSynced = false
                )
                dao.insertLog(newLog)
                scheduleOfflineSync()
            }
        }
    }

    fun startLunch() {
        viewModelScope.launch(Dispatchers.IO) {
            val date = LocalDate.now().toString()
            val log = dao.getLogForDateSync(date)
            if (log != null && log.lunchStart == null && log.clockOut == null) {
                dao.insertLog(log.copy(lunchStart = Instant.now().toString()))
            }
        }
    }

    fun endLunch() {
        viewModelScope.launch(Dispatchers.IO) {
            val date = LocalDate.now().toString()
            val log = dao.getLogForDateSync(date)
            if (log != null && log.lunchStart != null && log.lunchEnd == null && log.clockOut == null) {
                dao.insertLog(log.copy(lunchEnd = Instant.now().toString()))
            }
        }
    }

    fun clockOut(accomplishments: String, tags: List<String>, selfiePath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = LocalDate.now().toString()
            val log = dao.getLogForDateSync(date)
            if (log != null && log.clockOut == null) {
                val now = Instant.now().toString()
                
                // Compute total minutes worked
                val clockInTime = Instant.parse(log.clockIn)
                val clockOutTime = Instant.parse(now)
                var activeDuration = Duration.between(clockInTime, clockOutTime)
                
                // Handle lunch break
                if (log.lunchStart != null) {
                    val lunchStartTime = Instant.parse(log.lunchStart)
                    val lunchEndTime = if (log.lunchEnd != null) {
                        Instant.parse(log.lunchEnd)
                    } else {
                        // If they forgot to end lunch, assume 60 mins break
                        lunchStartTime.plusSeconds(3600)
                    }
                    val lunchDuration = Duration.between(lunchStartTime, lunchEndTime)
                    activeDuration = activeDuration.minus(lunchDuration)
                }

                val totalMins = maxOf(0, activeDuration.toMinutes().toInt())

                val updatedLog = log.copy(
                    clockOut = now,
                    totalWorkedMinutes = totalMins,
                    accomplishments = accomplishments,
                    competencyTags = tags,
                    verificationStatus = "PENDING",
                    selfiePath = selfiePath ?: log.selfiePath, // Keep original clock-in selfie if clock-out selfie is null
                    isSynced = false
                )
                dao.insertLog(updatedLog)
                scheduleOfflineSync()
            }
        }
    }

    fun saveProfile(updatedProfile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertProfile(updatedProfile)
        }
    }

    private fun addAuditEntry(
        log: TimeLog,
        field: String,
        oldVal: String,
        newVal: String,
        reason: String
    ): TimeLog {
        val entry = AuditTrailEntry(
            timestamp = Instant.now().toString(),
            fieldChanged = field,
            oldValue = oldVal,
            newValue = newVal,
            changeReason = reason
        )
        return log.copy(auditLogs = log.auditLogs + entry)
    }

    fun addManualLog(
        dateStr: String,
        clockInStr: String, // HH:mm
        clockOutStr: String, // HH:mm
        accomplishments: String,
        tags: List<String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Parse strings into standard Timestamps
                val date = LocalDate.parse(dateStr)
                val inTime = LocalDateTime.parse("${dateStr}T${clockInStr}:00")
                val outTime = LocalDateTime.parse("${dateStr}T${clockOutStr}:00")
                
                val clockInInstant = inTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
                val clockOutInstant = outTime.atZone(java.time.ZoneId.systemDefault()).toInstant()

                val duration = Duration.between(clockInInstant, clockOutInstant)
                if (duration.isNegative || duration.isZero) {
                    _validationError.value = "Clock-out time must be after clock-in time."
                    return@launch
                }

                val rawMins = duration.toMinutes().toInt()
                if (rawMins > 720) {
                    _validationError.value = "Manual entries cannot exceed 12 hours (720 minutes) per day."
                    return@launch
                }

                // Check for chronological time-range overlaps with existing logs on the target date
                val existingLogs = dao.getAllLogsSync().filter { it.date == dateStr }
                val hasOverlap = existingLogs.any { existing ->
                    val existingIn = Instant.parse(existing.clockIn)
                    val existingOut = existing.clockOut?.let { Instant.parse(it) }
                    
                    if (existingOut != null) {
                        val proposedInInside = clockInInstant.isAfter(existingIn) && clockInInstant.isBefore(existingOut)
                        val proposedOutInside = clockOutInstant.isAfter(existingIn) && clockOutInstant.isBefore(existingOut)
                        val proposedEncompasses = clockInInstant.isBefore(existingIn) &&
                                                  clockOutInstant.isAfter(existingOut)
                        proposedInInside || proposedOutInside || proposedEncompasses
                    } else {
                        clockOutInstant.isAfter(existingIn)
                    }
                }
                if (hasOverlap) {
                    _validationError.value = "This time range overlaps with an existing log on the same date."
                    return@launch
                }

                val baseLog = TimeLog(
                    id = UUID.randomUUID().toString(),
                    date = dateStr,
                    clockIn = clockInInstant.toString(),
                    clockOut = clockOutInstant.toString(),
                    totalWorkedMinutes = rawMins,
                    accomplishments = accomplishments,
                    competencyTags = tags,
                    isManualEntry = true,
                    verificationStatus = "PENDING",
                    workLocationType = "UNVERIFIED",
                    isSynced = false
                )
                
                val auditedLog = addAuditEntry(
                    baseLog,
                    field = "log_creation",
                    oldVal = "NONE",
                    newVal = "MANUAL_ENTRY",
                    reason = "Historical manual timecard logbook entry created by student trainee."
                )

                dao.insertLog(auditedLog)
                scheduleOfflineSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun approveLog(logId: String, rating: Int, comment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = dao.getLogByIdSync(logId)
            if (log != null) {
                val auditedLog = addAuditEntry(
                    log,
                    field = "verificationStatus",
                    oldVal = log.verificationStatus,
                    newVal = "APPROVED",
                    reason = "Supervisor approved timecard log with rating $rating. Comment: $comment"
                )
                dao.insertLog(
                    auditedLog.copy(
                        verificationStatus = "APPROVED",
                        supervisorRating = rating,
                        supervisorComment = comment
                    )
                )
            }
        }
    }

    fun rejectLog(logId: String, comment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = dao.getLogByIdSync(logId)
            if (log != null) {
                val auditedLog = addAuditEntry(
                    log,
                    field = "verificationStatus",
                    oldVal = log.verificationStatus,
                    newVal = "REJECTED",
                    reason = "Supervisor rejected timecard log. Comment: $comment"
                )
                dao.insertLog(
                    auditedLog.copy(
                        verificationStatus = "REJECTED",
                        supervisorComment = comment
                    )
                )
            }
        }
    }

    fun deleteLog(log: TimeLog) {
        viewModelScope.launch(Dispatchers.IO) {
            // Block deletion of supervisor-approved logs to preserve audit integrity
            if (log.verificationStatus == "APPROVED") {
                _validationError.value = "Cannot delete an APPROVED log. Contact your supervisor."
                return@launch
            }
            val auditedLog = addAuditEntry(
                log,
                field = "log_deletion",
                oldVal = "EXISTS",
                newVal = "DELETED",
                reason = "Student trainee initiated log deletion."
            )
            // Perform soft-delete (flag isDeleted = true) to keep audit log row intact in database
            val softDeletedLog = auditedLog.copy(isDeleted = true)
            dao.insertLog(softDeletedLog)
        }
    }

    fun syncLogsWithPortal() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = 0.15f
            _syncStatus.value = "Checking offline synchronization queue..."
            delay(800)
            
            // Trigger background WorkManager sync
            scheduleOfflineSync()
            
            _syncProgress.value = 0.60f
            _syncStatus.value = "Executing background sync task..."
            delay(1200)
            
            _syncProgress.value = 1.0f
            _syncStatus.value = "Sync task enqueued! Check status in notification bar."
            delay(800)
            
            _isSyncing.value = false
            _syncProgress.value = 0f
            _syncStatus.value = "Idle"
        }
    }

    fun scheduleOfflineSync() {
        val context = getApplication<Application>().applicationContext
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "DtrSyncJob",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun exportDtrToCsv(onSuccess: (java.io.File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileData = dao.getProfileSync()
                val logs = dao.getAllLogsSync()
                if (profileData != null) {
                    val context = getApplication<Application>().applicationContext
                    val downloadsDir = context.getExternalFilesDir(null)
                    val csvFile = java.io.File(downloadsDir, "OJT_DTR_${profileData.studentName.replace(" ", "_")}.csv")
                    
                    CsvExporter().exportDtrToCsv(profileData, logs, csvFile)
                    viewModelScope.launch(Dispatchers.Main) {
                        onSuccess(csvFile)
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        onError("Profile not configured yet.")
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                viewModelScope.launch(Dispatchers.Main) {
                    onError(e.message ?: "Export failed.")
                }
            }
        }
    }

    fun backupLogsToGoogleDrive(onComplete: (String) -> Unit) {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupStatus.value = "Preparing secure local zip database backup..."
            delay(1000)
            
            val profileData = dao.getProfileSync()
            if (profileData != null) {
                val context = getApplication<Application>().applicationContext
                val backupZip = java.io.File(
                    context.getExternalFilesDir(null),
                    "AuraDTR_Backup_${profileData.studentId.replace(" ", "_")}.zip"
                )
                
                // Compress physical database and captured selfies folders dynamically in background IO thread
                val success = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    ZipBackupHelper().createBackupZip(context, backupZip)
                }
                
                _isBackingUp.value = false
                if (success) {
                    _backupStatus.value = "Last backup: Just now (Secure local offline zip)"
                    onComplete("Offline DTR archive backup created successfully! Saved SQLite databases and captured biometric selfies securely inside your external files storage: ${backupZip.name}")
                } else {
                    _backupStatus.value = "Last backup: Failed"
                    onComplete("Backup compression failed: Could not compile archive write streams.")
                }
            } else {
                _isBackingUp.value = false
                _backupStatus.value = "Last backup: Failed"
                onComplete("Backup failed: Trainee profile details not loaded.")
            }
        }
    }

    companion object {
        private const val DEFAULT_STUDENT_NAME = "Juan Dela Cruz"
        private const val DEFAULT_STUDENT_ID = "2022-10452-CS"
        private const val DEFAULT_COURSE = "BS Computer Science"
        private const val DEFAULT_DEPARTMENT = "College of Computing"
        private const val DEFAULT_COMPANY = "Aura Tech Solutions"
        private const val DEFAULT_SUPERVISOR = "Ms. Sarah Cruz"
        private const val DEFAULT_SUPERVISOR_TITLE = "Software Engineering Lead"
        private const val DEFAULT_TARGET_HOURS = 486
        private const val DEFAULT_SUPERVISOR_PIN = "1234"
    }
}

