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
import com.sambhavdwivedi.musicplayer.data.MusicRepository
import com.sambhavdwivedi.musicplayer.model.Song
import com.sambhavdwivedi.musicplayer.playback.MusicPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder { TITLE_AZ, DATE_ADDED }

data class SongGroup(val header: String?, val songs: List<Song>)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_AZ)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val displayGroups: StateFlow<List<SongGroup>> =
        combine(_songs, _sortOrder) { songs, order -> buildGroups(songs, order) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
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
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) { repository.getAllSongs() }
            _songs.value = result
            _isLoading.value = false
        }
    }

    fun playSong(song: Song) {
        val mediaItem = MediaItem.fromUri(song.uri)
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
        _currentSong.value = song
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
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

    fun deleteSelectedLegacy() {
        val resolver = getApplication<Application>().contentResolver
        selectedSongs().forEach { song ->
            try {
                resolver.delete(song.uri, null, null)
            } catch (_: Exception) {
                // Older Android versions: best-effort delete
            }
        }
    }

    private fun buildGroups(list: List<Song>, order: SortOrder): List<SongGroup> {
        return when (order) {
            SortOrder.DATE_ADDED -> {
                listOf(SongGroup(header = null, songs = list.sortedByDescending { it.dateAdded }))
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
