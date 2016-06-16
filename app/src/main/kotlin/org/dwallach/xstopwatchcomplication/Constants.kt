/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

object Constants {
    const val PREFIX = "org.dwallach.xstopwatchcomplication"
    const val PREFERENCES = "$PREFIX.prefs"
    const val ACTION_TIMER_COMPLETE = "$PREFIX.timer.ACTION_TIMER_COMPLETE"
    const val ACTION_COMPLICATION_CLICK = "$PREFIX.CLICK"
    const val COMPLICATION_ID = "complicationId"
    const val PREFERENCES_VERSION = 1

    const val TYPE_STOPWATCH = "stopwatch"
    const val TYPE_TIMER = "timer"

    const val SUFFIX_TYPE = ".type"
    const val SUFFIX_RUNNING = ".running"
    const val SUFFIX_RESET = ".reset"

    // stopwatch-specific
    const val SUFFIX_START_TIME = ".startTime"
    const val SUFFIX_PRIOR_TIME = ".priorTime"

    // timer-specific
    const val SUFFIX_ELAPSED_TIME = ".elapsedTime"
    const val SUFFIX_DURATION = ".duration"

    const val TIMER_DEFAULT_VALUE = 600000L // 10 minutes in milliseconds
}
