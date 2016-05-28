/*
 * Stopwatch provider
 * Copyright (C) 2016, Dan Wallach <dwallach@gmail.com>
 */

package org.dwallach.xstopwatchcomplication

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.support.wearable.complications.*
import org.jetbrains.anko.*

class StopwatchProviderService: ComplicationProviderService(), AnkoLogger {
    private lateinit var componentName: ComponentName

    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    override fun onComplicationActivated(complicationId: Int, complicationType: Int, complicationManager: ComplicationManager) {
        debug { "onComplicationActivated(): complicationId($complicationId), complicationType($complicationType)" }
        super.onComplicationActivated(complicationId, complicationType, complicationManager);

        restoreState()
        StopwatchState(complicationId).register(this) // create state for the complication and save it away
        saveState()
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
        debug("onComplicationUpdate: complicationId($complicationId), complicationTYpe($complicationType)");

        var state = SharedState[complicationId]
        if(state == null) {
            error { "No stopwatch complication found for id# $complicationId" }
            return
        }
        if(state !is StopwatchState) {
            // dealing with case #1 in the comment above
            info { "complicationId($complicationId) wasn't stopwatch! Updating." }
            SharedState[complicationId]?.deregister()
            state = StopwatchState(complicationId)
            state.register(this)
            saveState()
        }

        val data = when (complicationType) {
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
                warn { "Unexpected complication type: $complicationType" }
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
        debug { "onComplicationDeactivated: $complicationId" }
        super.onComplicationDeactivated(complicationId);

        SharedState[complicationId]?.deregister()
        saveState()
    }

    override fun onCreate() {
        verbose("onCreate")
        restoreState()
    }

    override fun onDestroy() {
        // TODO save state?!
        verbose("onDestroy")
    }

    private fun restoreState() {
        NotificationService.kickStart(this) // start the service if it's not already active
        componentName = ComponentName(this, javaClass) // we might need this if we want to force an update

        // TODO pull content in from the on-disk preferences
    }

    private fun saveState() {
        // TODO dump state to on-disk preferences
    }
}


