package com.photo.plan.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.ui.theme.CompletedOverlay
import com.photo.plan.ui.theme.Gray100
import com.photo.plan.ui.theme.Gray300
import com.photo.plan.ui.theme.Gray500
import com.photo.plan.ui.theme.Gray700
import com.photo.plan.ui.theme.Green500
import com.photo.plan.ui.theme.Green700
import com.photo.plan.ui.theme.White
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlin.math.sqrt

private sealed class ViewerState {
    object Closed : ViewerState()
    data class Open(val initialIndex: Int, val filterCompleted: Boolean) : ViewerState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    planId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (Long, Int, Boolean) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(planId) { viewModel.loadPlan(planId) }

    val uiState by viewModel.uiState.collectAsState()
    val plan = uiState.plan
    val samples = uiState.samples
    val totalCount = uiState.totalCount
    val completedCount = uiState.completedCount
    val isGridView = uiState.isGridView
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val progressColor by animateColorAsState(
        targetValue = if (progress >= 1f) Green700 else Green500,
        label = "progressColor"
    )

    var editingSample by remember { mutableStateOf<SampleEntity?>(null) }
    var viewerState by remember { mutableStateOf<ViewerState>(ViewerState.Closed) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        IconButton(onClick = { viewModel.toggleLayout() }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                                contentDescription = if (isGridView) "切换列表" else "切换网格",
                                tint = Gray500
                            )
                        }
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

                    if (isGridView) {
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
                                ) { index, sample ->
                                    SampleGridCard(
                                        sample = sample,
                                        globalIndex = index,
                                        onClick = { viewModel.toggleCompleted(sample) },
                                        onViewImage = { viewerState = ViewerState.Open(it, false) },
                                        onEditComment = { editingSample = sample }
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
                                ) { index, sample ->
                                    SampleGridCard(
                                        sample = sample,
                                        globalIndex = index,
                                        onClick = { viewModel.toggleCompleted(sample) },
                                        onViewImage = { viewerState = ViewerState.Open(it, true) },
                                        onEditComment = { editingSample = sample }
                                    )
                                }
                            }

                            item(span = { GridItemSpan(2) }) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (incompleteSamples.isNotEmpty()) {
                                item { SectionHeader("待拍摄", incompleteSamples.size) }
                                itemsIndexed(
                                    incompleteSamples,
                                    key = { _, s -> s.id }
                                ) { index, sample ->
                                    SampleListCard(
                                        sample = sample,
                                        globalIndex = index,
                                        onClick = { viewModel.toggleCompleted(sample) },
                                        onViewImage = { viewerState = ViewerState.Open(it, false) },
                                        onEditComment = { editingSample = sample }
                                    )
                                }
                            }

