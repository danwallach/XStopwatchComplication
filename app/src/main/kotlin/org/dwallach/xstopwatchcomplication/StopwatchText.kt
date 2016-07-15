/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatchcomplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import org.jetbrains.anko.warn

import java.lang.ref.WeakReference

/**
 * This class acts something like android.widget.Chronometer, but that class only knows
 * how to count up, and we need to be able to go up (for a stopwatch) and down (for a timer).

 * When running, the text is updated once a second, with text derived from the SharedState
 * (which might be either StopwatchState or TimerState).
 */
class StopwatchText : View, AnkoLogger {
    private var visible = true
    private var state: SharedState? = null
    private var shortName: String? = null
    private val textPaint: Paint
    private val textPaintAmbient: Paint

    /** Handler to update the time once a second in interactive mode. */
    private val updateTimeHandler: MyHandler

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        setWillNotDraw(false)

        textPaint = Paint(Paint.SUBPIXEL_TEXT_FLAG or Paint.HINTING_ON).apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = context.getColor(R.color.primary)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        textPaintAmbient = Paint(Paint.SUBPIXEL_TEXT_FLAG or Paint.HINTING_ON).apply {
            isAntiAlias = false
            style = Paint.Style.FILL // or maybe some kind of outline text?
            color = Color.WHITE // or maybe RED?
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        updateTimeHandler = MyHandler(this)
    }


    fun setSharedState(sharedState: SharedState) {
        this.state = sharedState
        this.shortName = sharedState.shortName
    }

    override fun onVisibilityChanged(changedView: View?, visibility: Int) {
        visible = visibility == View.VISIBLE

        verbose { "$shortName visible: $visible" }

        state?.isVisible = visible

        restartRedrawLoop()
    }


    fun restartRedrawLoop() {
        val lState = state ?: return

        if (lState.isVisible)
            invalidate() // gets the most current state drawn no matter what

        if (lState.isVisible && lState.isRunning) {
            updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
        } else {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
        }
    }

    private var textX: Float = 0f
    private var textY: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        verbose { "$shortName size change: $w,$h" }
        this._width = w
        this._height = h
        val textSize = h * .9f

        verbose { "$shortName new text size: $textSize" }

        textPaint.textSize = textSize
        textPaintAmbient.textSize = textSize
        //
        // note: metrics.ascent is a *negative* number while metrics.descent is a *positive* number
        //
        val metrics = textPaint.fontMetrics
        textY = -metrics.ascent
        textX = w / 2f

        //
        // In some weird cases, we get an onSizeChanged but not an onVisibilityChanged
        // event, even though visibility did change; Lovely.
        //
        onVisibilityChanged(null, View.VISIBLE)
    }

    private var _width: Int = 0
    private var _height: Int = 0


    public override fun onDraw(canvas: Canvas) {
        val lState = state ?: return

        verbose { shortName + "onDraw -- visible: " + visible + ", running: " + lState.isRunning }

        val timeText = lState.displayTime()

//        verbose { "update text to: " + timeText }

        if (_width == 0 || _height == 0) {
            warn { "$shortName zero-width or zero-height, can't draw yet" }
            return
        }

        // clear the screen
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.drawText(timeText, textX, textY, textPaint)

        // TODO add ambient mode
    }

    companion object: AnkoLogger {
        const val MSG_UPDATE_TIME = 0

        private class MyHandler internal constructor(stopwatchText: StopwatchText) : Handler(), AnkoLogger {
            private val stopwatchTextRef = WeakReference(stopwatchText)
            override val loggerTag = "StopwatchText" // more useful than "Companion"

            override fun handleMessage(message: Message) {
                val stopwatchText = stopwatchTextRef.get() ?: return
                // oops, it died

                when (message.what) {
                    MSG_UPDATE_TIME -> {
                        TimeWrapper.update()
                        val localTime = TimeWrapper.gmtTime
                        stopwatchText.invalidate()
                        if (stopwatchText.visible && (stopwatchText.state?.isRunning ?: false)) {
                            val timeMs = localTime
                            val delayMs = 1000 - timeMs % 1000
                            sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
                        } else {
                            verbose { "${stopwatchText.shortName}: time handler complete" }
                        }
                    }
                    else -> {
                        errorLogAndThrow("unknown message: $message")
                    }
                }
            }

            // TODO add 60Hz redraw loop, use for some cool rendering as in CalWatch
        }
    }
}
