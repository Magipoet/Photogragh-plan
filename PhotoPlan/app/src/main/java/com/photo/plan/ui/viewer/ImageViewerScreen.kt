package com.photo.plan.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.photo.plan.data.local.entity.SampleEntity
import com.photo.plan.ui.detail.DetailViewModel
import com.photo.plan.ui.theme.White

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (samples.isNotEmpty()) {
                        Text("${pagerState.currentPage + 1} / ${samples.size}")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            if (samples.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val sample = samples.getOrNull(page) ?: return@HorizontalPager
                    ZoomableImage(
                        sample = sample,
                        pagerState = pagerState
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    sample: SampleEntity,
    pagerState: PagerState
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var previousCenter: Offset? = null
                var previousDistance = 0f

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val activeChanges = event.changes.filter { it.pressed }

                    if (activeChanges.isEmpty()) {
                        previousCenter = null
                        previousDistance = 0f
                        if (scale <= 1.01f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                        continue
                    }

                    when {
                        activeChanges.size >= 2 -> {
                            val center = activeChanges.map { it.position }
                                .reduce { a, b -> a + b } / activeChanges.size
                            val distance = (activeChanges[0].position - activeChanges[1].position)
                                .getDistance().coerceAtLeast(1f)

                            if (previousDistance > 1f && previousCenter != null) {
                                val zoom = distance / previousDistance
                                val pan = center - previousCenter
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
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

                            previousDistance = distance
                            previousCenter = center
                            activeChanges.forEach { it.consume() }
                        }
                        activeChanges.size == 1 && scale > 1.01f -> {
                            val change = activeChanges[0]
                            val currentPos = change.position
                            if (previousCenter != null) {
                                val pan = currentPos - previousCenter
                                val maxX = (scale - 1) * size.width / 2
                                val maxY = (scale - 1) * size.height / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            }
                            previousCenter = currentPos
                            previousDistance = 0f
                            change.consume()
                        }
                        else -> {
                            previousCenter = activeChanges[0].position
                            previousDistance = 0f
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
