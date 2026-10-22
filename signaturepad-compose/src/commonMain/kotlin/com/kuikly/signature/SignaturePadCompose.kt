package com.kuikly.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.DeclarativeBaseView

class SignaturePadController {
    internal var pad: SignaturePadView? = null

    fun clear() {
        pad?.clear()
    }

    fun toImage(callback: (String?) -> Unit) {
        val view = pad
        if (view == null) {
            callback(null)
        } else {
            view.toImage(callback)
        }
    }

    fun exportImage(
        type: DeclarativeBaseView.ImageType,
        sampleSize: Int = 1,
        callback: (String?) -> Unit,
    ) {
        val view = pad
        if (view == null) {
            callback(null)
        } else {
            view.exportImage(type, sampleSize, callback)
        }
    }
}

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    placeholder: String = "",
    placeholderFontSize: Float = 16f,
    penColor: Color = Color.BLACK,
    penWidth: Float = 3f,
    paperColor: Color = Color.WHITE,
    controller: SignaturePadController = remember { SignaturePadController() },
) {
    MakeKuiklyComposeNode(
        factory = { SignaturePadView() },
        modifier = modifier,
        viewUpdate = { view ->
            controller.pad = view
            view.getViewAttr().apply {
                placeholder(placeholder)
                placeholderFontSize(placeholderFontSize)
                penColor(penColor)
                penWidth(penWidth)
                paperColor(paperColor)
            }
        },
    )
}
