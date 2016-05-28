package org.dwallach.xstopwatchcomplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose

class Receiver: BroadcastReceiver(), AnkoLogger {
    override fun onReceive(context: Context?, intent: Intent?) {
        verbose { "onReceive: context(${context.toString()}), intent(${intent.toString()}" }

        if (context != null) {
            // this is a no-op if the service is already running
            NotificationService.kickStart(context)
        }

        if(intent == null) {
            // nothing to do
            return
        }

        NotificationService.handleIntent(intent)
    }
}
