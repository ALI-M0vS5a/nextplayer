package dev.anilbeesetti.nextplayer.feature.player.extensions

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import timber.log.Timber

/**
 * Switches to selected track.
 *
 * @param trackType The type of track to switch.
 * @param trackIndex The index of the track to switch to, or null to enable the track.
 *
 * if trackIndex is a negative number, the track will be disabled
 * if trackIndex is a valid index, the track will be switched to that index
 */
fun Player.switchTrack(trackType: @C.TrackType Int, trackIndex: Int) {
    val trackTypeText = when (trackType) {
        C.TRACK_TYPE_AUDIO -> "audio"
        C.TRACK_TYPE_TEXT -> "subtitle"
        else -> throw IllegalArgumentException("Invalid track type: $trackType")
    }

    if (trackIndex < 0) {
        Timber.d("Disabling $trackTypeText")
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    } else {
        val tracks = currentTracks.groups.filter { it.type == trackType }

        if (tracks.isEmpty() || trackIndex >= tracks.size) {
            Timber.d("Operation failed: Invalid track index: $trackIndex")
            return
        }

        Timber.d("Setting $trackTypeText track: $trackIndex")
        val trackSelectionOverride = TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)

        // Override the track selection parameters to force the selection of the specified track.
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(trackSelectionOverride)
            .build()
    }
}

/**
 * Sets the seek parameters for the player.
 *
 * @param seekParameters The seek parameters to set.
 */
@UnstableApi
fun Player.setSeekParameters(seekParameters: SeekParameters) {
    when (this) {
        is ExoPlayer -> this.setSeekParameters(seekParameters)
    }
}

/**
 * Seeks to the specified position.
 *
 * @param positionMs The position to seek to, in milliseconds.
 * @param shouldFastSeek Whether to seek to the nearest keyframe.
 */
@UnstableApi
fun Player.seekBack(positionMs: Long, shouldFastSeek: Boolean = false) {
    if (currentMediaItem == null) return
    setSeekParameters(if (shouldFastSeek) SeekParameters.PREVIOUS_SYNC else SeekParameters.DEFAULT)
    this.seekTo(positionMs)
}

/**
 * Seeks to the specified position.
 *
 * @param positionMs The position to seek to, in milliseconds.
 * @param shouldFastSeek Whether to seek to the nearest keyframe.
 */
@UnstableApi
fun Player.seekForward(positionMs: Long, shouldFastSeek: Boolean = false) {
    if (currentMediaItem == null) return
    setSeekParameters(if (shouldFastSeek) SeekParameters.NEXT_SYNC else SeekParameters.DEFAULT)
    this.seekTo(positionMs)
}

fun Player.addAdditionalSubtitleConfiguration(subtitle: MediaItem.SubtitleConfiguration) {
    val currentMediaItemLocal = currentMediaItem ?: return
    val existingSubConfigurations = currentMediaItemLocal.localConfiguration?.subtitleConfigurations ?: emptyList()

    if (existingSubConfigurations.any { it.id == subtitle.id }) {
        return
    }

    val updateMediaItem = currentMediaItemLocal
        .buildUpon()
        .setSubtitleConfigurations(existingSubConfigurations + listOf(subtitle))
        .build()

    val index = currentMediaItemIndex
    addMediaItem(index + 1, updateMediaItem)
    removeMediaItem(index)
}
