package com.kuikly.signature

import kotlin.math.abs
import kotlin.math.roundToInt

class SignaturePoint(val x: Float, val y: Float)

/**
 * 纯 Kotlin 的笔迹模型，坐标单位与 Kuikly 布局一致（dp/pt）。
 * 自研 DSL 与 Compose DSL 共用，不依赖渲染层，导出在所有平台都可用。
 */
class SignatureStrokes(
    /** 与上一采样点距离小于该值的移动点会被丢弃。 */
    var minDistance: Float = DEFAULT_MIN_DISTANCE,
) {
    private val finished = ArrayList<List<SignaturePoint>>()
    private var active: ArrayList<SignaturePoint>? = null

    /** 已落笔完成的笔画。 */
    val finishedStrokes: List<List<SignaturePoint>> get() = finished

    /** 正在书写的笔画，没有时为 null。 */
    val activeStroke: List<SignaturePoint>? get() = active

    val strokeCount: Int get() = finished.size + if (active == null) 0 else 1

    val isEmpty: Boolean get() = finished.isEmpty() && active == null

    fun begin(x: Float, y: Float) {
        end()
        active = arrayListOf(SignaturePoint(x, y))
    }

    /** 返回 false 表示点被距离过滤丢弃，或当前没有进行中的笔画。 */
    fun extend(x: Float, y: Float): Boolean {
        val stroke = active ?: return false
        val last = stroke[stroke.size - 1]
        val dx = x - last.x
        val dy = y - last.y
        if (dx * dx + dy * dy < minDistance * minDistance) {
            return false
        }
        stroke.add(SignaturePoint(x, y))
        return true
    }

    /** 返回 true 表示有一笔被提交。 */
    fun end(): Boolean {
        val stroke = active ?: return false
        active = null
        finished.add(stroke)
        return true
    }

    fun clear() {
        finished.clear()
        active = null
    }

    /**
     * 点序列 JSON：`{"width":W,"height":H,"strokes":[[x0,y0,x1,y1,...],...]}`。
     * 坐标保留一位小数，单位 dp/pt。
     */
    fun toJson(width: Float, height: Float): String {
        val out = StringBuilder()
        out.append("{\"width\":").append(formatNumber(width))
        out.append(",\"height\":").append(formatNumber(height))
        out.append(",\"strokes\":[")
        forEachStroke { index, stroke ->
            if (index > 0) out.append(',')
            out.append('[')
            stroke.forEachIndexed { i, point ->
                if (i > 0) out.append(',')
                out.append(formatNumber(point.x)).append(',').append(formatNumber(point.y))
            }
            out.append(']')
        }
        out.append("]}")
        return out.toString()
    }

    /**
     * SVG 文档，`viewBox` 为 `0 0 width height`。
     * 颜色为 ARGB（与 `com.tencent.kuikly.core.base.Color.hexColor` 相同）；
     * [paperColor] 为 null 时背景透明。
     */
    fun toSvg(
        width: Float,
        height: Float,
        penColor: Long = 0xFF000000L,
        penWidth: Float = 3f,
        paperColor: Long? = 0xFFFFFFFFL,
    ): String {
        val w = formatNumber(width)
        val h = formatNumber(height)
        val out = StringBuilder()
        out.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(w)
            .append("\" height=\"").append(h)
            .append("\" viewBox=\"0 0 ").append(w).append(' ').append(h).append("\">")
        if (paperColor != null) {
            out.append("<rect width=\"100%\" height=\"100%\"")
            appendColor(out, "fill", paperColor)
            out.append("/>")
        }
        if (!isEmpty) {
            out.append("<path fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\"")
            appendColor(out, "stroke", penColor)
            out.append(" stroke-width=\"").append(formatNumber(penWidth)).append("\" d=\"")
            forEachStroke { index, stroke ->
                if (index > 0) out.append(' ')
                val first = stroke[0]
                out.append('M').append(formatNumber(first.x)).append(' ').append(formatNumber(first.y))
                if (stroke.size == 1) {
                    out.append("l0.1 0")
                } else {
                    for (i in 1 until stroke.size) {
                        val point = stroke[i]
                        out.append('L').append(formatNumber(point.x)).append(' ').append(formatNumber(point.y))
                    }
                }
            }
            out.append("\"/>")
        }
        out.append("</svg>")
        return out.toString()
    }

    private inline fun forEachStroke(action: (Int, List<SignaturePoint>) -> Unit) {
        var index = 0
        for (stroke in finished) {
            action(index++, stroke)
        }
        active?.let { action(index, it) }
    }

    private fun appendColor(out: StringBuilder, name: String, argb: Long) {
        out.append(' ').append(name).append("=\"#")
        val rgb = argb and 0xFFFFFF
        out.append(rgb.toString(16).padStart(6, '0'))
        out.append('"')
        val alpha = ((argb shr 24) and 0xFF).toInt()
        if (alpha != 0xFF) {
            out.append(' ').append(name).append("-opacity=\"").append(formatNumber(alpha / 255f, 3)).append('"')
        }
    }

    companion object {
        const val DEFAULT_MIN_DISTANCE = 1.5f

        internal fun formatNumber(value: Float, decimals: Int = 1): String {
            var scale = 1
            repeat(decimals) { scale *= 10 }
            val scaled = (value * scale).roundToInt()
            if (scaled == 0) return "0"
            val magnitude = abs(scaled)
            val whole = magnitude / scale
            val fraction = (magnitude % scale).toString().padStart(decimals, '0').trimEnd('0')
            val sign = if (scaled < 0) "-" else ""
            return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
        }
    }
}
