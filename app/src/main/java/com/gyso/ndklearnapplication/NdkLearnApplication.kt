package com.gyso.ndklearnapplication

import android.app.Application
import android.content.Context

class NdkLearnApplication: Application(){
    companion object{
        lateinit var appContext:Context
    }
    init {
        appContext = this
    }
}