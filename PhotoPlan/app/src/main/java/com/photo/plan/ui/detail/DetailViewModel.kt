package com.photo.plan.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photo.plan.PhotoPlanApp
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.data.repository.PlanRepository
import com.photo.plan.data.repository.SampleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val plan: PlanEntity? = null,
    val samples: List<SampleEntity> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val planRepository = PlanRepository((application as PhotoPlanApp).database.planDao())
    private val sampleRepository = SampleRepository(
        (application as PhotoPlanApp).database.sampleDao(),
        application
    )

    private val _plan = MutableStateFlow<PlanEntity?>(null)
    private val _samples = MutableStateFlow<List<SampleEntity>>(emptyList())
    private val _totalCount = MutableStateFlow(0)
    private val _completedCount = MutableStateFlow(0)

    val uiState: StateFlow<DetailUiState> = combine(
        _plan,
        _samples,
        _totalCount,
        _completedCount
    ) { plan, samples, total, completed ->
        DetailUiState(
            plan = plan,
            samples = samples,
            totalCount = total,
            completedCount = completed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun loadPlan(planId: Long) {
        viewModelScope.launch {
            val plan = planRepository.getPlanById(planId)
            _plan.value = plan
        }
        viewModelScope.launch {
            sampleRepository.getSamplesByPlanId(planId).collect { list ->
                _samples.value = list
                _totalCount.value = list.size
                _completedCount.value = list.count { it.isCompleted }
            }
        }
    }

    fun toggleCompleted(sample: SampleEntity) {
        viewModelScope.launch {
            sampleRepository.toggleCompleted(sample.id, sample.isCompleted)
        }
    }
}
