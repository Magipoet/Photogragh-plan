package com.photo.plan.data.repository

import com.photo.plan.data.local.dao.PlanDao
import com.photo.plan.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

class PlanRepository(private val planDao: PlanDao) {

    fun getAllPlans(): Flow<List<PlanEntity>> = planDao.getAllPlans()

    fun getPinnedPlans(): Flow<List<PlanEntity>> = planDao.getPinnedPlans()

    suspend fun getPlanById(planId: Long): PlanEntity? = planDao.getPlanById(planId)

    suspend fun insertPlan(plan: PlanEntity): Long = planDao.insertPlan(plan)

    suspend fun updatePlan(plan: PlanEntity) = planDao.updatePlan(plan)

    suspend fun updatePinned(planId: Long, isPinned: Boolean) = planDao.updatePinned(planId, isPinned)

    suspend fun deletePlan(planId: Long) = planDao.deletePlanById(planId)

    suspend fun getAllPlanNames(): List<String> = planDao.getAllPlanNames()

    suspend fun updatePinnedOrder(planId: Long, order: Int) = planDao.updatePinnedOrder(planId, order)

    suspend fun getMaxPinnedOrder(): Int? = planDao.getMaxPinnedOrder()
}
