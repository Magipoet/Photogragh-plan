package com.photo.plan.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photo.plan.PhotoPlanApp
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val planRepository = PlanRepository((application as PhotoPlanApp).database.planDao())
    private val sampleDao = (application as PhotoPlanApp).database.sampleDao()

    val plans: StateFlow<List<PlanEntity>> = combine(
        planRepository.getAllPlans(),
        sampleDao.getTotalSampleCount()
    ) { plans, _ -> plans }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _planProgressMap = MutableStateFlow<Map<Long, Pair<Int, Int>>>(emptyMap())
    val planProgressMap: StateFlow<Map<Long, Pair<Int, Int>>> = _planProgressMap

    init {
        viewModelScope.launch {
            plans.collect { planList ->
                val progressMap = mutableMapOf<Long, Pair<Int, Int>>()
                planList.forEach { plan ->
                    val total = sampleDao.getSampleCount(plan.id)
                    val completed = sampleDao.getCompletedCount(plan.id)
                    progressMap[plan.id] = Pair(total, completed)
                }
                _planProgressMap.value = progressMap
            }
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            val sampleRepo = com.photo.plan.data.repository.SampleRepository(
                sampleDao, getApplication()
            )
            sampleRepo.deleteSamplesByPlanId(planId)
            planRepository.deletePlan(planId)
        }
    }
}
