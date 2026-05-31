package com.photo.plan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "samples",
    foreignKeys = [ForeignKey(
        entity = PlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val localPath: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long = System.currentTimeMillis()
)
