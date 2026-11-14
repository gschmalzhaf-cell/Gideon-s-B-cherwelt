package com.gideon.buecherwelt

import android.app.Application
import com.gideon.buecherwelt.di.AppContainer

class BuecherweltApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
