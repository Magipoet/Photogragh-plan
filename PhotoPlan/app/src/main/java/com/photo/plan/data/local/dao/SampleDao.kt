package com.photo.plan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.plan.data.local.entity.SampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    @Query("SELECT * FROM samples WHERE planId = :planId ORDER BY isCompleted ASC, sortOrder ASC")
    fun getSamplesByPlanId(planId: Long): Flow<List<SampleEntity>>

    @Query("SELECT COUNT(*) FROM samples WHERE planId = :planId")
    fun getSampleCountByPlanId(planId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM samples WHERE planId = :planId AND isCompleted = 1")
    fun getCompletedCountByPlanId(planId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<SampleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: SampleEntity): Long

    @Query("UPDATE samples SET isCompleted = :completed WHERE id = :sampleId")
    suspend fun updateCompleted(sampleId: Long, completed: Boolean)

    @Query("DELETE FROM samples WHERE id = :sampleId")
    suspend fun deleteSampleById(sampleId: Long)

    @Query("DELETE FROM samples WHERE planId = :planId")
    suspend fun deleteSamplesByPlanId(planId: Long)

    @Query("SELECT MAX(sortOrder) FROM samples WHERE planId = :planId")
    suspend fun getMaxSortOrder(planId: Long): Int?

    @Query("SELECT COUNT(*) FROM samples WHERE planId = :planId")
    suspend fun getSampleCount(planId: Long): Int

    @Query("SELECT COUNT(*) FROM samples WHERE planId = :planId AND isCompleted = 1")
    suspend fun getCompletedCount(planId: Long): Int
}
