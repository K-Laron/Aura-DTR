package com.auradtr.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DtrDao {
    @Query("SELECT * FROM profile WHERE id = 'default_user' LIMIT 1")
    fun getProfile(): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE id = 'default_user' LIMIT 1")
    suspend fun getProfileSync(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Query("SELECT * FROM time_logs WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllLogs(): Flow<List<TimeLog>>

    @Query("SELECT * FROM time_logs WHERE isDeleted = 0 ORDER BY date DESC")
    suspend fun getAllLogsSync(): List<TimeLog>

    @Query("SELECT * FROM time_logs WHERE date = :date AND isDeleted = 0 LIMIT 1")
    fun getLogForDate(date: String): Flow<TimeLog?>

    @Query("SELECT * FROM time_logs WHERE date = :date AND isDeleted = 0 LIMIT 1")
    suspend fun getLogForDateSync(date: String): TimeLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TimeLog)

    @Delete
    suspend fun deleteLog(log: TimeLog)

    @Query("SELECT * FROM time_logs WHERE isSynced = 0")
    fun getUnsyncedLogs(): Flow<List<TimeLog>>

    @Query("SELECT * FROM time_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogsSync(): List<TimeLog>

    @Query("UPDATE time_logs SET isSynced = 1 WHERE id = :id")
    suspend fun markLogSynced(id: String)

    @Query("SELECT * FROM time_logs WHERE id = :id LIMIT 1")
    suspend fun getLogByIdSync(id: String): TimeLog?

    @Query("SELECT accomplishments FROM time_logs WHERE isDeleted = 0 AND accomplishments != '' ORDER BY date DESC, clockIn DESC LIMIT 1")
    suspend fun getLatestAccomplishmentSync(): String?
}
