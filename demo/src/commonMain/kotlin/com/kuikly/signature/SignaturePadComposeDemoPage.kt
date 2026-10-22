package com.kuikly.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kuikly.signature.compose.ComposeSignaturePad
import com.kuikly.signature.compose.rememberSignaturePadState
import com.tencent.kuikly.compose.ComposeContainer
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
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("signature_pad_compose_demo")
class SignaturePadComposeDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            SignaturePadComposeDemo()
        }
    }
}

@Composable
private fun SignaturePadComposeDemo() {
    val padState = rememberSignaturePadState()
    var hint by remember { mutableStateOf("尚未导出") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(text = "手写签名", fontSize = 20.sp, color = Color(0xFF1F2328))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "在框内书写，清除后可以重签。", fontSize = 14.sp, color = Color(0xFF667085))
        Spacer(modifier = Modifier.height(16.dp))
        ComposeSignaturePad(
            state = padState,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            placeholder = "请在此签名",
            onChange = { empty -> hint = if (empty) "已清除" else "尚未导出" },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            // Surface 在 modifier 之后绘制 containerColor，Modifier.background 会被主题主色盖住。
            Button(
                onClick = { padState.clear() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2F4F7),
                    contentColor = Color(0xFF1F2328),
                ),
            ) {
                Text(text = "清除", fontSize = 16.sp, color = Color(0xFF1F2328))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    hint = if (padState.isEmpty) {
                        "还没有签名"
                    } else {
                        "已导出 SVG（${padState.toSvg().length} 字符）"
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F6FEB),
                    contentColor = Color.White,
                ),
            ) {
                Text(text = "导出", fontSize = 16.sp, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = hint, fontSize = 14.sp, color = Color(0xFF667085))
    }
}
