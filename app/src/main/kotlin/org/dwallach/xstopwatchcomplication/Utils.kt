package org.dwallach.xstopwatchcomplication

import android.content.Intent
import android.support.wearable.complications.ComplicationText
import org.jetbrains.anko.*

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
    info { "intent action(${intent.action}), dataString(${intent.dataString}), flags(0x%x), type(${intent.type})".format(intent.flags) }

    intent.categories?.forEach {
        info { "--- found category: ${it}" }
    }

    intent.extras?.keySet()?.forEach {
        info { "--- found extra: $it -> ${intent.extras[it].toString()}" }
    }
}
