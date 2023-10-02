package com.litongjava.localmusic.callback;

import android.widget.ImageView;

import androidx.viewpager2.widget.ViewPager2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Ping E Lee
 * @email itonglinux@qq.com
 * @date 2022/1/25
 */

public class ViewPager2OnPageChangeCallback extends ViewPager2.OnPageChangeCallback {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private List<ImageView> imageViews;

  public ViewPager2OnPageChangeCallback(List<ImageView> imageViews) {
    this.imageViews = imageViews;
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    super.onPageScrolled(position, positionOffset, positionOffsetPixels);
  }

  @Override
  public void onPageSelected(int position) {
    super.onPageSelected(position);
    changeFragment(position);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    super.onPageScrollStateChanged(state);
  }


  private void changeFragment(int position) {
    log.info("position:{}", position);
    for (int i = 0; i < imageViews.size(); i++) {
      ImageView imageView = imageViews.get(i);
      int ivid = imageView.getId();
      if (i == position) {
        log.info("{}的状态为激活", ivid);
        imageView.setSelected(true);
      } else {
        log.info("{}的状态为不激活", ivid);
        imageView.setSelected(false);
      }
    }

//    switch (position){
//      case 0:
//        break;
//
//      case 1:
//        break;
//
//      case 2:
//        break;
//
//      case 3:
//        break;
//    }
  }
}