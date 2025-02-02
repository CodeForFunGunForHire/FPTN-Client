package com.filantrop.pvnclient

import android.app.Application
import com.filantrop.pvnclient.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PvnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PvnApplication)
            modules(appModule)
        }
    }
}
