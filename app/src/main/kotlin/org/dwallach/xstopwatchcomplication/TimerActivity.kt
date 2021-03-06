/*
 * XStopwatch / XTimer
 * Copyright (C) 2014-2016 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.app.Dialog
import android.app.DialogFragment
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.support.wearable.activity.WearableActivity
import android.widget.ImageButton
import android.widget.TimePicker

import kotlinx.android.synthetic.main.activity_timer.*
import org.jetbrains.anko.*

class TimerActivity : WearableActivity(), AnkoLogger {
    private lateinit var digits: StopwatchText
    private lateinit var playPauseButton: ImageButton
    private var state: TimerState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verbose("onCreate")
        logBuildVersion(this)

        createInternal(intent)
    }

    /**
     * this uses the built-in TimePickerDialog to ask the user to specify the hours and minutes
     * for the count-down timer. In Wear 1.0 and 1.5 / Android 4.4W and 5.0, TimePickerDialog was
     * a Holo-style picker on some devices and a malformed Material-style picker on others (digits
     * visible, but round selector wheel below the screen). Somewhere after this, it was the
     * malformed Material-style picker for everybody. That's why we instead have our own
     * [TimePickerFragment], which is another Holo-ish picker that I had to write.
     *
     * <p>In Wear 2.0 / Android N? Now it's just a huge white screen with an okay/cancel button
     * at the bottom.
     *
     * TODO: move back to this code and kill TimePickerFragment once they fix the bug in Wear
     */
    class BuiltinTimePickerFragment(val state: TimerState? = null, val stopwatchText: StopwatchText? = null) : DialogFragment(), TimePickerDialog.OnTimeSetListener, AnkoLogger {
        override val loggerTag = "TimerActivity"

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if(state == null) errorLogAndThrow("need an active time state for dialog")

            // Use the current time as the default values for the picker
            val duration = state.duration // in milliseconds
            val minute = (duration / 60000 % 60).toInt()
            val hour = (duration / 3600000).toInt()

            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(activity,
//                    R.style.Theme_Wearable_Modal,                  // this doesn't work -- gives everything white
//                    AlertDialog.THEME_DEVICE_DEFAULT_DARK,         // this one is deprecated
                    this, hour, minute, true)
        }

        override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
            if(state == null) errorLogAndThrow("need an active time state for dialog")

            // Do something with the time chosen by the user
            verbose { "User selected time: %d:%02d".format(hour, minute) }
            state.setDuration(hour * 3600000L + minute * 60000L, activity)
            stopwatchText?.invalidate() // forces a redraw
        }
    }

    // call to this specified in the layout xml files
    fun showTimePickerDialog() = BuiltinTimePickerFragment(state, digits).show(fragmentManager, "timePicker")

    // this one gets called if we already are running, but now get another message,
    // perhaps from a different complication
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        verbose("onNewIntent")

        createInternal(intent)
    }

    private fun createInternal(intent: Intent) {
        verbose("createInternal")
        logIntent(intent)

        val actionTap = getString(R.string.action_tap)

        // if the user said "OK Google, set a 7 minute timer", then this is how we can tell
        when (intent.action) {
            "android.intent.action.SET_TIMER" -> {
                verbose("user voice action detected: starting the timer")
                val paramLength: Long = intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0) * 1000L // convert to milliseconds

                // find the first stopwatch, based on the smallest complicationId (therefore the oldest)
                // and tell it to run itself
                val lState = SharedState.activeIds()
                        .sorted()
                        .map { SharedState[it] }
                        .filter { it is TimerState }
                        .firstOrNull() as TimerState?

                if(lState == null) {
                    verbose("no active timers, so we'll ignore the launch request")
                    // TODO notify the user that they need to set a complication first?
                    return
                }

                state = lState

                if(paramLength > 0 && paramLength < TimeWrapper.hours(24))
                    lState.setDuration(paramLength)
                lState.run(this)
                launchTimer()
            }
            actionTap -> {
                val complicationId = intent.extras.getInt(Constants.COMPLICATION_ID, -1)
                info { "we were tapped, presumably from the watchface!, complicationId($complicationId)" }
                state = SharedState[complicationId] as TimerState?
                launchTimer()
            }
            else -> {
                state = null
                errorLogAndThrow("activity launched, but why? state unknown")
            }
        }
    }

    private fun launchTimer() {
        verbose("launchTimer")
        setContentView(R.layout.activity_timer)

        watch_view_stub.setOnLayoutInflatedListener {
            verbose("onLayoutInflated")

            val resetButton = it.find<ImageButton>(R.id.resetButton)
            val setButton = it.find<ImageButton>(R.id.setButton)
            playPauseButton = it.find<ImageButton>(R.id.playPauseButton)
            digits = it.find<StopwatchText>(R.id.digits)

            val lState = state ?: errorLogAndThrow("need non-null state")

            // now that we've loaded the state, we know whether we're playing or paused
            setPlayButtonIcon()
            digits.setSharedState(lState)

            digits.restartRedrawLoop()

            // get the notification service running as well; it will stick around to make sure
            // the broadcast receiver is alive
            NotificationService.kickStart(this)

            resetButton.setOnClickListener {
                verbose("resetButton: clicked!")
                lState.reset(this)
                setPlayButtonIcon()
                digits.restartRedrawLoop()
            }

            setButton.setOnClickListener {
                showTimePickerDialog()
            }

            playPauseButton.setOnClickListener {
                verbose("playPauseButton: clicked!")
                lState.playpause(this)
                setPlayButtonIcon()
                digits.restartRedrawLoop()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        verbose("onStart")
    }

    override fun onResume() {
        super.onResume()
        verbose("onResume")
    }

    override fun onPause() {
        super.onPause()
        verbose("onPause")
    }

    override fun onDestroy() {
        verbose("onDestroy")

        super.onDestroy()
    }

    private fun setPlayButtonIcon() =
            playPauseButton.setImageResource(
                    if (state?.isRunning ?: false)
                        android.R.drawable.ic_media_pause
                    else
                        android.R.drawable.ic_media_play)
}
