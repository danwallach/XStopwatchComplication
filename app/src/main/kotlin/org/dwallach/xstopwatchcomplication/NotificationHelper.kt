package org.dwallach.xstopwatchcomplication

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import org.jetbrains.anko.*

class NotificationHelper(private val context: Context, private val title: String, private val state: SharedState): AnkoLogger {

    private val notificationId = state.complicationId
    private val iconId = state.flatIconId

    fun kill() {
        verbose { "nuking notification #$notificationId" }

        try {
            context.notificationManager.cancel(notificationId)

        } catch (throwable: Throwable) {
            error("failed to cancel notifications", throwable)
        }

    }

    fun notify(eventTime: Long, isRunning: Boolean) {
        // Google docs for this:
        // http://developer.android.com/training/notify-user/build-notification.html

        // Also helpful but not enough:
        // http://mrigaen.blogspot.com/2014/03/the-all-important-setdeleteintent-for.html

        // This seems to explain what I want to do:
        // http://stackoverflow.com/questions/24494663/how-to-add-button-directly-on-notification-on-android-wear

        val bg = BitmapFactory.decodeResource(context.resources, iconId)

        // TODO add media button goodies
        val notification = Notification.Builder(context).apply {
            if(!isRunning)
                addAction(context, android.R.drawable.ic_media_play, "", state.clickPlayPausePendingIntent)
                        .setContentTitle(state.toString())
                        .setContentText(title) // deliberately backwards for these two so the peek card has the important stuff above the fold
            else
                addAction(context, android.R.drawable.ic_media_pause, "", state.clickPlayPausePendingIntent)
                        .setWhen(eventTime)
                        .setUsesChronometer(true)
                        .setShowWhen(true)
        }
                .setOngoing(true)
                .setLocalOnly(true)
                .setSmallIcon(iconId)
//                .addAction(context, iconId, title, launchPendingIntent)
                .extend(Notification.WearableExtender()
                        .setHintHideIcon(true)
                        .setContentAction(0)
                        .setBackground(bg))
                .build()

        // launch the notification
        context.notificationManager.notify(notificationId, notification)
    }
}

/**
 * The addAction builder that we want to use has been deprecated, "because reasons", so this brings
 * it back for us. Let's hear it for Kotlin extension methods!
 */
fun Notification.Builder.addAction(context: Context, iconId: Int, title: String, intent: PendingIntent?): Notification.Builder =
        if(intent == null)
            this
        else
            // TODO replace with NotificationCompat to avoid this whole deprecation issue
            this.addAction(iconId, title, intent)

// The above call to addAction is deprecated. Below is my attempt to solve this, but it turns out to crash
// in a funny way, saying it can't find Icon.createWithResources at runtime. Weird. We'll just leave this
// here for now, and sort this out later if/when the deprecated method finally dies and we're forced to deal
// with it. For now, "if it ain't broke, don't fix it."

//        this.addAction(
//                Notification.Action.Builder(
//                        Icon.createWithResource(context, iconId),
//                        title, intent) .build() )
