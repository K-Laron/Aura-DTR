package com.auradtr.app.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * SecurityUtils provides standard salted hashing functions to safeguard
 * user credentials and sensitive parameters against database inspection exposure.
 */
object SecurityUtils {

    /**
     * Generates a random cryptographic salt.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a raw credentials PIN combined with a secure salt using SHA-256.
     */
    fun hashPin(pin: String, salt: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val inputBytes = (pin + salt).toByteArray(Charsets.UTF_8)
            val hashBytes = md.digest(inputBytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
