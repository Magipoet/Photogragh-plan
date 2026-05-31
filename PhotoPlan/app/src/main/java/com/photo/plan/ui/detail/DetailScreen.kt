package com.photo.plan.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.ui.theme.CompletedOverlay
import com.photo.plan.ui.theme.Gray100
import com.photo.plan.ui.theme.Gray300
import com.photo.plan.ui.theme.Gray500
import com.photo.plan.ui.theme.Green500
import com.photo.plan.ui.theme.Green700
import com.photo.plan.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    planId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (Long, Int) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(planId) { viewModel.loadPlan(planId) }

    val uiState by viewModel.uiState.collectAsState()
    val plan = uiState.plan
    val samples = uiState.samples
    val totalCount = uiState.totalCount
    val completedCount = uiState.completedCount
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val progressColor by animateColorAsState(
        targetValue = if (progress >= 1f) Green700 else Green500,
        label = "progressColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = plan?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (totalCount > 0) {
                            Text(
                                text = "$completedCount/$totalCount 已完成",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(planId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Gray500)
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
            if (totalCount > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = progressColor,
                    trackColor = Gray300,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (samples.isEmpty()) {
                EmptyDetailState()
            } else {
                val incompleteSamples = samples.filter { !it.isCompleted }
                val completedSamples = samples.filter { it.isCompleted }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (incompleteSamples.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader("待拍摄", incompleteSamples.size)
                        }
                        itemsIndexed(
                            incompleteSamples,
                            key = { _, s -> s.id }
                        ) { _, sample ->
                            SampleCard(
                                sample = sample,
                                globalIndex = samples.indexOf(sample),
                                onClick = { viewModel.toggleCompleted(sample) },
                                onViewImage = { idx -> onNavigateToViewer(planId, idx) }
                            )
                        }
                    }

                    if (completedSamples.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader("已完成", completedSamples.size)
                        }
                        itemsIndexed(
                            completedSamples,
                            key = { _, s -> s.id }
                        ) { _, sample ->
                            SampleCard(
                                sample = sample,
                                globalIndex = samples.indexOf(sample),
                                onClick = { viewModel.toggleCompleted(sample) },
                                onViewImage = { idx -> onNavigateToViewer(planId, idx) }
                            )
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (title == "待拍摄") MaterialTheme.colorScheme.primary else Gray500
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = Gray500
        )
        if (title == "已完成") {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .weight(1f)
                    .background(Gray300)
            )
        }
    }
}

@Composable
private fun SampleCard(
    sample: SampleEntity,
    globalIndex: Int,
    onClick: () -> Unit,
    onViewImage: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onViewImage(globalIndex) },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sample.isCompleted) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sample.isCompleted) Gray100 else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = sample.localPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (sample.isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CompletedOverlay)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Green500),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "已完成",
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = if (sample.isCompleted) Alignment.BottomStart else Alignment.TopEnd
            ) {
                val btnColor = if (sample.isCompleted) Gray500 else Green500
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(btnColor.copy(alpha = 0.9f))
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (sample.isCompleted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "取消完成",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "标记完成",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "暂无样图",
                style = MaterialTheme.typography.titleMedium,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "点击右上角编辑按钮添加参考样图",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500
            )
        }
    }
}
