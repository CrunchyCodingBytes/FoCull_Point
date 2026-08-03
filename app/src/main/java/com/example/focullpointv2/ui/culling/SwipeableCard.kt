package com.example.focullpointv2.ui.culling

import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.focullpointv2.filemanager.RawThumbnailExtractor
import com.example.focullpointv2.filemanager.SupportedExtensions
import com.example.focullpointv2.model.CullAction
import com.example.focullpointv2.model.PhotoItem
import com.example.focullpointv2.ui.theme.FavoriteGreen
import com.example.focullpointv2.ui.theme.RejectRed
import com.example.focullpointv2.ui.theme.SkipYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * The interactive preview card. Tracks the drag offset to render a live colored
 * border and commits the [CullAction] only when the touch ends past the threshold.
 * Supports double-tap and (while zoomed) pinch-to-zoom + pan; swipes are disabled
 * while zoomed in.
 */
@Composable
fun SwipeableCard(
    item: PhotoItem,
    onAction: (CullAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 140.dp.toPx() }

    // Swipe offset (animatable so it can snap back on release below threshold).
    val swipeOffset = remember(item.id) { Animatable(Offset.Zero, Offset.VectorConverter) }

    // Zoom state.
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var pan by remember(item.id) { mutableStateOf(Offset.Zero) }
    val zoomed = scale > 1f

    val (pendingAction, intensity) = pendingActionFor(swipeOffset.value, thresholdPx)
    val borderColor = when (pendingAction) {
        CullAction.FAVORITE -> FavoriteGreen
        CullAction.REJECT -> RejectRed
        CullAction.SKIP -> SkipYellow
        null -> Color.Transparent
    }.copy(alpha = intensity)

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = swipeOffset.value.x
                translationY = swipeOffset.value.y
                rotationZ = (swipeOffset.value.x / 60f).coerceIn(-12f, 12f)
            }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
            .then(
                if (intensity > 0f) {
                    Modifier.borderStroke(borderColor)
                } else {
                    Modifier
                }
            )
            .pointerInput(item.id) {
                detectTapGestures(
                    onDoubleTap = {
                        // Toggle: double-tap zooms in, and while zoomed it zooms back out
                        // (restoring normal swiping).
                        if (scale > 1f) {
                            scale = 1f
                            pan = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(item.id) {
                // Pinch-to-zoom (two fingers) works at any time; single-finger pan only
                // while already zoomed. Single-finger movement while not zoomed is left
                // unconsumed so the swipe detector below can handle it.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.count { it.pressed } >= 2
                        if (multiTouch || scale > 1f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            if (scale <= 1f) {
                                scale = 1f
                                pan = Offset.Zero
                            } else {
                                pan += panChange
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(item.id, zoomed) {
                if (!zoomed) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                swipeOffset.snapTo(swipeOffset.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            val (action, _) = pendingActionFor(swipeOffset.value, thresholdPx)
                            val past = maxOf(
                                abs(swipeOffset.value.x),
                                abs(swipeOffset.value.y)
                            ) >= thresholdPx
                            if (action != null && past) {
                                onAction(action)
                            } else {
                                scope.launch { swipeOffset.animateTo(Offset.Zero) }
                            }
                        },
                        onDragCancel = {
                            scope.launch { swipeOffset.animateTo(Offset.Zero) }
                        }
                    )
                }
            }
    ) {
        PreviewImage(
            item = item,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                }
        )
    }
}

private fun Modifier.borderStroke(color: Color): Modifier =
    this.then(
        Modifier.border(
            BorderStroke(6.dp, color),
            RoundedCornerShape(20.dp)
        )
    )

/** Determines the pending action and its 0..1 intensity for a drag [offset]. */
private fun pendingActionFor(offset: Offset, thresholdPx: Float): Pair<CullAction?, Float> {
    val ax = abs(offset.x)
    val ay = abs(offset.y)
    if (ax < 8f && ay < 8f) return null to 0f

    return if (ax > ay) {
        val action = if (offset.x > 0) CullAction.FAVORITE else CullAction.REJECT
        action to (ax / thresholdPx).coerceIn(0f, 1f)
    } else {
        if (offset.y < 0) {
            CullAction.SKIP to (ay / thresholdPx).coerceIn(0f, 1f)
        } else {
            null to 0f
        }
    }
}

/** Resolves the Glide model for [item]: a File for regular images, or the embedded
 *  thumbnail bytes for RAW-only items. Safe to call off the main thread. */
private fun resolvePreviewModel(item: PhotoItem): Any {
    val preview = item.previewFile
    return if (SupportedExtensions.isRaw(preview)) {
        RawThumbnailExtractor().extractThumbnail(preview) ?: preview
    } else {
        preview
    }
}

/**
 * Invisibly warms Glide's cache for [item] so it renders instantly once it becomes
 * the current card. Renders nothing.
 */
@Composable
fun PreloadImage(item: PhotoItem) {
    val context = LocalContext.current
    LaunchedEffect(item.id) {
        val model = withContext(Dispatchers.IO) { resolvePreviewModel(item) }
        Glide.with(context).load(model).preload()
    }
}

@Composable
private fun PreviewImage(item: PhotoItem, modifier: Modifier = Modifier) {
    // Resolve the Glide model off the main thread: a File for regular images or the
    // embedded thumbnail bytes for RAW-only items.
    val model by produceState<Any?>(initialValue = null, item.id) {
        value = withContext(Dispatchers.IO) { resolvePreviewModel(item) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (model == null) {
            RawPlaceholder(item)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { imageView ->
                    Glide.with(imageView).load(model).into(imageView)
                }
            )
        }
    }
}

@Composable
private fun RawPlaceholder(item: PhotoItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.previewFile.name,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
