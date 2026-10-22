package com.foowrites.kuikly.host.adapter

import com.tencent.kuikly.core.render.android.adapter.IKRThreadAdapter
import java.util.concurrent.Executors

class KRThreadAdapter : IKRThreadAdapter {
    override fun executeOnSubThread(task: () -> Unit) {
        pool.execute(task)
    }

    companion object {
        private val pool by lazy { Executors.newFixedThreadPool(2) }
    }
}
