package com.litongjava.localmusic.instance;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExoPlayerInstanceTest {

  @Test
  public void test01() {
    int currentTrackIndex = 1;
    int playMaxTracks = 1;
    boolean b = currentTrackIndex % 1 >= playMaxTracks;
    System.out.println(b);
  }

}