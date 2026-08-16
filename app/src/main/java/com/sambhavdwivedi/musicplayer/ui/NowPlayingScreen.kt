package com.sambhavdwivedi.musicplayer.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.Player
import com.sambhavdwivedi.musicplayer.model.Song
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.VolumeUp

private val PlayerBackground = Color(0xFF080808)
private val ArtBoxColor = Color(0xFF3B3F4C)
private val ArtIconColor = Color(0xFFC7CAD6)
private val DimText = Color(0xFFAAAAAA)
private val InactiveIcon = Color(0xFF6E6E70)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(viewModel: MusicViewModel, onBack: () -> Unit) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var direction by remember { mutableIntStateOf(1) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    val currentSong = song ?: return
    val durationMs = currentSong.durationMs
    val isFavorite = currentSong.id in favoriteIds

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.loadSongs()
        onBack()
    }

    androidx.activity.compose.BackHandler {
        onBack()
    }

    fun goNext() {
        direction = 1
        viewModel.skipNext()
    }

    fun goPrevious() {
        direction = -1
        viewModel.skipPrevious()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBackground)
            .pointerInput(Unit) {
                var accX = 0f
                var accY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (abs(accX) > abs(accY) && abs(accX) > 150f) {
                            if (accX < 0) goNext() else goPrevious()
                        }
                        accX = 0f
                        accY = 0f
                        showVolumeHud = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accX += dragAmount.x
                        accY += dragAmount.y
                        if (abs(accY) > abs(accX) && abs(dragAmount.y) > 2f) {
                            showVolumeHud = true
                            val direction = if (dragAmount.y < 0)
                                AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                direction,
                                0
                            )
                            volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        }
                    }
                )
            }
    ) {
        val isTablet = maxWidth > 600.dp

        if (isTablet) {
            TabletPlayerLayout(
                song = currentSong,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                isDragging = isDragging,
                dragPosition = dragPosition,
                isFavorite = isFavorite,
                direction = direction,
                onBack = onBack,
                onTogglePlay = { viewModel.togglePlayPause() },
                onNext = { goNext() },
                onPrevious = { goPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                onSeekChange = { isDragging = true; dragPosition = it },
                onSeekFinished = {
                    viewModel.seekTo((dragPosition * durationMs).toLong())
                    isDragging = false
                },
                onDoubleTapSeek = { back -> viewModel.seekBy(if (back) -10_000 else 10_000) },
                onShare = { shareSong(context, currentSong) },
                onMenuClick = { menuExpanded = true },
                onInfoClick = { showInfoDialog = true },
                onSpeedClick = { showSpeedSheet = true },
                onDeleteClick = { showDeleteConfirm = true },
                playbackSpeed = playbackSpeed,
                menuExpanded = menuExpanded,
                onMenuDismiss = { menuExpanded = false },
                showVolumeHud = showVolumeHud,
                volumeLevel = volumeLevel,
                maxVolume = maxVolume
            )
        } else {
            MobilePlayerLayout(
                song = currentSong,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                isDragging = isDragging,
                dragPosition = dragPosition,
                isFavorite = isFavorite,
                direction = direction,
                onBack = onBack,
                onTogglePlay = { viewModel.togglePlayPause() },
                onNext = { goNext() },
                onPrevious = { goPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                onSeekChange = { isDragging = true; dragPosition = it },
                onSeekFinished = {
                    viewModel.seekTo((dragPosition * durationMs).toLong())
                    isDragging = false
                },
                onDoubleTapSeek = { back -> viewModel.seekBy(if (back) -10_000 else 10_000) },
                onShare = { shareSong(context, currentSong) },
                onMenuClick = { menuExpanded = true },
                onInfoClick = { showInfoDialog = true },
                onSpeedClick = { showSpeedSheet = true },
                onDeleteClick = { showDeleteConfirm = true },
                playbackSpeed = playbackSpeed,
                menuExpanded = menuExpanded,
                onMenuDismiss = { menuExpanded = false },
                showVolumeHud = showVolumeHud,
                volumeLevel = volumeLevel,
                maxVolume = maxVolume
            )
        }
    }

    if (showInfoDialog) {
        SongInfoDialog(song = currentSong, onDismiss = { showInfoDialog = false })
    }

    if (showSpeedSheet) {
        PlaybackSpeedSheet(
            currentSpeed = playbackSpeed,
            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF080808),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Delete this song?") },
            text = { Text("\"${currentSong.title}\" will be permanently deleted from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pendingIntent = viewModel.buildDeletePendingIntentForSong(currentSong)
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } else {
                        viewModel.deleteSongLegacy(currentSong)
                        viewModel.loadSongs()
                        onBack()
                    }
                }) {
                    Text("Delete", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MobilePlayerLayout(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isDragging: Boolean,
    dragPosition: Float,
    isFavorite: Boolean,
    direction: Int,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onDoubleTapSeek: (Boolean) -> Unit,
    onShare: () -> Unit,
    onMenuClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onDeleteClick: () -> Unit,
    playbackSpeed: Float,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    showVolumeHud: Boolean,
    volumeLevel: Int,
    maxVolume: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        PlayerTopBar(
            title = null,
            onBack = onBack,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onMenuClick = onMenuClick,
            onInfoClick = onInfoClick,
            onDeleteClick = onDeleteClick,
            onSpeedClick = onSpeedClick,
            playbackSpeed = playbackSpeed,
            menuExpanded = menuExpanded,
            onMenuDismiss = onMenuDismiss
        )

        Spacer(Modifier.height(24.dp))

        // ART BOX AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        if (direction >= 0) {
                            (
                                    slideInHorizontally(tween(280)) { w -> w } +
                                            fadeIn(tween(280))
                                    ) togetherWith (
                                    slideOutHorizontally(tween(280)) { w -> -w } +
                                            fadeOut(tween(280))
                                    )
                        } else {
                            (
                                    slideInHorizontally(tween(280)) { w -> -w } +
                                            fadeIn(tween(280))
                                    ) togetherWith (
                                    slideOutHorizontally(tween(280)) { w -> w } +
                                            fadeOut(tween(280))
                                    )
                        }
                    },
                    label = "art"
                ) { _ ->
                    ArtBox(
                        modifier = Modifier
                            .fillMaxSize(),
                        iconSize = 96.dp,
                        onDoubleTapSeek = onDoubleTapSeek
                    )
                }

                if (showVolumeHud) {
                    VolumeHud(
                        percent = (volumeLevel.toFloat() / maxVolume.toFloat())
                            .coerceIn(0f, 1f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))

        ProgressSection(
            positionMs = positionMs,
            durationMs = durationMs,
            isDragging = isDragging,
            dragPosition = dragPosition,
            onSeekChange = onSeekChange,
            onSeekFinished = onSeekFinished
        )

        Spacer(Modifier.height(12.dp))

        ControlsRow(
            isPlaying = isPlaying,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrevious = onPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun TabletPlayerLayout(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isDragging: Boolean,
    dragPosition: Float,
    isFavorite: Boolean,
    direction: Int,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onDoubleTapSeek: (Boolean) -> Unit,
    onShare: () -> Unit,
    onMenuClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onDeleteClick: () -> Unit,
    playbackSpeed: Float,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    showVolumeHud: Boolean,
    volumeLevel: Int,
    maxVolume: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        PlayerTopBar(
            title = song.title,
            onBack = onBack,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onMenuClick = onMenuClick,
            onInfoClick = onInfoClick,
            onDeleteClick = onDeleteClick,
            onSpeedClick = onSpeedClick,
            playbackSpeed = playbackSpeed,
            menuExpanded = menuExpanded,
            onMenuDismiss = onMenuDismiss
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    if (direction >= 0) {
                        (
                                slideInHorizontally(tween(280)) { w -> w } +
                                        fadeIn(tween(280))
                                ) togetherWith (
                                slideOutHorizontally(tween(280)) { w -> -w } +
                                        fadeOut(tween(280))
                                )
                    } else {
                        (
                                slideInHorizontally(tween(280)) { w -> -w } +
                                        fadeIn(tween(280))
                                ) togetherWith (
                                slideOutHorizontally(tween(280)) { w -> w } +
                                        fadeOut(tween(280))
                                )
                    }
                },
                label = "art_tablet"
            ) { _ ->
                Box(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(ArtBoxColor)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        onDoubleTapSeek(offset.x < size.width / 2)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = ArtIconColor,
                            modifier = Modifier.size(140.dp)
                        )
                    }

                    if (showVolumeHud) {
                        VolumeHud(
                            percent = (volumeLevel.toFloat() / maxVolume.toFloat())
                                .coerceIn(0f, 1f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        ControlsRow(
            isPlaying = isPlaying,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrevious = onPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            modifier = Modifier
        )

        Spacer(Modifier.height(12.dp))

        ProgressSection(
            positionMs = positionMs,
            durationMs = durationMs,
            isDragging = isDragging,
            dragPosition = dragPosition,
            onSeekChange = onSeekChange,
            onSeekFinished = onSeekFinished,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun PlayerTopBar(
    title: String?,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onMenuClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSpeedClick: () -> Unit,
    playbackSpeed: Float,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        if (title != null) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Favorite",
                tint = Color.White
            )
        }
        Box {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onMenuDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1C1C1E))
            ) {
                DropdownMenuItem(
                    text = { Text("Song info", color = Color.White) },
                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White) },
                    onClick = {
                        onMenuDismiss()
                        onInfoClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Playback speed  •  ${"%.1f".format(playbackSpeed)}x",
                            color = Color.White
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White) },
                    onClick = {
                        onMenuDismiss()
                        onSpeedClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.White) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White) },
                    onClick = {
                        onMenuDismiss()
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ArtBox(
    modifier: Modifier,
    iconSize: androidx.compose.ui.unit.Dp,
    onDoubleTapSeek: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ArtBoxColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        onDoubleTapSeek(offset.x < size.width / 2)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = ArtIconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun ProgressSection(
    positionMs: Long,
    durationMs: Long,
    isDragging: Boolean,
    dragPosition: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderValue = if (isDragging) dragPosition
    else if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF3A3A3C)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(if (isDragging) (dragPosition * durationMs).toLong() else positionMs),
                color = DimText,
                fontSize = 13.sp
            )
            Text(text = formatMs(durationMs), color = DimText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) Color.White else InactiveIcon,
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode == Player.REPEAT_MODE_OFF) InactiveIcon else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSpeedSheet(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var speed by remember { mutableFloatStateOf(currentSpeed) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5A5A5C))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text("Playback speed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(20.dp))

            Text(
                text = "%.1fx".format(speed),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    onSpeedChange(it)
                },
                valueRange = 0.5f..2.0f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF3A3A3C)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.5x", color = DimText, fontSize = 12.sp)
                Text("1.0x", color = DimText, fontSize = 12.sp)
                Text("2.0x", color = DimText, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(0.5f, 0.8f, 1.0f, 1.5f, 2.0f).forEach { preset ->
                    val isSelected = abs(speed - preset) < 0.01f
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color(0xFF2C2C2E))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else Color(0xFF4A4A4C),
                                shape = CircleShape
                            )
                            .clickable {
                                speed = preset
                                onSpeedChange(preset)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%.1f".format(preset),
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Song info",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                InfoRow("Title", song.title)
                InfoRow("Artist", song.artist)
                InfoRow("Album", song.album)
                InfoRow("Duration", formatMs(song.durationMs))
                InfoRow("Size", formatSizeDialog(song.sizeBytes))
                InfoRow("Date added", formatDateDialog(song.dateAdded))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, color = DimText, fontSize = 14.sp, modifier = Modifier.width(100.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun shareSong(context: Context, song: Song) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, song.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share song"))
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatSizeDialog(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}

private fun formatDateDialog(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}

@Composable
private fun VolumeHud(percent: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 34.dp)
                .width(3.dp)
                .fillMaxHeight(percent.coerceIn(0.02f, 1f))
                .align(Alignment.BottomCenter)
                .background(Color(0xFF2196F3), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .background(Color.White)
            )
        }

        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(18.dp)
        )
    }
}
