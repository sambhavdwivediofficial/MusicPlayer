package com.sambhavdwivedi.musicplayer.ui

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sambhavdwivedi.musicplayer.data.FavoritesRepository
import com.sambhavdwivedi.musicplayer.data.MusicRepository
import com.sambhavdwivedi.musicplayer.model.Song
import com.sambhavdwivedi.musicplayer.playback.MusicPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder { TITLE_AZ, DATE_ADDED, FAVORITES }

data class SongGroup(val header: String?, val songs: List<Song>)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val favoritesRepository = FavoritesRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val favoriteIds: StateFlow<Set<Long>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val displayGroups: StateFlow<List<SongGroup>> =
        combine(_songs, _sortOrder, favoriteIds) { songs, order, favorites ->
            buildGroups(songs, order, favorites)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            if (id != null) {
                _currentSong.value = _songs.value.find { it.id == id }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
    }

    init {
        val sessionToken = SessionToken(
            application,
            ComponentName(application, MusicPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())

        viewModelScope.launch {
            while (true) {
                controller?.let {
                    _positionMs.value = it.currentPosition.coerceAtLeast(0)
                    _durationMs.value = it.duration.coerceAtLeast(0)
                }
                delay(500)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                val bundled = repository.getBundledSongs()
                val scanned = repository.getAllSongs()
                bundled + scanned
            }
            _songs.value = result
            _isLoading.value = false
        }
    }

    fun playSong(song: Song, queue: List<Song> = _songs.value) {
        val mediaItems = queue.map {
            MediaItem.Builder()
                .setUri(it.uri)
                .setMediaId(it.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(it.title)
                        .setArtist(it.artist)
                        .setAlbumTitle(it.album)
                        .build()
                )
                .build()
        }
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        _currentSong.value = song
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0))
    }

    fun seekBy(deltaMs: Long) {
        val current = controller?.currentPosition ?: 0
        controller?.seekTo((current + deltaMs).coerceAtLeast(0))
    }

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = next
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(song.id)
        }
    }

    fun toggleSelection(song: Song) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(song.id)) remove(song.id) else add(song.id)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectedSongs(): List<Song> {
        val ids = _selectedIds.value
        return _songs.value.filter { it.id in ids }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun buildDeletePendingIntent(): PendingIntent {
        val uris = selectedSongs().map { it.uri }
        return MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, uris)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun buildDeletePendingIntentForSong(song: Song): PendingIntent {
        return MediaStore.createDeleteRequest(
            getApplication<Application>().contentResolver,
            listOf(song.uri)
        )
    }

    fun deleteSongLegacy(song: Song) {
        try {
            getApplication<Application>().contentResolver.delete(song.uri, null, null)
        } catch (_: Exception) {
        }
    }

    fun deleteSelectedLegacy() {
        val resolver = getApplication<Application>().contentResolver
        selectedSongs().forEach { song ->
            try {
                resolver.delete(song.uri, null, null)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildGroups(list: List<Song>, order: SortOrder, favorites: Set<Long>): List<SongGroup> {
        return when (order) {
            SortOrder.DATE_ADDED -> {
                listOf(SongGroup(header = null, songs = list.sortedByDescending { it.dateAdded }))
            }
            SortOrder.FAVORITES -> {
                val favSongs = list.filter { it.id in favorites }.sortedBy { it.title.uppercase() }
                listOf(SongGroup(header = "Favourites", songs = favSongs))
            }
            SortOrder.TITLE_AZ -> {
                list.sortedBy { it.title.uppercase() }
                    .groupBy { song ->
                        val first = song.title.trim().firstOrNull()?.uppercaseChar()
                        if (first != null && first.isLetter()) first.toString() else "#"
                    }
                    .toSortedMap()
                    .map { (letter, songsInGroup) -> SongGroup(header = letter, songs = songsInGroup) }
            }
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
