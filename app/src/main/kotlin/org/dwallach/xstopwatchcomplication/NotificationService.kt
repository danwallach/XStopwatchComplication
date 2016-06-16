/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
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
        verbose("onCreate")

        SharedState.restoreEverything(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // we're only doing this so we can log when we get destroyed, which will help us debug things
        verbose("onDestroy")
    }

    override fun onHandleIntent(intent: Intent) = handleIntent(intent)

    companion object: AnkoLogger {
        var singletonService: NotificationService? = null
        private set

        /**
         * Start the notification service, if it's not already running. This is the service
         * that waits for alarms, when a timer runs out. By having it running, we'll also
         * be around for broadcast intents, supporting all the communication goodness in
         * Receiver
         */
        fun kickStart(context: Context) {
            if (singletonService == null) {
                verbose("launching watch calendar service")
                context.startService(context.intentFor<NotificationService>().setAction(Intent.ACTION_DEFAULT))
            }
        }

        /**
         * Given an intent, crack it open and figure out what we're supposed to do with it.
         * This might have arrived via the NotificationService or the BroadcastReceiver. Whatever.
         * It's dispatched here.
         */
        fun handleIntent(intent: Intent) {
            val action = intent.action
            val context = singletonService

            verbose { "onHandleIntent: action($action)" }

            if(action == Intent.ACTION_DEFAULT || action == Intent.ACTION_BOOT_COMPLETED) {
                verbose("kickstart launch, we're good to go")
                return
            }

            if(context == null) {
                error("no service yet for handleIntent context")
                return
            }

            // We could have multiple timers, so each one will have its own action, which
            // is just the same prefix with the complicationId bolted on at the end.
            if(action.startsWith(Constants.ACTION_TIMER_COMPLETE)) {
                val complicationId = action.substring(Constants.ACTION_TIMER_COMPLETE.length).toInt()
                SharedState[complicationId]?.alarm(context)
                return
            }

            // In this case, we're getting a click from the complication. Same encoding as above.
            if(action.startsWith(Constants.ACTION_COMPLICATION_CLICK)) {
                val complicationId = action.substring(Constants.ACTION_COMPLICATION_CLICK.length).toInt()
                SharedState[complicationId]?.alarm(context)
                return
            }


            throw InternalError("Undefined action: $action")
        }
    }
}
