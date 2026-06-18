package com.photo.plan.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photo.plan.PhotoPlanApp
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.data.repository.PlanRepository
import com.photo.plan.data.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val planRepository = PlanRepository((application as PhotoPlanApp).database.planDao())
    private val sampleDao = (application as PhotoPlanApp).database.sampleDao()

    val plans: StateFlow<List<PlanEntity>> = planRepository.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedPlans: StateFlow<List<PlanEntity>> = planRepository.getPinnedPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planProgressMap: StateFlow<Map<Long, Pair<Int, Int>>> =
        plans
            .map { planList -> planList.map { it.id } }
            .flatMapLatest { planIds ->
                if (planIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val countFlows = planIds.map { planId ->
                        combine(
                            sampleDao.getSampleCountByPlanId(planId),
                            sampleDao.getCompletedCountByPlanId(planId)
                        ) { total, completed ->
                            planId to (total to completed)
                        }
                    }
                    combine(countFlows) { pairs ->
                        pairs.associate { it }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun pinPlan(planId: Long) {
        viewModelScope.launch {
            val maxOrder = planRepository.getMaxPinnedOrder() ?: -1
            val plan = planRepository.getPlanById(planId)
            if (plan != null) {
                planRepository.updatePlan(
                    plan.copy(isPinned = true, pinnedOrder = maxOrder + 1, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    fun unpinPlan(planId: Long) {
        viewModelScope.launch {
            val plan = planRepository.getPlanById(planId)
            if (plan != null) {
                planRepository.updatePlan(
                    plan.copy(isPinned = false, pinnedOrder = 0, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    fun reorderPinnedPlans(planIds: List<Long>) {
        viewModelScope.launch {
            val database = (getApplication() as PhotoPlanApp).database
            database.withTransaction {
                planIds.forEachIndexed { index, planId ->
                    planRepository.updatePinnedOrder(planId, index)
                }
            }
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            val sampleRepo = SampleRepository(sampleDao, getApplication())
            sampleRepo.deleteSamplesByPlanId(planId)
            planRepository.deletePlan(planId)
        }
    }
}
