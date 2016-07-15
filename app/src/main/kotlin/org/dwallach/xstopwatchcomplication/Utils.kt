package org.dwallach.xstopwatchcomplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

fun AnkoLogger.logBuildVersion(context: Context) {
    try {
        val pinfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionNumber = pinfo.versionCode
        val versionName = pinfo.versionName

        info { "Version: $versionName ($versionNumber)" }

    } catch (e: PackageManager.NameNotFoundException) {
        error("couldn't read version", e)
    }
}
