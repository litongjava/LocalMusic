package com.litongjava.localmusic.instance;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.device.DeviceInfo;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.video.VideoSize;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.localmusic.properties.MemoryPropKeys;
import com.litongjava.project.config.ConfigKeys;
import com.litongjava.project.config.ProjectConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Ping E Lee
 * @email itonglinux@qq.com
 * @date 2023-08-20
 */
public class ExoPlayerInstance {

  private static Logger log = LoggerFactory.getLogger(ExoPlayerInstance.class);
  private static SimpleExoPlayer player;
  public static int currentTrackIndex = 0;

  public static SimpleExoPlayer getInstance(Context context) {

    SimpleExoPlayer.Builder builder = new SimpleExoPlayer.Builder(context);
    player = builder.build();
    //noinspection deprecation
    Player.Listener listener = new Player.Listener() {
      @Override
      public void onTimelineChanged(Timeline timeline, int reason) {

      }

      @Override
      public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
          currentTrackIndex++;
          ProjectConfig projectConfig = Aop.get(ProjectConfig.class);
          Integer play_max_tracks = projectConfig.getInt(ConfigKeys.play_max_tracks);
          if (play_max_tracks != null && currentTrackIndex % play_max_tracks >= play_max_tracks) {
            // 停止播放
            player.setPlayWhenReady(false);
          }
        }
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
        }
      }

      @Override
      public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

      }

      @Override
      public void onIsLoadingChanged(boolean isLoading) {

      }

      @Override
      public void onAvailableCommandsChanged(Player.Commands availableCommands) {

      }

      @Override
      public void onPlaybackStateChanged(int playbackState) {

      }

      @Override
      public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {

      }

      @Override
      public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {

      }

      @Override
      public void onIsPlayingChanged(boolean isPlaying) {

      }

      @Override
      public void onRepeatModeChanged(int repeatMode) {

      }

      @Override
      public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

      }

      @Override
      public void onPlayerError(PlaybackException error) {

      }

      @Override
      public void onPlayerErrorChanged(@Nullable PlaybackException error) {

      }

      @Override
      public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {

      }

      @Override
      public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

      }

      @Override
      public void onSeekForwardIncrementChanged(long seekForwardIncrementMs) {

      }

      @Override
      public void onSeekBackIncrementChanged(long seekBackIncrementMs) {

      }

      @Override
      public void onAudioSessionIdChanged(int audioSessionId) {

      }

      @Override
      public void onAudioAttributesChanged(AudioAttributes audioAttributes) {

      }

      @Override
      public void onVolumeChanged(float volume) {

      }

      @Override
      public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {

      }

      @Override
      public void onDeviceInfoChanged(DeviceInfo deviceInfo) {

      }

      @Override
      public void onDeviceVolumeChanged(int volume, boolean muted) {

      }

      @Override
      public void onEvents(Player player, Player.Events events) {

      }

      @Override
      public void onVideoSizeChanged(VideoSize videoSize) {

      }

      @Override
      public void onSurfaceSizeChanged(int width, int height) {

      }

      @Override
      public void onRenderedFirstFrame() {

      }

      @Override
      public void onCues(List<Cue> cues) {

      }

      @Override
      public void onMetadata(Metadata metadata) {

      }

      @Override
      public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {

      }

      @Override
      public void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {

      }

      @Override
      public void onStaticMetadataChanged(List<Metadata> metadataList) {

      }

      @Override
      public void onLoadingChanged(boolean isLoading) {

      }

      @Override
      public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

      }

      @Override
      public void onPositionDiscontinuity(int reason) {

      }

      @Override
      public void onMaxSeekToPreviousPositionChanged(int maxSeekToPreviousPositionMs) {

      }

      @Override
      public void onSeekProcessed() {

      }

      @Override
      public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

      }

      @Override
      public int hashCode() {
        return super.hashCode();
      }

      @Override
      public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
      }

      @NonNull
      @Override
      protected Object clone() throws CloneNotSupportedException {
        return super.clone();
      }

      @NonNull
      @Override
      public String toString() {
        return super.toString();
      }

      @Override
      protected void finalize() throws Throwable {
        super.finalize();
      }
    };
    player.addListener(listener);
    return player;
  }

  public static SimpleExoPlayer getExoPlayer() {
    return player;
  }

  public static void updateData() {
    currentTrackIndex = 0;
  }
}
