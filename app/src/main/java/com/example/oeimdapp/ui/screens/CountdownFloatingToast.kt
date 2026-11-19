package com.example.oeimdapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.oeimdapp.ui.theme.OeiTheme
import kotlinx.coroutines.delay

@Composable
fun CountdownFloatingToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long = 5000L,
    icon: @Composable (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    backgroundColor: Color = OeiTheme.colors.elevatedSurface,
    textColor: Color = OeiTheme.colors.textPrimary,
    borderColor: Color = OeiTheme.colors.borderStrong,
    progressColor: Color = OeiTheme.colors.primaryAction
) {
    var isVisible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(message) {
        isVisible = true
        progress.snapTo(1f)
        try {
            progress.animateTo(0f, animationSpec = tween(durationMs.toInt(), easing = LinearEasing))
            isVisible = false
            delay(300L)
        } finally {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp),
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (icon != null) {
                            icon()
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (actionLabel != null && onAction != null) {
                        TextButton(onClick = {
                            isVisible = false
                            onAction()
                        }) {
                            Text(
                                text = actionLabel,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp),
                    color = progressColor,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
