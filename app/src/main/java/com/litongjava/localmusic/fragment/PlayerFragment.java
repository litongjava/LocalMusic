package com.litongjava.localmusic.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.litongjava.android.view.inject.annotation.FindViewById;
import com.litongjava.android.view.inject.annotation.OnClick;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.localmusic.R;
import com.litongjava.localmusic.instance.ExoPlayerInstance;
import com.litongjava.localmusic.properties.MemoryPropKeys;
import com.litongjava.project.config.ConfigKeys;
import com.litongjava.project.config.ProjectConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerFragment extends Fragment {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @FindViewById(R.id.musicTitile)
  private TextView musicTitile;
  @FindViewById(R.id.styled_player_view)
  private StyledPlayerView playerView;

  @FindViewById(R.id.playCurrentTracksTextView)
  public TextView playCurrentTracksTextView;

  @FindViewById(R.id.playMaxTracksTextView)
  public TextView playMaxTracksTextView;

  ProjectConfig projectConfig = Aop.get(ProjectConfig.class);

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_player, container, false);
    ViewInjectUtils.injectViewAndOnClick(rootView, this);
    initView();
    return rootView;
  }

  private void initView() {
    SimpleExoPlayer exoPlayer = ExoPlayerInstance.getExoPlayer();
    if (exoPlayer != null) {
      playerView.setPlayer(exoPlayer);
    }
    if (MemoryPropKeys.current_music_path != null) {
      musicTitile.setText(MemoryPropKeys.current_music_path);
    } else {
      musicTitile.setText("No Music");
    }
    referesh();
  }

  @OnClick(R.id.timerBtn)
  public void timerBtn_onClick(View v) {
    showDialog();
  }

  @OnClick(R.id.RefereshBtn)
  public void refereshBtn_onClick(View v) {
    referesh();
  }

  private void referesh() {
    playCurrentTracksTextView.setText("Current:" + ExoPlayerInstance.currentTrackIndex);
    playMaxTracksTextView.setText("Max:" + projectConfig.getInt(ConfigKeys.play_max_tracks));
  }

  private void showDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
    CharSequence[] items = {"1", "2", "3", "4"};
    builder.setTitle("Select a item")
      .setItems(items, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int selectedIndex) {
          // 在这里处理选择的选项
          projectConfig.put(ConfigKeys.play_max_tracks, selectedIndex + 1);
          // 可以根据选择执行不同的操作
          ExoPlayerInstance.updateData();
          referesh();
        }
      });

    // 创建并显示对话框
    AlertDialog dialog = builder.create();
    dialog.show();
  }
}