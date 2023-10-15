package com.litongjava.localmusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.litongjava.localmusic.R;
import com.litongjava.localmusic.constants.MessageConstants;
import com.litongjava.localmusic.model.MessageWrap;
import com.litongjava.localmusic.properties.MemoryPropKeys;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicRecyclerViewAdapter extends RecyclerView.Adapter<MusicRecyclerViewAdapter.AlbumViewHolder> {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private List<File> fileList;

  public MusicRecyclerViewAdapter(List<File> fileList) {
    this.fileList = fileList;
  }

  @NonNull
  @Override
  public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
    return new AlbumViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
    File file = fileList.get(position);
    String path = file.getAbsolutePath();
    holder.albumTextView.setText(file.getName());
    holder.albumTextView.setOnClickListener((v) -> {
      List<String> musicList = new ArrayList<>(fileList.size());
      List<String> folderList = new ArrayList<>();
      for (File f : fileList) {
        String absolutePath = f.getAbsolutePath();
        if (f.isDirectory()) {
          folderList.add(absolutePath);
        } else {
          musicList.add(absolutePath);
        }
      }

      boolean directory = file.isDirectory();
      log.info("file:{},isDirectory:{}", file.getName(), directory);
      if (directory) {
        //发送消息
        HashMap<String, Object> args = new HashMap<>(1);
        args.put("path", path);
        log.info("path:{}", path);
        EventBus.getDefault().post(MessageWrap.getInstance(MessageConstants.go_to_music_list, args));

      } else {
        MemoryPropKeys.current_music_path = path;
        Map<String, Object> playMusicArgs = new HashMap<>(3);
        playMusicArgs.put("music_list", musicList);
        playMusicArgs.put("folder_list", folderList);
        playMusicArgs.put("play_index", position);
        EventBus.getDefault().post(MessageWrap.getInstance(MessageConstants.play_music, playMusicArgs));
        HashMap<String, Object> args = new HashMap<>(1);
        EventBus.getDefault().post(MessageWrap.getInstance(MessageConstants.go_to_player, args));
      }


    });
  }


  @Override
  public int getItemCount() {
    return fileList.size();
  }

  public static class AlbumViewHolder extends RecyclerView.ViewHolder {

    TextView albumTextView;

    public AlbumViewHolder(@NonNull View itemView) {
      super(itemView);
      albumTextView = itemView.findViewById(R.id.albumNameTextView);
    }
  }
}
