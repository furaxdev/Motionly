package com.motionly.motionpoints.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Draws a field of points that drift according to the device accelerometer readings.
 */
class MotionPointsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Point(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val radius: Float,
        val color: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val points = mutableListOf<Point>()
    private val palette = intArrayOf(
        Color.parseColor("#00E5FF"),
        Color.parseColor("#FF4081"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#76FF03"),
        Color.parseColor("#FF9100")
    )

    // Current accelerometer-driven acceleration, in view-space units per frame.
    var accelX: Float = 0f
    var accelY: Float = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { step() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (points.isEmpty() && w > 0 && h > 0) {
            val count = 60
            repeat(count) {
                points += Point(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    vx = 0f,
                    vy = 0f,
                    radius = 8f + Random.nextFloat() * 14f,
                    color = palette[it % palette.size]
                )
            }
        }
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
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        for (p in points) {
            // Accelerometer pushes the points; friction keeps things stable.
            p.vx = (p.vx + accelX) * 0.96f
            p.vy = (p.vy + accelY) * 0.96f

            p.x += p.vx
            p.y += p.vy

            if (p.x < 0f) { p.x = 0f; p.vx = -p.vx * 0.5f }
            if (p.x > w) { p.x = w; p.vx = -p.vx * 0.5f }
            if (p.y < 0f) { p.y = 0f; p.vy = -p.vy * 0.5f }
            if (p.y > h) { p.y = h; p.vy = -p.vy * 0.5f }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        for (p in points) {
            paint.color = p.color
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }
    }
}
