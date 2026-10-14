package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0.7f) }
    val glowProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        glowProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = LinearEasing)
        )
        delay(1200) // Brief stylish presentation
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
            .clickable { onSplashFinished() } // tap to skip immediately
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle cyber ambient glow in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00F0FF).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width.coerceAtLeast(size.height) * 0.45f
                ),
                radius = size.width.coerceAtLeast(size.height) * 0.45f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scaleAnim.value)
                .padding(24.dp)
        ) {
            // S!pad Emblem
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF00F0FF),
                                Color(0xFFBD00FF),
                                Color(0xFFFF007F),
                                Color(0xFF00F0FF)
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                Text(
                    text = "S!",
                    color = Color(0xFF00F0FF),
                    fontWeight = FontWeight.Black,
                    fontSize = 42.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "S!pad",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 2.sp
            )

            Text(
                text = "Customizable Virtual Gamepad for Samsung TV",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(32.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp),
                color = Color(0xFF00F0FF),
                trackColor = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Made in India 🇮🇳",
                color = Color(0xFFFF9933),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = "By Shubham Kumar",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
