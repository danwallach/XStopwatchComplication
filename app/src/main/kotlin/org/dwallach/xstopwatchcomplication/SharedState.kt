/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.wearable.complications.ComplicationData
import android.text.format.DateUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import java.util.*

/**
 * We'll implement this abstract class for StopwatchState and TimerState.
 */
abstract class SharedState(val complicationId: Int): Observable(), AnkoLogger {
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

            makeUpdateTimestamp()
            pingObservers()
        }

    private fun makeUpdateTimestamp() {
        updateTimestamp = currentTime()
    }

    open fun reset(context: Context?) {
        verbose { "${shortName} reset" }
        isRunning = false
        isReset = true

        makeUpdateTimestamp()
        pingObservers()
    }

    open fun alarm(context: Context) {
        verbose { "${shortName} alarm!" }
        // the timer will do more with this; it's meaningless for the stopwatch
    }

    open fun run(context: Context) {
        verbose { "${shortName} run" }

        isReset = false
        isRunning = true

        makeUpdateTimestamp()
        pingObservers()
    }

    open fun pause(context: Context) {
        verbose { "${shortName} pause" }

        isRunning = false

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

    fun currentTime() = System.currentTimeMillis()

    /**
     * We're maintaining a shared registry, mapping from complicationId to the shared-state instance.
     * This is called when the complication is activated.
     */
    fun register(context: Context) {
        stateRegistry.put(complicationId, this)

        // we only ever have to do this once, regardless of registration and deregistration

        if(intentRegistry[complicationId] == null) {
            val intent = Intent(Constants.ACTION_COMPLICATION_CLICK, null, context, NotificationService::class.java)
            intent.extras.putInt("complicationId", complicationId)
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            intentRegistry.put(complicationId, pendingIntent)
        }
    }

    /**
     * We're maintaining a shared registry, mapping from complicationId to the shared-state instance.
     * This is called when the complication is deactivated.
     */
    fun deregister() {
        stateRegistry.remove(complicationId)
    }

    companion object {
        private val stateRegistry: MutableMap<Int,SharedState> = HashMap()
        private val intentRegistry: MutableMap<Int,PendingIntent> = HashMap()

        /**
         * Fetch the shared state for a given complication. Results might be null
         * if there is no such complication.
         */
        operator fun get(complicationId: Int) = stateRegistry.get(complicationId)

        /**
         * Fetch the pending intent for a given complication. Results might be null
         * if there is no such complication.
         */
        fun getIntent(complicationId: Int) = intentRegistry.get(complicationId)
    }
}
