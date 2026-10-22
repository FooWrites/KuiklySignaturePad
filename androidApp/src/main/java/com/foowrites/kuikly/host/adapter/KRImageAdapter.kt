package com.foowrites.kuikly.host.adapter

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import com.foowrites.kuikly.host.HostApplication
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter
import org.json.JSONObject

/** 只加载 base64 data URI 与本地文件，足够显示签名导出预览；不处理网络图片。 */
class KRImageAdapter : IKRImageAdapter {
    override val shouldWaitViewDidLoad: Boolean get() = false

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        callback: (Drawable?) -> Unit
    ) {
        callback(decode(imageLoadOption))
    }

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        imageParams: JSONObject?,
        callback: (drawable: Drawable?) -> Unit
    ) {
        callback(decode(imageLoadOption))
    }

    private fun decode(option: HRImageLoadOption): Drawable? {
        val src = option.src
        val bitmap = when {
            option.isBase64() -> {
                val payload = src.substringAfter("base64,", "")
                if (payload.isEmpty()) return null
                val bytes = try {
                    Base64.decode(payload, Base64.DEFAULT)
                } catch (e: IllegalArgumentException) {
                    return null
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            option.isFile() -> BitmapFactory.decodeFile(src.removePrefix(HRImageLoadOption.SCHEME_FILE))
            src.startsWith("/") -> BitmapFactory.decodeFile(src)
            else -> null
        } ?: return null
        return BitmapDrawable(HostApplication.instance.resources, bitmap)
    }
}
