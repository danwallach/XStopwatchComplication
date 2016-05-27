/*
 * Stopwatch provider
 * Copyright (C) 2016, Dan Wallach <dwallach@gmail.com>
 */

package org.dwallach.xstopwatchcomplication

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.support.wearable.complications.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.error
import org.jetbrains.anko.warn

class StopwatchProviderService: ComplicationProviderService(), AnkoLogger {
    private lateinit var componentName: ComponentName

    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    override fun onComplicationActivated(complicationId: Int, dataType: Int, complicationManager: ComplicationManager) {
        debug { "onComplicationActivated(): " + complicationId }
        super.onComplicationActivated(complicationId, dataType, complicationManager);

        NotificationService.kickStart(this) // start the service if it's not already active
        componentName = ComponentName(this, javaClass) // we might need this if we want to force an update
        StopwatchState(complicationId).register(this) // create state for the complication and save it away
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
    override fun onComplicationUpdate(complicationId: Int, dataType: Int, complicationManager: ComplicationManager) {
        debug("onComplicationUpdate()");

        val state = SharedState[complicationId]
        if(state == null) {
            error { "No stopwatch complication found for id#" + complicationId }
            return
        }
        if(state !is StopwatchState) {
            error { "Wrong complication type for id#" + complicationId }
            return
        }


        val data = when (dataType) {
            ComplicationData.TYPE_ICON ->
                ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_stopwatch_flat))
                        .setTapAction(SharedState.getIntent(complicationId))
                        .build()

            ComplicationData.TYPE_SHORT_TEXT ->
                ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_stopwatch_flat))
                        .setTapAction(SharedState.getIntent(complicationId))
                        .styleText(this, true, state)
                        .build()

            ComplicationData.TYPE_LONG_TEXT ->
                ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_stopwatch_flat))
                        .setTapAction(SharedState.getIntent(complicationId))
                        .styleText(this, false, state)
                        .build()

            else -> {
                warn { "Unexpected complication type: " + dataType }
                null
            }
        }

        if (data != null) {
            complicationManager.updateComplicationData(complicationId, data);
        }
    }

    /*
     * Called when the complication has been deactivated. If you are updating the complication
     * manager outside of this class with updates, you will want to update your class to stop.
     */
    override fun onComplicationDeactivated(complicationId: Int) {
        debug { "onComplicationDeactivated(): " + complicationId }
        super.onComplicationDeactivated(complicationId);

        SharedState[complicationId]?.deregister()
    }
}


