package com.litongjava.localmusic.utils;

public class AudioUtils {
  public static boolean isAudioFile(String fileExtension) {
    String[] audioExtensions = {"mp3", "wav", "aac", "ogg", "m4a"};
    for (String ext : audioExtensions) {
      if (ext.equals(fileExtension)) {
        return true;
      }
    }
    return false;
  }

}
