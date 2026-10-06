package com.example.elementspace.p7c3a91e4.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.elementspace.p7c3a91e4.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
