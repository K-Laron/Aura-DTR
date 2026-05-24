package com.auradtr.app.ui.export

import com.auradtr.app.data.Profile
import com.auradtr.app.data.TimeLog
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CsvExporter outputs trainee profile details and DTR log items into standard CSV formats.
 * Uses AutoCloseable .use bounds to guarantee system writer cleanup on all execution branches.
 */
class CsvExporter {

    fun exportDtrToCsv(profile: Profile, logs: List<TimeLog>, outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            val lineSeparator = "\n"
            
            // 1. Intern Information Header
            writer.append("AURA OJT DTR SPREADSHEET REPORT").append(lineSeparator)
            writer.append("Trainee Name,${profile.studentName}").append(lineSeparator)
            writer.append("Student ID No.,${profile.studentId}").append(lineSeparator)
            writer.append("Course / Major,${profile.course}").append(lineSeparator)
            writer.append("College / Department,${profile.department ?: "N/A"}").append(lineSeparator)
            writer.append("Host Training Company,${profile.companyName}").append(lineSeparator)
            writer.append("Industry Supervisor,${profile.supervisorName}").append(lineSeparator)
            writer.append("Supervisor Title,${profile.supervisorTitle ?: "N/A"}").append(lineSeparator)
            writer.append("Required Target Hours,${profile.targetHours}").append(lineSeparator)
            writer.append(lineSeparator)

            // 2. Table Headers
            writer.append("Date,Clock In,Lunch Start,Lunch End,Clock Out,Total Worked Minutes,Hours Worked,Verification Status,Location Type,Supervisor Rating,Supervisor Feedback,Accomplishment Report,Competency Tags").append(lineSeparator)

            // 3. Table Rows
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

            logs.forEach { log ->
                val date = log.date
                val inTime = formatTime(log.clockIn, timeFormatter)
                val lunchStart = formatTime(log.lunchStart, timeFormatter)
                val lunchEnd = formatTime(log.lunchEnd, timeFormatter)
                val outTime = formatTime(log.clockOut, timeFormatter)
                val mins = log.totalWorkedMinutes
                val hrs = String.format("%.2f", mins / 60f)
                val status = log.verificationStatus
                val locType = log.workLocationType
                val rating = log.supervisorRating?.toString() ?: "N/A"
                
                // Clean accomplishments and comments from commas to preserve CSV alignment
                val accomplishments = log.accomplishments.replace(",", ";").replace("\n", " ").replace("\r", "")
                val comment = (log.supervisorComment ?: "N/A").replace(",", ";").replace("\n", " ").replace("\r", "")
                val tags = log.competencyTags.joinToString("; ")

                writer.append("$date,$inTime,$lunchStart,$lunchEnd,$outTime,$mins,$hrs,$status,$locType,$rating,$comment,\"$accomplishments\",\"$tags\"").append(lineSeparator)
            }
        }
    }

    private fun formatTime(isoTimestamp: String?, formatter: DateTimeFormatter): String {
        if (isoTimestamp == null) return "N/A"
        return try {
            formatter.format(Instant.parse(isoTimestamp))
        } catch (e: Exception) {
            "N/A"
        }
    }
}
