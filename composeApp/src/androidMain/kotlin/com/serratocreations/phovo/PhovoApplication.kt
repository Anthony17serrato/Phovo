package com.serratocreations.phovo

import android.app.Application
import com.serratocreations.phovo.di.initKoin
import org.koin.android.ext.koin.androidContext

class PhovoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@PhovoApplication)
        }
    }
}