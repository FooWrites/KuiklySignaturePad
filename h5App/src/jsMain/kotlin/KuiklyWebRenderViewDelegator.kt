package com.kuikly.signature.h5

import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyRenderViewDelegator
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate

class KuiklyWebRenderViewDelegator : KuiklyRenderViewDelegatorDelegate {
    private val delegate = KuiklyRenderViewDelegator(this)

    fun init(
        containerId: String,
        pageName: String,
        pageData: Map<String, Any>,
        size: SizeI,
    ) {
        delegate.onAttach(containerId, pageName, pageData, size)
    }

    fun resume() {
        delegate.onResume()
    }

    fun pause() {
        delegate.onPause()
    }
}
