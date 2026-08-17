package com.sambhavdwivedi.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.sambhavdwivedi.musicplayer.model.Song

class MusicRepository(private val context: Context) {

    fun getAllSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            queryMediaStore(songs)
        } catch (_: SecurityException) {
        }
        return songs
    }

    private fun queryMediaStore(songs: MutableList<Song>) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection, projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Title"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val duration = cursor.getLong(durationCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val size = cursor.getLong(sizeCol)
                val albumId = cursor.getLong(albumIdCol)

                val songUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        dateAdded = dateAdded,
                        sizeBytes = size,
                        uri = songUri,
                        albumArtUri = albumArtUri
                    )
                )
            }
        }
    }

    fun getBundledSongs(): List<Song> {
        val bundled = mutableListOf<Song>()
        try {
            val assetManager = context.assets
            val fileNames = assetManager.list("bundled_songs") ?: emptyArray()

            fileNames.filter { it.endsWith(".mp3", ignoreCase = true) }
                .forEachIndexed { index, fileName ->
                    val assetPath = "bundled_songs/$fileName"
                    var durationMs = 0L
                    var sizeBytes = 0L
                    var title = fileName.substringBeforeLast(".")
                    var artist = "Unknown Artist"

                    try {
                        val afd = assetManager.openFd(assetPath)
                        sizeBytes = afd.length
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()?.let { durationMs = it }
                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?.takeIf { it.isNotBlank() }?.let { title = it }
                        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?.takeIf { it.isNotBlank() }?.let { artist = it }
                        retriever.release()
                        afd.close()
                    } catch (_: Exception) {
                    }

                    bundled.add(
                        Song(
                            id = -(1000L + index),
                            title = title,
                            artist = artist,
                            album = "",
                            durationMs = durationMs,
                            dateAdded = 0L,
                            sizeBytes = sizeBytes,
                            uri = Uri.parse("asset:///$assetPath"),
                            albumArtUri = null,
                            isBundled = true
                        )
                    )
                }
        } catch (_: Exception) {
        }
        return bundled
    }
}
