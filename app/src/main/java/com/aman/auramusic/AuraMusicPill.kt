package com.aman.auramusic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.util.ArtworkExtractor
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AuraMusicPill(
    song: Song?,
    isPlaying: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier,
    sizeScale: Float = 1.0f,
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    if (song == null) return

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = LocalContext.current

    val artworkModel = remember(song.id, song.uri, song.artworkUri) {
        if (song.id == -1L && song.uri.isNotBlank()) {
            ArtworkExtractor.getArtwork(context, song.uri)
        } else {
            song.artworkUri
        }
    }
    
    // Auto-collapse logic: return to minimal state after 5 seconds
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            kotlinx.coroutines.delay(5000L)
            onExpandToggle()
        }
    }

    val animatedWidth by animateDpAsState(
        targetValue = when {
            isExpanded -> 312.dp // 260dp * 1.2
            isPlaying -> 55.dp * sizeScale // Reduced from 60dp
            else -> 22.dp * sizeScale // Reduced from 24dp for ultra-minimal look
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillWidth",
    )

    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 62.dp else 22.dp * sizeScale, // 52dp * 1.2 ≈ 62dp
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillHeight",
    )

    val animatedCorner by animateDpAsState(
        targetValue = if (isExpanded) 20.dp else 11.dp * sizeScale, // 16dp * 1.2 ≈ 20dp
        label = "pillCorner"
    )

    // Invisible larger container for better touch target
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp) // Large hit area without pushing content
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onExpandToggle()
            },
        contentAlignment = Alignment.TopCenter // Pill stays at absolute top of window
    ) {
        Surface(
            modifier = Modifier
                .width(animatedWidth)
                .height(animatedHeight),
            shape = RoundedCornerShape(animatedCorner),
            color = Color.Black.copy(alpha = 0.96f),
            contentColor = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "pillContent"
            ) { expanded ->
                if (!expanded) {
                    // Minimal State
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp * sizeScale),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp * sizeScale) // Smaller artwork size (18dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = artworkModel,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (song.artworkUri == null && song.id != -1L) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(9.dp * sizeScale), tint = Color.White.copy(0.6f))
                            }
                        }

                        if (isPlaying) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(4.dp * sizeScale))
                                AuraVisualizer(
                                    isPlaying = true,
                                    color = Color(0xFF1DB954),
                                    modifier = Modifier
                                        .width(20.dp * sizeScale) // Smaller visualizer
                                        .height(8.dp * sizeScale)
                                )
                            }
                        }
                    }
                } else {
                    // Expanded State: Art, Info, Controls
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp), // 10dp * 1.2
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art
                        Box(
                            modifier = Modifier
                                .size(43.dp) // 36dp * 1.2
                                .clip(RoundedCornerShape(12.dp)) // 10dp * 1.2
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = artworkModel,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (song.artworkUri == null && song.id != -1L) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(20.dp), tint = Color.White.copy(0.6f))
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp)) // 8dp * 1.2

                        // Info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onExpandToggle() }
                        ) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 14.sp, // 12sp * 1.2 ≈ 14sp
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp, // 10sp * 1.2 = 12sp
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp) // 2dp * 1.2 ≈ 4dp
                        ) {
                            IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) { // 28dp * 1.2 ≈ 34dp
                                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(22.dp)) // 18dp * 1.2 ≈ 22dp
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp) // 28dp * 1.2 ≈ 34dp
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { onPlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp) // 18dp * 1.2 ≈ 22dp
                                )
                            }

                            IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) { // 28dp * 1.2 ≈ 34dp
                                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(22.dp)) // 18dp * 1.2 ≈ 22dp
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        val points = 30
        val segmentWidth = size.width / points
        
        drawWave(points, segmentWidth, centerY, phase, color.copy(alpha = 0.25f), isPlaying, 0.7f)
        drawWave(points, segmentWidth, centerY, phase + 1.2f, color.copy(alpha = 0.5f), isPlaying, 0.9f)
        drawWave(points, segmentWidth, centerY, phase + 2.4f, color, isPlaying, 1.1f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    points: Int,
    segmentWidth: Float,
    centerY: Float,
    phase: Float,
    color: Color,
    isPlaying: Boolean,
    amplitudeMult: Float
) {
    if (!isPlaying) return // Do not draw anything if not playing

    val path = Path()
    path.moveTo(0f, centerY)
    
    for (i in 0..points) {
        val x = i * segmentWidth
        val progress = i.toFloat() / points
        val envelope = sin(progress * PI.toFloat())
        val wave = (sin((progress * 3.5f * PI.toFloat()) - phase) * 0.7f) +
                (sin((progress * 6f * PI.toFloat()) + (phase * 0.6f)) * 0.3f)

        val y = centerY + wave * (size.height / 2f) * envelope * amplitudeMult
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )
}
