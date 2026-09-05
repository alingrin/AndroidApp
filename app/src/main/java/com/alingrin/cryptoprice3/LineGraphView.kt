package com.alingrin.cryptoprice3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class LineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var dataPoints: List<DataPoint> = emptyList()
    private var minPrice = 0.0
    private var maxPrice = 0.0
    private var coinName = ""
    private var selectedDataPoint: DataPoint? = null
    private val textPaint = Paint().apply { color = Color.LTGRAY; textSize = 40f }
    private val titlePaint = Paint().apply {
        color = Color.LTGRAY; textSize = 80f; isAntiAlias = true; isFakeBoldText = true
    }
    private val linePaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 7f }
    private val dotPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 11f }
    private val tooltipPaint = Paint().apply { color = Color.TRANSPARENT; style = Paint.Style.FILL; alpha = 70 }
    private val axisPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 3f }

    fun setDataPoints(points: List<DataPoint>) {
        dataPoints = points
        if (points.isNotEmpty()) {
            minPrice = points.minOf { it.value }
            maxPrice = points.maxOf { it.value }
        }
        linePaint.color = when {
            points.size < 2 -> Color.BLUE
            points.last().value > points[points.lastIndex - 1].value -> Color.GREEN
            points.last().value < points[points.lastIndex - 1].value -> Color.RED
            else -> Color.BLUE
        }
        invalidate()
    }

    fun setCoinName(name: String) {
        coinName = name
        selectedDataPoint = null
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                selectedDataPoint = findClosestDataPoint(event.x)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> return true
            MotionEvent.ACTION_CANCEL -> {
                selectedDataPoint = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.size < 2 || maxPrice == minPrice) return
        val width = width.toFloat()
        val height = height.toFloat()
        val xInterval = width / (dataPoints.size - 1)
        val yInterval = height / (maxPrice - minPrice).toFloat()
        fun y(point: DataPoint) = height - ((point.value - minPrice) * yInterval).toFloat()
        val path = Path().apply { moveTo(0f, y(dataPoints.first())) }
        dataPoints.drop(1).forEachIndexed { index, point ->
            val i = index + 1
            val x = i * xInterval
            val previous = dataPoints[i - 1]
            path.cubicTo((i - 1) * xInterval + xInterval / 2, y(previous), x - xInterval / 2, y(point), x, y(point))
            val labelStep = max(1, dataPoints.size / 5)
            if (i % labelStep == 0) canvas.drawText(point.label, x - textPaint.measureText(point.label) / 2, height + 40, textPaint)
        }
        canvas.drawPath(path, linePaint)
        selectedDataPoint?.let { point ->
            val x = xCoordinate(point, xInterval)
            canvas.drawCircle(x, y(point), 10f, dotPaint.apply { color = Color.BLUE })
            drawTooltip(canvas, point, xInterval, y(point))
        }
        val titleWidth = titlePaint.measureText(coinName)
        canvas.drawText(coinName, (width - titleWidth) / 2, 80f, titlePaint)
    }

    private fun drawTooltip(canvas: Canvas, point: DataPoint, xInterval: Float, pointY: Float) {
        val tooltipWidth = 285f
        val tooltipHeight = 90f
        val x = min(max(0f, xCoordinate(point, xInterval)), width - tooltipWidth)
        val y = min(max(tooltipHeight, pointY), height.toFloat())
        canvas.drawRoundRect(RectF(x, y - tooltipHeight, x + tooltipWidth, y), 16f, 16f, tooltipPaint)
        canvas.drawText(point.label, x + 10, y - 50, textPaint)
        canvas.drawText(String.format("Price: %.2f", point.value), x + 10, y - 10, textPaint)
    }

    private fun xCoordinate(point: DataPoint, interval: Float): Float = dataPoints.indexOf(point).coerceAtLeast(0) * interval

    private fun findClosestDataPoint(touchX: Float): DataPoint? {
        if (dataPoints.size < 2 || width == 0) return null
        val interval = width.toFloat() / (dataPoints.size - 1)
        return dataPoints[((touchX / interval) + 0.5f).toInt().coerceIn(dataPoints.indices)]
    }

    data class DataPoint(val label: String, val value: Double)
}