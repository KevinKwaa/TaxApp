package com.example.taxapp.chatbot

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A test screen with a simple draggable element
 * Add this to your Navigation.kt for testing purposes
 */
@Composable
fun TestDraggableScreen() {
    var offsetX by remember { mutableStateOf(100f) }
    var offsetY by remember { mutableStateOf(100f) }

    Log.d("TestDraggable", "Rendering test draggable at $offsetX, $offsetY")

    Box(modifier = Modifier.fillMaxSize()) {
        // A simple blue circle that can be dragged
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(80.dp)
                .background(Color.Blue, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        Log.d("TestDraggable", "Dragged to: $offsetX, $offsetY")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Drag Me",
                color = Color.White
            )
        }
    }
}