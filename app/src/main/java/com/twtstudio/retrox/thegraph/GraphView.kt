package com.twtstudio.retrox.thegraph

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates

class GraphView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    val typeface = ResourcesCompat.getFont(context, R.font.custom_light) as Typeface
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val linePaint = Paint()
    val linePath = Path()
    val goalLinePath = Path()
    val fillLinePath = Path()
    val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val lastLinePath = Path()
    val halfLastLinePath = Path()
    val lastLinePaint = Paint()
    val pathMeasure = PathMeasure()
    val pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    val pathTop = Path()
    val pathBottom = Path()

    val list = listOf<PointF>(
            PointF(0f, 400f),
            PointF(100f, 350f),
            PointF(200f, 300f),
            PointF(300f, 200f),
            PointF(300f, 200f),
            PointF(450f, 400f),
            PointF(500f, 300f) // 最后一个不画
    )

    val listGoal = listOf<PointF>(
            PointF(0f, 600f),
            PointF(100f, 400f),
            PointF(200f, 400f),
            PointF(300f, 270f),
            PointF(450f, 270f),
            PointF(500f, 0f),
            PointF(500f, 0f),
            PointF(500f, 500f),
            PointF(500f, 500f))

    private val pointsX = mutableListOf<Float>()
    private val pointsY = mutableListOf<Float>()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        linePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 7f
            color = Color.parseColor("#8AD5E2")
            setShadowLayer(3f, 0F, 0F, Color.parseColor("#39405F"))
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        //画背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint.apply { color = Color.parseColor("#2C3251") })

        val pointList = list.orEmpty()
        val contentWidth = width - paddingStart - paddingEnd
        val contentHeight = height - paddingTop - paddingBottom
        val widthStep = contentWidth.toFloat() / (pointList.size - 1)

        val maxData = pointList.maxBy { it.y }?.y ?: 1.0f
        val minData = pointList.minBy { it.y }?.y ?: 0.0f
        val dataSpan = if (maxData != minData) maxData - minData else 1.0f
        val minDataExtended = minData - dataSpan / 4F
        val maxDataExtended = maxData + dataSpan / 4F
        val dataSpanExtended = maxDataExtended - minDataExtended

        (0 until list.size).mapTo(pointsX.apply { clear() }) {
            paddingLeft + widthStep * it
        }

        list.mapTo(pointsY.apply { clear() }) {
            paddingTop + ((1 - ((it.y - minDataExtended) / dataSpanExtended)) * contentHeight)
        }

        linePath.apply {
            reset()
            moveTo(pointsX[0], pointsY[0])
            //这里限定画多少个点..
            (0 until list.size - 2).forEach {
//                val middleX = (pointsX[it] + pointsX[it + 1]) / 2
                cubicTo(pointsX[it+1], pointsY[it], pointsX[it], pointsY[it + 1], pointsX[it + 1], pointsY[it + 1])
            }
        }

        fillLinePath.addPath(linePath)
        fillLinePath.apply {
            lineTo(pointsX[list.size - 2], contentHeight.toFloat())
            lineTo(0f, contentHeight.toFloat());
        }
        val fillGradient = LinearGradient(
                0f,
                pointsY.min() ?: 1.0f,
                0f,
                pointsY.max() ?: 1.0f,
                Color.parseColor("#668AD5E2"),
                Color.parseColor("#4D51599A"),
                Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillLinePath, fillPaint.apply {
            shader = fillGradient
        })

        canvas.drawPath(linePath, linePaint)

        var linearGradient: LinearGradient by Delegates.notNull()
        lastLinePath.apply {
            // 这里也是限定最后一个点的高亮
            val index = list.size - 3
            val middleX = (pointsX[index] + pointsX[index + 1]) / 2

            linearGradient = LinearGradient(
                    middleX,
                    (pointsY[index] + pointsY[index + 1]) / 2,
                    pointsX[index + 1],
                    pointsY[index + 1],
                    Color.parseColor("#00FFFFFF"),
                    Color.parseColor("#99ffffff"),
                    Shader.TileMode.CLAMP
            )

            moveTo(pointsX[index], pointsY[index])
            cubicTo(pointsX[index+1], pointsY[index], pointsX[index], pointsY[index + 1], pointsX[index + 1], pointsY[index + 1])
        }
        pathMeasure.setPath(lastLinePath, false)
        val length = pathMeasure.length
        pathMeasure.getSegment(length / 2, length, halfLastLinePath, true)
        lastLinePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 7f
            shader = linearGradient
            setShadowLayer(15f, 0F, 0F, Color.WHITE)
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawPath(halfLastLinePath, lastLinePaint)

        pathTop.moveTo(0f, (pointsY.min() ?: 0.0f) - 30f)
        pathTop.rLineTo(contentWidth.toFloat(), 0f)
        paint.apply {
            reset()
            style = Paint.Style.STROKE
            strokeWidth = 0f
            pathEffect = this@GraphView.pathEffect
            color = Color.parseColor("#89D5E2")
        }
        canvas.drawPath(pathTop, paint)



        pathBottom.moveTo(0f, (pointsY.max() ?: 1.0f) + 30f)
        pathBottom.rLineTo(contentWidth.toFloat(), 0f)
        canvas.drawPath(pathBottom,paint.apply {
            color = Color.parseColor("#C2C8E6")
        })

    }

    fun Path.lineToPoint(pointF: PointF) {
        lineTo(pointF.x, pointF.y)
    }

}