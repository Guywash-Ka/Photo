package com.example.photoclassification

import android.app.Application

class ImageApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        // Some other logic
    }

    companion object {
        const val TAG = "MainLoggingTag"
    }
}
