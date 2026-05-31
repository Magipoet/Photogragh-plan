package com.photo.plan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.photo.plan.data.local.dao.PlanDao
import com.photo.plan.data.local.dao.SampleDao
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.data.local.entity.SampleEntity

@Database(
    entities = [PlanEntity::class, SampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun sampleDao(): SampleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photo_plan_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
