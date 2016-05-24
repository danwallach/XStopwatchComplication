/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.content.Context
import org.dwallach.xstopwatchprovider.TimerState
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose

object PreferencesHelper: AnkoLogger {
    fun savePreferences(context: Context) {
        verbose("savePreferences")
        context.getSharedPreferences(Constants.sharedPrefsStopwatch, Context.MODE_PRIVATE).edit().apply {
            putLong(Constants.prefStopwatchStartTime, StopwatchState.startTime)
            putLong(Constants.prefStopwatchBaseTime, StopwatchState.priorTime)
            putBoolean(Constants.prefStopwatchRunning, StopwatchState.isRunning)
            putBoolean(Constants.prefStopwatchReset, StopwatchState.isReset)
            putLong(Constants.prefStopwatchUpdateTimestamp, StopwatchState.updateTimestamp)

            if (!commit())
                verbose("savePreferences commit failed ?!")
        }

        context.getSharedPreferences(Constants.sharedPrefsTimer, Context.MODE_PRIVATE).edit().apply {
            putLong(Constants.prefTimerStartTime, TimerState.startTime)
            putLong(Constants.prefTimerPauseElapsed, TimerState.elapsedTime)
            putLong(Constants.prefTimerDuration, TimerState.duration)
            putBoolean(Constants.prefTimerRunning, TimerState.isRunning)
            putBoolean(Constants.prefTimerReset, TimerState.isReset)
            putLong(Constants.prefTimerUpdateTimestamp, TimerState.updateTimestamp)

            if (!commit())
                verbose("savePreferences commit failed ?!")
        }
    }

    fun loadPreferences(context: Context) {
        verbose("loadPreferences")

        // brackets just so that the variables go away when we leave scope
        context.getSharedPreferences(Constants.sharedPrefsStopwatch, Context.MODE_PRIVATE).apply {
            val priorTime = getLong(Constants.prefStopwatchBaseTime, 0L)
            val startTime = getLong(Constants.prefStopwatchStartTime, 0L)
            val isRunning = getBoolean(Constants.prefStopwatchRunning, false)
            val isReset = getBoolean(Constants.prefStopwatchReset, true)
            val updateTimestamp = getLong(Constants.prefStopwatchUpdateTimestamp, 0L)

            verbose {
                "Stopwatch:: startTime($startTime), priorTime($priorTime), isRunning($isRunning), isReset($isReset), updateTimestamp($updateTimestamp)"
            }

            StopwatchState.restoreState(priorTime, startTime, isRunning, isReset, updateTimestamp)
        }

        context.getSharedPreferences(Constants.sharedPrefsTimer, Context.MODE_PRIVATE).apply {
            val startTime = getLong(Constants.prefTimerStartTime, 0L)
            val pauseDelta = getLong(Constants.prefTimerPauseElapsed, 0L)
            val duration = getLong(Constants.prefTimerDuration, Constants.timerDefaultDuration)
            var isRunning = getBoolean(Constants.prefTimerRunning, false)
            var isReset = getBoolean(Constants.prefTimerReset, true)
            val updateTimestamp = getLong(Constants.prefTimerUpdateTimestamp, 0L)

            // sanity checking: if we're coming back from whatever, and we discover that we *used* to
            // be running, but we've gotten way past the deadline, then just reset things.
            val currentTime = System.currentTimeMillis()
            if (isRunning && startTime + duration < currentTime) {
                isReset = true
                isRunning = false
            }

            verbose {
                "Timer:: startTime($startTime), pauseDelta($pauseDelta), duration($duration), isRunning($isRunning), isReset($isReset), updateTimestamp($updateTimestamp)"
            }

            TimerState.restoreState(context, duration, pauseDelta, startTime, isRunning, isReset, updateTimestamp)
        }
    }
}
