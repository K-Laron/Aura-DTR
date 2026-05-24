package com.auradtr.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auradtr.app.data.DtrDao
import com.auradtr.app.data.DtrDatabase
import com.auradtr.app.data.Profile
import com.auradtr.app.data.TimeLog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DtrDatabaseTest {
    private lateinit var db: DtrDatabase
    private lateinit var dao: DtrDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DtrDatabase::class.java)
            .allowMainThreadQueries() // Standard practice for db testing
            .build()
        dao = db.dtrDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadProfile() = runBlocking {
        val profile = Profile(
            studentName = "Alice Doe",
            studentId = "2026-9999-CS",
            course = "BS Data Science"
        )
        dao.insertProfile(profile)
        
        val retrieved = dao.getProfileSync()
        assertNotNull(retrieved)
        assertEquals("Alice Doe", retrieved?.studentName)
        assertEquals("2026-9999-CS", retrieved?.studentId)
    }

    @Test
    @Throws(Exception::class)
    fun writeReadAndUpdateTimeLogWithSelfie() = runBlocking {
        val log = TimeLog(
            id = "test-log-uuid",
            date = "2026-05-23",
            clockIn = "2026-05-23T08:00:00Z",
            workLocationType = "OFFICE",
            selfiePath = "/cache/selfies/sample.jpg"
        )
        dao.insertLog(log)
        
        val retrieved = dao.getLogForDateSync("2026-05-23")
        assertNotNull(retrieved)
        assertEquals("test-log-uuid", retrieved?.id)
        assertEquals("/cache/selfies/sample.jpg", retrieved?.selfiePath)
        
        // Update log with clock out and new selfie path
        val updatedLog = retrieved!!.copy(
            clockOut = "2026-05-23T17:00:00Z",
            totalWorkedMinutes = 540,
            selfiePath = "/cache/selfies/updated_selfie.jpg"
        )
        dao.insertLog(updatedLog)
        
        val finalRetrieved = dao.getLogForDateSync("2026-05-23")
        assertNotNull(finalRetrieved)
        assertEquals(540, finalRetrieved?.totalWorkedMinutes)
        assertEquals("/cache/selfies/updated_selfie.jpg", finalRetrieved?.selfiePath)
        
        // Delete log
        dao.deleteLog(finalRetrieved!!)
        val afterDelete = dao.getLogForDateSync("2026-05-23")
        assertNull(afterDelete)
    }
}
