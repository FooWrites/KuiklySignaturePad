package com.kuikly.signature.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.nativeRef
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.input.pointer.PointerId
import com.tencent.kuikly.compose.ui.input.pointer.changedToDownIgnoreConsumed
import com.tencent.kuikly.compose.ui.input.pointer.changedToUpIgnoreConsumed
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

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

/**
 * Compose DSL 手写签名板的状态。
 *
 * 笔迹变化通过 Compose 状态触发重绘，触摸由 `pointerInput` 处理；
 * 导出时通过 [Modifier.nativeRef] 获取底层 View，再调用 [DeclarativeBaseView.toImage]。
 *
 * 指针坐标与 [Canvas] 一致，单位为像素。`penWidth` 等尺寸参数为 [Dp]，开始一笔时通过 `toPx()` 换算。
 */
class SignaturePadComposeState {
    private val strokes = mutableListOf<Ink>()
    private var active: Ink? = null
    private var surfaceRef: ViewRef<DeclarativeBaseView<*, *>>? = null
    private var placeholderRef: ViewRef<DeclarativeBaseView<*, *>>? = null
    internal var onSigned: (Boolean) -> Unit = {}

    /**
     * 重绘计数，只在 [drawBoard] 中读取。
     * 绘制阶段读取的状态变化只触发 Canvas 重绘，不会重组 [SignaturePadCompose]；
     * 绘制一笔的过程中如果发生重组，`pointerInput` 会重启并取消当前手势。
     */
    private var drawTick by mutableStateOf(0)

    /**
     * 是否组合提示文字。
     * 绘制一笔的过程中不修改该状态，只隐藏提示文字的原生 View，以免重组中断当前手势；
     * 一笔结束或清除后再更新。
     */
    internal var showPlaceholder by mutableStateOf(true)
        private set

    /** 画板没有笔迹时为 true。单击也会留下笔迹。该属性不是 Compose 状态，读取它不会触发重组。 */
    val isEmpty: Boolean
        get() = strokes.isEmpty()

    /**
     * 清除全部笔迹，并回调 `onSigned(false)`。画板本来为空时不回调。
     */
    fun clear() {
        val hadInk = strokes.isNotEmpty() || active != null
        strokes.clear()
        active = null
        if (!hadInk) {
            return
        }
        showPlaceholder = true
        setPlaceholderVisible(true)
        drawTick++
        onSigned(false)
    }

