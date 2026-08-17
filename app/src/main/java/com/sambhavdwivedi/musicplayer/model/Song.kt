package com.sambhavdwivedi.musicplayer.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAdded: Long,
    val sizeBytes: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val isBundled: Boolean = false
)
