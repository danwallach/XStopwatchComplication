/*
 * XStopwatch / XTimer
 * Copyright (C) 2014-2016 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
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
        info { "Version: ${context.buildName} (${context.buildNumber})" }

    } catch (e: PackageManager.NameNotFoundException) {
        error("couldn't read version", e)
    }
}

val Context.buildName: String
    get() = packageManager.getPackageInfo(packageName, 0).versionName

val Context.buildNumber: Int
  get() = packageManager.getPackageInfo(packageName, 0).versionCode
