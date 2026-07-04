package com.ccard.tracker

import android.app.Application
import com.ccard.tracker.data.AppDatabase

class CCardApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
