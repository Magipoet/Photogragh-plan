package com.photo.plan.data.repository

import com.photo.plan.data.local.dao.PlanDao
import com.photo.plan.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

class PlanRepository(private val planDao: PlanDao) {

    fun getAllPlans(): Flow<List<PlanEntity>> = planDao.getAllPlans()

    suspend fun getPlanById(planId: Long): PlanEntity? = planDao.getPlanById(planId)

    suspend fun insertPlan(plan: PlanEntity): Long = planDao.insertPlan(plan)

    suspend fun updatePlan(plan: PlanEntity) = planDao.updatePlan(plan)

    suspend fun deletePlan(planId: Long) = planDao.deletePlanById(planId)
}
