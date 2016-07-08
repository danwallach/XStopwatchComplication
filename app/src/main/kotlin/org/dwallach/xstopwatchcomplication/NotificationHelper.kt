package org.dwallach.xstopwatchcomplication

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v7.app.NotificationCompat
import org.jetbrains.anko.*

class NotificationHelper(private val state: SharedState): AnkoLogger {
    private val notificationId = state.complicationId
    private val iconId = state.flatIconId
    private val title = state.shortName

    fun kill(context: Context) {
        verbose { "nuking notification #$notificationId" }

        try {
            context.notificationManager.cancel(notificationId)

        } catch (throwable: Throwable) {
            error("failed to cancel notifications", throwable)
        }

    }

    fun notify(context: Context, eventTime: Long) {
        verbose { "Notifying for complicationId($notificationId), eventTime($eventTime)" }

        // Google docs for this:
        // http://developer.android.com/training/notify-user/build-notification.html

        // Also helpful but not enough:
        // http://mrigaen.blogspot.com/2014/03/the-all-important-setdeleteintent-for.html

        // This seems to explain what I want to do:
        // http://stackoverflow.com/questions/24494663/how-to-add-button-directly-on-notification-on-android-wear

        val bg = BitmapFactory.decodeResource(context.resources, iconId)

        if (state.clickResetPendingIntent == null) {
            error { "No clickResetPendingIntent" }
            return
        }

        if (state.clickPlayPausePendingIntent == null) {
            error { "No clickPlayPausePendingIntent" }
            return
        }

        // we're using a weird combination of NotificationCompat (v7 by default and v4 when called
        // for in specific places below), which really shouldn't be necessary but hopefully does the job

        val notification = NotificationCompat.Builder(context).apply {
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "", state.clickResetPendingIntent)

            if(state.clickConfigurePendingIntent != null)
                addAction(android.R.drawable.ic_menu_edit, "", state.clickConfigurePendingIntent)

            if (!state.isRunning) {
                addAction(android.R.drawable.ic_media_play, "", state.clickPlayPausePendingIntent)
                setContentText(state.displayTime())
                setContentTitle(title) // deliberately backwards for these two so the peek card has the important stuff above the fold
            } else {
                addAction(android.R.drawable.ic_media_pause, "", state.clickPlayPausePendingIntent)
                setWhen(eventTime)
                setContentText(title)
                setUsesChronometer(true)
                setShowWhen(true)
            }

            // we want the media buttons to appear in our tiny notification, so here we're saying that
            // we do indeed want all of them
            if (state.clickConfigurePendingIntent != null)
                setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            else
                setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))

            setOngoing(true)
            setLocalOnly(true)
            setSmallIcon(iconId)

            extend(android.support.v4.app.NotificationCompat.WearableExtender()
//                    .setHintHideIcon(true)
                    .setContentAction(0)
                    .setBackground(bg))
        }.build()

        // launch the notification
        context.notificationManager.notify(notificationId, notification)
    }
}
