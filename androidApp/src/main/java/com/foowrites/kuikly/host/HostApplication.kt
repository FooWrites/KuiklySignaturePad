package com.foowrites.kuikly.host

import android.app.Application

class HostApplication : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance: Application
            private set
    }
}
