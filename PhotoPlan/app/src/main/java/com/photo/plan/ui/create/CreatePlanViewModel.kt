package com.photo.plan.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photo.plan.PhotoPlanApp
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.data.repository.PlanRepository
import com.photo.plan.data.repository.SampleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CreatePlanState(
    val name: String = "",
    val selectedUris: List<Uri> = emptyList(),
    val existingSamples: List<SampleEntity> = emptyList(),
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val planId: Long? = null
)

class CreatePlanViewModel(application: Application) : AndroidViewModel(application) {
    private val planRepository = PlanRepository((application as PhotoPlanApp).database.planDao())
    private val sampleRepository = SampleRepository(
        (application as PhotoPlanApp).database.sampleDao(),
        application
    )

    private val _state = MutableStateFlow(CreatePlanState())
    val state: StateFlow<CreatePlanState> = _state

    fun loadPlan(planId: Long) {
        viewModelScope.launch {
            val plan = planRepository.getPlanById(planId) ?: return@launch
            val samples = sampleRepository.getSamplesByPlanId(planId)
                .collect { list ->
                    _state.value = _state.value.copy(
                        name = plan.name,
                        existingSamples = list,
                        isEditMode = true,
                        planId = planId
                    )
                }
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun addUris(uris: List<Uri>) {
        val current = _state.value.selectedUris.toMutableList()
        current.addAll(uris)
        _state.value = _state.value.copy(selectedUris = current)
    }

    fun removeUri(index: Int) {
        val current = _state.value.selectedUris.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _state.value = _state.value.copy(selectedUris = current)
        }
    }

    fun removeExistingSample(sample: SampleEntity) {
        viewModelScope.launch {
            sampleRepository.deleteSample(sample)
        }
    }

    fun savePlan(onSaved: (Long) -> Unit) {
        val name = _state.value.name.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            try {
                val planId = if (_state.value.isEditMode && _state.value.planId != null) {
                    val pid = _state.value.planId
                    val existing = planRepository.getPlanById(pid)!!
                    planRepository.updatePlan(
                        existing.copy(name = name, updatedAt = System.currentTimeMillis())
                    )
                    pid
                } else {
                    planRepository.insertPlan(PlanEntity(name = name))
                }

                val maxSort = sampleRepository.getMaxSortOrder(planId) ?: -1
                val uris = _state.value.selectedUris
                uris.forEachIndexed { index, uri ->
                    sampleRepository.saveImageToInternalStorage(
                        planId = planId,
                        sourceUri = uri,
                        sortOrder = maxSort + index + 1
                    )
                }

                onSaved(planId)
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}
