/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.content.Context
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.text.format.DateUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose

import java.util.Observable

/**
 * We'll implement this abstract class for StopwatchState and TimerState.
 */
abstract class SharedState: Observable(), AnkoLogger {
    var isRunning: Boolean = false
        protected set
    var isReset: Boolean = true
        protected set
    var updateTimestamp: Long = 0 // when the last user interaction was
        protected set
    var isVisible: Boolean = false
        set(visible) {
            verbose { "${shortName} visible: $visible" }
            field = visible
            isInitialized = true

            makeUpdateTimestamp()
            pingObservers()
        }
    var isInitialized: Boolean = false
        protected set

    private fun makeUpdateTimestamp() {
        updateTimestamp = currentTime()
    }

    open fun reset(context: Context?) {
        verbose { "${shortName} reset" }
        isRunning = false
        isReset = true
        isInitialized = true

        makeUpdateTimestamp()
        pingObservers()
    }

    open fun run(context: Context) {
        verbose { "${shortName} run" }

        isReset = false
        isRunning = true
        isInitialized = true

        makeUpdateTimestamp()
        pingObservers()
    }

    open fun pause(context: Context) {
        verbose { "${shortName} pause" }

        isRunning = false
        isInitialized = true

        makeUpdateTimestamp()
        pingObservers()
    }

    fun click(context: Context) {
        verbose { "${shortName} click" }
        if (isRunning)
            pause(context)
        else
            run(context)
    }

    fun pingObservers() {
        // this incantation will make observers elsewhere aware that there's new content
        verbose { "${shortName} pinging" }
        setChanged()
        notifyObservers()
        clearChanged()
        verbose { "${shortName} ping complete" }
    }

    /**
     * Return the time of either when the stopwatch began or when the countdown ends.
     * IF RUNNING, this time will be consistent with System.currentTimeMillis(), i.e., in GMT.
     * IF PAUSED, this time will be relative to zero and will be what should be displayed.
     * Make sure to call isRunning() to know how to interpret this result.
     * @return GMT time in milliseconds
     */
    abstract fun eventTime(): Long

    /**
     * Convert from the internal representation to a "ComplicationText" object, suitable
     * for passing to a watchface.
     */
    abstract fun styleComplicationBuilder(context: Context, small: Boolean, builder: ComplicationData.Builder): Unit

    /**
     * This converts an absolute time, as returned by eventTime, to a relative time
     * that might be displayed
     */
    fun relativeTimeString(eventTime: Long): String =
            DateUtils.formatElapsedTime(
                    if (isRunning)
                        Math.abs(currentTime() - eventTime) / 1000
                    else
                        Math.abs(eventTime) / 1000)

    override fun toString() = relativeTimeString(eventTime())

    abstract val flatIconID: Int

    abstract val selectedIconID: Int

    abstract val shortName: String

    companion object {
        fun currentTime() = System.currentTimeMillis()
    }
}
