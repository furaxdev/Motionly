package com.motionly.motionpoints.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Reproduces iOS's "Vehicle Motion Cues" indicators: a row of dots anchored to the
 * top and bottom edges that drift a few pixels in the direction of detected motion,
 * then ease back to their resting position.
 */
class MotionPointsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Dot(val anchorXFraction: Float, val edgeInsetDp: Float, val onTopEdge: Boolean)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3C3C43")
    }
    private val dotRadiusPx = 5f * resources.displayMetrics.density
    private val edgeInsetPx = 20f * resources.displayMetrics.density
    private val maxOffsetPx = 14f * resources.displayMetrics.density

    private val xFractions = floatArrayOf(0.08f, 0.36f, 0.64f, 0.92f)
    private val dots = xFractions.flatMap { fx ->
        listOf(Dot(fx, edgeInsetPx, onTopEdge = true), Dot(fx, edgeInsetPx, onTopEdge = false))
    }

    // Smoothed, screen-space drift offset shared by every dot (in pixels).
    private var offsetX = 0f
    private var offsetY = 0f

    // Raw accelerometer input for the current frame; set by the host activity.
    var accelX: Float = 0f
    var accelY: Float = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { step() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    private fun step() {
        // Low-pass filter towards the target drift, then clamp so the cue stays subtle.
        val targetX = (accelX * dotRadiusPx).coerceIn(-maxOffsetPx, maxOffsetPx)
        val targetY = (accelY * dotRadiusPx).coerceIn(-maxOffsetPx, maxOffsetPx)
        offsetX += (targetX - offsetX) * 0.08f
        offsetY += (targetY - offsetY) * 0.08f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#F2F2F2"))

        val w = width.toFloat()
        val h = height.toFloat()
        for (dot in dots) {
            val x = dot.anchorXFraction * w + offsetX
            val y = if (dot.onTopEdge) dot.edgeInsetDp + offsetY else h - dot.edgeInsetDp + offsetY
            canvas.drawCircle(x, y, dotRadiusPx, paint)
        }
    }
}
