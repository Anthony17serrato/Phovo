package com.serratocreations.phovo

import android.app.Application
import com.serratocreations.phovo.di.initApplication
import org.koin.android.ext.koin.androidContext

class PhovoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initApplication {
            androidContext(this@PhovoApplication)
        }
    }
}