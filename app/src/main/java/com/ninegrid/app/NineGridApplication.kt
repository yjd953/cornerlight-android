package com.ninegrid.app

import android.app.Application

class NineGridApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
