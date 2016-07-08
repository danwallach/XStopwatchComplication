package org.dwallach.xstopwatchcomplication

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v7.app.NotificationCompat
import org.jetbrains.anko.*

class NotificationHelper(context: Context, private val state: SharedState): AnkoLogger {
    private val notificationId = 0 // we're going to have exactly one notification, so always easy to kill
    private val smallIconId = state.flatIconId
    private val largeIconId = state.selectedIconId
    private val title = state.shortName

    val bg = BitmapFactory.decodeResource(context.resources, largeIconId)

    fun kill(context: Context) {
        verbose { "nuking notification" }

        try {
            context.notificationManager.cancel(notificationId)

        } catch (throwable: Throwable) {
            info("failed to cancel notifications", throwable)
        }
    }

    fun notify(context: Context) {
        verbose { "Notifying for $title, display(${state.displayTime()}" }

        kill(context) // kill off any old notification

        // Google docs for this:
        // http://developer.android.com/training/notify-user/build-notification.html

        // Also helpful but not enough:
        // http://mrigaen.blogspot.com/2014/03/the-all-important-setdeleteintent-for.html

        // This seems to explain what I want to do:
        // http://stackoverflow.com/questions/24494663/how-to-add-button-directly-on-notification-on-android-wear

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
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", state.clickResetPendingIntent)

            if(state.clickConfigurePendingIntent != null)
                addAction(android.R.drawable.ic_menu_edit, "Configure", state.clickConfigurePendingIntent)

            if (!state.isRunning) {
                addAction(android.R.drawable.ic_media_play, "Play", state.clickPlayPausePendingIntent)
                setContentText("+>" + state.displayTime())
                setContentTitle(title) // deliberately backwards for these two so the peek card has the important stuff above the fold
            } else {
                addAction(android.R.drawable.ic_media_pause, "Pause", state.clickPlayPausePendingIntent)
//                setWhen(eventTime)
                setContentText(title)
                setContentText("||" + state.displayTime())
                // we'll disable the chronometer feature while we're sorting out the rest
//                setUsesChronometer(true)
//                setShowWhen(true)
            }

            // we want the media buttons to appear in our tiny notification, so here we're saying that
            // we do indeed want all of them
//            if (state.clickConfigurePendingIntent != null)
//                setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
//            else
//                setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))

            setOngoing(true)
            setLocalOnly(true)
            setSmallIcon(smallIconId)
            setLargeIcon(bg)

            // TODO: add setHintLaunchesActivity for the configuration button
            // TODO: redo the configuration intent to launch an activity
//            extend(android.support.v4.app.NotificationCompat.WearableExtender()
//                    .setHintHideIcon(true)
//                    .setContentAction(0)
//                    .setBackground(bg))
        }.build()

        // launch the notification
        context.notificationManager.notify(notificationId, notification)
    }
}
