package com.example.taxapp.CalendarEvent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A composable that provides a smooth fade transition between screens
 */
@Composable
fun FadeTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 500,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = durationMillis)),
        exit = fadeOut(animationSpec = tween(durationMillis = durationMillis)),
        modifier = modifier
    ) {
        content()
    }
}