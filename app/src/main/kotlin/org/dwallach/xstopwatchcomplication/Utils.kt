package org.dwallach.xstopwatchcomplication

import android.content.ComponentName
import android.content.Context
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.support.wearable.complications.ProviderUpdateRequester

//
// Assorted extension and utility functions. Kotlin FTW!
//

/**
 * Converts any string to a plain ComplicationText
 */
fun String.toComplicationText() = ComplicationText.plainText(this)

/**
 * Forces the watchface to request an update to the complication
 */
fun ComplicationProviderService.forceUpdate(context: Context, name: ComponentName, complicationID: Int) {
    val updater = ProviderUpdateRequester(context, name)
    updater.requestUpdate(complicationID)
}
