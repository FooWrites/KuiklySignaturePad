package com.kuikly.signature

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.View
import kotlin.math.PI

/**
 * 手写签名板的属性。
 *
 * 笔迹保存在内存中，已完成的笔画和进行中的笔画分别绘制在两层 [Canvas] 上，
 * 书写过程中只重绘进行中的笔画。
 * 需为组件指定宽高；通过 [ViewRef] 获取 [SignaturePadView] 后调用清除和导出。
 */
class SignaturePadAttr : ComposeAttr() {
    /** 笔迹颜色。修改后只影响之后绘制的笔画。 */
    var penColor: Color by observable(Color.BLACK)

    /** 笔迹宽度，单位与 Kuikly 布局一致。修改后只影响之后绘制的笔画。 */
    var penWidth: Float by observable(3f)

    /** 画板底色，包含在导出图片中。 */
    var boardColor: Color by observable(Color.WHITE)

    /** 圆角半径。 */
    var cornerRadius: Float by observable(12f)

    /** 边框宽度，0 表示无边框。 */
    var borderWidth: Float by observable(1f)

    /** 边框颜色。 */
    var borderColor: Color by observable(Color(0xFFD9D1C7))

    /** 画板为空时显示的提示文字，空字符串表示不显示。 */
    var placeholder: String by observable("请在此处签名")

    /** 提示文字颜色。 */
    var placeholderColor: Color by observable(Color(0xFFB7B1A8))

    /** 提示文字字号。 */
    var placeholderFontSize: Float by observable(15f)
}

/**
 * 签名板事件。
 *
 * [onSigned] 在每笔结束、触摸取消或调用 [SignaturePadView.clear] 后触发，
 * 参数表示画板当前是否有笔迹。
 */
class SignaturePadEvent : ComposeEvent() {
    internal var signedHandler: ((hasInk: Boolean) -> Unit)? = null

    fun onSigned(handler: (hasInk: Boolean) -> Unit) {
        signedHandler = handler
    }
}

/**
 * 签名导出结果。
 *
 * [code] 为 0 表示导出成功。[data] 的格式由导出时传入的 [DeclarativeBaseView.ImageType] 决定：
 * 缓存 key、`data:image/png;base64,...` 或文件路径。
 */
class SignatureExportResult(
    val code: Int,
    val data: String,
    val message: String
) {
    val success: Boolean
        get() = code == 0 && data.isNotEmpty()

    companion object {
        /** 画板为空，未执行导出。 */
        const val EMPTY_CODE: Int = -2

        /** 画板尚未完成布局，无法导出。 */
        const val NOT_READY_CODE: Int = -3
    }
}

class SignaturePadView : ComposeView<SignaturePadAttr, SignaturePadEvent>() {
    // Canvas 只会响应绘制回调中读取的 observable。笔迹数据不是 observable，
    // 因此用计数触发重绘：inkRevision 对应已完成笔画层，activeRevision 对应进行中笔画层。
    private var inkRevision by observable(0)
    private var activeRevision by observable(0)
    private var surfaceRef: ViewRef<DivView>? = null
    private val ink = SignatureInk(MIN_POINT_DISTANCE)

    // H5 和小程序的渲染层不支持 batchDraw 指令，开启后不会绘制任何内容。
    private val batchDrawSupported by lazy(LazyThreadSafetyMode.NONE) {
        val pageData = getPager().pageData
        pageData.isAndroid || pageData.isIOS || pageData.isOhOs
    }

    /** 画板没有笔迹时为 true。单击也会留下笔迹。 */
    val isEmpty: Boolean
        get() = ink.isEmpty

    override fun createAttr(): SignaturePadAttr = SignaturePadAttr()

    override fun createEvent(): SignaturePadEvent = SignaturePadEvent()

    /**
     * 清除全部笔迹，并通过 [SignaturePadEvent.onSigned] 回调 `false`。
     */
    fun clear() {
        if (!ink.clear()) {
            return
        }
        inkRevision++
        activeRevision++
        notifySigned()
    }

