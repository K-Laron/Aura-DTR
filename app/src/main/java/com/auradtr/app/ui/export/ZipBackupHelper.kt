package com.auradtr.app.ui.export

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ZipBackupHelper implements safe compression streams to archive SQLite time log records
 * and biometric selfie captures into a portable offline archive package.
 */
class ZipBackupHelper {

    fun createBackupZip(context: Context, destZipFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath("dtr_database")
            val dbShm = context.getDatabasePath("dtr_database-shm")
            val dbWal = context.getDatabasePath("dtr_database-wal")
            val selfiesDir = File(context.filesDir, "selfies")

            // Flush WAL to ensure consistent database snapshot before archiving
            val db = com.auradtr.app.data.DtrDatabase.getDatabase(context)
            db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")

            val filesToBackup = mutableListOf<File>()
            if (dbFile.exists()) filesToBackup.add(dbFile)
            if (dbShm.exists()) filesToBackup.add(dbShm)
            if (dbWal.exists()) filesToBackup.add(dbWal)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZipFile))).use { zos ->
                // Archive database binaries
                filesToBackup.forEach { file ->
                    zos.putNextEntry(ZipEntry("database/${file.name}"))
                    file.inputStream().use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }

                // Archive audit captured selfie images
                if (selfiesDir.exists() && selfiesDir.isDirectory) {
                    selfiesDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.lowercase().endsWith(".jpg")) {
                            zos.putNextEntry(ZipEntry("selfies/${file.name}"))
                            file.inputStream().use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
