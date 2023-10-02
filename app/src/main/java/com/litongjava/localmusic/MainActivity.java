package com.litongjava.localmusic;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.litongjava.android.utils.acp.AcpUtils;
import com.litongjava.android.utils.toast.ToastUtils;
import com.litongjava.android.view.inject.annotation.FindViewById;
import com.litongjava.android.view.inject.annotation.FindViewByIdLayout;
import com.litongjava.android.view.inject.annotation.OnClick;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.constants.MessageConstants;
import com.litongjava.localmusic.constants.SPConstants;
import com.litongjava.localmusic.fragment.MainFragment;
import com.litongjava.localmusic.fragment.MineFragment;
import com.litongjava.localmusic.fragment.MusicListFragment;
import com.litongjava.localmusic.fragment.PlayerFragment;
import com.litongjava.localmusic.instance.ExoPlayerInstance;
import com.litongjava.localmusic.model.MessageWrap;
import com.litongjava.localmusic.services.MainServices;
import com.mylhyl.acp.AcpListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FindViewByIdLayout(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
  private MainServices mainServices = new MainServices();

  private Logger log = LoggerFactory.getLogger(this.getClass());
  @FindViewById(R.id.fragmentContainer)
  private FrameLayout fragmentContainer;

  @FindViewById(R.id.bottom_weixin_linearLayout)
  private LinearLayout bottom_weixin_linearLayout;
  @FindViewById(R.id.bottom_contact_list_linearLayout)
  private LinearLayout bottom_contact_list_linearLayout;
  @FindViewById(R.id.bottom_find_linearLayout)
  private LinearLayout bottom_find_linearLayout;
  @FindViewById(R.id.bottom_profile_linearLayout)
  private LinearLayout bottom_profile_linearLayout;

  @FindViewById(R.id.bottom_weixin_imageView)
  private ImageView bottom_weixin_imageView;
  @FindViewById(R.id.bottom_contact_list_imageView)
  private ImageView bottom_contact_list_imageView;
  @FindViewById(R.id.bottom_find_imageView)
  private ImageView bottom_find_imageView;
  @FindViewById(R.id.bottom_profile_imageView)
  private ImageView bottom_profile_imageView;

  @FindViewById(R.id.bottom_weixin_textView)
  private TextView bottom_weixin_textView;
  @FindViewById(R.id.bottom_contact_list_textView)
  private TextView bottom_contact_list_textView;
  @FindViewById(R.id.bottom_find_textView)
  private TextView bottom_find_textView;
  @FindViewById(R.id.bottom_profile_textView)
  private TextView bottom_profile_textView;


  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    ViewInjectUtils.injectActivity(this, this);
    //注册事件
    EventBus.getDefault().register(this);
    initView();
    initPermission();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    ExoPlayerInstance.getExoPlayer().release();
  }

  private void initPermission() {
    String[] permissions = {
      //写入外部设备权限
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    //创建acpListener
    AcpListener acpListener = new AcpListener() {
      @Override
      public void onGranted() {
        ToastUtils.defaultToast(getApplicationContext(), "获取权限成功");
      }

      @Override
      public void onDenied(List<String> permissions) {
        ToastUtils.defaultToast(getApplicationContext(), permissions.toString() + "权限拒绝");
      }
    };

    AcpUtils.requestPermissions(this, permissions, acpListener);
  }

  private void initView() {
    //设置为默认是选中状态
    bottom_weixin_imageView.setSelected(true);
    replaceFragemnt(new MainFragment());
    // 创建媒体播放器
    ExoPlayerInstance.getInstance(this);

  }

  /**
   * 动态切换fragment
   *
   * @param fragment
   */
  private void replaceFragemnt(Fragment fragment) {
    mainServices.replaceFragemnt(this, fragment);
    getSupportFragmentManager();
  }


  @OnClick(R.id.bottom_weixin_imageView)
  public void bottom_weixin_imageView_OnClick(View view) {
    mainServices.switchToFragment(this, new MainFragment(), bottom_weixin_imageView, bottom_contact_list_imageView, bottom_find_imageView, bottom_profile_imageView);

  }

  @OnClick(R.id.bottom_contact_list_imageView)
  public void bottom_contact_list_textView_OnClick(View view) {
    mainServices.switchToFragment(this, new MusicListFragment(), bottom_contact_list_imageView, bottom_weixin_imageView, bottom_find_imageView, bottom_profile_imageView);
  }

  @OnClick(R.id.bottom_find_imageView)
  public void bottom_find_textView_OnClick(View view) {
    Fragment fragment01 = new PlayerFragment();
    mainServices.switchToFragment(this, fragment01, bottom_find_imageView, bottom_contact_list_imageView, bottom_weixin_imageView, bottom_profile_imageView);

  }

  @OnClick(R.id.bottom_profile_imageView)
  public void bottom_profile_textView_OnClick(View view) {
    MineFragment fragment = new MineFragment();
    mainServices.switchToFragment(this, fragment, bottom_profile_imageView, bottom_contact_list_imageView, bottom_weixin_imageView, bottom_find_imageView);
  }

  /**
   * 监听事件
   *
   * @param message
   */
  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onGetMessage(MessageWrap message) {
    String instructions = message.instructions;
    log.info("message:{}", instructions);
    if (MessageConstants.go_to_music_list.equals(instructions)) {
      Map<String, Object> args = message.args;
      String path = (String) args.get("path");
      //写入SP
      SharedPreferences sharedPreferences = super.getSharedPreferences(SPConstants.config_file_name, Context.MODE_PRIVATE);
      //获取sp编辑器
      SharedPreferences.Editor edit = sharedPreferences.edit();
      //添加key,value
      edit.putString(SPConstants.current_album_path, path);
      //写入到配置文件
      edit.apply();
      //跳转
      mainServices.switchToFragment(this, new MusicListFragment(), bottom_contact_list_imageView, bottom_weixin_imageView, bottom_find_imageView, bottom_profile_imageView);
    }

    if (MessageConstants.go_to_player.equals(instructions)) {
      //跳转
      mainServices.switchToFragment(this, new PlayerFragment(), bottom_find_imageView, bottom_contact_list_imageView, bottom_weixin_imageView, bottom_profile_imageView);
    }
    if (MessageConstants.play_music.equals(instructions)) {
      //    simpleExoPlayer.clearMediaItems();
      Integer playIndex = (Integer) message.args.get("play_index");
      List<String> musicList = (List<String>) message.args.get("music_list");
      List<MediaItem> mediaItems = new ArrayList<>(musicList.size());
      for (String musicPath : musicList) {
        MediaItem mediaItem = MediaItem.fromUri(musicPath);
        mediaItems.add(mediaItem);
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

    }
  }
}