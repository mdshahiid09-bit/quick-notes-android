package com.quicknotes.shahid.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    val color: Color,
    val angle: Double,
    val speed: Float,
    val size: Float,
    val initialOffset: Offset
)

@Composable
fun ParticleExplosion(
    trigger: Boolean,
    onAnimationEnd: () -> Unit = {},
    centerOffset: Offset = Offset.Zero
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val colors = listOf(
        Color(0xFF006495),
        Color(0xFF8FCDFF),
        Color(0xFF00796B),
        Color(0xFFFFA000),
        Color(0xFFE91E63)
    )

    val particles = remember {
        List(24) {
            Particle(
                color = colors[it % colors.size],
                angle = Random.nextDouble(0.0, 2 * Math.PI),
                speed = Random.nextFloat() * 180f + 60f,
                size = Random.nextFloat() * 6f + 4f,
                initialOffset = centerOffset
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutLinearInEasing)
        )
        onAnimationEnd()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = progress.value
        val alpha = (1f - p).coerceIn(0f, 1f)

        particles.forEach { particle ->
            val distance = particle.speed * p
            val x = particle.initialOffset.x + (Math.cos(particle.angle) * distance).toFloat()
            val y = particle.initialOffset.y + (Math.sin(particle.angle) * distance).toFloat()

            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size * (1f - p * 0.5f),
                center = Offset(x, y)
            )
        }
    }
}
