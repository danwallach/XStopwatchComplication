package org.dwallach.xstopwatchcomplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.support.wearable.complications.ProviderUpdateRequester
import android.util.Log
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose

//
// Assorted extension and utility functions. Kotlin FTW!
//

/**
 * Converts any string to a plain ComplicationText
 */
fun String.toComplicationText() = ComplicationText.plainText(this)

/**
 * Dumps all of the interesting contents of an intent to the log.
 */
fun AnkoLogger.logIntent(intent: Intent) {
    verbose { "intent action(${intent.action}), dataString(${intent.dataString}), flags(0x%x), type(${intent.type})".format(intent.flags) }

    intent.categories?.forEach {
        verbose { "--- found category: ${it}" }
    }

    intent.extras?.keySet()?.forEach {
        verbose { "--- found extra: $it -> ${intent.extras[it].toString()}" }
    }
}
