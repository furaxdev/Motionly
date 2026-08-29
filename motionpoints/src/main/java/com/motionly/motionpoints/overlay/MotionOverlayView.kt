package com.motionly.motionpoints.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.View
import java.nio.ByteBuffer

/**
 * Transparent full-screen overlay drawing a row of edge-anchored dots that drift with
 * device acceleration and flip between black/white based on the screen content sampled
 * just behind them, so they stay visible on any background.
 */
class MotionOverlayView(context: Context) : View(context) {

    class Dot(val anchorXFraction: Float, val onTopEdge: Boolean, var backgroundIsBright: Boolean = true)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotRadiusPx = 5f * resources.displayMetrics.density
    private val edgeInsetPx = 20f * resources.displayMetrics.density
    private val maxOffsetPx = 14f * resources.displayMetrics.density
    private val sampleOffsetPx = dotRadiusPx * 3f

    private val xFractions = floatArrayOf(0.08f, 0.36f, 0.64f, 0.92f)
    private val dots = xFractions.flatMap { fx ->
        listOf(Dot(fx, onTopEdge = true), Dot(fx, onTopEdge = false))
    }

    var accelX = 0f
    var accelY = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 32
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
        val targetX = (accelX * dotRadiusPx).coerceIn(-maxOffsetPx, maxOffsetPx)
        val targetY = (accelY * dotRadiusPx).coerceIn(-maxOffsetPx, maxOffsetPx)
        val newX = offsetX + (targetX - offsetX) * 0.08f
        val newY = offsetY + (targetY - offsetY) * 0.08f
        // Skip redraws once the drift has settled, so the overlay isn't repainting the
        // whole screen 60 times a second while the phone sits still.
        if (kotlin.math.abs(newX - offsetX) > 0.05f || kotlin.math.abs(newY - offsetY) > 0.05f) {
            offsetX = newX
            offsetY = newY
            invalidate()
        }
    }

    private fun dotPosition(dot: Dot, w: Float, h: Float): PointF {
        val x = dot.anchorXFraction * w + offsetX
        val y = if (dot.onTopEdge) edgeInsetPx + offsetY else h - edgeInsetPx + offsetY
        return PointF(x, y)
    }

    /** Called from the capture background thread with the latest RGBA_8888 frame buffer. */
    fun sampleAndUpdate(buffer: ByteBuffer, rowStride: Int, pixelStride: Int, capW: Int, capH: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val scaleX = capW / w
        val scaleY = capH / h

        for (dot in dots) {
            val pos = dotPosition(dot, w, h)
            // Sample a bit away from the dot's own drawn position so it doesn't read itself.
            val sampleY = if (dot.onTopEdge) pos.y + sampleOffsetPx else pos.y - sampleOffsetPx
            val sx = (pos.x * scaleX).toInt().coerceIn(0, capW - 1)
            val sy = (sampleY * scaleY).toInt().coerceIn(0, capH - 1)

            val index = sy * rowStride + sx * pixelStride
            if (index < 0 || index + 2 >= buffer.capacity()) continue
            val r = buffer.get(index).toInt() and 0xFF
            val g = buffer.get(index + 1).toInt() and 0xFF
            val b = buffer.get(index + 2).toInt() and 0xFF
            val luma = 0.299f * r + 0.587f * g + 0.114f * b

            // Hysteresis band avoids flicker when the background sits near mid-grey.
            dot.backgroundIsBright = when {
                luma > 150f -> true
                luma < 90f -> false
                else -> dot.backgroundIsBright
            }
        }
        post { invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        for (dot in dots) {
            val pos = dotPosition(dot, w, h)
            paint.color = if (dot.backgroundIsBright) Color.BLACK else Color.WHITE
            canvas.drawCircle(pos.x, pos.y, dotRadiusPx, paint)
        }
    }
}
