package com.sambhavdwivedi.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val context: Context) {

    private val favoriteIdsKey = stringSetPreferencesKey("favorite_song_ids")

    val favoriteIds: Flow<Set<Long>> = context.favoritesDataStore.data.map { prefs ->
        prefs[favoriteIdsKey]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun toggleFavorite(songId: Long) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[favoriteIdsKey]?.toMutableSet() ?: mutableSetOf()
            val idStr = songId.toString()
            if (current.contains(idStr)) current.remove(idStr) else current.add(idStr)
            prefs[favoriteIdsKey] = current
        }
    }
}
