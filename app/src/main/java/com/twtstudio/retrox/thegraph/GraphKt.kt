package com.twtstudio.retrox.thegraph

data class PointData(val content: String, val value: Float)

data class LinePoints(val pointsX: List<Float>, val pointsY: List<Float>)

fun getViewPoints(pointDataList: List<PointData>, contentWidth: Float, contentHeight: Float): LinePoints {
    val pointsX = mutableListOf<Float>()
    val pointsY = mutableListOf<Float>()

    val widthStep = contentWidth / (pointDataList.size - 1)
    val maxData = pointDataList.maxBy { it.value }?.value ?: 1.0f
    val minData = pointDataList.minBy { it.value }?.value ?: 0.0f
    val dataSpan = if (maxData != minData) maxData - minData else 1.0f
    val minDataExtended = minData - dataSpan / 4f
    val maxDataExtended = maxData + dataSpan / 4f
    val dataSpanExtended = maxDataExtended - minDataExtended

    (0 until pointDataList.size).mapTo(pointsX.apply { clear() }) {
        widthStep * it
    }

    pointDataList.mapTo(pointsY.apply { clear() }) {
        (1 - ((it.value - minDataExtended) / dataSpanExtended)) * contentHeight
    }

    return LinePoints(pointsX, pointsY)
}



