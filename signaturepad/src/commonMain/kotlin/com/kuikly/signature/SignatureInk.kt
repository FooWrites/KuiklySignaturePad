package com.kuikly.signature

import com.tencent.kuikly.core.base.Color

internal class SignaturePoint(val x: Float, val y: Float)

internal class SignatureStroke(
    val color: Color,
    val width: Float
) {
    val points = mutableListOf<SignaturePoint>()
}

/**
 * 签名笔迹数据。只跟踪第一个触点，其他触点的事件会被忽略。
 *
 * 进行中的笔画保存在 [activeStroke]，结束后移入 [strokes]。
 */
internal class SignatureInk(private val minPointDistance: Float) {
    private val finishedStrokes = mutableListOf<SignatureStroke>()
    private var activePointerId: Int? = null

    /** 已完成的笔画。 */
    val strokes: List<SignatureStroke>
        get() = finishedStrokes

    var activeStroke: SignatureStroke? = null
        private set

    val isEmpty: Boolean
        get() = finishedStrokes.isEmpty() && activeStroke == null

    /** 开始一笔。已有进行中的笔画时返回 false。 */
    fun begin(pointerId: Int, x: Float, y: Float, color: Color, width: Float): Boolean {
        if (activePointerId != null) {
            return false
        }
        val stroke = SignatureStroke(color, width)
        stroke.points.add(SignaturePoint(x, y))
        activeStroke = stroke
        activePointerId = pointerId
        return true
    }

    /** 追加一个点。不是当前触点或移动距离不足时返回 false。 */
    fun extend(pointerId: Int, x: Float, y: Float): Boolean {
        if (pointerId != activePointerId) {
            return false
        }
        val stroke = activeStroke ?: return false
        if (!movedEnough(stroke.points.last(), x, y)) {
            return false
        }
        stroke.points.add(SignaturePoint(x, y))
        return true
    }

    /** 结束当前笔画并移入 [strokes]。没有对应的进行中笔画时返回 false。 */
    fun end(pointerId: Int, x: Float, y: Float): Boolean {
        if (pointerId != activePointerId) {
            return false
        }
        val stroke = activeStroke ?: return false
        if (movedEnough(stroke.points.last(), x, y)) {
            stroke.points.add(SignaturePoint(x, y))
        }
        finishedStrokes.add(stroke)
        activeStroke = null
        activePointerId = null
        return true
    }

    /** 清除全部笔迹。原本为空时返回 false。 */
    fun clear(): Boolean {
        val hadInk = !isEmpty
        finishedStrokes.clear()
        activeStroke = null
        activePointerId = null
        return hadInk
    }

    private fun movedEnough(from: SignaturePoint, x: Float, y: Float): Boolean {
        val dx = x - from.x
        val dy = y - from.y
        return dx * dx + dy * dy >= minPointDistance * minPointDistance
    }
}
