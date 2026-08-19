package com.sambhavdwivedi.musicplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sambhavdwivedi.musicplayer.model.Song
import com.sambhavdwivedi.musicplayer.ui.theme.AppRowPlaying
import com.sambhavdwivedi.musicplayer.ui.theme.AppRowSelected
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.ui.res.vectorResource
import com.sambhavdwivedi.musicplayer.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image


private const val DEVELOPER_NAME = "Sambhav Dwivedi"
private const val GITHUB_PROFILE_URL = "https://github.com/sambhavdwivediofficial"
private const val PROJECT_REPO_URL = "https://github.com/sambhavdwivediofficial/MusicPlayer"
private const val REPORT_ISSUE_URL = "https://github.com/sambhavdwivediofficial/MusicPlayer/issues/new"
private const val LINKEDIN_URL = "https://www.linkedin.com/in/sambhavdwivedi"
private const val REDDIT_URL = "https://www.reddit.com/user/sambhavdwivedi"
private const val WEBSITE_URL = "https://www.sambhavdwivedi.in"
private const val BLOG_URL = "https://blog.sambhavdwivedi.in"
private const val COMMUNITY_URL = "https://www.unitedtechcommunity.in"
//private const val COMPANY_ONE_URL = "https://www.publicon.in"
private const val COMPANY_TWO_URL = "https://www.peerlink.in"
private const val APP_SHARE_URL = "https://github.com/sambhavdwivediofficial/MusicPlayer/releases/download/v0.2.0/MusicPlayer.apk"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListScreen(viewModel: MusicViewModel, onSongOpen: () -> Unit) {
    val displayGroups by viewModel.displayGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlayingGlobal by viewModel.isPlaying.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    BackHandler(enabled = !showAbout && !showEqualizer && !selectionMode && sortOrder != SortOrder.DATE_ADDED) {
        viewModel.setSortOrder(SortOrder.DATE_ADDED)
    }

    BackHandler(enabled = showAbout) {
        showAbout = false
    }

    BackHandler(enabled = showEqualizer) {
        showEqualizer = false
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.clearSelection()
        viewModel.loadSongs()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF080808))
                    .padding(top = 2.dp, bottom = 2.dp, start = 26.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cancel selection",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        "${selectedIds.size} selected",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    IconButton(onClick = {
                        val songsToShare = viewModel.selectedSongs()
                        val uris = ArrayList(songsToShare.map { it.uri })
                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "audio/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share songs"))
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Text(
                        text = when {
                            showAbout -> "About"
                            showEqualizer -> "Equalizer"
                            else -> "Music"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (!showAbout) {
                        IconButton(onClick = {
                            showAbout = true
                            showEqualizer = false
                        }) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = {
                        showEqualizer = true
                        showAbout = false
                    }) {
                        Icon(
                            Icons.Filled.Equalizer,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF111111))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Favourites") },
                                onClick = {
                                    sortMenuExpanded = false
                                    showAbout = false
                                    showEqualizer = false
                                    viewModel.setSortOrder(SortOrder.FAVORITES)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort: Title (A-Z)") },
                                onClick = {
                                    sortMenuExpanded = false
                                    showAbout = false
                                    showEqualizer = false
                                    viewModel.setSortOrder(SortOrder.TITLE_AZ)
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh library") },
                                onClick = {
                                    sortMenuExpanded = false
                                    viewModel.loadSongs()
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF080808)
    ) { innerPadding ->
        if (showAbout) {
            AboutContent(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else if (showEqualizer) {
            EqualizerContent(viewModel = viewModel, modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                displayGroups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No songs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 4.dp,
                            bottom = 16.dp,
                            start = 12.dp,
                            end = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        displayGroups.forEach { group ->
                            if (group.header != null) {
                                stickyHeader(key = "header_${group.header}") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF080808))
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = group.header,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            items(group.songs, key = { it.id }) { song ->
                                val isCurrent = song.id == currentSong?.id
                                SongRow(
                                    song = song,
                                    isPlaying = isCurrent,
                                    isSelected = song.id in selectedIds,
                                    selectionMode = selectionMode,
                                    showWave = isCurrent && isPlayingGlobal,
                                    onClick = {
                                        if (selectionMode) {
                                            if (!song.isBundled) viewModel.toggleSelection(song)
                                        } else if (isCurrent) {
                                            onSongOpen()
                                        } else {
                                            viewModel.playSong(song, displayGroups.flatMap { it.songs })
                                            onSongOpen()
                                        }
                                    },
                                    onLongClick = { if (!song.isBundled) viewModel.toggleSelection(song) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color(0xFF080808),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = { Text("Delete ${selectedIds.size} song(s)?") },
                text = { Text("Permanently delete these songs?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val pendingIntent = viewModel.buildDeletePendingIntent()
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        } else {
                            viewModel.deleteSelectedLegacy()
                            viewModel.clearSelection()
                            viewModel.loadSongs()
                        }
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun EqualizerContent(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val enabled by viewModel.eqEnabled.collectAsState()
    val bands by viewModel.eqBands.collectAsState()
    val presets by viewModel.eqPresets.collectAsState()
    val minLevel by viewModel.eqMinLevel.collectAsState()
    val maxLevel by viewModel.eqMaxLevel.collectAsState()

    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (enabled) "Enabled" else "Disabled",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = { viewModel.toggleEqualizer() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3A3A3C),
                    uncheckedThumbColor = Color(0xFF888888),
                    uncheckedTrackColor = Color(0xFF222222)
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEachIndexed { index, name ->
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable { viewModel.applyEqPreset(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bands.forEach { band ->
                VerticalBandSlider(
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    value = band.levelMillibels.toFloat(),
                    onValueChange = { viewModel.setEqBandLevel(band.index, it.toInt().toShort()) },
                    label = formatFrequency(band.frequencyHz)
                )
            }
        }
    }
}

@Composable
private fun VerticalBandSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(-placeable.width, 0)
                        }
                    }
                    .width(180.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF3A3A3C)
                )
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(text = label, color = Color(0xFFAAAAAA), fontSize = 10.sp)
    }
}

private fun formatFrequency(hz: Int): String {
    val khz = hz / 1000
    return if (hz >= 1000) "${khz}kHz" else "${hz}Hz"
}

private data class AboutLink(
    val label: String,
    val icon: ImageVector,
    val url: String
)

@Composable
private fun AboutContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val links = listOf(
        AboutLink("Website", Icons.Filled.Public, WEBSITE_URL),
        AboutLink("LinkedIn", ImageVector.vectorResource(R.drawable.ic_linkedin), LINKEDIN_URL),
        AboutLink("Reddit", ImageVector.vectorResource(R.drawable.ic_reddit), REDDIT_URL),
        AboutLink("GitHub", Icons.Filled.Code, GITHUB_PROFILE_URL),
        AboutLink("Project", Icons.Filled.FolderOpen, PROJECT_REPO_URL),
//        AboutLink("Blog", Icons.Filled.Article, BLOG_URL),
//        AboutLink("Community", Icons.Filled.Groups, COMMUNITY_URL),
//        AboutLink("Company", Icons.Filled.Business, COMPANY_TWO_URL)
    )

    Column(
        modifier = modifier.padding(
            start = 24.dp,
            end = 24.dp,
            top = 50.dp,
            bottom = 20.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "Music Player",
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Music Player",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "A private, offline music player for your own device.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF888888))) {
                    append("Developed by ")
                }
                withStyle(SpanStyle(color = Color.White)) {
                    append(DEVELOPER_NAME)
                }
            },
            fontSize = 12.sp
        )

        Spacer(Modifier.height(24.dp))

        links.chunked(3).forEach { rowLinks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowLinks.forEach { link ->
                    AboutLinkTile(link = link) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AboutActionButton(
                icon = Icons.Filled.BugReport,
                label = "Report an issue",
                modifier = Modifier.weight(1f)
            ) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPORT_ISSUE_URL)))
            }
            AboutActionButton(
                icon = Icons.Filled.Share,
                label = "Share this app",
                modifier = Modifier.weight(1f)
            ) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, APP_SHARE_URL)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share app"))
            }
        }
    }
}

@Composable
private fun AboutLinkTile(link: AboutLink, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = link.icon,
                contentDescription = link.label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = link.label,
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AboutActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    showWave: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> AppRowSelected
                isPlaying -> AppRowPlaying
                else -> Color(0xFF111111)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt()

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (song.isBundled) {
                        formatSize(song.sizeBytes)
                    } else {
                        "${formatSize(song.sizeBytes)} • ${formatDate(song.dateAdded)}"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = if (showWave) 8.dp else 0.dp)
            )

            if (showWave) {
                EqualizerBars()
            }

            if (selectionMode && !song.isBundled) {
                Spacer(Modifier.width(10.dp))
                SelectionCircle(isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun EqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val barColor = Color(0xFF8A8A8E)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(bar1.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
        Box(
            Modifier
                .width(3.dp)
                .height(bar2.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
        Box(
            Modifier
                .width(3.dp)
                .height(bar3.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun SelectionCircle(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AlbumArt() {
    Image(
        painter = painterResource(id = R.drawable.icon),
        contentDescription = "Music Player",
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}

private fun formatDate(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}
