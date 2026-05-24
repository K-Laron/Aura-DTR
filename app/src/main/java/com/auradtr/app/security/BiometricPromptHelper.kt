package com.auradtr.app.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * BiometricPromptHelper encapsulates Android BiometricPrompt operations securely.
 * Uses WeakReference to avoid leaking the FragmentActivity instance or local Compose
 * context scopes during device rotation or configuration transitions.
 */
class BiometricPromptHelper(
    activity: FragmentActivity,
    private val onSuccess: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val activityRef = WeakReference(activity)

    fun showBiometricPrompt(
        title: String = "Supervisor Verification",
        subtitle: String = "Authenticate using fingerprint or face unlock"
    ) {
        val activity = activityRef.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val currentActivity = activityRef.get()
                    if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                        return
                    }

                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    } else {
                        onError("PIN_FALLBACK")
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val currentActivity = activityRef.get()
                    if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                        return
                    }

                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    val currentActivity = activityRef.get()
                    if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                        return
                    }

                    onError("Authentication failed. Please try again.")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN Fallback")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
