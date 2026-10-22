package com.kuikly.signature

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 手写签名板。墨迹画在两层 Canvas 上：已完成的笔画一层，正在书写的一笔一层，
 * 书写时只重绘当前笔画。占位文字是同级 Text，不进入导出结果。
 */
class SignaturePadView : ViewContainer<SignaturePadAttr, SignaturePadEvent>() {
    private val ink = SignatureStrokes()
    private var surfaceRef: ViewRef<DivView>? = null
    private var activePointer: Long? = null

    private var placeholderText by observable("")
    private var placeholderSize by observable(16f)
    private var inkColor by observable(Color.BLACK)
    private var inkWidth by observable(3f)
    private var paperColor by observable(Color.WHITE)
    private var empty by observable(true)
    private var finishedRevision by observable(0)
    private var activeRevision by observable(0)

    override fun createAttr(): SignaturePadAttr {
        return SignaturePadAttr().apply {
            backgroundColor(Color.WHITE)
            border(Border(1f, BorderStyle.SOLID, Color(0xFFD0D5DDL)))
            borderRadius(8f)
            overflow(true)
        }
    }

    override fun createEvent(): SignaturePadEvent = SignaturePadEvent()

    override fun viewName(): String = ViewConst.TYPE_VIEW

    override fun didInit() {
        super.didInit()
        attr.onStyleChanged = { syncStyle() }
        syncStyle()
        mountSurface()
    }

    fun isEmpty(): Boolean = ink.isEmpty

    fun clear() {
        val wasEmpty = ink.isEmpty
        ink.clear()
        activePointer = null
        empty = true
        finishedRevision++
        activeRevision++
        if (!wasEmpty) {
            event.changeHandler?.invoke(true)
        }
    }

    /** 点序列 JSON，见 [SignatureStrokes.toJson]。所有平台可用。 */
    fun toJson(): String = ink.toJson(flexNode.layoutFrame.width, flexNode.layoutFrame.height)

    /** 当前笔迹的 SVG 文档，所有平台可用。[withPaper] 为 false 时背景透明。 */
    fun toSvg(withPaper: Boolean = true): String = ink.toSvg(
        width = flexNode.layoutFrame.width,
        height = flexNode.layoutFrame.height,
        penColor = inkColor.hexColor,
        penWidth = inkWidth,
        paperColor = if (withPaper) paperColor.hexColor else null,
    )

    /**
     * 当前宿主是否支持位图导出。Kuikly 2.4.2 只有鸿蒙渲染层实现了 `toImage`；
     * 宿主渲染层升级到 2.17.0 及以上后可用 [SignaturePadAttr.bitmapExportOnAllPlatforms] 打开其他平台。
     */
    fun isBitmapExportSupported(): Boolean {
        return attr.bitmapExportEverywhere || getPager().pageData.isOhOs
    }

    /** 位图导出为 data URI。不支持的平台立即回调 null，见 [exportImage]。 */
    fun toImage(callback: (String?) -> Unit) {
        exportImage(DeclarativeBaseView.ImageType.DATA_URI, 1, DEFAULT_EXPORT_TIMEOUT_MS, callback)
    }

    /**
     * 位图导出。回调恰好触发一次：不支持的平台立即回调 null，
     * 渲染层在 [timeoutMs] 内没有响应时也回调 null。
     */
    fun exportImage(
        type: DeclarativeBaseView.ImageType,
        sampleSize: Int = 1,
        timeoutMs: Int = DEFAULT_EXPORT_TIMEOUT_MS,
        callback: (String?) -> Unit,
    ) {
        val surface = surfaceRef?.view
        if (surface == null || !isBitmapExportSupported()) {
            callback(null)
            return
        }
        var settled = false
        var timer: String? = null
        val settle = { data: String? ->
            if (!settled) {
                settled = true
                timer?.let { clearTimeout(it) }
                callback(data)
            }
        }
        timer = setTimeout(pagerId, timeoutMs) { settle(null) }
        surface.toImage(type, sampleSize) { payload ->
            settle(payload.exportData())
        }
    }

    private fun syncStyle() {
        placeholderText = attr.placeholderText
        placeholderSize = attr.placeholderSize
        inkColor = attr.pen
        inkWidth = attr.penStrokeWidth
        paperColor = attr.paper
        ink.minDistance = attr.sampleDistance
    }

    private fun mountSurface() {
        val ctx = this
        View {
            ref { ctx.surfaceRef = it }
            attr {
                absolutePositionAllZero()
                backgroundColor(ctx.paperColor)
            }
            Canvas({
                attr {
                    absolutePositionAllZero()
                }
            }) { context, _, _ ->
                ctx.finishedRevision
                ctx.prepareInk(context)
                for (stroke in ctx.ink.finishedStrokes) {
                    drawStroke(context, stroke)
                }
            }
            Canvas({
                attr {
                    absolutePositionAllZero()
                }
            }) { context, _, _ ->
                ctx.activeRevision
                val stroke = ctx.ink.activeStroke ?: return@Canvas
                ctx.prepareInk(context)
                drawStroke(context, stroke)
            }
        }
        vif({ ctx.empty && ctx.placeholderText.isNotEmpty() }) {
            View {
                attr {
                    absolutePositionAllZero()
                    allCenter()
                }
                Text {
                    attr {
                        text(ctx.placeholderText)
                        fontSize(ctx.placeholderSize)
                        color(Color(0xFFB0B0B0L))
                        textAlignCenter()
                        marginLeft(12f)
                        marginRight(12f)
                    }
                }
            }
        }
        View {
            attr {
                absolutePositionAllZero()
                backgroundColor(Color.TRANSPARENT)
                touchEnable(true)
                consumeTouchDown(true)
            }
            event {
                touchDown(true) { ctx.onTouchDown(it) }
                touchMove(true) { ctx.onTouchMove(it) }
                touchUp(true) { ctx.onTouchUp(it) }
                touchCancel(true) { ctx.onTouchCancel() }
            }
        }
    }

