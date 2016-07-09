package org.dwallach.xstopwatchcomplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_stopwatch.*
import org.jetbrains.anko.*

class StopwatchActivity : Activity(), AnkoLogger {
    private lateinit var playPauseButton: ImageButton
    private lateinit var stopwatchText: TextView
    private lateinit var handler: Handler
    private var state: StopwatchState? = null

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

    // this one gets called if we already are running, but now get another message,
    // perhaps from a different complication
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        verbose("onNewIntent")

        createInternal(intent)
    }

    private fun createInternal(intent: Intent) {
        logIntent(intent)
        stopRedrawing() // paranoia
        StopwatchState.nukeActivity() // more paranoia

        val actionTap = getString(R.string.action_tap)

        // if the user said "OK Google, start stopwatch", then this is how we can tell
        when (intent.action) {
            "com.google.android.wearable.action.STOPWATCH" -> {
                verbose("user voice action detected: starting the stopwatch")
                // find the first stopwatch, based on the smallest complicationId (therefore the oldest)
                // and tell it to run itself
                state = SharedState.activeIds()
                        .sorted()
                        .map { SharedState[it] }
                        .filter { it is StopwatchState }
                        .firstOrNull() as StopwatchState?

                state?.run(this)
                launchStopwatch()
            }
            actionTap -> {
                verbose("we were tapped, presumably from the watchface!")
                val complicationId = intent.extras.getInt(Constants.COMPLICATION_ID, -1)
                state = SharedState[complicationId] as StopwatchState?
                launchStopwatch()
            }
            else -> {
                state = null
                errorLogAndThrow("activity launched, but why? state unknown")
            }
        }
    }

    private lateinit var everySecond: Runnable
    private var continueRefresh = true

    fun startRedrawing() {
        verbose("startRedrawing")
        everySecond = Runnable {
            setStopwatchText()

            if (continueRefresh) {
                val currentTime: Long = SharedState.currentTime()
                val nextTime: Long = (currentTime / 1000L) * 1000L
                handler.postAtTime(everySecond, nextTime)
            }
        }

        everySecond.run()
    }

    fun stopRedrawing() {
        verbose("stopRedrawing")
        continueRefresh = false
    }


    private fun launchStopwatch() {
        setContentView(R.layout.activity_stopwatch)

        watch_view_stub.setOnLayoutInflatedListener {
            verbose("onLayoutInflated")

            val resetButton = it.find<ImageButton>(R.id.resetButton)
            playPauseButton = it.find<ImageButton>(R.id.playPauseButton)
            stopwatchText = it.find<TextView>(R.id.digits)

            // now that we've loaded the state, we know whether we're playing or paused
            setPlayButtonIcon()
            setStopwatchText()
            state?.setActivity(this)

            startRedrawing()

            // get the notification service running as well; it will stick around to make sure
            // the broadcast receiver is alive
            NotificationService.kickStart(this)

            resetButton.setOnClickListener {
                verbose { "resetButton: clicked!" }
                state?.reset(this)
            }
            playPauseButton.setOnClickListener {
                verbose { "playPauseButton: clicked!" }
                state?.click(this)
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

        startRedrawing()

    }

    override fun onPause() {
        super.onPause()
        verbose("onPause")

        stopRedrawing()
    }

    override fun onDestroy() {
        verbose("onDestroy")
        stopRedrawing()
        StopwatchState.nukeActivity()

        super.onDestroy()
    }

    private fun setPlayButtonIcon() =
            playPauseButton.setImageResource(
                    if(state?.isRunning ?: false)
                        android.R.drawable.ic_media_pause
                    else
                        android.R.drawable.ic_media_play)

    private fun setStopwatchText() {
        stopwatchText.text = state?.displayTime() ?: "fail!"
    }
}
