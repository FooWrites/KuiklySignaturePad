package com.kuikly.signature.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kuikly.signature.SignaturePoint
import com.kuikly.signature.SignatureStrokes
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.gestures.awaitFirstDown
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.StrokeJoin
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.graphics.toArgb
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * [ComposeSignaturePad] 的状态。笔迹存在 [SignatureStrokes] 里，坐标单位为 dp，
 * 与自研 DSL 版导出的 SVG / JSON 格式一致。
 */
@Stable
class SignaturePadState {
    internal val ink = SignatureStrokes()
    internal var finishedRevision by mutableIntStateOf(0)
    internal var activeRevision by mutableIntStateOf(0)
    internal var widthDp = 0f
    internal var heightDp = 0f
    internal var penArgb = 0xFF000000L
    internal var penWidthDp = 3f
    internal var paperArgb = 0xFFFFFFFFL
    internal var onChange: ((isEmpty: Boolean) -> Unit)? = null

    var isEmpty by mutableStateOf(true)
        private set

    fun clear() {
        val wasEmpty = ink.isEmpty
        ink.clear()
        isEmpty = true
        finishedRevision++
        activeRevision++
        if (!wasEmpty) {
            onChange?.invoke(true)
        }
    }

    /** 点序列 JSON，见 [SignatureStrokes.toJson]。 */
    fun toJson(): String = ink.toJson(widthDp, heightDp)

    /** SVG 文档。[withPaper] 为 false 时背景透明。 */
    fun toSvg(withPaper: Boolean = true): String = ink.toSvg(
        width = widthDp,
        height = heightDp,
        penColor = penArgb,
        penWidth = penWidthDp,
        paperColor = if (withPaper) paperArgb else null,
    )

    internal fun begin(x: Float, y: Float) {
        ink.begin(x, y)
        isEmpty = false
        activeRevision++
    }

    internal fun extend(x: Float, y: Float) {
        if (ink.extend(x, y)) {
            activeRevision++
        }
    }

    internal fun end() {
        if (ink.end()) {
            finishedRevision++
            activeRevision++
            onChange?.invoke(false)
        }
    }
}

@Composable
fun rememberSignaturePadState(): SignaturePadState = remember { SignaturePadState() }

/**
 * Compose DSL 签名板。已完成的笔画与正在书写的一笔分两层 Canvas，书写时只重绘当前笔画；
 * 多指时只跟踪第一根手指。导出用 [SignaturePadState.toSvg] / [SignaturePadState.toJson]。
 */
@Composable
fun ComposeSignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    placeholderFontSize: TextUnit = 16.sp,
    placeholderColor: Color = Color(0xFFB0B0B0),
    penColor: Color = Color.Black,
    penWidth: Dp = 3.dp,
    paperColor: Color = Color.White,
    borderColor: Color = Color(0xFFD0D5DD),
    shape: Shape = RoundedCornerShape(8.dp),
    minPointDistance: Dp = SignatureStrokes.DEFAULT_MIN_DISTANCE.dp,
    onChange: ((isEmpty: Boolean) -> Unit)? = null,
) {
    val pixelsPerDp = LocalDensity.current.density
    SideEffect {
        state.penArgb = penColor.toArgb().toLong() and 0xFFFFFFFFL
        state.penWidthDp = penWidth.value
        state.paperArgb = paperColor.toArgb().toLong() and 0xFFFFFFFFL
        state.ink.minDistance = minPointDistance.value
        state.onChange = onChange
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(paperColor, shape)
            .border(1.dp, borderColor, shape)
            .onSizeChanged { size ->
                state.widthDp = size.width / pixelsPerDp
                state.heightDp = size.height / pixelsPerDp
            }
            .pointerInput(state, pixelsPerDp) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    state.begin(down.position.x / pixelsPerDp, down.position.y / pixelsPerDp)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) {
                                break
                            }
                            state.extend(change.position.x / pixelsPerDp, change.position.y / pixelsPerDp)
                        }
                    } finally {
                        state.end()
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            state.finishedRevision
            drawInk(state.ink.finishedStrokes, penColor, penWidth)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            state.activeRevision
            state.ink.activeStroke?.let { drawInk(listOf(it), penColor, penWidth) }
        }
        if (state.isEmpty && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp),
                color = placeholderColor,
                fontSize = placeholderFontSize,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawInk(strokes: List<List<SignaturePoint>>, color: Color, width: Dp) {
    if (strokes.isEmpty()) {
        return
    }
    val scale = density
    val path = Path()
    for (stroke in strokes) {
        if (stroke.isEmpty()) {
            continue
        }
        val first = stroke[0]
        path.moveTo(first.x * scale, first.y * scale)
        if (stroke.size == 1) {
            path.lineTo(first.x * scale + 0.1f, first.y * scale)
        } else {
            for (index in 1 until stroke.size) {
                val point = stroke[index]
                path.lineTo(point.x * scale, point.y * scale)
            }
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
