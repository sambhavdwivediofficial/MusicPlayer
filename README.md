# Music Player

A private, offline-first music player built for a single Android device. No accounts, no backend, no analytics, no network calls — the app's only job is to find the audio already on the device and play it well.

## Why this exists

Most music players on the Play Store are bloated with streaming tie-ins, ads, or cloud sync you never asked for. This project takes the opposite approach: a small, native Android app that does exactly one thing — index and play local audio — using nothing but on-device APIs. If a feature would require a server, a login, or an internet permission, it doesn't belong here.

## What it does

- **Library scanning** — Indexes every audio track visible to `MediaStore` on the device: title, artist, album, duration, file size, and date added, with no manual folder configuration required.
- **Sorting** — Switch between alphabetical (with sticky A–Z section headers) and most-recently-added ordering from a single menu, without leaving the list.
- **Multi-select actions** — Long-press any track to enter selection mode, pick any number of songs, and share or delete them as a batch, with a confirmation step before anything is removed from the device.
- **Full playback engine** — Built on Media3/ExoPlayer with a proper `MediaSessionService`, so playback survives app backgrounding, shows transport controls in the notification shade and on the lock screen, and responds to Bluetooth/wired headset buttons out of the box.
- **Queueing, shuffle, and repeat** — The currently displayed library order becomes the play queue automatically; shuffle and repeat (off / all / one) are reflected live from the player's own state, not guessed at in the UI layer.
- **Now Playing screen** — A dedicated full-screen player with a draggable progress bar, double-tap-to-seek zones, edge-swipe for next/previous with an animated transition, and a live volume gesture — laid out differently on phones versus tablets so the controls never feel cramped or lost on a large display.
- **Song info at a glance** — A quick-access dialog surfaces title, artist, album, duration, file size, and date added for whatever is currently playing.

## Design principles

- **Dark by default, no exceptions.** The app doesn't offer a light theme because it was never designed for one — every surface, control, and piece of text was tuned for a near-black background.
- **A restrained palette.** No decorative purples, gradients, or brand colors. Everything reads in black, grey, white, and a single warm off-white accent, so the UI stays out of the way of the content.
- **No dead taps.** Every interactive element gives real feedback — pressed states, selection states, and playing states are all visually distinct — but nothing flashes or highlights just for the sake of it.
- **Two layouts, one identity.** Rather than stretching a phone layout across a tablet screen, the Now Playing view has a genuinely different arrangement on large screens (controls centered over the artwork) while keeping the same colors, iconography, and behavior.

## How it's put together

The app is split into three responsibilities that don't know about each other's implementation details:

- **Data** — A repository talks to `MediaStore` directly and returns plain data objects. It has no awareness of Compose, ViewModels, or the player.
- **Playback** — A `MediaSessionService` owns a single ExoPlayer instance and exposes it as a session. This is the only part of the app that touches the Android media APIs directly; everything else talks to it through a `MediaController`.
- **UI** — A ViewModel exposes state as `StateFlow`s (song list, sort order, current track, playback position, selection state) and Composable screens render that state and forward user actions back up. Screens hold no playback logic themselves.

This separation is what makes it possible to add features like sorting or multi-select without touching the playback code at all, and vice versa.

## Built with

- Kotlin, targeting current Android APIs (minimum SDK 26)
- Jetpack Compose for the entire UI — no XML layouts
- Media3 (ExoPlayer + MediaSession) for playback
- Android's native `MediaStore` for library scanning — no third-party indexing

## Current limitations

- Favorites and custom playlists are UI-only right now and don't persist between app launches yet.
- Folder-based browsing (as opposed to a flat library view) isn't implemented.
- Album art is intentionally not rendered — every track shows a neutral placeholder icon rather than attempting to load embedded artwork.

These are deliberate scoping choices for the current version, not oversights, and can be extended later without restructuring what's already built.
