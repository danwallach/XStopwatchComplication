/*
 * XStopwatch / XTimer
 * Copyright (C) 2014-2016 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.support.wearable.complications.*
import org.jetbrains.anko.*

class StopwatchProviderService: ComplicationProviderService(), AnkoLogger {
    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    override fun onComplicationActivated(complicationId: Int, complicationType: Int, complicationManager: ComplicationManager) {
        info { "onComplicationActivated: complicationId($complicationId), complicationType($complicationType)" }
        super.onComplicationActivated(complicationId, complicationType, complicationManager)

        StopwatchState(complicationId).register(this) // create state for the complication and save it away
        SharedState.saveEverything(this)
    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    override fun onComplicationUpdate(complicationId: Int, complicationType: Int, complicationManager: ComplicationManager) {
        info { "onComplicationUpdate: complicationId($complicationId), complicationType($complicationType)" }

        val state = SharedState[complicationId] ?: errorLogAndThrow("No stopwatch complication found for id# $complicationId")

        if(state !is StopwatchState) {
            errorLogAndThrow("complicationId($complicationId) wasn't a stopwatch!")
        }

        val tapPendingIntent: PendingIntent = state.tapComplicationPendingIntent ?:
            errorLogAndThrow("complicationId($complicationId) missing a tapPendingIntent!")

        val data = when (complicationType) {
            ComplicationData.TYPE_ICON ->
                ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_stopwatch_flat))
                        .setTapAction(tapPendingIntent)
                        .build()

            ComplicationData.TYPE_SHORT_TEXT ->
                ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_stopwatch_flat))
                        .setTapAction(tapPendingIntent)
                        .styleStopwatchText(this, true, state)
                        .build()

            // For now, we're doing basically the same thing with "long" as "short" text,
            // since a stopwatch / timer's output fits in 7 characters or less. At some
            // point we might try to be fancier.
            ComplicationData.TYPE_LONG_TEXT ->
                ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_stopwatch_flat))
                        .setTapAction(tapPendingIntent)
                        .styleStopwatchText(this, false, state)
                        .build()

            else -> {
                warn { "Unexpected complication type: $complicationType" }
                null
            }
        }

        if (data != null) {
            complicationManager.updateComplicationData(complicationId, data)
        }
    }

    /*
     * Called when the complication has been deactivated. If you are updating the complication
     * manager outside of this class with updates, you will want to update your class to stop.
     */
    override fun onComplicationDeactivated(complicationId: Int) {
        info { "onComplicationDeactivated: $complicationId" }
        super.onComplicationDeactivated(complicationId)

        SharedState[complicationId]?.deregister(this)
        SharedState.saveEverything(this)
    }

    override fun onCreate() {
        super.onCreate()
        info("onCreate")

        NotificationService.kickStart(this) // start the service if it's not already active
        SharedState.restoreEverything(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // we're only doing this so we can log when we get destroyed, which will help us debug things
        info("onDestroy")
    }
}
