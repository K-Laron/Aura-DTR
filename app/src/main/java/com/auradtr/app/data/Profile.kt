package com.auradtr.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Profile represents the student intern and OJT deployment settings.
 * Employs salted supervisor PIN hashes to safeguard supervisor administrative access.
 */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: String = "default_user",
    val studentName: String = "",
    val studentId: String = "",
    val course: String = "",
    val department: String = "",
    val companyName: String = "",
    val supervisorName: String = "",
    val supervisorTitle: String = "",
    val targetHours: Int = 486,
    val startDate: String = "",
    val endDate: String = "",
    val geofenceEnabled: Boolean = false,
    val workLatitude: Double? = null,
    val workLongitude: Double? = null,
    val workGeofenceRadius: Int = 100,
    val verifiedBSSID: String? = null,
    val supervisorPinHash: String = "",
    val supervisorPinSalt: String = "",
    val signatureDataUrl: String? = null
)
