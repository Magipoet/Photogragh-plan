package com.photo.plan.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
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
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris -> viewModel.addUris(uris) }
    )

    val launchImagePicker: () -> Unit = {
        photoPickerLauncher.launch(arrayOf("image/*"))
    }

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
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val itemSpacing = 8.dp
                        val itemWidth = (maxWidth - itemSpacing * 2) / 3
                        Column(verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
                            state.existingSamples.chunked(3).forEach { rowSamples ->
                                Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
                                    rowSamples.forEach { sample ->
                                        ExistingImageItem(
                                            sample = sample,
                                            itemWidth = itemWidth,
                                            onRemove = { viewModel.removeExistingSample(sample) }
                                        )
                                    }
                                    repeat(3 - rowSamples.size) {
                                        Spacer(modifier = Modifier.width(itemWidth))
                                    }
                                }
                            }
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
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val itemSpacing = 8.dp
                        val itemWidth = (maxWidth - itemSpacing * 2) / 3
                        Column(verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
                            state.selectedUris.chunked(3).forEachIndexed { rowIndex, rowUris ->
                                Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
                                    rowUris.forEachIndexed { colIndex, uri ->
                                        val globalIndex = rowIndex * 3 + colIndex
                                        NewImageItem(
                                            uri = uri,
                                            itemWidth = itemWidth,
                                            onRemove = { viewModel.removeUri(globalIndex) }
                                        )
                                    }
                                    repeat(3 - rowUris.size) {
                                        Spacer(modifier = Modifier.width(itemWidth))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AddPhotoButton(
                    onClick = launchImagePicker
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
                text = {
                    Column {
                        Text("为了更好地管理您的摄影策划，请输入一个策划名称。")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "推荐名称：${state.recommendedName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = viewModel::dismissNamePrompt) {
                            Text("手动编辑")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.saveWithDefaultName(onNavigateToDetail) },
                            colors = ButtonDefaults.buttonColors(containerColor = Green500)
                        ) {
                            Text("使用推荐名称")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ExistingImageItem(
    sample: SampleEntity,
    itemWidth: Dp,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(itemWidth)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = sample.localPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun NewImageItem(
    uri: Uri,
    itemWidth: Dp,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(itemWidth)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
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
            text = "从相册中选择图片（点击多选）",
            style = MaterialTheme.typography.bodyLarge,
            color = Green500
        )
    }
}
