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
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 手写签名板。墨迹画在 Canvas 上，占位文字是同级 Text，导出时不会进入图片。
 */
class SignaturePadView : ViewContainer<SignaturePadAttr, SignaturePadEvent>() {
    private val strokes = ArrayList<ArrayList<StrokePoint>>()
    private var activeStroke: ArrayList<StrokePoint>? = null
    private var canvasRef: ViewRef<CanvasView>? = null

    private var placeholderText by observable("")
    private var placeholderSize by observable(16f)
    private var inkColor by observable(Color.BLACK)
    private var inkWidth by observable(3f)
    private var paperColor by observable(Color.WHITE)
    private var empty by observable(true)
    private var revision by observable(0)

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

    fun clear() {
        strokes.clear()
        activeStroke = null
        empty = true
        revision++
    }

    fun toImage(callback: (String?) -> Unit) {
        exportImage(DeclarativeBaseView.ImageType.DATA_URI, 1, callback)
    }

    fun exportImage(
        type: DeclarativeBaseView.ImageType,
        sampleSize: Int = 1,
        callback: (String?) -> Unit,
    ) {
        val canvas = canvasRef?.view
        if (canvas == null) {
            callback(null)
            return
        }
        canvas.toImage(type, sampleSize) { payload ->
            callback(payload.exportData())
        }
    }

    private fun syncStyle() {
        placeholderText = attr.placeholderText
        placeholderSize = attr.placeholderSize
        inkColor = attr.pen
        inkWidth = attr.penStrokeWidth
        paperColor = attr.paper
    }

    private fun mountSurface() {
        val ctx = this
        Canvas({
            ref { ctx.canvasRef = it }
            attr {
                absolutePositionAllZero()
            }
        }) { context, width, height ->
            // Touch revision so redraw runs when strokes change.
            ctx.revision
            context.beginPath()
            context.fillStyle(ctx.paperColor)
            context.moveTo(0f, 0f)
            context.lineTo(width, 0f)
            context.lineTo(width, height)
            context.lineTo(0f, height)
            context.closePath()
            context.fill()

            context.lineCapRound()
            context.lineWidth(ctx.inkWidth)
            context.strokeStyle(ctx.inkColor)
            for (stroke in ctx.strokes) {
                if (stroke.isEmpty()) {
                    continue
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
                touchDown(true) { params ->
                    ctx.beginStroke(params.x, params.y)
                }
                touchMove(true) { params ->
                    ctx.extendStroke(params.x, params.y)
                }
                touchUp(true) {
                    ctx.finishStroke()
                }
                touchCancel(true) {
                    ctx.finishStroke()
                }
            }
        }
    }

    private fun beginStroke(x: Float, y: Float) {
        val stroke = ArrayList<StrokePoint>()
        stroke.add(StrokePoint(x, y))
        strokes.add(stroke)
        activeStroke = stroke
        empty = false
        revision++
    }

    private fun extendStroke(x: Float, y: Float) {
        val stroke = activeStroke ?: return
        stroke.add(StrokePoint(x, y))
        revision++
    }

    private fun finishStroke() {
        activeStroke = null
    }

    private fun JSONObject?.exportData(): String? {
        val payload = this ?: return null
        if (!payload.has("code") || payload.optInt("code") != 0 || !payload.has("data")) {
            return null
        }
        return payload.optString("data")
    }
}

class SignaturePadAttr : ContainerAttr() {
    internal var placeholderText: String = ""
    internal var placeholderSize: Float = 16f
    internal var pen: Color = Color.BLACK
    internal var penStrokeWidth: Float = 3f
    internal var paper: Color = Color.WHITE
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
}

class SignaturePadEvent : Event()

fun ViewContainer<*, *>.SignaturePad(init: SignaturePadView.() -> Unit) {
    addChild(SignaturePadView(), init)
}

private class StrokePoint(val x: Float, val y: Float)
