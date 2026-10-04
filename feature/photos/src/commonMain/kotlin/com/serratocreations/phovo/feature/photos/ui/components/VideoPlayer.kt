package com.serratocreations.phovo.feature.photos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.vinceglb.filekit.PlatformFile
import org.jetbrains.compose.resources.stringResource
import phovo.feature.photos.generated.resources.Res
import phovo.feature.photos.generated.resources.video_mute
import phovo.feature.photos.generated.resources.video_pause
import phovo.feature.photos.generated.resources.video_play
import phovo.feature.photos.generated.resources.video_seek
import phovo.feature.photos.generated.resources.video_unmute

/**
 * Plays [videoPlatformFile] with playback controls along the bottom.
 *
 * @param poster drawn under the video, so there is something on screen while the player loads and
 *   for the shared element transition to carry.
 * @param surfaceModifier applied to the video and [poster] only, not the controls. This is where a
 *   shared element goes, so the controls don't fly along with the video.
 * @param controlsBottomPadding keeps the controls clear of whatever overlaps the bottom of the
 *   screen, such as the bottom toolbar.
 */
@Composable
internal fun VideoPlayer(
    videoPlatformFile: PlatformFile,
    controlsVisible: Boolean,
    controlsBottomPadding: Dp,
    onClick: () -> Unit,
    poster: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier
) {
    val playerState = rememberVideoPlayerState()
    var knownAspectRatio by remember(videoPlatformFile) { mutableStateOf<Float?>(null) }
    // TODO: Need to add alternate UI for unsupported video formats that would allow the user to
    //  open the video in an external application, see handleVideoDesktop().
    LaunchedEffect(videoPlatformFile) {
        knownAspectRatio = playerState.openVideo(videoPlatformFile)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = surfaceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            poster()
            // When the aspect ratio is known up front, size the video here and have the player
            // fill that box, rather than trusting the player's own fit, which is wrong on iOS.
            val aspectRatio = knownAspectRatio
            VideoPlayerSurface(
                playerState = playerState,
                contentScale = if (aspectRatio != null) ContentScale.Crop else ContentScale.Fit,
                modifier = if (aspectRatio != null) {
                    Modifier.aspectRatio(aspectRatio)
                } else {
                    Modifier.fillMaxSize()
                }
            )
        }
        // On Android and iOS the video is a native view, which takes any touch that lands on it.
        // Covering it keeps taps and drags in Compose, so tapping still toggles the bars and
        // dragging still dismisses the screen.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onClick
                )
        )
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VideoPlaybackControls(
                playerState = playerState,
                // The scrim runs under the toolbar to the bottom of the screen, so it fades out
                // instead of ending in a hard edge above the toolbar.
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
                    .padding(bottom = controlsBottomPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlaybackControls(
    playerState: VideoPlayerState,
    modifier: Modifier = Modifier
) {
    // Times are shown from positionText and durationText because they are the only times every
    // platform lets Compose observe. On iOS currentTime and duration are plain fields, so text built
    // from them would not redraw during playback.
    val positionText = if (playerState.userDragging) {
        // While scrubbing, show where the video will land rather than where it is.
        (playerState.sliderPos / SLIDER_MAX * playerState.duration).toPlaybackTimeText()
    } else {
        playerState.positionText
    }
    val isMuted = playerState.volume == 0f
    val seekDescription = stringResource(Res.string.video_seek)

    // Always white, since the controls sit on the video and not on a themed surface.
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        when {
                            playerState.isPlaying -> playerState.pause()
                            playerState.hasEnded() -> playerState.restart()
                            else -> playerState.play()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) PhovoIcons.Pause else PhovoIcons.Play,
                        contentDescription = stringResource(
                            if (playerState.isPlaying) Res.string.video_pause else Res.string.video_play
                        )
                    )
                }
                Text(
                    text = "$positionText / ${playerState.durationText}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { playerState.volume = if (isMuted) 1f else 0f }
                ) {
                    Icon(
                        imageVector = if (isMuted) PhovoIcons.VolumeOff else PhovoIcons.VolumeUp,
                        contentDescription = stringResource(
                            if (isMuted) Res.string.video_unmute else Res.string.video_mute
                        )
                    )
                }
            }
            val sliderColors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                activeTickColor = Color.White,
                inactiveTickColor = Color.White
            )
            val sliderInteractionSource = remember { MutableInteractionSource() }
            Slider(
                value = playerState.sliderPos,
                onValueChange = { playerState.seekStart(it) },
                onValueChangeFinished = { playerState.seekFinished() },
                valueRange = 0f..SLIDER_MAX,
                colors = sliderColors,
                interactionSource = sliderInteractionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = sliderInteractionSource,
                        colors = sliderColors,
                        thumbSize = DpSize(4.dp, 24.dp)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = sliderColors,
                        modifier = Modifier.height(8.dp)
                    )
                },
                modifier = Modifier.semantics { contentDescription = seekDescription }
            )
        }
    }
}

/**
 * Formats seconds the same way as the player's own [VideoPlayerState.positionText], so the time
 * doesn't change style when scrubbing starts. AVPlayer reports NaN for a time it doesn't know yet.
 */
private fun Double.toPlaybackTimeText(): String {
    val totalSeconds = if (isFinite()) toLong() else 0L
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours.padTime()}:${minutes.padTime()}:${seconds.padTime()}"
    } else {
        "${minutes.padTime()}:${seconds.padTime()}"
    }
}

private fun Long.padTime(): String = toString().padStart(2, '0')

/** The player reports its position as a value from 0 to this. */
private const val SLIDER_MAX = 1000f

/** The players stop at the last frame instead of resetting, so play has to start over by hand. */
private fun VideoPlayerState.hasEnded(): Boolean =
    duration > 0 && currentTime >= duration - END_TOLERANCE_SECONDS

private const val END_TOLERANCE_SECONDS = 0.25
