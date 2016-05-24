/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

object Constants {
    const val prefStopwatchRunning = "running"
    const val prefStopwatchReset = "reset"
    const val prefStopwatchStartTime = "start"
    const val prefStopwatchBaseTime = "base"
    const val prefStopwatchUpdateTimestamp = "updateTimestamp"

    const val sharedPrefsStopwatch = "org.dwallach.x.stopwatch.prefs"

    const val prefTimerRunning = "running"
    const val prefTimerReset = "reset"
    const val prefTimerStartTime = "start"
    const val prefTimerPauseElapsed = "elapsed"
    const val prefTimerDuration = "duration"
    const val prefTimerUpdateTimestamp = "updateTimestamp"

    const val sharedPrefsTimer = "org.dwallach.x.timer.prefs"

    const val actionTimerComplete = "org.dwallach.x.timer.ACTION_TIMER_COMPLETE"

    const val timerDefaultDuration = 600000L // 10 minutes in milliseconds
}
