/*
 * XStopwatch / XTimer
 * Copyright (C) 2014-2016 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import org.jetbrains.anko.*

class StopwatchState(complicationId: Int, prefs: SharedPreferences? = null): SharedState(complicationId, prefs), AnkoLogger {

    /**
     * extra time to add in (accounting for prior pause/restart cycles) -- analogous to the "base" time in android.widget.Chronometer
     */
    var priorTime = prefs.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_PRIOR_TIME}", 0)
        private set

    /**
     * subtract this from the start time to know how long it's been running (when the stopwatch started running (GMT))
     */
    var startTime = prefs.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_START_TIME}", 0)
        private set

    override fun saveState(editor: SharedPreferences.Editor) {
        super.saveState(editor)

        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_PRIOR_TIME}", priorTime)
        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_START_TIME}", startTime)
    }

    override fun logState() {
        super.logState()
        verbose { "id$complicationId.${Constants.SUFFIX_PRIOR_TIME}: ${priorTime}" }
        verbose { "id$complicationId.${Constants.SUFFIX_START_TIME}: ${startTime}" }
    }


    override fun reset(context: Context) {
        priorTime = 0
        startTime = 0

        super.reset(context)
    }

    override fun run(context: Context) {
        TimeWrapper.update()
        startTime = TimeWrapper.gmtTime

        super.run(context)
    }

    override fun click(context: Context) {
        with(context) {
            startActivity(intentFor<StopwatchActivity>(Constants.COMPLICATION_ID to complicationId)
                    .setAction(getString(R.string.action_tap)))
        }

        super.click(context)
    }

    override fun pause(context: Context) {
        TimeWrapper.update()
        val rightNow = TimeWrapper.gmtTime
        priorTime += rightNow - startTime

        super.pause(context)
    }

    override fun displayTime() = displayTime(
            if(isRunning)
                (TimeWrapper.gmtTime - startTime + priorTime)
            else priorTime)

    private fun stopwatchDiffText(start: Long): ComplicationText {
        verbose { "Computing stopwatchDiffText($start) -> ${displayTime(start)}" }
        return ComplicationText.TimeDifferenceBuilder()
                .setReferencePeriodEnd(start)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                .build()
    }

    override fun styleComplicationBuilder(context: Context, small: Boolean, builder: ComplicationData.Builder) {
        val complicationText: ComplicationText = when {
            isRunning -> {
                verbose("Stopwatch running")
                stopwatchDiffText(startTime - priorTime)
            }

        // complicated way of finding out how to represent "0"
            isReset -> {
                verbose("Stopwatch is reset")
                // TODO: figure out why the code below bombs
//                stopwatchDiffText(startTime).getText(context, startTime).toString().toComplicationText()
                displayTime(0).toComplicationText()
            }

        // complicated way of finding how how to represent the time when the user hit "pause"
            else -> {
                verbose("Stopwatch paused")
//                val resultStr = stopwatchDiffText(startTime - priorTime).getText(context, startTime).toString()
//                resultStr.toComplicationText()
                displayTime().toComplicationText()
            }
        }

        verbose { "Setting stopwatch text to {${complicationText.getText(context, TimeWrapper.gmtTime)}}" }

        if(small)
            builder.setShortText(complicationText)
        else
            builder.setLongText(complicationText)
    }

    override val selectedIconId: Int
        get() = R.drawable.ic_stopwatch_selected

    override val flatIconId: Int
        get() = R.drawable.ic_stopwatch_flat

    override val type: String
        get() = Constants.TYPE_STOPWATCH

    override val shortName: String
        get() = "[Stopwatch] "

    override val componentName: ComponentName
        get() = ComponentName.createRelative(Constants.PREFIX, ".StopwatchProviderService")

    override fun toString(): String = "${super.toString()}, priorTime($priorTime), startTime($startTime)"
}

/**
 * Kotlin extension functions FTW. This just calls StopwatchState.styleComplicationBuilder.
 */
fun ComplicationData.Builder.styleStopwatchText(context: Context, small: Boolean, stopwatchState: StopwatchState): ComplicationData.Builder {
    stopwatchState.styleComplicationBuilder(context, small, this)
    return this
}
