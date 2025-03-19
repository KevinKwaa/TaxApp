package com.example.taxapp.chatbot

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * A simple draggable container for the chat FAB that starts in the bottom right corner
 */
@Composable
fun SimpleDraggableBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Track position with state, start with -1 to detect initial positioning
    var offsetX by remember { mutableStateOf(-1f) }
    var offsetY by remember { mutableStateOf(-1f) }

    // Track parent and element sizes
    var parentSize by remember { mutableStateOf(IntSize(0, 0)) }
    var elementSize by remember { mutableStateOf(IntSize(0, 0)) }

    // Flag to trigger initial positioning
    var needsInitialPosition by remember { mutableStateOf(true) }

    // Calculate initial position once we have both parent and element sizes
    LaunchedEffect(parentSize, elementSize, needsInitialPosition) {
        if (needsInitialPosition && parentSize.width > 0 && elementSize.width > 0) {
            // Position at bottom right with a small margin (16dp ≈ 48px)
            offsetX = (parentSize.width - elementSize.width - 1050f).coerceAtLeast(0f)
            offsetY = (parentSize.height - elementSize.height - 300f).coerceAtLeast(0f)
            needsInitialPosition = false
            Log.d("SimpleDraggableBox", "Initial position set to bottom right: $offsetX, $offsetY")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                parentSize = size
                Log.d("SimpleDraggableBox", "Parent size changed: $size")
            }
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        // Use 0,0 until we have valid coordinates
                        x = if (offsetX >= 0) offsetX.roundToInt() else 0,
                        y = if (offsetY >= 0) offsetY.roundToInt() else 0
                    )
                }
                .onSizeChanged { size ->
                    elementSize = size
                    Log.d("SimpleDraggableBox", "Element size changed: $size")
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        // Simple position update with boundary checks
                        offsetX = (offsetX + dragAmount.x).coerceIn(
                            0f,
                            (parentSize.width - elementSize.width).toFloat().coerceAtLeast(0f)
                        )

                        offsetY = (offsetY + dragAmount.y).coerceIn(
                            0f,
                            (parentSize.height - elementSize.height).toFloat().coerceAtLeast(0f)
                        )

                        Log.d("SimpleDraggableBox", "Dragged to: $offsetX, $offsetY")
                    }
                },
            content = content
        )
    }
}