    /**
     * 将画板（底色、边框和笔迹）导出为图片。
     *
     * 基于 [DeclarativeBaseView.toImage]，支持 Android、iOS、HarmonyOS 和 H5，H5 不支持 [DeclarativeBaseView.ImageType.FILE]。
     * 画板为空时不执行导出，直接回调 code = [SignatureExportResult.EMPTY_CODE]。
     */
    fun export(
        type: DeclarativeBaseView.ImageType = DeclarativeBaseView.ImageType.DATA_URI,
        sampleSize: Int = 1,
        callback: (SignatureExportResult) -> Unit
    ) {
        exportWith(callback) { view, done ->
            view.toImage(type, sampleSize, done)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                ref {
                    ctx.surfaceRef = it
                }
                attr {
                    absolutePosition(top = 0f, left = 0f, bottom = 0f, right = 0f)
                    backgroundColor(ctx.attr.boardColor)
                    borderRadius(ctx.attr.cornerRadius)
                    if (ctx.attr.borderWidth > 0f) {
                        border(
                            Border(
                                lineWidth = ctx.attr.borderWidth,
                                lineStyle = BorderStyle.SOLID,
                                color = ctx.attr.borderColor
                            )
                        )
                    }
                    overflow(true)
                    // 防止父级滚动容器拦截签名操作，同时保留当前 View 的触摸回调。
                    preventTouch(true)
                }
                event {
                    touchDown(true) { params ->
                        ctx.beginStroke(params)
                    }
                    touchMove(true) { params ->
                        ctx.extendStroke(params)
                    }
                    touchUp(true) { params ->
                        ctx.endStroke(params)
                    }
                    touchCancel(true) { params ->
                        ctx.endStroke(params)
                    }
                }
                Canvas({
                    attr {
                        absolutePosition(top = 0f, left = 0f, bottom = 0f, right = 0f)
                        // 由外层 View 接收触摸事件，使事件坐标与笔迹坐标保持一致。
                        touchEnable(false)
                    }
                }) { context, width, height ->
                    ctx.drawInk(context, width, height)
                }
                Canvas({
                    attr {
                        absolutePosition(top = 0f, left = 0f, bottom = 0f, right = 0f)
                        touchEnable(false)
                    }
                }) { context, _, _ ->
                    ctx.drawActiveStroke(context)
                }
            }
        }
    }

    private fun beginStroke(params: TouchParams) {
        val wasEmpty = ink.isEmpty
        val width = attr.penWidth.coerceAtLeast(MIN_PEN_WIDTH)
        if (!ink.begin(params.pointerId, params.x, params.y, attr.penColor, width)) {
            return
        }
        if (wasEmpty) {
            // 隐藏提示文字
            inkRevision++
        }
        activeRevision++
    }

    private fun extendStroke(params: TouchParams) {
        if (ink.extend(params.pointerId, params.x, params.y)) {
            activeRevision++
        }
    }

    private fun endStroke(params: TouchParams) {
        if (!ink.end(params.pointerId, params.x, params.y)) {
            return
        }
        inkRevision++
        activeRevision++
        notifySigned()
    }

    private fun notifySigned() {
        event.signedHandler?.invoke(!isEmpty)
    }

    private fun drawInk(context: CanvasContext, width: Float, height: Float) {
        context.batchDraw = batchDrawSupported
        if (inkRevision < 0) {
            return
        }
        if (ink.isEmpty) {
            drawPlaceholder(context, width, height)
            return
        }
        for (stroke in ink.strokes) {
            drawStroke(context, stroke)
        }
    }

    private fun drawActiveStroke(context: CanvasContext) {
        context.batchDraw = batchDrawSupported
        if (activeRevision < 0) {
            return
        }
        ink.activeStroke?.let { drawStroke(context, it) }
    }

    private fun drawPlaceholder(context: CanvasContext, width: Float, height: Float) {
        val text = attr.placeholder
        if (text.isEmpty() || width <= 0f || height <= 0f) {
            return
        }
        context.beginPath()
        context.font(size = attr.placeholderFontSize.coerceAtLeast(0f))
        context.textAlign(TextAlign.CENTER)
        context.fillStyle(attr.placeholderColor)
        val metrics = context.measureText(text)
        val baseline = height / 2f + (metrics.actualBoundingBoxAscent - metrics.actualBoundingBoxDescent) / 2f
        context.fillText(text, width / 2f, baseline)
    }

    private fun drawStroke(context: CanvasContext, stroke: SignatureStroke) {
        val points = stroke.points
        if (points.isEmpty()) {
            return
        }
        context.beginPath()
        context.lineCapRound()
        context.lineWidth(stroke.width)
        context.strokeStyle(stroke.color)
        context.fillStyle(stroke.color)
        if (points.size == 1) {
            val point = points[0]
            val radius = (stroke.width / 2f).coerceAtLeast(0.5f)
            context.arc(point.x, point.y, radius, 0f, (PI * 2.0).toFloat(), false)
            context.fill()
            return
        }
        if (points.size == 2) {
            context.moveTo(points[0].x, points[0].y)
            context.lineTo(points[1].x, points[1].y)
            context.stroke()
            return
        }
        context.moveTo(points[0].x, points[0].y)
        for (index in 1 until points.size - 1) {
            val current = points[index]
            val next = points[index + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            context.quadraticCurveTo(current.x, current.y, midX, midY)
        }
        val last = points.last()
        context.lineTo(last.x, last.y)
        context.stroke()
    }

    private fun exportWith(
        callback: (SignatureExportResult) -> Unit,
        capture: (DivView, (JSONObject?) -> Unit) -> Unit
    ) {
        if (isEmpty) {
            callback(SignatureExportResult(SignatureExportResult.EMPTY_CODE, "", "签名为空"))
            return
        }
        val view = surfaceRef?.view
        if (view == null) {
            callback(SignatureExportResult(SignatureExportResult.NOT_READY_CODE, "", "签名板尚未完成布局"))
            return
        }
        capture(view) { json ->
            callback(
                SignatureExportResult(
                    code = json?.optInt("code", -1) ?: -1,
                    data = json?.optString("data").orEmpty(),
                    message = json?.optString("message").orEmpty()
                )
            )
        }
    }

    private fun movedEnough(from: SignaturePoint, x: Float, y: Float): Boolean {
        val dx = x - from.x
        val dy = y - from.y
        return dx * dx + dy * dy >= MIN_POINT_DISTANCE * MIN_POINT_DISTANCE
    }

    companion object {
        private const val MIN_PEN_WIDTH = 0.5f
        private const val MIN_POINT_DISTANCE = 1.5f
    }
}

fun ViewContainer<*, *>.SignaturePad(init: SignaturePadView.() -> Unit) {
    addChild(SignaturePadView(), init)
}
