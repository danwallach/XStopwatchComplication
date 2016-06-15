/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.content.Context
import android.content.SharedPreferences
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import org.jetbrains.anko.*

class StopwatchState(complicationId: Int, prefs: SharedPreferences? = null): SharedState(complicationId, prefs), AnkoLogger {
    /**
     * extra time to add in (accounting for prior pause/restart cycles) -- analogous to the "base" time in android.widget.Chronometer
     */
    var priorTime: Long = 0
        private set

    /**
     * When the stopwatch started running
     */
    var startTime: Long = 0
        private set

    init {
        priorTime = prefs?.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_PRIOR_TIME}", 0) ?: 0
        startTime = prefs?.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_START_TIME}", 0) ?: 0
    }

    override fun saveState(editor: SharedPreferences.Editor) {
        super.saveState(editor)

        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_PRIOR_TIME}", priorTime)
        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_START_TIME}", startTime)
    }

    override fun reset(context: Context) {
        priorTime = 0
        startTime = 0

        super.reset(context)
    }

    override fun run(context: Context) {
        startTime = currentTime()

        super.run(context)
    }

    override fun pause(context: Context) {
        val pauseTime = currentTime()
        priorTime += pauseTime - startTime

        super.pause(context)
    }

    override fun eventTime(): Long =
        // IF RUNNING, this time will be consistent with System.currentTimeMillis(), i.e., in GMT.
        // IF PAUSED, this time will be relative to zero and will be what should be displayed.

        if (isRunning) {
            startTime - priorTime
        } else {
            priorTime
        }

    private fun stopwatchDiffText(start: Long): ComplicationText =
            ComplicationText.TimeDifferenceBuilder()
                    .setReferencePeriodEnd(start)
                    .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                    .build()


    override fun styleComplicationBuilder(context: Context, small: Boolean, builder: ComplicationData.Builder) {
        if(isReset) return // we'll set no styles when the stopwatch is zeroed

        val complicationText = when {
            isRunning -> stopwatchDiffText(startTime - priorTime)

        // complicated way of finding out how to represent "0"
//            isReset -> stopwatchDiffText(startTime).getText(context, startTime).toString().toComplicationText()

        // complicated way of finding how how to represent the time when the user hit "pause"
            else -> stopwatchDiffText(startTime - priorTime).getText(context, startTime).toString().toComplicationText()
        }

        if(small)
            builder.setShortText(complicationText)
        else
            builder.setLongText(complicationText)
    }

    override val selectedIconID: Int
        get() = R.drawable.ic_stopwatch_selected

    override val flatIconID: Int
        get() = R.drawable.ic_stopwatch_flat

    override val type: String
        get() = Constants.TYPE_STOPWATCH

    override val shortName: String
        get() = "[Stopwatch] "
}

/**
 * Kotlin extension functions FTW. This just calls StopwatchState.styleComplicationBuilder.
 */
fun ComplicationData.Builder.styleStopwatchText(context: Context, small: Boolean, stopwatchState: StopwatchState): ComplicationData.Builder {
    stopwatchState.styleComplicationBuilder(context, small, this)
    return this
}