    /**
     * 将画板（底色、边框和笔迹）导出为图片。
     *
     * 基于 [DeclarativeBaseView.toImage]，Kuikly 2.17.0 起支持 Android、iOS 和 HarmonyOS，不支持 H5。
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

    internal fun attach(ref: ViewRef<DeclarativeBaseView<*, *>>) {
        surfaceRef = ref
    }

    internal fun attachPlaceholder(ref: ViewRef<DeclarativeBaseView<*, *>>) {
        placeholderRef = ref
    }

    internal fun begin(position: Offset, color: Color, widthPx: Float) {
        if (active != null) {
            return
        }
        val stroke = Ink(color, widthPx)
        stroke.points.add(position)
        strokes.add(stroke)
        active = stroke
        setPlaceholderVisible(false)
        drawTick++
    }

    internal fun extend(position: Offset, minDistancePx: Float) {
        val stroke = active ?: return
        val last = stroke.points.last()
        if (!movedEnough(last, position, minDistancePx)) {
            return
        }
        stroke.points.add(position)
        drawTick++
    }

    internal fun end(position: Offset?, minDistancePx: Float) {
        val stroke = active ?: return
        if (position != null) {
            val last = stroke.points.lastOrNull()
            if (last != null && movedEnough(last, position, minDistancePx)) {
                stroke.points.add(position)
            }
        }
        active = null
        drawTick++
        val empty = strokes.isEmpty()
        showPlaceholder = empty
        if (empty) {
            setPlaceholderVisible(true)
        }
        onSigned(!empty)
    }

    internal fun drawBoard(scope: DrawScope) {
        // 在绘制阶段读取 drawTick，笔迹变化时才会触发重绘。
        if (drawTick < 0) {
            return
        }
        for (stroke in strokes) {
            scope.drawStroke(stroke)
        }
    }

    private fun setPlaceholderVisible(visible: Boolean) {
        placeholderRef?.view?.getViewAttr()?.visibility(visible)
    }

    private fun DrawScope.drawStroke(stroke: Ink) {
        val points = stroke.points
        if (points.isEmpty()) {
            return
        }
        if (points.size == 1) {
            val point = points[0]
            drawCircle(
                color = stroke.color,
                radius = (stroke.widthPx / 2f).coerceAtLeast(0.5.dp.toPx()),
                center = point,
            )
            return
        }
        val path = Path()
        if (points.size == 2) {
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
        } else {
            path.moveTo(points[0].x, points[0].y)
            for (index in 1 until points.size - 1) {
                val current = points[index]
                val next = points[index + 1]
                val midX = (current.x + next.x) / 2f
                val midY = (current.y + next.y) / 2f
                path.quadraticBezierTo(current.x, current.y, midX, midY)
            }
            val last = points.last()
            path.lineTo(last.x, last.y)
        }
        drawPath(
            path = path,
            color = stroke.color,
            style = Stroke(width = stroke.widthPx, cap = StrokeCap.Round),
        )
    }

    private fun exportWith(
        callback: (SignatureExportResult) -> Unit,
        capture: (DeclarativeBaseView<*, *>, (JSONObject?) -> Unit) -> Unit
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

    private fun movedEnough(from: Offset, to: Offset, minDistancePx: Float): Boolean {
        val dx = to.x - from.x
        val dy = to.y - from.y
        return dx * dx + dy * dy >= minDistancePx * minDistancePx
    }

    private class Ink(
        val color: Color,
        val widthPx: Float
    ) {
        val points = mutableListOf<Offset>()
    }
}

@Composable
fun rememberSignaturePadState(): SignaturePadComposeState = remember { SignaturePadComposeState() }

/**
 * Compose DSL 手写签名板。需通过 [modifier] 指定尺寸。
 *
 * ```
 * val padState = rememberSignaturePadState()
 * SignaturePadCompose(
 *     state = padState,
 *     modifier = Modifier.fillMaxWidth().height(240.dp),
 *     penColor = Color(0xFF1A2744),
 *     penWidth = 3.5.dp,
 *     onSigned = { hasInk -> },
 * )
 * padState.clear()
 * padState.export(DeclarativeBaseView.ImageType.DATA_URI) { result ->
 *     if (result.success) result.data
 * }
 * ```
 *
 * 只响应第一个触点。使用底层指针事件，以保留按下时的位置。
 * `pointerInput` 的 key 固定为 [state]，笔迹颜色和宽度通过 [rememberUpdatedState] 读取，
 * 参数变化时不会中断当前手势。
 */
@Composable
fun SignaturePadCompose(
    modifier: Modifier = Modifier,
    state: SignaturePadComposeState = rememberSignaturePadState(),
    penColor: Color = Color.Black,
    penWidth: Dp = 3.dp,
    boardColor: Color = Color.White,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0xFFD9D1C7),
    placeholder: String = "请在此处签名",
    placeholderColor: Color = Color(0xFFB7B1A8),
    placeholderFontSize: TextUnit = 15.sp,
    onSigned: (Boolean) -> Unit = {},
) {
    val penColorState = rememberUpdatedState(penColor)
    val penWidthState = rememberUpdatedState(penWidth)
    val shape = RoundedCornerShape(cornerRadius)
    SideEffect {
        state.onSigned = onSigned
    }

    Box(
        modifier = modifier
            .background(boardColor, shape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .nativeRef { viewRef ->
                // 防止父级滚动容器拦截签名操作，同时保留 Compose 的指针事件处理。
                getViewAttr().preventTouch(true)
                state.attach(viewRef)
            }
            .pointerInput(state) {
                val minDistancePx = 1.5.dp.toPx()
                awaitEachGesture {
                    var pointerId: PointerId? = null
                    var up: Offset? = null
                    try {
                        var finished = false
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                when {
                                    change.changedToDownIgnoreConsumed() &&
                                        (pointerId == null || change.id == pointerId) -> {
                                        if (pointerId != null) {
                                            state.end(null, minDistancePx)
                                        }
                                        pointerId = change.id
                                        change.consume()
                                        val widthPx = penWidthState.value
                                            .toPx()
                                            .coerceAtLeast(0.5.dp.toPx())
                                        state.begin(
                                            change.position,
                                            penColorState.value,
                                            widthPx,
                                        )
                                    }
                                    change.id == pointerId && change.changedToUpIgnoreConsumed() -> {
                                        change.consume()
                                        up = change.position
                                        finished = true
                                        break
                                    }
                                    change.id == pointerId && change.pressed -> {
                                        change.consume()
                                        state.extend(change.position, minDistancePx)
                                    }
                                }
                            }
                            if (finished) {
                                break
                            }
                        }
                    } finally {
                        state.end(up, minDistancePx)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .nativeRef { _ ->
                    // 由外层 Box 接收触摸事件，使事件坐标与笔迹坐标保持一致。
                    getViewAttr().touchEnable(false)
                }
        ) {
            state.drawBoard(this)
        }
        if (state.showPlaceholder && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                color = placeholderColor,
                fontSize = placeholderFontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .nativeRef { viewRef ->
                        getViewAttr().touchEnable(false)
                        state.attachPlaceholder(viewRef)
                    }
            )
        }
    }
}
