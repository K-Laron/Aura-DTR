package com.auradtr.app.security

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Tail-recursive context unwrap function to securely find the active FragmentActivity
 * even if the LocalContext is nested within multiple layers of ContextThemeWrapper.
 */
tailrec fun Context.findActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
