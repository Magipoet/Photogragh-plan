package com.photo.plan.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photo.plan.data.local.entity.PlanEntity
import com.photo.plan.ui.theme.Gray300
import com.photo.plan.ui.theme.Gray500
import com.photo.plan.ui.theme.Gray700
import com.photo.plan.ui.theme.Green500
import com.photo.plan.ui.theme.Green700
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val plans by viewModel.plans.collectAsState()
    val pinnedPlans by viewModel.pinnedPlans.collectAsState()
    val progressMap by viewModel.planProgressMap.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    var planToUnpin by remember { mutableStateOf<PlanEntity?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    var draggingPlanId by remember { mutableStateOf<Long?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var isDraggingFromTaskBar by remember { mutableStateOf(false) }
    var targetInsertIndex by remember { mutableStateOf(-1) }

    var taskBarBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var rootBoxTopLeft by remember { mutableStateOf(Offset.Zero) }

    val taskBarListState = rememberLazyListState()
    var planIdToEnsureVisible by remember { mutableStateOf<Long?>(null) }

    val draggingPlan = plans.find { it.id == draggingPlanId }
        ?: pinnedPlans.find { it.id == draggingPlanId }

    LaunchedEffect(pinnedPlans) {
        planIdToEnsureVisible?.let { planId ->
            val index = pinnedPlans.indexOfFirst { it.id == planId }
            if (index >= 0) {
                val layoutInfo = taskBarListState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                val visibleIndices = visibleItemsInfo.map { it.index }
                if (index !in visibleIndices) {
                    val firstVisible = visibleIndices.firstOrNull() ?: 0
                    val lastVisible = visibleIndices.lastOrNull() ?: 0
                    if (index < firstVisible) {
                        taskBarListState.animateScrollToItem(index)
                    } else if (index > lastVisible) {
                        taskBarListState.animateScrollToItem(
                            index = (index - (lastVisible - firstVisible)).coerceAtLeast(0)
                        )
                    }
                }
            }
            planIdToEnsureVisible = null
        }
    }

    LaunchedEffect(isDraggingFromTaskBar, draggingPlanId) {
        if (isDraggingFromTaskBar && draggingPlanId != null) {
            while (true) {
                kotlinx.coroutines.delay(12)
                if (taskBarBounds != androidx.compose.ui.geometry.Rect.Zero) {
                    val viewportStartPx = taskBarBounds.left
                    val viewportEndPx = taskBarBounds.right
                    val edgeZone = with(density) { 80.dp.toPx() }
                    val scrollStep = with(density) { 10.dp.toPx() }

                    val shouldScrollLeft = dragPosition.x < viewportStartPx + edgeZone
                    val shouldScrollRight = dragPosition.x > viewportEndPx - edgeZone

                    if (shouldScrollLeft || shouldScrollRight) {
                        val distanceIntoZone = if (shouldScrollLeft) {
                            viewportStartPx + edgeZone - dragPosition.x
                        } else {
                            dragPosition.x - (viewportEndPx - edgeZone)
                        }
                        val speedMultiplier = (distanceIntoZone / edgeZone).coerceIn(0.5f, 3f)
                        val actualStep = scrollStep * speedMultiplier

                        taskBarListState.scroll {
                            scrollBy(if (shouldScrollLeft) -actualStep else actualStep)
                        }
                    }
                }
            }
        }
    }

    fun isDragOverTaskBar(): Boolean {
        if (draggingPlanId == null) return false
        val previewWidthPx = with(density) { 200.dp.toPx() }
        val previewHeightPx = with(density) { 70.dp.toPx() }
        val previewLeft = dragPosition.x - previewWidthPx / 2
        val previewRight = dragPosition.x + previewWidthPx / 2
        val previewTop = dragPosition.y - previewHeightPx / 2
        val previewBottom = dragPosition.y + previewHeightPx / 2

        return previewLeft < taskBarBounds.right &&
            previewRight > taskBarBounds.left &&
            previewTop < taskBarBounds.bottom &&
            previewBottom > taskBarBounds.top
    }

    fun computeTargetInsertIndex(): Int {
        if (!isDraggingFromTaskBar || draggingPlanId == null || pinnedPlans.size <= 1) return -1
        if (!isDragOverTaskBar()) return -1
        val itemWidthPx = with(density) { 150.dp.toPx() }
        val spacingPx = with(density) { 8.dp.toPx() }
        val cardPaddingPx = with(density) { 12.dp.toPx() }
        val dropRelativeX = dragPosition.x - taskBarBounds.left - cardPaddingPx
        val draggedIndex = pinnedPlans.indexOfFirst { it.id == draggingPlanId }
        var targetIndex = ((dropRelativeX + itemWidthPx / 2) / (itemWidthPx + spacingPx)).toInt()
            .coerceIn(0, pinnedPlans.size)
        if (draggedIndex >= 0 && targetIndex > draggedIndex) targetIndex -= 1
        return targetIndex
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "摄影策划",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Green500,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建策划")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onGloballyPositioned { layoutCoordinates ->
                    rootBoxTopLeft = layoutCoordinates.boundsInRoot().topLeft
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .then(
                            if (isDragOver && !isDraggingFromTaskBar)
                                Modifier.border(2.dp, Green500, RoundedCornerShape(12.dp))
                            else if (isDraggingFromTaskBar)
                                Modifier.border(2.dp, Green500.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .onGloballyPositioned { layoutCoordinates ->
                            taskBarBounds = layoutCoordinates.boundsInRoot()
                        },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Green500
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "任务栏",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "长按拖拽策划卡片至此可添加，任务栏内可长按拖动排序",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (pinnedPlans.isNotEmpty()) {
                            LazyRow(
                                state = taskBarListState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pinnedPlans, key = { it.id }) { plan ->
                                    val index = pinnedPlans.indexOf(plan)
                                    val isThisPinnedDragging = draggingPlanId == plan.id && isDraggingFromTaskBar
                                    val draggedIndex = if (isDraggingFromTaskBar && draggingPlanId != null)
                                        pinnedPlans.indexOfFirst { it.id == draggingPlanId } else -1
                                    val shouldShiftRight = isDraggingFromTaskBar &&
                                        targetInsertIndex >= 0 &&
                                        draggedIndex >= 0 &&
                                        !isThisPinnedDragging &&
                                        index >= targetInsertIndex &&
                                        index < draggedIndex
                                    val shouldShiftLeft = isDraggingFromTaskBar &&
                                        targetInsertIndex >= 0 &&
                                        draggedIndex >= 0 &&
                                        !isThisPinnedDragging &&
                                        index > draggedIndex &&
                                        index < targetInsertIndex
                                    val isTargetSlot = isDraggingFromTaskBar &&
                                        targetInsertIndex == index &&
                                        draggedIndex != index
                                    val isNearTargetSlot = isDraggingFromTaskBar &&
                                        (targetInsertIndex == index || targetInsertIndex == index + 1) &&
                                        !isThisPinnedDragging
                                    val dimmed = isDraggingFromTaskBar &&
                                        !isThisPinnedDragging &&
                                        !isNearTargetSlot

                                    Box {
                                        if (isTargetSlot && targetInsertIndex >= 0 && targetInsertIndex <= pinnedPlans.size) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .offset { IntOffset((-with(density) { 84.dp.toPx() }).toInt(), 0) }
                                                    .width(150.dp)
                                                    .height(70.dp)
                                                    .zIndex(5f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            color = Green500.copy(alpha = 0.12f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 2.dp,
                                                            color = Green500.copy(alpha = 0.6f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .offset { IntOffset(-1, 0) }
                                                        .width(3.dp)
                                                        .height(54.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Green500)
                                                        .graphicsLayer {
                                                            shadowElevation = 8f
                                                        }
                                                )
                                            }
                                        }
                                        if (index == pinnedPlans.lastIndex &&
                                            isDraggingFromTaskBar &&
                                            targetInsertIndex == pinnedPlans.size) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .offset { IntOffset(with(density) { 8.dp.toPx() }.toInt(), 0) }
                                                    .width(150.dp)
                                                    .height(70.dp)
                                                    .zIndex(5f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            color = Green500.copy(alpha = 0.12f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 2.dp,
                                                            color = Green500.copy(alpha = 0.6f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .offset { IntOffset(-1, 0) }
                                                        .width(3.dp)
                                                        .height(54.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Green500)
                                                        .graphicsLayer {
                                                            shadowElevation = 8f
                                                        }
                                                )
                                            }
                                        }
                                        PinnedPlanItem(
                                            plan = plan,
                                            onClick = { onNavigateToDetail(plan.id) },
                                            onUnpinRequest = { planToUnpin = plan },
                                            onDragStart = { globalPosition ->
                                                draggingPlanId = plan.id
                                                dragPosition = globalPosition
                                                isDraggingFromTaskBar = true
                                                isDragOver = isDragOverTaskBar()
                                                targetInsertIndex = computeTargetInsertIndex()
                                            },
                                            onDrag = { globalPosition ->
                                                dragPosition = globalPosition
                                                isDragOver = isDragOverTaskBar()
                                                targetInsertIndex = computeTargetInsertIndex()
                                            },
                                            onDragEnd = {
                                                if (isDragOver && draggingPlanId != null) {
                                                    val itemWidthPx = with(density) { 150.dp.toPx() }
                                                    val spacingPx = with(density) { 8.dp.toPx() }
                                                    val cardPaddingPx = with(density) { 12.dp.toPx() }
                                                    val dropRelativeX = dragPosition.x - taskBarBounds.left - cardPaddingPx
                                                    val targetGapIndex = ((dropRelativeX + itemWidthPx / 2) / (itemWidthPx + spacingPx)).toInt()
                                                        .coerceIn(0, pinnedPlans.size)
                                                    val otherPinnedPlans = pinnedPlans.filter { it.id != draggingPlanId }
                                                    val draggedPlan = pinnedPlans.find { it.id == draggingPlanId }
                                                    val draggedIdx = draggedPlan?.let { pinnedPlans.indexOf(it) } ?: -1
                                                    if (draggedPlan != null) {
                                                        val newOrder = otherPinnedPlans.toMutableList()
                                                        var insertIndex = targetGapIndex
                                                        if (draggedIdx >= 0 && insertIndex > draggedIdx) insertIndex -= 1
                                                        insertIndex = insertIndex.coerceAtMost(newOrder.size)
                                                        if (insertIndex != draggedIdx) {
                                                            newOrder.add(insertIndex, draggedPlan)
                                                            planIdToEnsureVisible = draggingPlanId
                                                            viewModel.reorderPinnedPlans(newOrder.map { it.id })
                                                        }
                                                    }
                                                } else if (draggingPlanId != null) {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.unpinPlan(draggingPlanId!!)
                                                }
                                                draggingPlanId = null
                                                isDraggingFromTaskBar = false
                                                isDragOver = false
                                                dragPosition = Offset.Zero
                                                targetInsertIndex = -1
                                            },
                                            isDragging = isThisPinnedDragging,
                                            shiftOffsetPx = when {
                                                shouldShiftRight -> with(density) { 158.dp.toPx() }
                                                shouldShiftLeft -> -with(density) { 158.dp.toPx() }
                                                else -> 0f
                                            },
                                            dimmed = dimmed,
                                            highlighted = isNearTargetSlot
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Gray300
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "暂无任务",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                    }
                }

                if (plans.isEmpty()) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        items(plans, key = { it.id }) { plan ->
                            val (total, completed) = progressMap[plan.id] ?: (0 to 0)
                            val isThisDragging = draggingPlanId == plan.id
                            PlanCard(
                                plan = plan,
                                totalCount = total,
                                completedCount = completed,
                                onClick = { onNavigateToDetail(plan.id) },
                                onEdit = { onNavigateToEdit(plan.id) },
                                onDelete = { viewModel.deletePlan(plan.id) },
                                onPin = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (plan.isPinned) viewModel.unpinPlan(plan.id)
                                    else viewModel.pinPlan(plan.id)
                                },
                                onDragStart = { globalPosition ->
                                    draggingPlanId = plan.id
                                    dragPosition = globalPosition
                                    isDragOver = isDragOverTaskBar()
                                    isDraggingFromTaskBar = false
                                    targetInsertIndex = -1
                                },
                                onDrag = { globalPosition ->
                                    dragPosition = globalPosition
                                    isDragOver = isDragOverTaskBar()
                                },
                                onDragEnd = {
                                    if (isDragOver && draggingPlanId != null && !plan.isPinned) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.pinPlan(draggingPlanId!!)
                                    }
                                    draggingPlanId = null
                                    isDraggingFromTaskBar = false
                                    isDragOver = false
                                    dragPosition = Offset.Zero
                                    targetInsertIndex = -1
                                },
                                isDragging = isThisDragging
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            if (draggingPlan != null) {
                val previewWidthPx = with(density) { 150.dp.toPx() }
                val previewHeightPx = with(density) { 70.dp.toPx() }
                Box(
                    modifier = Modifier
                        .zIndex(1000f)
                        .width(150.dp)
                        .height(70.dp)
                        .offset {
                            val relativeX = dragPosition.x - rootBoxTopLeft.x
                            val relativeY = dragPosition.y - rootBoxTopLeft.y
                            IntOffset(
                                x = (relativeX - previewWidthPx / 2).toInt(),
                                y = (relativeY - previewHeightPx / 2).toInt()
                            )
                        }
                        .graphicsLayer(
                            scaleX = 1f,
                            scaleY = 1f,
                            alpha = 0.95f,
                            shadowElevation = 8f,
                            rotationZ = 0f,
                            clip = true
                        )
                        .then(
                            if (isDraggingFromTaskBar && isDragOverTaskBar())
                                Modifier.border(3.dp, Green500, RoundedCornerShape(10.dp))
                            else if (isDragOver && !isDraggingFromTaskBar)
                                Modifier.border(3.dp, Green500, RoundedCornerShape(10.dp))
                            else if (isDraggingFromTaskBar)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Green500
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = draggingPlan.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
                            Text(
                                text = dateFormatter.format(Date(draggingPlan.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    planToUnpin?.let { plan ->
        UnpinConfirmDialog(
            planName = plan.name,
            onConfirm = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.unpinPlan(plan.id)
                planToUnpin = null
            },
            onDismiss = { planToUnpin = null }
        )
    }
}

@Composable
private fun UnpinConfirmDialog(
    planName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移出任务栏", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                text = "确定要将「$planName」移出任务栏吗？",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("移出任务栏", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Gray700)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedPlanItem(
    plan: PlanEntity,
    onClick: () -> Unit,
    onUnpinRequest: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    isDragging: Boolean,
    shiftOffsetPx: Float = 0f,
    dimmed: Boolean = false,
    highlighted: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var itemTopLeft by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    val shiftAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(shiftOffsetPx) {
        shiftAnim.animateTo(
            targetValue = shiftOffsetPx,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    val targetAlpha = when {
        isDragging -> 0f
        dimmed -> 0.45f
        else -> 1f
    }
    LaunchedEffect(targetAlpha) {
        alphaAnim.animateTo(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 180)
        )
    }

    val bgColor by animateColorAsState(
        targetValue = if (highlighted)
            Green500.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        animationSpec = tween(durationMillis = 150),
        label = "pinnedCardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (highlighted)
            Green500.copy(alpha = 0.5f)
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "pinnedCardBorder"
    )

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(70.dp)
            .onGloballyPositioned { layoutCoordinates ->
                itemTopLeft = layoutCoordinates.boundsInRoot().topLeft
            }
            .graphicsLayer {
                translationX = shiftAnim.value
                alpha = alphaAnim.value
                scaleX = 1f
                scaleY = 1f
            }
            .then(
                if (highlighted) Modifier.border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startPosition ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart(itemTopLeft + startPosition)
                    },
                    onDrag = { change, _ ->
                        onDrag(itemTopLeft + change.position)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 1.dp,
            focusedElevation = 1.dp,
            hoveredElevation = 1.dp,
            draggedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onUnpinRequest,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "移出任务栏",
                        modifier = Modifier.size(14.dp),
                        tint = Green500
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
            Text(
                text = dateFormatter.format(Date(plan.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Gray300
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "还没有策划",
                style = MaterialTheme.typography.titleMedium,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "点击右下角 + 创建你的第一个拍摄策划",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanEntity,
    totalCount: Int,
    completedCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    isDragging: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val progressColor by animateColorAsState(
        targetValue = if (progress >= 1f) Green700 else Green500,
        label = "progressColor"
    )
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dragTranslationY = with(density) { 4.dp.toPx() }

    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                cardTopLeft = layoutCoordinates.boundsInRoot().topLeft
            }
            .graphicsLayer {
                alpha = if (isDragging) 0.3f else 1f
                scaleX = if (isDragging) 0.95f else 1f
                scaleY = if (isDragging) 0.95f else 1f
                translationY = if (isDragging) dragTranslationY else 0f
            }
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startPosition ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart(cardTopLeft + startPosition)
                    },
                    onDrag = { change, _ ->
                        onDrag(cardTopLeft + change.position)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (plan.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Green500
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormatter.format(Date(plan.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Gray500)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (plan.isPinned) "从任务栏移除" else "添加到任务栏") },
                            onClick = { showMenu = false; onPin() },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (plan.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = Green500
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = Gray300,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$completedCount/$totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700,
                        fontSize = 13.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无样图",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}
