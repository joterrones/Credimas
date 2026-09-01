package com.credimax.app

import android.app.Application
import com.credimax.app.data.AppContainer

class CredimaxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
