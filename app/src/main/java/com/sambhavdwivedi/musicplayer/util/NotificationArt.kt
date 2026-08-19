package com.sambhavdwivedi.musicplayer.util

import android.content.Context

object NotificationArt {

    fun bytes(context: Context): ByteArray {
        return context.resources
            .openRawResource(com.sambhavdwivedi.musicplayer.R.drawable.icon)
            .use { it.readBytes() }
    }
}
