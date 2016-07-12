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
import android.content.SharedPreferences
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ProviderUpdateRequester
import android.text.format.DateUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.intentFor
import java.util.*

/**
 * Useful helpers. Let us avoid repeating ourselves later on when we might have a
 * null SharedPreferences reference.
 */
fun SharedPreferences?.getBoolean(s: String, default: Boolean): Boolean =
        if(this == null) default else getBoolean(s, default)

fun SharedPreferences?.getLong(s: String, default: Long): Long =
        if(this == null) default else getLong(s, default)

/**
 * Turns out that "".split(whatever) yields a list with one element, when we'd
 * rather it be an empty list. Also, what if the list is null? This extension
 * function does the right thing.
 */
fun String?.safeSplit(separator: String): List<String> =
    if(this == null || this == "") emptyList() else split(separator)

/**
 * We'll implement this abstract class for StopwatchState and TimerState.
 */
abstract class SharedState(val complicationId: Int, prefs: SharedPreferences?): AnkoLogger {
    var isRunning: Boolean = prefs.getBoolean("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_RUNNING}", false)
        protected set
    var isReset: Boolean = prefs.getBoolean("${Constants.PREFERENCES}.id$complicationId${Constants.SUFFIX_RESET}", true)
        protected set
    var isVisible: Boolean = false // this is tweaked by StopwatchText


    open fun reset(context: Context) {
        verbose { "$type($complicationId) reset" }
        isRunning = false
        isReset = true

        forceUpdate(context)
    }

    open fun alarm(context: Context) {
        verbose { "$type($complicationId) alarm!" }
        // the timer will do more with this; it's meaningless for the stopwatch

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

        forceUpdate(context)
    }

    open fun pause(context: Context) {
        verbose { "$type($complicationId) pause" }

        isRunning = false

        forceUpdate(context)
    }

    /**
     * click from the watchface: launch the activity!
     */
    open fun click(context: Context) {
        verbose { "$type($complicationId) click" }
    }

    fun playpause(context: Context) = if (isRunning) pause(context) else run(context)

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

    abstract fun displayTime(): String

    override fun toString() = "$shortName[$complicationId]: running($isRunning), reset($isReset), display(${displayTime()})"

    abstract val flatIconId: Int

    abstract val selectedIconId: Int

    abstract val shortName: String

    var tapComplicationPendingIntent: PendingIntent? = null
        protected set

    /**
     * We're maintaining a shared registry, mapping from complicationId to the shared-state instance.
     * This is initialized when the complication is activated.
     */
    open fun register(context: Context) {
        verbose { "Registering $complicationId" }

        stateRegistry[complicationId] = this

        tapComplicationPendingIntent = PendingIntent.getService(context, 0,
                context.intentFor<NotificationService>(Constants.COMPLICATION_ID to complicationId)
                        .setAction(context.getString(R.string.action_tap)),
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * We're maintaining a shared registry, mapping from complicationId to the shared-state instance.
     * This is called when the complication is deactivated.
     */
    open fun deregister(context: Context) {
        verbose { "Deregistering $complicationId" }

        stateRegistry.remove(complicationId)

        tapComplicationPendingIntent?.cancel()
        tapComplicationPendingIntent = null
    }

    /**
     * Forces the watchface to request an update to the complication
     */
    fun forceUpdate(context: Context) {
        verbose { "forceUpdate: componentName($componentName), complicationId($complicationId)" }
        ProviderUpdateRequester(context, componentName).requestUpdate(complicationId)
    }

    abstract val componentName: ComponentName

    companion object: AnkoLogger {
        protected val stateRegistry: MutableMap<Int,SharedState> = HashMap()
        private var restoreNecessary: Boolean = true // starts off true, set false once we've restored


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
                        .safeSplit(",")
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

        fun displayTime(timeMS: Long): String = DateUtils.formatElapsedTime(Math.abs(timeMS / 1000))
    }
}
