/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.text.format.DateUtils

import org.jetbrains.anko.*

class TimerState(complicationId: Int, prefs: SharedPreferences? = null): SharedState(complicationId, prefs), AnkoLogger {
    /**
     * if the timer's not running, this says how far we got (i.e., we're at startTime + elapsedTime, and 0 <= elapsedTime <= duration)
     */
    var elapsedTime = prefs.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_ELAPSED_TIME}", 0)
        private set

    /**
     * when the timer started running
     */
    var startTime = prefs.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_START_TIME}", 0)
        private set

    /**
     * when the timer ends (i.e., the timer completes at startTime + duration, assuming it's running). Default: one minute (60,000 sec)
     */
    var duration = prefs.getLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_DURATION}", Constants.TIMER_DEFAULT_VALUE)
        private set

    override fun saveState(editor: SharedPreferences.Editor) {
        super.saveState(editor)

        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_ELAPSED_TIME}", elapsedTime)
        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_START_TIME}", startTime)
        editor.putLong("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_DURATION}", duration)
    }


    fun setDuration(context: Context, duration: Long) {
        this.duration = duration
        reset(context)
    }

    override fun reset(context: Context) {
        // don't overwrite duration -- that's a user setting
        elapsedTime = 0
        startTime = 0

        super.reset(context)
        updateBuzzTimer(context)
    }

    override fun run(context: Context) {
        if (duration == 0L) return  // don't do anything unless there's a non-zero duration

        if (isReset)
            startTime = TimeWrapper.gmtTime
        else {
            // we're resuming from a pause, so we need to shove up the start time
            val pauseTime = startTime + elapsedTime
            startTime += TimeWrapper.gmtTime - pauseTime
        }

        super.run(context)
        updateBuzzTimer(context)
    }

    override fun pause(context: Context) {
        val pauseTime = TimeWrapper.gmtTime
        elapsedTime = pauseTime - startTime
        if (elapsedTime > duration) elapsedTime = duration

        super.pause(context)
        updateBuzzTimer(context)
    }

    override fun configure(context: Context) {
        verbose("TODO: launch configuration activity")
    }

    override fun alarm(context: Context) {
        // four short buzzes within one second total time
        val vibratorPattern = longArrayOf(100, 200, 100, 200, 100, 200, 100, 200)
        verbose("buzzing!")

        reset(context) // timer state, also resets the alarm and does a forceUpdate()
        saveEverything(context)

        context.vibrator.vibrate(vibratorPattern, -1, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
    }

    override fun displayTime(): String =
            DateUtils.formatElapsedTime(Math.abs(when {
                isReset -> duration
                !isRunning -> duration - elapsedTime
                else -> duration + startTime - TimeWrapper.gmtTime
            } / 1000.0).toLong())

    private fun updateBuzzTimer(context: Context) =
        if (isRunning) {
            val timeNow = TimeWrapper.gmtTime
            val alarmTime = duration + startTime
            if (alarmTime > timeNow) {
                verbose { "setting alarm: ${alarmTime - timeNow} ms in the future" }
                registerTimerCompleteAlarm(context, alarmTime)
            } else {
                verbose { "alarm in the past, not setting" }
            }
        } else {
            // note that we're only killing off the alarm; we're not killing off everything else

            verbose("removing alarm")
            deregisterTimerCompleteAlarm(context)
        }

    private var pendingIntentCache: PendingIntent? = null

    private fun getPendingIntent(context: Context): PendingIntent {
        val result = pendingIntentCache ?:
                // Engineering note: we're using one Intent "action" per complication, each with its own
                // action name. We could potentially make all the intents with the same action name and
                // instead use extras for the complicationId.

                // TODO should we instead use one action? Would that work?
                PendingIntent.getService(context, 0,
                        context.intentFor<NotificationService>(Constants.COMPLICATION_ID to complicationId)
                                .setAction(context.getString(R.string.action_timer_complete)),
                        PendingIntent.FLAG_UPDATE_CURRENT)

        pendingIntentCache = result
        return result
    }


    private fun registerTimerCompleteAlarm(context: Context, wakeupTime: Long) {
        verbose { "registerTimerCompleteAlarm: wakeUp($wakeupTime)" }

        context.alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeupTime, getPendingIntent(context))
    }

    private fun deregisterTimerCompleteAlarm(context: Context) {
        context.alarmManager.cancel(getPendingIntent(context))
        pendingIntentCache = null
    }

    override fun register(context: Context) {
        super.register(context)

        tapComplicationPendingIntent = PendingIntent.getService(context, 0,
                // TODO change to TimerActivity
                context.intentFor<StopwatchActivity>(Constants.COMPLICATION_ID to complicationId)
                        .setAction(context.getString(R.string.action_tap)),
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun deregister(context: Context) {
        verbose("deregister")
        super.deregister(context)
        deregisterTimerCompleteAlarm(context)
    }

    private fun timerDiffText(completionTime: Long): ComplicationText =
            ComplicationText.TimeDifferenceBuilder()
                    .setReferencePeriodStart(completionTime)
                    .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                    .build()


    override fun styleComplicationBuilder(context: Context, small: Boolean, builder: ComplicationData.Builder) {
        if(isReset) return // we'll set no styles when the stopwatch is zeroed

        val complicationText = when {
            isRunning -> timerDiffText(startTime + duration)

        // complicated way of finding out how to represent "0"
            isReset -> timerDiffText(startTime).getText(context, startTime).toString().toComplicationText()

        // complicated way of finding how how to represent the time when the user hit "pause"
            else -> timerDiffText(startTime + elapsedTime + duration)
                    .getText(context, startTime + elapsedTime)
                    .toString().toComplicationText()
        }

        if(small)
            builder.setShortText(complicationText)
        else
            builder.setLongText(complicationText)
    }


    override val flatIconId: Int
        get() = R.drawable.ic_sandwatch_flat

    override val selectedIconId: Int
        get() = R.drawable.ic_sandwatch_selected

    override val type: String
        get() = Constants.TYPE_TIMER

    override val shortName: String
        get() = "[Timer] "

    override val componentName: ComponentName
        get() = ComponentName.createRelative(Constants.PREFIX, ".TimerProviderService")

    override fun toString(): String = "${super.toString()} elapsedTime($elapsedTime), startTime($startTime), duration($duration)"
}

/**
 * Kotlin extension functions FTW. This just calls TimerState.styleComplicationBuilder.
 */
fun ComplicationData.Builder.styleTimerText(context: Context, small: Boolean, timerState: TimerState): ComplicationData.Builder {
    timerState.styleComplicationBuilder(context, small, this)
    return this
}
