package com.photo.plan.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.ui.detail.DetailViewModel
import com.photo.plan.ui.theme.White
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    planId: Long,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    detailViewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(planId) { detailViewModel.loadPlan(planId) }
    val uiState by detailViewModel.uiState.collectAsState()
    val samples = uiState.samples

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { samples.size }
    )

    LaunchedEffect(samples.size) {
        if (samples.isNotEmpty()) {
            val targetPage = initialIndex.coerceIn(0, samples.size - 1)
            pagerState.scrollToPage(targetPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (samples.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val sample = samples.getOrNull(page) ?: return@HorizontalPager
                ZoomableImage(sample = sample)
            }
        }

        TopAppBar(
            title = {
                if (samples.isNotEmpty()) {
                    Text(
                        "${pagerState.currentPage + 1} / ${samples.size}",
                        color = White
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                titleContentColor = White
            ),
            modifier = Modifier.statusBarsPadding()
        )
    }
}

@Composable
private fun ZoomableImage(sample: SampleEntity) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                var lastTouchCount = 0
                var lastDistance = 0f
                var lastCenter = Offset.Zero

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val pressedChanges = changes.filter { c -> c.pressed }
                        val touchCount = pressedChanges.size

                        when (event.type) {
                            PointerEventType.Press,
                            PointerEventType.Move -> {
                                if (touchCount >= 2) {
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
                                }

                                lastTouchCount = touchCount
                            }
                            PointerEventType.Release -> {
                                if (touchCount == 0) {
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
                    translationY = offsetY
                },
            contentScale = ContentScale.Fit
        )
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