                            if (completedSamples.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                                item { SectionHeader("已完成", completedSamples.size) }
                                itemsIndexed(
                                    completedSamples,
                                    key = { _, s -> s.id }
                                ) { index, sample ->
                                    SampleListCard(
                                        sample = sample,
                                        globalIndex = index,
                                        onClick = { viewModel.toggleCompleted(sample) },
                                        onViewImage = { viewerState = ViewerState.Open(it, true) },
                                        onEditComment = { editingSample = sample }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }

        val openState = viewerState as? ViewerState.Open
        if (openState != null) {
            FullscreenImageViewer(
                samples = samples,
                initialIndex = openState.initialIndex,
                filterCompleted = openState.filterCompleted,
                onDismiss = { viewerState = ViewerState.Closed }
            )
        }
    }

    editingSample?.let { sample ->
        CommentEditDialog(
            sample = sample,
            onDismiss = { editingSample = null },
            onSave = { newComment ->
                viewModel.updateComment(sample.id, newComment)
                editingSample = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FullscreenImageViewer(
    samples: List<SampleEntity>,
    initialIndex: Int,
    filterCompleted: Boolean,
    onDismiss: () -> Unit
) {
    val filteredSamples = samples.filter { it.isCompleted == filterCompleted }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { filteredSamples.size }
    )

    LaunchedEffect(filteredSamples.size) {
        if (filteredSamples.isNotEmpty()) {
            val targetPage = initialIndex.coerceIn(0, filteredSamples.size - 1)
            pagerState.scrollToPage(targetPage)
        }
    }

    var bgAlpha by remember { mutableFloatStateOf(1f) }
    var topBarAlpha by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
    ) {
        if (filteredSamples.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                val sample = filteredSamples.getOrNull(page) ?: return@HorizontalPager
                ZoomableImageOverlay(
                    sample = sample,
                    onDismiss = onDismiss,
                    onBgAlphaChange = { newAlpha ->
                        bgAlpha = newAlpha
                        topBarAlpha = newAlpha
                    }
                )
            }
        }

        TopAppBar(
            title = {
                if (filteredSamples.isNotEmpty()) {
                    Text(
                        "${pagerState.currentPage + 1} / ${filteredSamples.size}",
                        color = White
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f * topBarAlpha),
                titleContentColor = White
            ),
            modifier = Modifier.statusBarsPadding()
        )
    }
}

@Composable
private fun ZoomableImageOverlay(
    sample: SampleEntity,
    onDismiss: () -> Unit,
    onBgAlphaChange: (Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dragYAnimatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dragThreshold = 200f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var lastTouchCount = 0
                var lastDistance = 0f
                var lastCenter = Offset.Zero
                var isVerticalDrag = false
                var isHorizontalDrag = false
                var dragStartX = 0f
                var dragStartY = 0f

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val pressedChanges = changes.filter { c -> c.pressed }
                        val touchCount = pressedChanges.size

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (touchCount == 1) {
                                    dragStartX = pressedChanges[0].position.x
                                    dragStartY = pressedChanges[0].position.y
                                    scope.launch {
                                        dragYAnimatable.stop()
                                        dragYAnimatable.snapTo(dragYAnimatable.value)
                                    }
                                }
                                lastTouchCount = touchCount
                            }
                            PointerEventType.Move -> {
                                if (touchCount >= 2) {
                                    isVerticalDrag = false
                                    isHorizontalDrag = false
                                    val p1 = pressedChanges[0].position
                                    val p2 = pressedChanges[1].position
                                    val currentDistance = distanceBetween(p1, p2)
                                    val currentCenter = centerOf(p1, p2)

                                    if (lastTouchCount >= 2 && lastDistance > 0f) {
                                        val zoom = currentDistance / lastDistance
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        val pan = currentCenter - lastCenter

                                        scale = newScale
                                        if (newScale > 1.01f) {
                                            val maxX = (newScale - 1) * size.width / 2
                                            val maxY = (newScale - 1) * size.height / 2
                                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }

                                    lastDistance = currentDistance
                                    lastCenter = currentCenter
                                    changes.forEach { it.consume() }
                                } else if (touchCount == 1 && scale > 1.01f) {
                                    val change = pressedChanges[0]
                                    if (lastTouchCount == 1) {
                                        val pan = change.position - change.previousPosition
                                        val maxX = (scale - 1) * size.width / 2
                                        val maxY = (scale - 1) * size.height / 2
                                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                    }
                                    change.consume()
                                } else if (touchCount == 1 && scale <= 1.01f) {
                                    val change = pressedChanges[0]
                                    val currentX = change.position.x
                                    val currentY = change.position.y
                                    val totalDx = currentX - dragStartX
                                    val totalDy = currentY - dragStartY

                                    if (!isVerticalDrag && !isHorizontalDrag) {
                                        if (kotlin.math.abs(totalDy) > 20f && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx)) {
                                            isVerticalDrag = true
                                        } else if (kotlin.math.abs(totalDx) > 20f) {
                                            isHorizontalDrag = true
                                        }
                                    }

                                    if (isVerticalDrag && totalDy > 0f) {
                                        scope.launch {
                                            dragYAnimatable.snapTo(totalDy)
                                        }
                                        onBgAlphaChange(
                                            (1f - totalDy / (size.height * 1.5f)).coerceIn(0f, 1f)
                                        )
                                        change.consume()
                                    }
                                }

                                lastTouchCount = touchCount
                            }
                            PointerEventType.Release -> {
                                if (touchCount == 0) {
                                    val currentDragY = dragYAnimatable.value
                                    if (isVerticalDrag && currentDragY > dragThreshold) {
                                        scope.launch {
                                            val targetY = size.height.toFloat()
                                            val animJob1 = launch {
                                                dragYAnimatable.animateTo(
                                                    targetValue = targetY,
                                                    animationSpec = tween(durationMillis = 220)
                                                )
                                            }
                                            onBgAlphaChange(0f)
                                            joinAll(animJob1)
                                            onDismiss()
                                        }
                                    } else {
                                        scope.launch {
                                            val animJob1 = launch {
                                                dragYAnimatable.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = tween(durationMillis = 200)
                                                )
                                            }
                                            joinAll(animJob1)
                                            onBgAlphaChange(1f)
                                        }
                                    }
                                    isVerticalDrag = false
                                    isHorizontalDrag = false
                                    lastTouchCount = 0
                                    lastDistance = 0f
                                    if (scale <= 1.01f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            else -> { lastTouchCount = touchCount }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = sample.localPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY + dragYAnimatable.value
                },
            contentScale = ContentScale.Fit
        )
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
private fun SampleGridCard(
    sample: SampleEntity,
    globalIndex: Int,
    onClick: () -> Unit,
    onViewImage: (Int) -> Unit,
    onEditComment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onViewImage(globalIndex) },
                contentScale = ContentScale.Crop
            )

            if (sample.isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CompletedOverlay)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = if (sample.isCompleted) Alignment.BottomEnd else Alignment.TopEnd
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
                            Icons.Filled.Close,
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

            if (sample.comment.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(8.dp)
                        .clickable { onEditComment() }
                ) {
                    Text(
                        text = sample.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = White,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleListCard(
    sample: SampleEntity,
    globalIndex: Int,
    onClick: () -> Unit,
    onViewImage: (Int) -> Unit,
    onEditComment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sample.isCompleted) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sample.isCompleted) Gray100 else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewImage(globalIndex) }
            ) {
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
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    val btnColor = if (sample.isCompleted) Gray500 else Green500
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(btnColor.copy(alpha = 0.9f))
                            .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sample.isCompleted) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "取消完成",
                                tint = White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "标记完成",
                                tint = White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 ${globalIndex + 1} 张",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        if (sample.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Green500)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "已完成",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = White,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Gray100)
                            .clickable { onEditComment() }
                            .padding(8.dp)
                    ) {
                        if (sample.comment.isEmpty()) {
                            Text(
                                text = "点击添加注释...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        } else {
                            Text(
                                text = sample.comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray700
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onEditComment() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑注释",
                            modifier = Modifier.size(16.dp),
                            tint = Gray500
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (sample.comment.isEmpty()) "添加注释" else "编辑注释",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentEditDialog(
    sample: SampleEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var commentText by remember { mutableStateOf(sample.comment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "编辑注释", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("输入注释内容...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    singleLine = false,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(commentText) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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

private fun distanceBetween(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun centerOf(a: Offset, b: Offset): Offset {
    return Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
}
