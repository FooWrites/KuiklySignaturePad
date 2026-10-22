package com.foowrites.kuikly.host

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.foowrites.kuikly.host.adapter.KRImageAdapter
import com.foowrites.kuikly.host.adapter.KRLogAdapter
import com.foowrites.kuikly.host.adapter.KRRouterAdapter
import com.foowrites.kuikly.host.adapter.KRThreadAdapter
import com.foowrites.kuikly.host.adapter.KRUncaughtExceptionHandlerAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import org.json.JSONObject

/**
 * Minimal Kuikly Android host for acceptance screenshots.
 * Default page: signature_pad_demo (自研 DSL).
 */
class KuiklyRenderActivity : AppCompatActivity() {

    private lateinit var hrContainerView: ViewGroup
    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator

    private val pageName: String
        get() {
            val pn = intent.getStringExtra(KEY_PAGE_NAME).orEmpty()
            return pn.ifEmpty { DEFAULT_PAGE }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAdapters()
        setContentView(R.layout.activity_hr)
        hrContainerView = findViewById(R.id.hr_container)

        val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
            override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase {
                return KuiklyRenderCoreExecuteModeBase.JVM
            }

            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                Log.i(TAG, "onPageLoadComplete succeed=$isSucceed reason=$errorReason mode=${executeMode.mode} page=$pageName")
            }

            override fun onUnhandledException(
                throwable: Throwable,
                errorReason: ErrorReason,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                Log.e(TAG, "onUnhandledException reason=$errorReason", throwable)
            }
        }
        kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)
        kuiklyRenderViewDelegator.onAttach(hrContainerView, "", pageName, createPageData())
        window.statusBarColor = Color.TRANSPARENT
        findViewById<View>(android.R.id.content).setBackgroundColor(Color.WHITE)
    }

    override fun onResume() {
        super.onResume()
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyRenderViewDelegator.onDetach()
    }

    private fun createPageData(): Map<String, Any> = mapOf(
        "appId" to 1,
        "acceptHost" to 1,
    )

    private fun setupAdapters() {
        with(KuiklyRenderAdapterManager) {
            if (krImageAdapter == null) krImageAdapter = KRImageAdapter()
            if (krLogAdapter == null) krLogAdapter = KRLogAdapter
            if (krUncaughtExceptionHandlerAdapter == null) {
                krUncaughtExceptionHandlerAdapter = KRUncaughtExceptionHandlerAdapter
            }
            if (krRouterAdapter == null) krRouterAdapter = KRRouterAdapter()
            if (krThreadAdapter == null) krThreadAdapter = KRThreadAdapter()
        }
    }

    companion object {
        private const val TAG = "KuiklyAcceptHost"
        private const val KEY_PAGE_NAME = "pageName"
        private const val KEY_PAGE_DATA = "pageData"
        const val DEFAULT_PAGE = "signature_pad_demo"

        fun start(context: Context, pageName: String, pageData: JSONObject) {
            val starter = Intent(context, KuiklyRenderActivity::class.java)
            starter.putExtra(KEY_PAGE_NAME, pageName)
            starter.putExtra(KEY_PAGE_DATA, pageData.toString())
            context.startActivity(starter)
        }
    }
}
