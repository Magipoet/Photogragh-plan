package com.photo.plan.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
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
import com.photo.plan.ui.theme.Green100
import com.photo.plan.ui.theme.Green500
import com.photo.plan.ui.theme.Green700
import com.photo.plan.ui.theme.White
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    var isUserScrollEnabled by remember { mutableStateOf(true) }
    var dragJustEnded by remember { mutableStateOf(false) }

    var taskBarBounds by remember { mutableStateOf(Rect.Zero) }
    var rootBoxTopLeft by remember { mutableStateOf(Offset.Zero) }
    var taskBarLazyRowTopLeft by remember { mutableStateOf(Offset.Zero) }

    var showCompletedPlans by remember { mutableStateOf(true) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var pendingScrollToPlanId by remember { mutableStateOf<Long?>(null) }
    var dragEndScrollTarget by remember { mutableStateOf<Int?>(null) }

    val taskBarListState = rememberLazyListState()
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val activePlans by remember {
        derivedStateOf {
            plans.filter { plan ->
                val (total, completed) = progressMap[plan.id] ?: (0 to 0)
                total == 0 || completed < total
            }
        }
    }

    val completedPlans by remember {
        derivedStateOf {
            plans.filter { plan ->
                val (total, completed) = progressMap[plan.id] ?: (0 to 0)
                total > 0 && completed >= total
            }
        }
    }

    val pinnedPlansDataVersion by remember {
        derivedStateOf {
            pinnedPlans.map { it.id }.hashCode()
        }
    }

    val itemWidthPx = with(density) { 150.dp.toPx() }
    val itemSpacingPx = with(density) { 8.dp.toPx() }
    val totalItemWidthPx = itemWidthPx + itemSpacingPx

    val targetInsertIndex by remember {
        derivedStateOf {
            if (!isDraggingFromTaskBar || draggingPlanId == null || pinnedPlans.size <= 1) {
                -1
            } else if (!isDragOver) {
                -1
            } else {
                val layoutInfo = taskBarListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val cardPaddingPx = with(density) { 12.dp.toPx() }
                val viewportStartOffset = layoutInfo.viewportStartOffset
                val dropXInContainer = dragPosition.x - taskBarBounds.left - cardPaddingPx + viewportStartOffset
                val draggedIndex = pinnedPlans.indexOfFirst { it.id == draggingPlanId }

                val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
                val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: (pinnedPlans.size - 1)

                val viewportLeft = viewportStartOffset.toFloat()
                val viewportRight = viewportStartOffset + layoutInfo.viewportSize.width.toFloat()

                val targetIdx: Int = when {
                    dropXInContainer <= 0f -> {
                        0
                    }
                    dropXInContainer < viewportLeft -> {
                        val distanceLeft = viewportLeft - dropXInContainer
                        val stepsBack = (distanceLeft / totalItemWidthPx).toInt() + 2
                        (firstVisibleIndex - stepsBack).coerceAtLeast(0)
                    }
                    dropXInContainer > viewportRight -> {
                        val distanceRight = dropXInContainer - viewportRight
                        val stepsForward = (distanceRight / totalItemWidthPx).toInt() + 2
                        (lastVisibleIndex + stepsForward).coerceAtMost(pinnedPlans.size)
                    }
                    else -> {
                        var idx = -1
                        for (item in visibleItems) {
                            val itemCenter = item.offset + item.size / 2f
                            if (dropXInContainer < itemCenter) {
                                idx = item.index
                                break
                            }
                        }
                        if (idx == -1) {
                            idx = lastVisibleIndex + 1
                        }
                        idx
                    }
                }

                val clampedIdx = targetIdx.coerceIn(0, pinnedPlans.size)
                if (draggedIndex >= 0 && clampedIdx > draggedIndex) clampedIdx - 1 else clampedIdx
            }
        }
    }

    val draggingPlan = plans.find { it.id == draggingPlanId }
        ?: pinnedPlans.find { it.id == draggingPlanId }

    LaunchedEffect(isDraggingFromTaskBar, draggingPlanId) {
        if (isDraggingFromTaskBar && draggingPlanId != null) {
            autoScrollJob?.cancel()
            autoScrollJob = launch {
                while (isDraggingFromTaskBar && draggingPlanId != null) {
                    delay(16)
                    if (taskBarBounds != Rect.Zero) {
                        val viewportStartPx = taskBarBounds.left
                        val viewportEndPx = taskBarBounds.right
                        val topBound = taskBarBounds.top - with(density) { 200.dp.toPx() }
                        val bottomBound = taskBarBounds.bottom + with(density) { 200.dp.toPx() }
                        val inYRange = dragPosition.y >= topBound && dragPosition.y <= bottomBound
                        val edgeZone = with(density) { 140.dp.toPx() }
                        val maxScrollStep = with(density) { 18.dp.toPx() }

                        val shouldScrollLeft = inYRange && dragPosition.x < viewportStartPx + edgeZone
                        val shouldScrollRight = inYRange && dragPosition.x > viewportEndPx - edgeZone

                        if (shouldScrollLeft || shouldScrollRight) {
                            val distanceIntoZone = if (shouldScrollLeft) {
                                (viewportStartPx + edgeZone - dragPosition.x).coerceAtLeast(0f)
                            } else {
                                (dragPosition.x - (viewportEndPx - edgeZone)).coerceAtLeast(0f)
                            }
                            val speedFactor = (distanceIntoZone / edgeZone).coerceIn(0f, 1f)
                            val actualStep = maxScrollStep * (0.3f + speedFactor * 0.7f)

                            taskBarListState.scroll {
                                scrollBy(if (shouldScrollLeft) -actualStep else actualStep)
                            }
                        }
                    }
                }
            }
            autoScrollJob?.join()
        } else {
            autoScrollJob?.cancel()
            autoScrollJob = null
        }
    }

    LaunchedEffect(pinnedPlans) {
        val targetIdx = dragEndScrollTarget
        if (targetIdx != null) {
            dragEndScrollTarget = null
            if (targetIdx >= 0 && targetIdx < pinnedPlans.size) {
                try {
                    taskBarListState.scrollToItem(
                        index = targetIdx,
                        scrollOffset = 0
                    )
                } catch (_: Exception) {}
            }
            return@LaunchedEffect
        }

        val targetId = pendingScrollToPlanId
        if (targetId != null) {
            val targetIndex = pinnedPlans.indexOfFirst { it.id == targetId }
            if (targetIndex >= 0) {
                pendingScrollToPlanId = null
                delay(80)
                try {
                    taskBarListState.animateScrollToItem(
                        index = targetIndex.coerceAtLeast(0),
                        scrollOffset = 0
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun isDragOverTaskBar(): Boolean {
        if (draggingPlanId == null) return false
        val previewWidthPx = with(density) { 150.dp.toPx() }
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

    fun handleTaskBarDragEnd() {
        val finalDraggingPlanId = draggingPlanId
        val finalInsertIndex = targetInsertIndex
        val finalIsDragOver = isDragOverTaskBar()
        val currentPinnedPlans = pinnedPlans

        val wasDraggingFromTaskBar = isDraggingFromTaskBar

        isDragOver = false
        dragPosition = Offset.Zero
        dragJustEnded = true
        draggingPlanId = null
        isDraggingFromTaskBar = false
        isUserScrollEnabled = true
        dragEndScrollTarget = null

        scope.launch {
            delay(200)
            dragJustEnded = false
        }

        if (wasDraggingFromTaskBar && finalDraggingPlanId != null) {
            if (finalIsDragOver) {
                val draggedPlan = currentPinnedPlans.find { it.id == finalDraggingPlanId }
                val draggedIdx = draggedPlan?.let { currentPinnedPlans.indexOf(it) } ?: -1
                if (draggedPlan != null && currentPinnedPlans.size > 1) {
                    val otherPinnedPlans = currentPinnedPlans.filter { it.id != finalDraggingPlanId }
                    val targetIdx = if (finalInsertIndex >= 0) finalInsertIndex else draggedIdx
                    val insertIndex = targetIdx.coerceIn(0, otherPinnedPlans.size)
                    if (insertIndex != draggedIdx) {
                        dragEndScrollTarget = insertIndex
                        val newOrder = otherPinnedPlans.toMutableList()
                        newOrder.add(insertIndex, draggedPlan)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.reorderPinnedPlans(newOrder.map { it.id })
                    }
                }
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.unpinPlan(finalDraggingPlanId)
            }
        }
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
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "筛选",
                                tint = Gray500
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (showCompletedPlans)
                                                Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = Green500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("显示已完成策划")
                                    }
                                },
                                onClick = {
                                    showCompletedPlans = !showCompletedPlans
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
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
                            val viewConfiguration = LocalViewConfiguration.current
                            val touchSlop = viewConfiguration.touchSlop
                            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { layoutCoordinates ->
                                        taskBarLazyRowTopLeft = layoutCoordinates.boundsInRoot().topLeft
                                    }
                                    .pointerInput(Unit) {

                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var dragStarted = false
                                            var movedBeyondSlop = false
                                            var longPressTriggered = false
                                            val downPosition = down.position
                                            val downTime = System.currentTimeMillis()

                                            fun tryTriggerLongPress(): Boolean {
                                                if (!isUserScrollEnabled) return false
                                                val layoutInfo = taskBarListState.layoutInfo
                                                val visibleItems = layoutInfo.visibleItemsInfo
                                                val viewportStartOffset = layoutInfo.viewportStartOffset
                                                val xInContent = downPosition.x + viewportStartOffset

                                                var targetPlan: PlanEntity? = null
                                                for (item in visibleItems) {
                                                    val itemEnd = item.offset + item.size
                                                    if (xInContent >= item.offset && xInContent < itemEnd) {
                                                        val idx = item.index
                                                        if (idx >= 0 && idx < pinnedPlans.size) {
                                                            targetPlan = pinnedPlans[idx]
                                                        }
                                                        break
                                                    }
                                                }

                                                if (targetPlan != null) {
                                                    dragStarted = true
                                                    down.consume()
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    isUserScrollEnabled = false
                                                    draggingPlanId = targetPlan.id
                                                    isDraggingFromTaskBar = true
                                                    val globalPos = taskBarLazyRowTopLeft + downPosition
                                                    dragPosition = globalPos
                                                    isDragOver = isDragOverTaskBar()
                                                    return true
                                                }
                                                return false
                                            }

                                            try {
                                                while (true) {
                                                    val currentTime = System.currentTimeMillis()
                                                    val remainingTimeout = (downTime + longPressTimeout) - currentTime
                                                    val waitTimeout = if (!dragStarted && !movedBeyondSlop && !longPressTriggered && remainingTimeout > 0) {
                                                        remainingTimeout.coerceAtMost(16L)
                                                    } else {
                                                        Long.MAX_VALUE
                                                    }

                                                    val result: Pair<PointerEvent?, Long> = if (waitTimeout == Long.MAX_VALUE) {
                                                        awaitPointerEvent() to System.currentTimeMillis()
                                                    } else {
                                                        val ev = withTimeoutOrNull(waitTimeout) { awaitPointerEvent() }
                                                        ev to System.currentTimeMillis()
                                                    }
                                                    val event = result.first
                                                    val nowTime = result.second

                                                    if (event == null) {
                                                        if (!dragStarted && !movedBeyondSlop && !longPressTriggered) {
                                                            if (nowTime - downTime >= longPressTimeout) {
                                                                longPressTriggered = true
                                                                tryTriggerLongPress()
                                                            }
                                                        }
                                                        continue
                                                    }

                                                    val change = event.changes.firstOrNull() ?: break

                                                    if (!dragStarted && !movedBeyondSlop && !longPressTriggered) {
                                                        val dx = change.position.x - downPosition.x
                                                        val dy = change.position.y - downPosition.y
                                                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                                        if (distance > touchSlop) {
                                                            movedBeyondSlop = true
                                                        } else if (currentTime - downTime >= longPressTimeout) {
                                                            longPressTriggered = true
                                                            tryTriggerLongPress()
                                                        }
                                                    }

                                                    if (dragStarted) {
                                                        val delta = change.positionChange()
                                                        change.consume()
                                                        if (delta != Offset.Zero) {
                                                            val globalPos = taskBarLazyRowTopLeft + change.position
                                                            dragPosition = globalPos
                                                            isDragOver = isDragOverTaskBar()
                                                        }
                                                        if (change.changedToUp()) {
                                                            break
                                                        }
                                                    } else {
                                                        if (change.changedToUp()) {
                                                            break
                                                        }
                                                    }
                                                }
                                            } finally {
                                                if (dragStarted) {
                                                    handleTaskBarDragEnd()
                                                }
                                            }
                                        }
                                    }
                            ) {
                                LazyRow(
                                    state = taskBarListState,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    userScrollEnabled = isUserScrollEnabled
                                ) {
                                    items(pinnedPlans, key = { it.id }) { plan ->
                                        val index = pinnedPlans.indexOf(plan)
                                        val isThisPinnedDragging = draggingPlanId == plan.id && isDraggingFromTaskBar
                                        val dragActiveWithTarget = isDraggingFromTaskBar && draggingPlanId != null
                                        val draggedIndex = if (dragActiveWithTarget)
                                            pinnedPlans.indexOfFirst { it.id == draggingPlanId } else -1
                                        val shouldShiftRight = dragActiveWithTarget &&
                                            !isThisPinnedDragging &&
                                            draggedIndex >= 0 &&
                                            targetInsertIndex >= 0 &&
                                            index >= targetInsertIndex &&
                                            index < draggedIndex
                                        val shouldShiftLeft = dragActiveWithTarget &&
                                            !isThisPinnedDragging &&
                                            draggedIndex >= 0 &&
                                            targetInsertIndex >= 0 &&
                                            index > draggedIndex &&
                                            index <= targetInsertIndex
                                        val isNearTargetSlot = dragActiveWithTarget &&
                                            !isThisPinnedDragging &&
                                            targetInsertIndex >= 0 &&
                                            !dragJustEnded &&
                                            (index == targetInsertIndex || index == targetInsertIndex - 1)
                                        val dimmed = dragActiveWithTarget &&
                                            !isThisPinnedDragging &&
                                            !isNearTargetSlot

                                        PinnedPlanItem(
                                            plan = plan,
                                            onClick = { onNavigateToDetail(plan.id) },
                                            onUnpinRequest = { planToUnpin = plan },
                                            isDragging = isThisPinnedDragging,
                                            shiftOffsetPx = when {
                                                shouldShiftRight -> with(density) { 158.dp.toPx() }
                                                shouldShiftLeft -> -with(density) { 158.dp.toPx() }
                                                else -> 0f
                                            },
                                            dimmed = dimmed,
                                            highlighted = isNearTargetSlot,
                                            isDragActive = isDraggingFromTaskBar,
                                            dataVersion = pinnedPlansDataVersion
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

                val isAnyPlanVisible = activePlans.isNotEmpty() || (showCompletedPlans && completedPlans.isNotEmpty())
                if (plans.isEmpty() || !isAnyPlanVisible) {
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

                        if (activePlans.isNotEmpty()) {
                            item {
                                PlanSectionHeader(
                                    title = "进行中",
                                    count = activePlans.size,
                                    isCompleted = false
                                )
                            }
                            items(activePlans, key = { it.id }) { plan ->
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
                                        else {
                                            pendingScrollToPlanId = plan.id
                                            viewModel.pinPlan(plan.id)
                                        }
                                    },
                                    onDragStart = { globalPosition ->
                                        draggingPlanId = plan.id
                                        dragPosition = globalPosition
                                        isDragOver = isDragOverTaskBar()
                                        isDraggingFromTaskBar = false
                                    },
                                    onDrag = { globalPosition ->
                                        dragPosition = globalPosition
                                        isDragOver = isDragOverTaskBar()
                                    },
                                    onDragEnd = {
                                        if (isDragOver && draggingPlanId != null && !plan.isPinned) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            pendingScrollToPlanId = draggingPlanId
                                            viewModel.pinPlan(draggingPlanId!!)
                                        }
                                        draggingPlanId = null
                                        isDraggingFromTaskBar = false
                                        isDragOver = false
                                        dragPosition = Offset.Zero
                                    },
                                    isDragging = isThisDragging
                                )
                            }
                        }

                        if (showCompletedPlans && completedPlans.isNotEmpty()) {
                            if (activePlans.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }
                            item {
                                PlanSectionHeader(
                                    title = "已完成",
                                    count = completedPlans.size,
                                    isCompleted = true
                                )
                            }
                            items(completedPlans, key = { it.id }) { plan ->
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
                                        else {
                                            pendingScrollToPlanId = plan.id
                                            viewModel.pinPlan(plan.id)
                                        }
                                    },
                                    onDragStart = { globalPosition ->
                                        draggingPlanId = plan.id
                                        dragPosition = globalPosition
                                        isDragOver = isDragOverTaskBar()
                                        isDraggingFromTaskBar = false
                                    },
                                    onDrag = { globalPosition ->
                                        dragPosition = globalPosition
                                        isDragOver = isDragOverTaskBar()
                                    },
                                    onDragEnd = {
                                        if (isDragOver && draggingPlanId != null && !plan.isPinned) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            pendingScrollToPlanId = draggingPlanId
                                            viewModel.pinPlan(draggingPlanId!!)
                                        }
                                        draggingPlanId = null
                                        isDraggingFromTaskBar = false
                                        isDragOver = false
                                        dragPosition = Offset.Zero
                                    },
                                    isDragging = isThisDragging
                                )
                            }
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
    isDragging: Boolean,
    shiftOffsetPx: Float = 0f,
    dimmed: Boolean = false,
    highlighted: Boolean = false,
    isDragActive: Boolean = false,
    dataVersion: Int = 0
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    val pinnedShiftAnim = remember(plan.id) { Animatable(0f) }
    val pinnedAlphaAnim = remember(plan.id) { Animatable(1f) }
    val pinnedScaleAnim = remember(plan.id) { Animatable(1f) }

    LaunchedEffect(dataVersion) {
        if (!isDragActive) {
            pinnedShiftAnim.snapTo(0f)
            pinnedAlphaAnim.snapTo(1f)
            pinnedScaleAnim.snapTo(1f)
        }
    }

    LaunchedEffect(isDragActive, shiftOffsetPx, isDragging, dimmed) {
        if (isDragActive) {
            val targetShiftVal = shiftOffsetPx
            val targetAlphaVal = when {
                isDragging -> 0f
                dimmed -> 0.45f
                else -> 1f
            }
            val targetScaleVal = if (isDragging) 0.92f else 1f

            pinnedShiftAnim.snapTo(targetShiftVal)
            pinnedAlphaAnim.snapTo(targetAlphaVal)
            pinnedScaleAnim.snapTo(targetScaleVal)
        } else {
            val resetAnimSpec = tween<Float>(durationMillis = 200)
            val shiftJob = launch { pinnedShiftAnim.animateTo(0f, resetAnimSpec) }
            val alphaJob = launch { pinnedAlphaAnim.animateTo(1f, resetAnimSpec) }
            val scaleJob = launch { pinnedScaleAnim.animateTo(1f, resetAnimSpec) }
            shiftJob.join()
            alphaJob.join()
            scaleJob.join()
        }
    }

    val targetBgColor = if (highlighted && isDragActive && !isDragging)
        Green500.copy(alpha = 0.08f)
    else
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    val targetBorderColor = if (highlighted && isDragActive && !isDragging)
        Green500.copy(alpha = 0.5f)
    else
        Color.Transparent
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 150),
        label = "pinnedCardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "pinnedCardBorder"
    )

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(70.dp)
            .graphicsLayer {
                translationX = pinnedShiftAnim.value
                alpha = pinnedAlphaAnim.value
                scaleX = pinnedScaleAnim.value
                scaleY = pinnedScaleAnim.value
            }
            .then(
                if (highlighted && isDragActive && !isDragging) Modifier.border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
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
private fun PlanSectionHeader(
    title: String,
    count: Int,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = if (isCompleted) Gray500 else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = Gray500
        )
        if (isCompleted) {
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
    val isCompleted = totalCount > 0 && completedCount >= totalCount
    val progressColor by animateColorAsState(
        targetValue = if (progress >= 1f) Green700 else Green500,
        label = "progressColor"
    )
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dragTranslationY = with(density) { 4.dp.toPx() }

    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }

    val cardAlphaAnim = remember(
        key1 = plan.id,
        key2 = isDragging
    ) { Animatable(1f) }
    val cardScaleAnim = remember(
        key1 = plan.id,
        key2 = isDragging
    ) { Animatable(1f) }
    val cardTranslationYAnim = remember(
        key1 = plan.id,
        key2 = isDragging
    ) { Animatable(0f) }

    LaunchedEffect(isDragging, dragTranslationY) {
        if (isDragging) {
            cardAlphaAnim.snapTo(0.3f)
            cardScaleAnim.snapTo(0.95f)
            cardTranslationYAnim.snapTo(dragTranslationY)
        } else {
            val animSpec = tween<Float>(
                durationMillis = 280,
                easing = EaseOutBack
            )
            launch { cardAlphaAnim.animateTo(1f, animSpec) }
            launch { cardScaleAnim.animateTo(1f, animSpec) }
            launch { cardTranslationYAnim.animateTo(0f, animSpec) }
        }
    }

    val cardBgColor by animateColorAsState(
        targetValue = if (isCompleted) Green100.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        label = "cardBgColor",
        animationSpec = tween(durationMillis = 200)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                cardTopLeft = layoutCoordinates.boundsInRoot().topLeft
            }
            .graphicsLayer {
                alpha = if (!isDragging) 1f else cardAlphaAnim.value
                scaleX = if (!isDragging) 1f else cardScaleAnim.value
                scaleY = if (!isDragging) 1f else cardScaleAnim.value
                translationY = if (!isDragging) 0f else cardTranslationYAnim.value
            }
            .then(
                if (isCompleted) Modifier.border(
                    1.5.dp,
                    Green500.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
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
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "已完成",
                                modifier = Modifier.size(18.dp),
                                tint = Green700
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormatter.format(Date(plan.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Green500)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "已完成",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
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
                        color = if (isCompleted) Green700 else Gray700,
                        fontWeight = if (isCompleted) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
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
