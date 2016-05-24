/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchprovider

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Vibrator
import android.util.Log
import org.dwallach.xstopwatchcomplication.*

import org.jetbrains.anko.*

object TimerState: SharedState() {

    /**
     * if the timer's not running, this says how far we got (i.e., we're at startTime + elapsedTime, and 0 <= elapsedTime <= duration)
     */
    var elapsedTime: Long = 0
        private set

    /**
     * when the timer started running
     */
    var startTime: Long = 0
        private set

    /**
     * when the timer ends (i.e., the timer completes at startTime + duration, assuming it's running). Default: one minute (60,000 sec)
     */
    var duration: Long = 60000 // one minute
        private set

    fun setDuration(context: Context?, duration: Long) {
        this.duration = duration
        reset(context)
    }

    override fun reset(context: Context?) {
        // don't overwrite duration -- that's a user setting
        elapsedTime = 0
        startTime = 0

        super.reset(context)
        updateBuzzTimer(context)
    }

    override fun run(context: Context) {
        if (duration == 0L) return  // don't do anything unless there's a non-zero duration

        if (isReset)
            startTime = SharedState.currentTime()
        else {
            // we're resuming from a pause, so we need to shove up the start time
            val pauseTime = startTime + elapsedTime
            startTime += SharedState.currentTime() - pauseTime
        }

        super.run(context)
        updateBuzzTimer(context)
    }

    override fun pause(context: Context) {
        val pauseTime = SharedState.currentTime()
        elapsedTime = pauseTime - startTime
        if (elapsedTime > duration) elapsedTime = duration

        super.pause(context)
        updateBuzzTimer(context)
    }

    fun restoreState(context: Context, duration: Long, elapsedTime: Long, startTime: Long, running: Boolean, reset: Boolean, updateTimestamp: Long) {
        verbose("restoring state")
        this.duration = duration
        this.elapsedTime = elapsedTime
        this.startTime = startTime
        this.isRunning = running
        this.isReset = reset
        this.updateTimestamp = updateTimestamp
        this.isInitialized = true

        updateBuzzTimer(context)
        pingObservers()
    }

    override fun eventTime(): Long = when {
        // IF RUNNING, this time will be consistent with System.currentTimeMillis(), i.e., in GMT.
        // IF PAUSED, this time will be relative to zero and will be what should be displayed.

        isReset -> duration
        !isRunning -> duration - elapsedTime
        else -> duration + startTime
    }

    private fun updateBuzzTimer(context: Context?) {
        if (isRunning) {
            val timeNow = SharedState.currentTime()
            val alarmTime = duration + startTime
            if (alarmTime > timeNow) {
                verbose { "setting alarm: ${alarmTime - timeNow} ms in the future" }
                registerTimerCompleteAlarm(context, alarmTime)
            } else {
                verbose { "alarm in the past, not setting" }
            }
        } else {
            verbose("removing alarm")
            clearTimerCompleteAlarm(context)
        }
    }

    private fun registerTimerCompleteAlarm(context: Context?, wakeupTime: Long) {
        verbose { "registerTimerCompleteAlarm: wakeUp($wakeupTime)" }

        if (context == null) {
            error("no context, can't set alarm")
            return
        }

        val alarm = context.alarmManager

        // Create intent that gets fired when the timer expires.

        //        Intent alarmIntent = new Intent(context, Receiver.class);
        //        alarmIntent.setAction(Constants.actionTimerComplete);
        //        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

//        val intent = Intent(Constants.actionTimerComplete, null, context, NotificationService::class.java)
//        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Schedule an alarm.
//        alarm.setExact(AlarmManager.RTC_WAKEUP, wakeupTime, pendingIntent)
    }

    private fun clearTimerCompleteAlarm(context: Context?) {
        verbose("clearTimerCompleteAlarm")
        if (context == null) {
            error("no context, can't clear alarm")
            return
        }

        val alarm = context.alarmManager

//        val intent = Intent(Constants.actionTimerComplete, null, context, NotificationService::class.java)
//        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//        alarm.cancel(pendingIntent)
    }


    /**
     * Handler to vibrate when the timer hits zero
     */
    fun handleTimerComplete(context: Context) {
        // four short buzzes within one second total time
        val vibratorPattern = longArrayOf(100, 200, 100, 200, 100, 200, 100, 200)

        verbose("buzzing!")
        reset(context) // timer state
        PreferencesHelper.savePreferences(context)

        context.vibrator.vibrate(vibratorPattern, -1, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
    }

    override val flatIconID: Int
        get() = R.drawable.ic_sandwatch_flat

    override val selectedIconID: Int
        get() = R.drawable.ic_sandwatch_selected

    override val shortName: String
        get() = "[Timer] "
}
