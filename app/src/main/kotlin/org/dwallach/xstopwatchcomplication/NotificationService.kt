/*
 * XStopwatch / XTimer
 * Copyright (C) 2014-2016 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.app.IntentService
import android.content.Context
import android.content.Intent

import org.jetbrains.anko.*

/**
 * This handles clicks coming back to us from the watchface as well as alarms when the timer
 * completes.
 */
class NotificationService : IntentService("NotificationService"), AnkoLogger {
    init {
        singletonService = this
    }

    override fun onCreate() {
        super.onCreate()
        info("onCreate")

        if(singletonService == null) singletonService = this // in case we somehow got here without kickstarting

        actionTimerComplete = getString(R.string.action_timer_complete)
        actionTap = getString(R.string.action_tap)

        SharedState.restoreEverything(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // we're only doing this so we can log when we get destroyed, which will help us debug things
        info("onDestroy")
    }

    override fun onHandleIntent(intent: Intent) {
        if(singletonService == null) singletonService = this // in case we somehow got here without kickstarting

        handleIntent(intent)
    }

    companion object: AnkoLogger {
        var singletonService: NotificationService? = null
            private set

        override val loggerTag = "NotificationService" // more useful than "Companion"

        /**
         * Start the notification service, if it's not already running. This is the service
         * that waits for alarms, when a timer runs out. By having it running, we'll also
         * be around for broadcast intents, but it's the alarms that matter here.
         */
        fun kickStart(context: Context) {
            if (singletonService == null) {
                info("launching notification service")
                context.startService(context.intentFor<NotificationService>().setAction(Intent.ACTION_DEFAULT))
            }
        }

        lateinit var actionTimerComplete: String
        lateinit var actionTap: String


        /**
         * Given an intent, crack it open and figure out what we're supposed to do with it.
         * This might have arrived via the NotificationService or the BroadcastReceiver. Whatever.
         * It's dispatched here.
         */
        fun handleIntent(intent: Intent) {
            val action = intent.action
            val context = singletonService
            val data = intent.dataString
            val complicationId = intent.getIntExtra(Constants.COMPLICATION_ID, -1)

            info { "onHandleIntent: action($action), complicationId($complicationId), data($data)" }

            if(action == Intent.ACTION_DEFAULT || action == Intent.ACTION_BOOT_COMPLETED) {
                info("kickstart launch, we're good to go")
                return
            }

            if(context == null) {
                error("no service yet for handleIntent context")
                return
            }


            val complicationState = SharedState[complicationId]

            if (complicationState == null) {
                // this happens if we've updated and the build versions don't match
                error { "no state to handle intent action($action) for complicationId($complicationId)" }
                return
            }

            // paranoia
            if(complicationState.complicationId != complicationId)
                errorLogAndThrow("DB corruption: found complication with id(${complicationState.complicationId}, not $complicationId")

            when(action) {
                actionTimerComplete -> complicationState.alarm(context)
                actionTap -> complicationState.click(context)
                else -> errorLogAndThrow("unknown action($action)")
            }
        }
    }
}
