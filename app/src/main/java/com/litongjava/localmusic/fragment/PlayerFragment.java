package com.litongjava.localmusic.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.litongjava.android.utils.toast.ToastUtils;
import com.litongjava.android.view.inject.annotation.FindViewById;
import com.litongjava.android.view.inject.annotation.OnClick;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.localmusic.R;
import com.litongjava.localmusic.constants.SPConstants;
import com.litongjava.localmusic.instance.ExoPlayerInstance;
import com.litongjava.localmusic.properties.MemoryPropKeys;
import com.litongjava.localmusic.utils.AssetUtils;
import com.litongjava.localmusic.utils.WaveEncoder;
import com.whispercppdemo.whisper.WhisperContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class PlayerFragment extends Fragment {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private WhisperContext whisperContext;

  @FindViewById(R.id.musicTitile)
  private TextView musicTitile;
  @FindViewById(R.id.styled_player_view)
  private StyledPlayerView playerView;

  @FindViewById(R.id.playCurrentTracksTextView)
  public TextView playCurrentTracksTextView;

  @FindViewById(R.id.playMaxTracksTextView)
  public TextView playMaxTracksTextView;
  @FindViewById(R.id.gotoText)
  private EditText gotoText;

  @FindViewById(R.id.asrBtn)
  private Button asrBtn;

  @FindViewById(R.id.text)
  private TextView text;


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

  @OnClick(R.id.gotoBtn)
  public void gotoBtn_OnClick(View v) {
    SimpleExoPlayer exoPlayer = ExoPlayerInstance.getExoPlayer();
    String text = gotoText.getText().toString();
    log.info("seek to text :{}", text);
    long l = Long.parseLong(text);
    log.info("seek to:{}", l);
    exoPlayer.seekTo(l);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @OnClick(R.id.loadModelBtn)
  public void loadModelBtn_OnClick(View v) {
    loadModel();
    ToastUtils.defaultToast(getContext(), "model loaded");

  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @OnClick(R.id.asrBtn)
  public void asrBtn_OnClick(View v) {
    // 加载模型
    loadModel();
    //识别样本
    transcribeSample();
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private void loadModel() {
    Context context = getContext();
    File filesDir = context.getFilesDir();
    String modelFilePath = "models/ggml-tiny.bin";
    File modelFile = AssetUtils.copyFileIfNotExists(context, filesDir, modelFilePath);
    modelFilePath = modelFile.getAbsolutePath();

    log.info("load model from :{}", modelFilePath);
    if (whisperContext == null) {
      whisperContext = WhisperContext.createContextFromFile(modelFilePath);
    }
  }

  private void transcribeSample() {
    Context context = getContext();
    File filesDir = context.getFilesDir();
    String sampleFilePath = "samples/jfk.wav";
    File sampleFile = AssetUtils.copyFileIfNotExists(context, filesDir, sampleFilePath);
    // 识别样本
    log.info("transcribe file from :{}", sampleFile.getAbsolutePath());
    float[] audioData = new float[0];  // 读取音频样本
    try {
      audioData = WaveEncoder.decodeWaveFile(sampleFile);
    } catch (IOException e) {
      e.printStackTrace();
    }

    String transcription = null;  // 转录音频数据
    try {
      transcription = whisperContext.transcribeData(audioData);
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    log.info("Transcription: {}", transcription);  // 打印转录结果
    text.setText(transcription);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (whisperContext != null) {
      try {
        whisperContext.release();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        whisperContext = null;
      }
    }
  }

  private void referesh() {
    playCurrentTracksTextView.setText("Current:" + ExoPlayerInstance.currentTrackIndex);
    SharedPreferences sharedPreferences = Aop.get(SharedPreferences.class);
    int play_max_tracks = sharedPreferences.getInt(SPConstants.play_max_tracks, 0);
    playMaxTracksTextView.setText("Max:" + play_max_tracks);
  }

  private void showDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
    CharSequence[] items = {"1", "2", "3", "4"};
    builder.setTitle("Select a item")
      .setItems(items, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int selectedIndex) {
          // 在这里处理选择的选项
          SharedPreferences sharedPreferences = Aop.get(SharedPreferences.class);
          SharedPreferences.Editor editor = sharedPreferences.edit();
          editor.putInt(SPConstants.play_max_tracks, selectedIndex + 1);
          editor.apply();
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