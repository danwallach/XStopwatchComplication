/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ProviderUpdateRequester
import android.text.format.DateUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import java.util.*

/**
 * We'll implement this abstract class for StopwatchState and TimerState.
 */
abstract class SharedState(val complicationId: Int, prefs: SharedPreferences?): AnkoLogger {
    var isRunning = prefs?.getBoolean("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_RUNNING}", false) ?: false
        protected set
    var isReset = prefs?.getBoolean("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_RESET}", true) ?: true
        protected set
    var notificationHelper: NotificationHelper? = null

    open fun reset(context: Context) {
        verbose { "$type($complicationId) reset" }
        isRunning = false
        isReset = true

        notify(context)
        forceUpdate(context)
    }

    open fun alarm(context: Context) {
        verbose { "$type($complicationId) alarm!" }
        // the timer will do more with this; it's meaningless for the stopwatch

        notify(context)
        forceUpdate(context)
    }

    open fun configure(context: Context) {
        verbose { "$type($complicationId) configure" }
        // the timer will do more with this; it's meaningless for the stopwatch
    }

    open fun run(context: Context) {
        verbose { "$type($complicationId) run" }

        isReset = false
        isRunning = true


        notify(context)
        forceUpdate(context)
    }

    open fun pause(context: Context) {
        verbose { "$type($complicationId) pause" }

        isRunning = false

        notify(context)
        forceUpdate(context)
    }

    fun click(context: Context) {
        verbose { "$type($complicationId) click" }

        notify(context)
    }

    fun playpause(context: Context) = if (isRunning) pause(context) else run(context)

    /**
     * Pops up a notification card for this complication and kills off any other
     * notification cards for any other stopwatch/timer notifications.
     */
    fun notify(context: Context) {
        if(notificationHelper == null) {
            // a different complication has it -- kill!
            activeStates().forEach {
                if(it.notificationHelper != null) {
                    it.notificationHelper?.kill(context)
                    it.notificationHelper = null
                }
            }
            notificationHelper = NotificationHelper(this)
        }
        // at this point, we own the notificationHelper, but Kotlin doesn't believe it
        verbose { "Posting notification: ${toString()}"}
        notificationHelper?.notify(context, eventTime())
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
     * Writes persistent state. This will be extended by our subclasses.
     */
    open fun saveState(editor: SharedPreferences.Editor) {
        editor.putBoolean("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_RUNNING}", isRunning)
        editor.putBoolean("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_RESET}", isReset)
        editor.putString("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_TYPE}", type)
    }

    /**
     * Returns a string naming what the type of this complication might be.
     */
    abstract val type: String

    /**
     * This converts an absolute time, as returned by eventTime, to a relative time
     * that might be displayed. This works whether the eventTime is before or after
     * the currentTime, making it useful for both timers and stopwatches.
     */
    fun displayTime(): String = DateUtils.formatElapsedTime(Math.abs(
            if (isRunning)
                currentTime() - eventTime() / 1000
            else
                eventTime() / 1000))



    override fun toString() = "$shortName[$complicationId]: running($isRunning), reset($isReset), display(${displayTime()})"

    abstract val flatIconId: Int

    abstract val selectedIconId: Int

    abstract val shortName: String

    var tapComplicationPendingIntent: PendingIntent? = null
        protected set
    var clickPlayPausePendingIntent: PendingIntent? = null
        protected set
    var clickResetPendingIntent: PendingIntent? = null
        protected set
    var clickConfigurePendingIntent: PendingIntent? = null
        protected set

    /**
     * We're maintaining a shared registry, mapping from complicationId to the shared-state instance.
     * This is initialized when the complication is activated.
     */
    open fun register(context: Context) {
        stateRegistry[complicationId] = this

        tapComplicationPendingIntent = PendingIntent.getService(context, 0,
                Intent(Constants.ACTION_COMPLICATION_TAP + complicationId, null, context, NotificationService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        clickPlayPausePendingIntent = PendingIntent.getService(context, 0,
                Intent(Constants.ACTION_PLAYPAUSE + complicationId, null, context, NotificationService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        clickResetPendingIntent = PendingIntent.getService(context, 0,
                Intent(Constants.ACTION_RESET + complicationId, null, context, NotificationService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        // we're not initializing the clickConfigurePendingIntent here, since that's only used
        // for the timer
    }

    /**
     * We're maintaining a shared registry, mapping from complicationId to the shared-state instance.
     * This is called when the complication is deactivated.
     */
    open fun deregister(context: Context) {
        notificationHelper?.kill(context)
        stateRegistry.remove(complicationId)

        clickPlayPausePendingIntent?.cancel()
        clickPlayPausePendingIntent = null

        clickResetPendingIntent?.cancel()
        clickResetPendingIntent = null

        clickConfigurePendingIntent?.cancel()
        clickConfigurePendingIntent = null

        tapComplicationPendingIntent?.cancel()
        tapComplicationPendingIntent = null
    }

    /**
     * Forces the watchface to request an update to the complication
     */
    fun forceUpdate(context: Context) =
        ProviderUpdateRequester(context, componentName).requestUpdate(complicationId)

    abstract val componentName: ComponentName

    companion object: AnkoLogger {
        protected val stateRegistry: MutableMap<Int,SharedState> = HashMap()
        private var restoreNecessary: Boolean = true // starts off true, set false once we've restored

        fun currentTime() = System.currentTimeMillis()

        /**
         * Fetch the shared state for a given complication. Results might be null
         * if there is no such complication.
         */
        operator fun get(complicationId: Int) = stateRegistry[complicationId]

        /**
         * Returns a set of all IDs currently known to be active.
         */
        fun activeIds() = stateRegistry.keys

        /**
         * Returns a set of all SharedStates currently known to be active.
         */
        fun activeStates() = stateRegistry.values

        fun saveEverything(context: Context) {
            verbose("saveEverything")

            context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE).edit().apply {
                val activeIds = SharedState.activeIds()

                putInt(Constants.PREFERENCES+".version", Constants.PREFERENCES_VERSION)
                putString(Constants.PREFERENCES+".activeIds", activeIds.joinToString(","))

                activeIds.forEach {
                    SharedState[it]?.saveState(this)
                }

                if (!commit())
                    error("savePreferences commit failed ?!")
            }
        }

        fun restoreEverything(context: Context) {
            verbose("restoreEverything: necessary?($restoreNecessary)")

            if(!restoreNecessary) return
            restoreNecessary = false

            context.getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE).apply {
                val version = getInt(Constants.PREFERENCES+".version", 1)
                val activeIds = getString(Constants.PREFERENCES+".activeIds", "")
                        .split(",") // if we get back the empty-string, as default above, then this returns a list with one element: the empty string
                        .filter { it.length > 0 } // so here we filter out any zero-length strings
                        .map { it.toInt() }

                verbose("restoreEverything: version($version), activeIds(${activeIds.joinToString(",")}")

                activeIds.forEach {
                    val typeString = getString("${Constants.PREFERENCES}.id$it${Constants.SUFFIX_TYPE}", "none")
                    when(typeString) {
                        Constants.TYPE_STOPWATCH -> StopwatchState(it, this).register(context)
                        Constants.TYPE_TIMER -> Unit // TODO TimerState...
                        else -> error { "unknown complication type: id($it) type($typeString)" }
                    }
                }
            }
        }
    }
}
