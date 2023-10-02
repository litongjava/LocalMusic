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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.StringUtils;
import com.litongjava.android.view.inject.annotation.FindViewById;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.R;
import com.litongjava.localmusic.adapter.MusicRecyclerViewAdapter;
import com.litongjava.localmusic.constants.SPConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicListFragment extends Fragment {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @FindViewById(R.id.musicItems)
  private RecyclerView recyclerView;
  private MusicRecyclerViewAdapter fileAdapter;
  private List<File> fileList;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_music, container, false);
    ViewInjectUtils.injectViewAndOnClick(rootView, this);
    recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

    SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(SPConstants.config_file_name, Context.MODE_PRIVATE);
    String string = sharedPreferences.getString(SPConstants.current_album_path, "");
    loadFiles(string);
    return rootView;
  }


  private void loadFiles(String currentAlbumPath) {
    if (StringUtils.isEmpty(currentAlbumPath)) {
      return;
    }

    File musicDir = new File(currentAlbumPath);
    if (musicDir.exists() && musicDir.isDirectory()) {
      File[] files = musicDir.listFiles();
      if (files != null) {
        fileList = new ArrayList<>(files.length);
        for (File file : files) {
          fileList.add(file);
        }
        log.info("fileList size:{}", fileList.size());
        fileAdapter = new MusicRecyclerViewAdapter(fileList);
        recyclerView.setAdapter(fileAdapter);
        fileAdapter.notifyDataSetChanged();
      }
    }
  }
}