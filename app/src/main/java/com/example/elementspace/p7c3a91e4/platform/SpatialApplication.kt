package com.example.elementspace.p7c3a91e4.platform

import android.app.Application
import com.example.elementspace.p7c3a91e4.mainApp
import com.example.elementspace.p7c3a91e4.ui.navigation.DebugBus
import com.pico.spatial.ui.foundation.dsl.launch

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BackKeys.install(this)
        DebugBus.register(this)
        launch(::mainApp)
    }
}
