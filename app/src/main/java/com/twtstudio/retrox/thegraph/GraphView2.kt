package com.twtstudio.retrox.thegraph

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.properties.Delegates

class GraphView2 @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    val typeface = ResourcesCompat.getFont(context, R.font.custom_regular) as Typeface
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = this@GraphView2.typeface
        textSize = 30f
        textAlign = Paint.Align.RIGHT
    }
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
    val pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    val pathTop = Path()
    val pathBottom = Path()

    // 两个坐标线之间沟通用
    // 坐标系都是左上角的
    var globalMaxSpan = 1.0f
        set(value) {
            if (value > field) {
                field = value
            }
        }
    var globalMinYValue = 0.0f
        set(value) {
            if (value < field) {
                field = value
            }
        }
    var globalMaxYValue = 0.0f
        set(value) {
            if (value > field) {
                field = value
            }
        }

    //前景线条坐标点
    val pointList = listOf<PointF>(
            PointF(0f, 400f),
            PointF(58f, 510f),
            PointF(83f, 510f),
            PointF(147f, 578f),
            PointF(167f, 578f),
            PointF(265f, 412f)
    )

    // Sketch坐标线居然和View坐标系是一样的
    // 背景的坐标点
    val listGoal = listOf<PointF>(
            PointF(0f, 341f), PointF(62f, 431f), PointF(122f, 431f), PointF(170f, 522f),
            PointF(216f, 522f), PointF(269f, 601f), PointF(282f, 601f), PointF(344f, 400f), PointF(375f, 400f)
    )
    private val points2X = mutableListOf<Float>()
    private val points2Y = mutableListOf<Float>()

    private val points1X = mutableListOf<Float>()
    private val points1Y = mutableListOf<Float>()

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
        layer {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint.apply {
                color = Color.parseColor("#282E4B")
            })
        }
        layer("背景线条") {
            val maxXValue = listGoal.maxBy { it.x }?.x ?: 1.0f
            val pointScaleX = width / maxXValue
            listGoal.mapTo(points2X.apply { clear() }) {
                it.x * pointScaleX
            }
            val minYValue = listGoal.minBy { it.y }?.y ?: 0.0f // 最小的在上面
            val maxYValue = listGoal.maxBy { it.y }?.y ?: 1.0f
            val paddingBottomYValue = 64f //背景Y的底距离
            val realMaxYValue = maxYValue + paddingBottomYValue
            val spanY = realMaxYValue - minYValue
            // 这里对于GlobalValue的处理并不严谨... 应该在所有绘制中先得出Global的相关值再处理 但是我太懒 而且知道背景的覆盖面积更大
            globalMaxSpan = spanY
            globalMinYValue = minYValue
            globalMaxYValue = realMaxYValue

            listGoal.mapTo(points2Y.apply { clear() }) {
                it.y - minYValue //变成相对坐标
            }.forEachIndexed { index, fl ->
                points2Y[index] = (fl / spanY) * height
            }

            goalLinePath.apply {
                moveTo(points2X[0], points2Y[0])
                (0 until 4).forEach {
                    val middleX = (points2X[it] + points2X[it + 1]) / 2
                    cubicTo(points2X[it + 1], points2Y[it], middleX, points2Y[it + 1], points2X[it + 1], points2Y[it + 1])
                }
                //该画第5个点了 GG
                draw(4) { index ->
                    val c1x = points2X[index] + (points2X[index + 1] - points2X[index]) * 0.8f
                    val c2x = points2X[index] + (points2X[index + 1] - points2X[index]) * 0.2f
                    cubicTo(c1x, points2Y[index], c2x, points2Y[index + 1], points2X[index + 1], points2Y[index + 1])
                }
                lineTo(points2X[6], points2Y[6])
                cubicTo(points2X[6] + (points2X[7] - points2X[6]) * 0.8f, points2Y[6], points2X[6], points2Y[7], points2X[7], points2Y[7])
                lineTo(points2X[8], points2Y[8])
                lineTo(width.toFloat(), height.toFloat())
                lineTo(0f, height.toFloat())
                close()
            }

            fillPaint.apply {
                color = Color.parseColor("#1A9BABFF")
                style = Paint.Style.FILL_AND_STROKE
            }

            canvas.drawPath(goalLinePath, fillPaint)

        }

        layer("前景线条") {
            val maxXValue = pointList.maxBy { it.x }?.x ?: 1.0f
            val pointScaleX = width * 0.68f / maxXValue //前景线条不能画完QAQ
            pointList.mapTo(points1X.apply { clear() }) {
                it.x * pointScaleX
            }

            pointList.mapTo(points1Y.apply { clear() }) {
                (1 - (globalMaxYValue - it.y) / globalMaxSpan) * height // 复杂的坐标转换
            }

            linePath.apply {
                moveTo(points1X[0], points1Y[0])
                draw(0) {
                    val c1x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.8f
                    val c2x = (points1X[it] + points1X[it + 1]) / 2
                    cubicTo(c1x, points1Y[it], c2x, points1Y[it + 1], points1X[it + 1], points1Y[it + 1])
                }
                lineTo(points1X[2], points1Y[2])
                draw(2) {
                    val c1x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.6f
                    val c2x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.4f
                    cubicTo(c1x, points1Y[it], c2x, points1Y[it + 1], points1X[it + 1], points1Y[it + 1])
                }
                lineTo(points1X[4], points1Y[4])
                draw(4) {
                    val c1x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.6f
                    val c2x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.1f
                    cubicTo(c1x, points1Y[it], c2x, points1Y[it + 1], points1X[it + 1], points1Y[it + 1])
                }
            }

            layer("前景渐变") {
                fillLinePath.apply {
                    addPath(linePath)
                    val pointNum = points1X.size
                    lineTo(points1X[pointNum - 1], height.toFloat())
                    lineTo(0f, height.toFloat())
                }
                val fillGradient = LinearGradient(
                        0f,
                        points1Y.min() ?: 1.0f,
                        0f,
                        height.toFloat(),
                        Color.parseColor("#408AD5E2"),
                        Color.parseColor("#4D51599A"),
                        Shader.TileMode.CLAMP
                )
                canvas.drawPath(fillLinePath, fillPaint.apply {
                    reset()
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    shader = fillGradient
                })
            }

            canvas.drawPath(linePath, linePaint)

            layer("线条白色阴影") {
                lastLinePath.apply {
                    draw(4) {
                        moveTo(points1X[it], points1Y[it])
                        val c1x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.6f
                        val c2x = points1X[it] + (points1X[it + 1] - points1X[it]) * 0.1f
                        cubicTo(c1x, points1Y[it], c2x, points1Y[it + 1], points1X[it + 1], points1Y[it + 1])
                    }
                }
                pathMeasure.setPath(lastLinePath, false)
                val length = pathMeasure.length
                pathMeasure.getSegment(length / 2, length, halfLastLinePath, true)

                val index = 4
                val linearGradient = LinearGradient(
                        (points1X[index] + points1X[index + 1]) / 2,
                        (points1Y[index] + points1Y[index + 1]) / 2,
                        points1X[index + 1],
                        points1Y[index + 1],
                        Color.parseColor("#00FFFFFF"),
                        Color.parseColor("#99ffffff"),
                        Shader.TileMode.CLAMP
                )

                lastLinePaint.apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 7f
                    shader = linearGradient
                    setShadowLayer(15f, 0F, 0F, Color.WHITE)
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(halfLastLinePath, lastLinePaint)

            }

        }

        layer("虚线以及文字") {
            val leftPadding = 16f

            pathTop.moveTo(0f, (points1Y.min() ?: 0.0f) - 30f)
            pathTop.rLineTo(width.toFloat(), 0f)
            paint.apply {
                reset()
                style = Paint.Style.STROKE
                strokeWidth = 0f
                pathEffect = this@GraphView2.pathEffect
                color = Color.parseColor("#89D5E2")
            }
            canvas.drawPath(pathTop, paint)

            canvas.drawText("GOAL: \$4 000", width - leftPadding, (points1Y.min() ?: 0.0f) - 60f, textPaint.apply {
                color = Color.parseColor("#89D5E2")
            })

            pathBottom.moveTo(0f, points2Y.max() ?: 1.0f)
            pathBottom.rLineTo(width.toFloat(), 0f)
            canvas.drawPath(pathBottom, paint.apply {
                color = Color.parseColor("#C2C8E6")
            })
            canvas.drawText("L: \$1 240", width - leftPadding, (points2Y.max() ?: 1.0f) - 30f, textPaint.apply {
                color = Color.parseColor("#C2C8E6")
            })

        }

        layer("底部文字") {
            val paddingHorizontal = 30f
            val widthStep = (width - paddingHorizontal * 2) / 4
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#C2C8E6")
            }
            val textList = listOf("1", "10", "15", "20", "31")
            (0..4).forEach {
                canvas.drawText(textList[it], paddingHorizontal + widthStep * it, height - 30f, textPaint)
            }
        }


    }

    private inline fun layer(description: String = "", crossinline block: () -> Unit) {
        if (description.length > 1) {
            Log.d("ViewOnDraw", description)
        }
        block()
    }

    private inline fun draw(index: Int, crossinline block: (Int) -> Unit) {
        block(index)
    }
}