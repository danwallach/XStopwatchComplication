/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.content.Context
import org.jetbrains.anko.verbose

object StopwatchState: SharedState() {
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
        priorTime = 0
        startTime = 0
    }

    override fun reset(context: Context?) {
        priorTime = 0
        startTime = 0

        super.reset(context)
    }

    override fun run(context: Context) {
        startTime = SharedState.currentTime()

        super.run(context)
    }

    override fun pause(context: Context) {
        val pauseTime = SharedState.currentTime()
        priorTime += pauseTime - startTime

        super.pause(context)
    }

    fun restoreState(priorTime: Long, startTime: Long, running: Boolean, reset: Boolean, updateTimestamp: Long) {
        verbose("restoring state")
        this.priorTime = priorTime
        this.startTime = startTime
        this.isRunning = running
        this.isReset = reset
        this.updateTimestamp = updateTimestamp
        this.isInitialized = true

        pingObservers()
    }

    override fun eventTime(): Long =
        // IF RUNNING, this time will be consistent with System.currentTimeMillis(), i.e., in GMT.
        // IF PAUSED, this time will be relative to zero and will be what should be displayed.

        if (isRunning) {
            startTime - priorTime
        } else {
            priorTime
        }

    override val selectedIconID: Int
        get() = R.drawable.ic_stopwatch_selected

    override val flatIconID: Int
        get() = R.drawable.ic_stopwatch_flat

    override val shortName: String
        get() = "[Stopwatch] "
}
