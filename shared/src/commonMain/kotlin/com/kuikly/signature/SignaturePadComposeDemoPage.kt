package com.kuikly.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kuikly.signature.compose.SignaturePadCompose
import com.kuikly.signature.compose.rememberSignaturePadState
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color as KuiklyColor
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.views.ImageView

@Page("signature_pad_compose_demo")
internal class SignaturePadComposeDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            SignaturePadComposeDemo()
        }
    }
}

@Composable
private fun SignaturePadComposeDemo() {
    val state = rememberSignaturePadState()
    var hint by remember { mutableStateOf("尚未导出") }
    var previewSrc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(text = "Compose DSL 签名板", fontSize = 20.sp, color = Color(0xFF1F2328))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "支持手写、清除和导出签名图片。",
            fontSize = 14.sp,
            color = Color(0xFF667085),
        )
        Spacer(modifier = Modifier.height(16.dp))
        SignaturePadCompose(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            penColor = Color(0xFF1A2744),
            penWidth = 3.5.dp,
            placeholder = "请在此签名",
            placeholderFontSize = 18.sp,
            onSigned = { hasInk ->
                hint = if (hasInk) "已签名" else "已清除"
                previewSrc = ""
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { state.clear() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            ) {
                Text(text = "清除", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    state.export(DeclarativeBaseView.ImageType.DATA_URI) { result ->
                        if (result.success) {
                            hint = "已导出"
                            previewSrc = result.data
                        } else {
                            hint = result.message.ifEmpty { "导出失败" }
                            previewSrc = ""
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            ) {
                Text(text = "导出", fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = hint, fontSize = 14.sp, color = Color(0xFF667085))
        if (previewSrc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ExportedPreview(
                src = previewSrc,
                modifier = Modifier
                    .width(280.dp)
                    .height(160.dp),
            )
        }
    }
}

@Composable
private fun ExportedPreview(src: String, modifier: Modifier = Modifier) {
    MakeKuiklyComposeNode(
        factory = { ImageView() },
        modifier = modifier.background(Color(0xFFF2F4F7), RoundedCornerShape(8.dp)),
        viewUpdate = { view ->
            view.getViewAttr().apply {
                src(src)
                resizeContain()
                backgroundColor(KuiklyColor(0xFFF2F4F7L))
                borderRadius(8f)
            }
        },
    )
}
