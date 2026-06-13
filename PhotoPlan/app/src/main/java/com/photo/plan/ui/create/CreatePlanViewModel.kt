package com.photo.plan.ui.create

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photo.plan.PhotoPlanApp
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.data.repository.PlanRepository
import com.photo.plan.data.repository.SampleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CreatePlanState(
    val name: String = "",
    val selectedUris: List<Uri> = emptyList(),
    val existingSamples: List<SampleEntity> = emptyList(),
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val planId: Long? = null,
    val showNamePrompt: Boolean = false
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
        viewModelScope.launch {
            val currentState = _state.value
            val current = currentState.selectedUris.toMutableList()
            val existing = currentState.existingSamples

            val distinctNewUris = uris.distinctBy { it.toString() }

            for (uri in distinctNewUris) {
                val isDuplicateInSelected = current.any { it.toString() == uri.toString() }
                if (isDuplicateInSelected) continue

                val isDuplicateInExisting = withContext(Dispatchers.IO) {
                    isUriDuplicateOfExisting(uri, existing)
                }
                if (isDuplicateInExisting) continue

                current.add(uri)
            }

            _state.value = currentState.copy(selectedUris = current)
        }
    }

    private fun isUriDuplicateOfExisting(uri: Uri, existing: List<SampleEntity>): Boolean {
        return try {
            val context = getApplication<Application>()
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (sizeIndex != -1 && nameIndex != -1) {
                        val newSize = it.getLong(sizeIndex)
                        val newName = it.getString(nameIndex)
                        return existing.any { sample ->
                            val file = File(sample.localPath)
                            file.exists() && file.length() == newSize && file.name == newName
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
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

        val hasImages = _state.value.selectedUris.isNotEmpty() || _state.value.existingSamples.isNotEmpty()
        if (!hasImages) return

        if (name.isEmpty()) {
            _state.value = _state.value.copy(showNamePrompt = true)
            return
        }

        _state.value = _state.value.copy(showNamePrompt = false)

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            try {
                val planId = if (_state.value.isEditMode) {
                    val pid = _state.value.planId
                    if (pid != null) {
                        val existing = planRepository.getPlanById(pid)
                        if (existing != null) {
                            planRepository.updatePlan(
                                existing.copy(name = name, updatedAt = System.currentTimeMillis())
                            )
                        }
                        pid
                    } else {
                        planRepository.insertPlan(PlanEntity(name = name))
                    }
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

    fun dismissNamePrompt() {
        _state.value = _state.value.copy(showNamePrompt = false)
    }

    fun getBaseDefaultName(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    suspend fun getDefaultName(): String {
        val baseName = getBaseDefaultName()
        val existingNames = planRepository.getAllPlanNames()

        if (!existingNames.contains(baseName)) {
            return baseName
        }

        var suffix = 1
        while (existingNames.contains("${baseName}_$suffix")) {
            suffix++
        }
        return "${baseName}_$suffix"
    }

    fun saveWithDefaultName(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val defaultName = getDefaultName()
            _state.value = _state.value.copy(name = defaultName, showNamePrompt = false)
            savePlan(onSaved)
        }
    }
}
