package com.photo.plan

import android.app.Application
import com.photo.plan.data.local.AppDatabase

class PhotoPlanApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