    private fun prepareInk(context: CanvasContext) {
        context.lineCapRound()
        context.lineWidth(inkWidth)
        context.strokeStyle(inkColor)
    }

    // 只跟踪第一根落下的手指。iOS 的 pointerId 是 touch.hash，顶层 Int 字段会截断，
    // 因此统一用 touches 里的 Long 型 id。
    private fun onTouchDown(params: TouchParams) {
        if (activePointer != null) {
            return
        }
        val touch = params.touches.firstOrNull()
        activePointer = touch?.pointerId ?: params.pointerId.toLong()
        ink.begin(touch?.x ?: params.x, touch?.y ?: params.y)
        empty = false
        activeRevision++
    }

    private fun onTouchMove(params: TouchParams) {
        val pointer = activePointer ?: return
        if (ink.activeStroke == null) {
            return
        }
        if (params.touches.isEmpty()) {
            if (ink.extend(params.x, params.y)) activeRevision++
            return
        }
        val touch = params.touches.firstOrNull { it.pointerId == pointer }
        if (touch == null) {
            commitStroke()
        } else if (ink.extend(touch.x, touch.y)) {
            activeRevision++
        }
    }

    private fun onTouchUp(params: TouchParams) {
        // Android 的 POINTER_UP 与 iOS 的 allTouches 都带着还按着的手指，
        // 无法区分是哪根手指抬起；只在最后一根手指抬起时收笔。
        if (params.touches.size <= 1) {
            commitStroke()
            activePointer = null
        }
    }

    private fun onTouchCancel() {
        commitStroke()
        activePointer = null
    }

    private fun commitStroke() {
        if (ink.end()) {
            finishedRevision++
            activeRevision++
            event.changeHandler?.invoke(false)
        }
    }

    private fun JSONObject?.exportData(): String? {
        val payload = this ?: return null
        if (!payload.has("code") || payload.optInt("code") != 0 || !payload.has("data")) {
            return null
        }
        return payload.optString("data").ifEmpty { null }
    }

    companion object {
        const val DEFAULT_EXPORT_TIMEOUT_MS = 3000
    }
}

private fun drawStroke(context: CanvasContext, stroke: List<SignaturePoint>) {
    if (stroke.isEmpty()) {
        return
    }
    context.beginPath()
    val first = stroke[0]
    context.moveTo(first.x, first.y)
    if (stroke.size == 1) {
        context.lineTo(first.x + 0.1f, first.y)
    } else {
        for (index in 1 until stroke.size) {
            val point = stroke[index]
            context.lineTo(point.x, point.y)
        }
    }
    context.stroke()
}

class SignaturePadAttr : ContainerAttr() {
    internal var placeholderText: String = ""
    internal var placeholderSize: Float = 16f
    internal var pen: Color = Color.BLACK
    internal var penStrokeWidth: Float = 3f
    internal var paper: Color = Color.WHITE
    internal var sampleDistance: Float = SignatureStrokes.DEFAULT_MIN_DISTANCE
    internal var bitmapExportEverywhere: Boolean = false
    internal var onStyleChanged: (() -> Unit)? = null

    fun placeholder(text: String) {
        placeholderText = text
        onStyleChanged?.invoke()
    }

    fun placeholderFontSize(size: Float) {
        placeholderSize = size
        onStyleChanged?.invoke()
    }

    fun penColor(color: Color) {
        pen = color
        onStyleChanged?.invoke()
    }

    fun penWidth(width: Float) {
        penStrokeWidth = width
        onStyleChanged?.invoke()
    }

    fun paperColor(color: Color) {
        paper = color
        onStyleChanged?.invoke()
    }

    /** 采样最小间距（dp/pt），默认 1.5。 */
    fun minPointDistance(distance: Float) {
        sampleDistance = distance
        onStyleChanged?.invoke()
    }

    /** 宿主渲染层 ≥ 2.17.0 时打开，Android/iOS/H5 也走 `toImage`。默认只在鸿蒙走。 */
    fun bitmapExportOnAllPlatforms(enabled: Boolean) {
        bitmapExportEverywhere = enabled
    }
}

class SignaturePadEvent : Event() {
    internal var changeHandler: ((isEmpty: Boolean) -> Unit)? = null

    /** 一笔写完或清除后触发，参数为当前是否为空。 */
    fun onChange(handler: (isEmpty: Boolean) -> Unit) {
        changeHandler = handler
    }
}

fun ViewContainer<*, *>.SignaturePad(init: SignaturePadView.() -> Unit) {
    addChild(SignaturePadView(), init)
}
