package com.kuikly.signature

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("signature_pad_demo_home")
internal class SignaturePadDemoHomePage : Pager() {
    private val routerModule by lazy(LazyThreadSafetyMode.NONE) {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
            flexDirectionColumn()
            backgroundColor(Color.WHITE)
            padding(24f)
        }
        Text {
            attr {
                text("Kuikly SignaturePad")
                fontSize(24f)
                color(Color(0xFF1F2328L))
            }
        }
        Text {
            attr {
                marginTop(8f)
                text("选择 DSL 查看签名板示例")
                fontSize(14f)
                color(Color(0xFF667085L))
            }
        }
        View {
            attr {
                marginTop(20f)
                height(72f)
                padding(16f)
                borderRadius(12f)
                backgroundColor(Color(0xFFF2F4F7L))
                flexDirectionColumn()
            }
            event {
                click {
                    ctx.routerModule.openPage("signature_pad_demo")
                }
            }
            Text {
                attr {
                    text("Kuikly DSL")
                    fontSize(18f)
                    color(Color(0xFF1F2328L))
                }
            }
            Text {
                attr {
                    marginTop(6f)
                    text("SignaturePadView")
                    fontSize(13f)
                    color(Color(0xFF667085L))
                }
            }
        }
        View {
            attr {
                marginTop(12f)
                height(72f)
                padding(16f)
                borderRadius(12f)
                backgroundColor(Color(0xFFF2F4F7L))
                flexDirectionColumn()
            }
            event {
                click {
                    ctx.routerModule.openPage("signature_pad_compose_demo")
                }
            }
            Text {
                attr {
                    text("Compose DSL")
                    fontSize(18f)
                    color(Color(0xFF1F2328L))
                }
            }
            Text {
                attr {
                    marginTop(6f)
                    text("SignaturePadCompose")
                    fontSize(13f)
                    color(Color(0xFF667085L))
                }
            }
        }
        }
    }
}
