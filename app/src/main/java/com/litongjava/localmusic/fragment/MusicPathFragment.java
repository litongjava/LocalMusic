package com.litongjava.localmusic.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.litongjava.android.view.inject.annotation.OnClick;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.R;
import com.litongjava.localmusic.constants.SPConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;

public class MusicPathFragment extends Fragment {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_music_path, container, false);
    ViewInjectUtils.injectViewAndOnClick(rootView, this);
    return rootView;
  }

  @OnClick(R.id.MusicPathTextView)
  public void onClick_MusicPathTextView(View view) {

    SharedPreferences sharedPreferences = super.getActivity().getSharedPreferences("watermark", Context.MODE_PRIVATE);
    String string = sharedPreferences.getString(SPConstants.musicPath, "/Music");
    //放弃开发
  }
}
