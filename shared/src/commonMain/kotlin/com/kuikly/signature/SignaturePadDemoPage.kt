package com.kuikly.signature

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("signature_pad_demo")
internal class SignaturePadDemoPage : Pager() {
    private var padRef: ViewRef<SignaturePadView>? = null
    private var exportHint by observable("尚未导出")
    private var previewSrc by observable("")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color.WHITE)
                padding(16f)
            }
            Text {
                attr {
                    text("Kuikly DSL 签名板")
                    fontSize(20f)
                    color(Color(0xFF1F2328L))
                }
            }
            Text {
                attr {
                    marginTop(6f)
                    text("支持手写、清除和导出签名图片。")
                    fontSize(14f)
                    color(Color(0xFF667085L))
                }
            }
            SignaturePad {
                ref { ctx.padRef = it }
                attr {
                    marginTop(16f)
                    height(240f)
                    placeholder = "请在此签名"
                    placeholderColor = Color(0xFFB7B1A8L)
                    placeholderFontSize = 18f
                    penColor = Color(0xFF1A2744L)
                    penWidth = 3.5f
                }
                event {
                    onSigned { hasInk ->
                        if (!hasInk) {
                            ctx.exportHint = "已清除"
                            ctx.previewSrc = ""
                        } else {
                            ctx.exportHint = "已签名"
                        }
                    }
                }
            }
            View {
                attr {
                    marginTop(16f)
                    flexDirectionRow()
                }
                View {
                    attr {
                        flex(1f)
                        height(44f)
                        marginRight(8f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFFF2F4F7L))
                        allCenter()
                    }
                    event {
                        click {
                            ctx.padRef?.view?.clear()
                        }
                    }
                    Text {
                        attr {
                            text("清除")
                            fontSize(16f)
                            color(Color(0xFF1F2328L))
                        }
                    }
                }
                View {
                    attr {
                        flex(1f)
                        height(44f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1F6FEBL))
                        allCenter()
                    }
                    event {
                        click {
                            ctx.padRef?.view?.export { result ->
                                if (result.success) {
                                    ctx.exportHint = "已导出"
                                    ctx.previewSrc = result.data
                                } else {
                                    ctx.exportHint = result.message.ifEmpty { "导出失败" }
                                    ctx.previewSrc = ""
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            text("导出")
                            fontSize(16f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
            Text {
                attr {
                    marginTop(12f)
                    text(ctx.exportHint)
                    fontSize(14f)
                    color(Color(0xFF667085L))
                }
            }
            vif({ ctx.previewSrc.isNotEmpty() }) {
                Image {
                    attr {
                        marginTop(12f)
                        width(280f)
                        height(160f)
                        src(ctx.previewSrc)
                        resizeContain()
                        borderRadius(8f)
                        backgroundColor(Color(0xFFF2F4F7L))
                    }
                }
            }
        }
    }
}
