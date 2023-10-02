package com.litongjava.localmusic.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.litongjava.android.view.inject.annotation.FindViewById;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.R;
import com.litongjava.localmusic.adapter.MainAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @FindViewById(R.id.albumItems)
  private RecyclerView recyclerView;
  private MainAdapter fileAdapter;
  private List<File> fileList;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    ViewInjectUtils.injectViewAndOnClick(rootView, this);
    recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

    loadFiles();
    return rootView;
  }


  private void loadFiles() {
    String musicPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
//    /storage/emulated/0/Music
//    log.info("musicPath:{}", musicPath);
    File musicDir = new File(musicPath);
    if (musicDir.exists() && musicDir.isDirectory()) {
      File[] files = musicDir.listFiles();
      if (files != null) {
        fileList = new ArrayList<>(files.length);
        for (File file : files) {
          fileList.add(file);
        }
        log.info("fileList size:{}", fileList.size());
        fileAdapter = new MainAdapter(fileList);
        recyclerView.setAdapter(fileAdapter);
        fileAdapter.notifyDataSetChanged();
      }
    }
  }
}