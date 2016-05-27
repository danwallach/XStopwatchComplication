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
import android.util.Log

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
    }

    override fun onHandleIntent(intent: Intent) {
        val action = intent.action
        val complicationId = intent.extras.getInt("complicationId", -1)

        verbose { "onHandleIntent: action($action), complicationId($complicationId)" }

        if(action != Intent.ACTION_DEFAULT && complicationId == -1)
            throw InternalError("Intent for unknown complication: $complicationId")

        when(action) {
            Intent.ACTION_DEFAULT -> verbose("kickstart launch!")

            Constants.ACTION_COMPLICATION_CLICK -> SharedState[complicationId]?.click(this)

            Constants.ACTION_TIMER_COMPLETE -> SharedState[complicationId]?.alarm(this)

            else -> throw InternalError("Undefined action: $action")
        }
    }

    companion object: AnkoLogger {
        var singletonService: NotificationService? = null


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
    }
}
