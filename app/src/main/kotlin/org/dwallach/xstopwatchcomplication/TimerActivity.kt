package org.dwallach.xstopwatchcomplication

import android.app.Dialog
import android.app.DialogFragment
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.AlarmClock
import android.support.wearable.activity.WearableActivity
import android.widget.ImageButton
import android.widget.TimePicker

import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.android.synthetic.main.rect_activity_stopwatch.*
import org.jetbrains.anko.*

class TimerActivity : WearableActivity(), AnkoLogger {
    private lateinit var digits: StopwatchText
    private lateinit var playPauseButton: ImageButton
    private var state: TimerState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verbose("onCreate")

        try {
            val pinfo = packageManager.getPackageInfo(packageName, 0)
            val versionNumber = pinfo.versionCode
            val versionName = pinfo.versionName

            info { "Version: $versionName ($versionNumber)" }

        } catch (e: PackageManager.NameNotFoundException) {
            error("couldn't read version", e)
        }

        createInternal(intent)
    }

    /**
     * this uses the built-in TimePickerDialog to ask the user to specify the hours and minutes
     * for the count-down timer. Of course, it works fine on the emulator and on a Moto360, but
     * totally fails on the LG G Watch and G Watch R, apparently trying to show a full-blown
     * Material Design awesome thing that was never tuned to fit on a watch. Instead, see
     * the separate TimePickerFragment class, which might be ugly, but at least it works consistently.

     * TODO: move back to this code and kill TimePickerFragment once they fix the bug in Wear
     */
    class FailedTimePickerFragment() : DialogFragment(), TimePickerDialog.OnTimeSetListener, AnkoLogger {
        private lateinit var state: TimerState

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker

            state = TimerState.getActive() ?: errorLogAndThrow("need an active tme state for dialog")

            val duration = state.duration // in milliseconds
            val minute = (duration / 60000 % 60).toInt()
            val hour = (duration / 3600000).toInt()

            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(activity, R.style.Theme_Wearable_Modal, this, hour, minute, true)
        }

        override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
            // Do something with the time chosen by the user
            verbose { "User selected time: %d:%02d".format(hour, minute) }
            state.setDuration(hour * 3600000L + minute * 60000L)
            digits.invalidate() // force redraw
        }
    }

    // call to this specified in the layout xml files
    fun showTimePickerDialog() =
            TimePickerFragment().show(fragmentManager, "timePicker")
//        FailedTimePickerFragment().show(fragmentManager, "timePicker")



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

        TimerState.nukeActive() // more paranoia

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
                verbose("we were tapped, presumably from the watchface!")
                val complicationId = intent.extras.getInt(Constants.COMPLICATION_ID, -1)
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
            lState.makeActive()
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

        TimerState.nukeActive()

        super.onDestroy()
    }

    private fun setPlayButtonIcon() =
            playPauseButton.setImageResource(
                    if (state?.isRunning ?: false)
                        android.R.drawable.ic_media_pause
                    else
                        android.R.drawable.ic_media_play)
}
