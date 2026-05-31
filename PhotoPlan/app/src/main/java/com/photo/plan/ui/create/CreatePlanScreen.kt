package com.photo.plan.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.ui.theme.Gray100
import com.photo.plan.ui.theme.Gray300
import com.photo.plan.ui.theme.Gray500
import com.photo.plan.ui.theme.Green500

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePlanScreen(
    planId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: CreatePlanViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(planId) {
        if (planId != null) {
            viewModel.loadPlan(planId)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> viewModel.addUris(uris) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "编辑策划" else "新建策划") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("策划名称") },
                    placeholder = { Text("例如：2025 夏季婚纱套餐") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "参考样图",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.existingSamples.isNotEmpty()) {
                    Text(
                        text = "已添加 (${state.existingSamples.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.existingSamples.forEach { sample ->
                            ExistingImageItem(
                                sample = sample,
                                onRemove = { viewModel.removeExistingSample(sample) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (state.selectedUris.isNotEmpty()) {
                    Text(
                        text = "待添加 (${state.selectedUris.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.selectedUris.forEachIndexed { index, uri ->
                            NewImageItem(
                                uri = uri,
                                onRemove = { viewModel.removeUri(index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AddPhotoButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            val hasImages = state.selectedUris.isNotEmpty() || state.existingSamples.isNotEmpty()
            Button(
                onClick = { viewModel.savePlan(onNavigateToDetail) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(50.dp),
                enabled = hasImages && !state.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green500,
                    disabledContainerColor = Gray300
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存策划")
                }
            }
        }

        if (state.showNamePrompt) {
            AlertDialog(
                onDismissRequest = viewModel::dismissNamePrompt,
                title = { Text("请填写策划名称") },
                text = { Text("为了更好地管理您的摄影策划，请输入一个策划名称。") },
                confirmButton = {
                    Button(
                        onClick = viewModel::dismissNamePrompt,
                        colors = ButtonDefaults.buttonColors(containerColor = Green500)
                    ) {
                        Text("好的")
                    }
                }
            )
        }
    }
}

@Composable
private fun ExistingImageItem(
    sample: SampleEntity,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = sample.localPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(2.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NewImageItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(2.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray100)
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.AddPhotoAlternate,
            contentDescription = null,
            tint = Green500,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "从相册选择图片",
            style = MaterialTheme.typography.bodyLarge,
            color = Green500
        )
    }
}
