package com.auradtr.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "time_logs")
data class TimeLog(
    @PrimaryKey val id: String, // UUID
    val date: String, // YYYY-MM-DD
    val clockIn: String, // ISO Timestamp
    val lunchStart: String? = null, // ISO Timestamp
    val lunchEnd: String? = null, // ISO Timestamp
    val clockOut: String? = null, // ISO Timestamp
    val totalWorkedMinutes: Int = 0, // Computed field
    val accomplishments: String = "", // Markdown accomplishments
    val competencyTags: List<String> = emptyList(), // Tagged competencies
    val isManualEntry: Boolean = false,
    val verificationStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
    val supervisorComment: String? = null,
    val supervisorRating: Int? = null, // 1-5 Stars
    val workLocationType: String = "UNVERIFIED", // OFFICE, REMOTE, UNVERIFIED
    val auditLogs: List<AuditTrailEntry> = emptyList(),
    val attachments: List<AttachmentItem> = emptyList(),
    val selfiePath: String? = null, // local camera selfie path
    val isSynced: Boolean = false, // track if synced to university portal
    val isDeleted: Boolean = false
)

data class AttachmentItem(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val localPathOrUrl: String
)

data class AuditTrailEntry(
    val timestamp: String, // ISO
    val fieldChanged: String,
    val oldValue: String,
    val newValue: String,
    val changeReason: String
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromAuditLogs(value: List<AuditTrailEntry>?): String {
        return gson.toJson(value ?: emptyList<AuditTrailEntry>())
    }

    @TypeConverter
    fun toAuditLogs(value: String?): List<AuditTrailEntry> {
        val listType = object : TypeToken<List<AuditTrailEntry>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromAttachments(value: List<AttachmentItem>?): String {
        return gson.toJson(value ?: emptyList<AttachmentItem>())
    }

    @TypeConverter
    fun toAttachments(value: String?): List<AttachmentItem> {
        val listType = object : TypeToken<List<AttachmentItem>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }
}
