package com.photo.plan.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.photo.plan.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY isPinned DESC, pinnedOrder ASC, updatedAt DESC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE isPinned = 1 ORDER BY pinnedOrder ASC")
    fun getPinnedPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("UPDATE plans SET isPinned = :isPinned WHERE id = :planId")
    suspend fun updatePinned(planId: Long, isPinned: Boolean)

    @Query("DELETE FROM plans WHERE id = :planId")
    suspend fun deletePlanById(planId: Long)

    @Query("SELECT name FROM plans")
    suspend fun getAllPlanNames(): List<String>

    @Query("UPDATE plans SET pinnedOrder = :order WHERE id = :planId")
    suspend fun updatePinnedOrder(planId: Long, order: Int)

    @Query("SELECT MAX(pinnedOrder) FROM plans WHERE isPinned = 1")
    suspend fun getMaxPinnedOrder(): Int?
}
