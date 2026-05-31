package com.photo.plan.data.repository

import android.content.Context
import com.photo.plan.data.local.dao.SampleDao
import com.photo.plan.data.local.entity.SampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SampleRepository(
    private val sampleDao: SampleDao,
    private val context: Context
) {

    fun getSamplesByPlanId(planId: Long): Flow<List<SampleEntity>> =
        sampleDao.getSamplesByPlanId(planId)

    suspend fun getSampleCount(planId: Long): Int = sampleDao.getSampleCount(planId)

    suspend fun getCompletedCount(planId: Long): Int = sampleDao.getCompletedCount(planId)

    suspend fun saveImageToInternalStorage(planId: Long, sourceUri: android.net.Uri, sortOrder: Int): SampleEntity {
        return withContext(Dispatchers.IO) {
            val imageDir = File(context.filesDir, "images/$planId")
            if (!imageDir.exists()) imageDir.mkdirs()

            val timeStamp = System.currentTimeMillis()
            val fileName = "sample_${sortOrder}_${timeStamp}.jpg"
            val destFile = File(imageDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val sample = SampleEntity(
                planId = planId,
                localPath = destFile.absolutePath,
                sortOrder = sortOrder
            )
            val id = sampleDao.insertSample(sample)
            sample.copy(id = id)
        }
    }

    suspend fun toggleCompleted(sampleId: Long, currentCompleted: Boolean) {
        sampleDao.updateCompleted(sampleId, !currentCompleted)
    }

    suspend fun deleteSample(sample: SampleEntity) {
        withContext(Dispatchers.IO) {
            val file = File(sample.localPath)
            if (file.exists()) file.delete()
            sampleDao.deleteSampleById(sample.id)
        }
    }

    suspend fun deleteSamplesByPlanId(planId: Long) {
        withContext(Dispatchers.IO) {
            val imageDir = File(context.filesDir, "images/$planId")
            if (imageDir.exists()) imageDir.deleteRecursively()
            sampleDao.deleteSamplesByPlanId(planId)
        }
    }

    suspend fun getMaxSortOrder(planId: Long): Int? =
        sampleDao.getMaxSortOrder(planId)
}
