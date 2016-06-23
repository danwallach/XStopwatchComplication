package org.dwallach.xstopwatchcomplication

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
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

    fun notify(eventTime: Long) {
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

        val notification = Notification.Builder(context).apply {
            addAction(context, android.R.drawable.ic_menu_close_clear_cancel, "", state.clickResetPendingIntent)

            addAction(context, android.R.drawable.ic_menu_edit, "", state.clickConfigurePendingIntent)

            if (!state.isRunning) {
                addAction(context, android.R.drawable.ic_media_play, "", state.clickPlayPausePendingIntent)
                setContentTitle(state.toString())
                setContentText(title) // deliberately backwards for these two so the peek card has the important stuff above the fold
            } else {
                addAction(context, android.R.drawable.ic_media_pause, "", state.clickPlayPausePendingIntent)
                setWhen(eventTime)
                setUsesChronometer(true)
                setShowWhen(true)
            }

            // we want the media buttons to appear in our tiny notification, so here we're saying that
            // we do indeed want all of them
            if (state.clickConfigurePendingIntent != null)
                setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            else
                setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1))

            setOngoing(true)
            setLocalOnly(true)
            setSmallIcon(iconId)

            extend(Notification.WearableExtender()
                    .setHintHideIcon(true)
                    .setContentAction(0)
                    .setBackground(bg))
        }.build()

        // launch the notification
        context.notificationManager.notify(notificationId, notification)
    }
}

/**
 * The addAction builder that we want to use has been deprecated, "because reasons", so this brings
 * it back for us. Let's hear it for Kotlin extension methods! Note that if the intent is null, this
 * will quietly be a no-op rather than indicating any sort of error.
 */
fun Notification.Builder.addAction(context: Context, iconId: Int, title: String, intent: PendingIntent?): Notification.Builder =
        // Helpful: http://stackoverflow.com/questions/35647821/android-notification-addaction-deprecated-in-api-23
        // I tried doing this with NotificationCompat, and it never quite worked. Something or other
        // always came out missing.
        if(intent == null)
            this
        else
            this.addAction(Notification.Action.Builder(Icon.createWithResource(context, iconId), title, intent).build())
