package com.litongjava.localmusic.services;

import android.net.Uri;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.common.net.MediaType;
import com.litongjava.localmusic.instance.ExoPlayerInstance;
import com.litongjava.localmusic.utils.AudioUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerService {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public void playMusic(List<String> musicList, Integer playIndex) {
    List<MediaItem> mediaItems = new ArrayList<>(musicList.size());
    for (String musicPath : musicList) {
      String fileExtension = FileUtils.getFileExtension(musicPath);
      if (!AudioUtils.isAudioFile(fileExtension)) {
        continue;
      }
      mediaItems.add(MediaItem.fromUri(musicPath));
    }

    SimpleExoPlayer simpleExoPlayer = ExoPlayerInstance.getExoPlayer();
    log.info("simpleExoPlayer:{}", simpleExoPlayer);
    simpleExoPlayer.clearMediaItems();
    //添加音频
    simpleExoPlayer.addMediaItems(mediaItems);
    //准备播放
    simpleExoPlayer.prepare();
    //指定播放索引
    simpleExoPlayer.seekTo(playIndex, 0);
    // 开始播放
    simpleExoPlayer.setPlayWhenReady(true);
    // play
    simpleExoPlayer.play();
  }

  public void seekTo(String text) {
    SimpleExoPlayer exoPlayer = ExoPlayerInstance.getExoPlayer();

    if (StringUtils.isEmpty(text)) {
      exoPlayer.seekTo(0);
    } else {
      log.info("seek to text :{}", text);
      long l = Long.parseLong(text);
      log.info("seek to:{}", l);
      exoPlayer.seekTo(l);

    }
  }

  public void checkCurrentTrack() {
    SimpleExoPlayer simpleExoPlayer = ExoPlayerInstance.getExoPlayer();
    TrackGroupArray trackGroups = simpleExoPlayer.getCurrentTrackGroups();
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

  public void checkTextComponent() {
    SimpleExoPlayer exoPlayer = ExoPlayerInstance.getExoPlayer();
    ExoPlayer.TextComponent textComponent = exoPlayer.getTextComponent();
    log.info("text component:{}", textComponent);
    List<Cue> currentCues = textComponent.getCurrentCues();
    log.info("cue size:{}", currentCues.size());
    ToastUtils.showLong(currentCues.toString());
    for (Cue currentCue : currentCues) {
      log.info(currentCue.text.toString());
    }
  }
}
