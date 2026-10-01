package com.litongjava.localmusic.instance;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
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
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoSize;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.localmusic.constants.SPConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

  public static SimpleExoPlayer getExoPlayer() {
    return player;
  }

  public static void updateData() {
    currentTrackIndex = 0;
  }

  public static SimpleExoPlayer getInstance(Context context, SubtitleView subtitleView) {
    if (player != null) {
      return player;
    }

//    //创建一个 DefaultTrackSelector 并设置参数以选择字幕轨道。
//    DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
//    //设置字幕为首选轨道：
//    DefaultTrackSelector.Parameters parameters = trackSelector.getParameters().buildUpon()
//      .setPreferredTextLanguage("en")  // 根据你的字幕语言设置
//      .setSelectUndeterminedTextLanguage(true)
//      .build();

//    trackSelector.setParameters(parameters);

    //使用上述 trackSelector 创建 SimpleExoPlayer：
    player = new SimpleExoPlayer.Builder(context)
//      .setTrackSelector(trackSelector)
      .build();

    //noinspection deprecation
    Player.Listener listener = getListener(subtitleView);
    player.addListener(listener);
//    player.addListener(new Player.EventListener() {
//      @Override
//      public void onPositionDiscontinuity(int reason) {
//        updateLyrics(player.getCurrentPosition());
//      }
//
//      @Override
//      public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//        if (playbackState == Player.STATE_READY && playWhenReady) {
//          updateLyrics(player.getCurrentPosition());
//        }
//      }
//
//      private void updateLyrics(long currentPosition) {
//        log.info("currentPosition:{}", currentPosition);
//        Cue cue = new Cue(currentPosition+"");
//        List<Cue> cues = Collections.singletonList(cue);
//        subtitleView.onCues(cues);
//      }
//    });
//
    return player;
  }


  private static Player.Listener getListener(SubtitleView subtitleView) {
    return new Player.Listener() {
      @Override
      public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
          currentTrackIndex++;
          SharedPreferences sharedPreferences = Aop.get(SharedPreferences.class);
          int playMaxTracks = sharedPreferences.getInt(SPConstants.play_max_tracks, 0);
          log.info("currentTrackIndex,{},playMaxTracks:{}", currentTrackIndex, playMaxTracks);
          if (playMaxTracks != 0 && currentTrackIndex % playMaxTracks == 0) {
            // 停止播放
            player.setPlayWhenReady(false);
          }
        }
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
        }
      }

      @Override
      public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        for (int i = 0; i < trackGroups.length; i++) {
          TrackGroup trackGroup = trackGroups.get(i);
          for (int j = 0; j < trackGroup.length; j++) {
            Format format = trackGroup.getFormat(j);
            String sampleMimeType = format.sampleMimeType;
            log.info("sampleMimeType:{}", sampleMimeType);
            if (MimeTypes.isText(sampleMimeType)) {
              // 字幕轨道存在
              log.info("Subtitle track found with MIME type: {}", sampleMimeType);
            }
          }
        }
      }

      @Override
      public void onPlayerError(PlaybackException error) {
        log.error("error:{}", error);
        error.printStackTrace();
      }

      @Override
      public void onPlayerErrorChanged(@Nullable PlaybackException error) {
        error.printStackTrace();
        log.error("error:{}", error);
      }
    };
  }
}
