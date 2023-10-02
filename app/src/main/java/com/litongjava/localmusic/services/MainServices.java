package com.litongjava.localmusic.services;

import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.litongjava.localmusic.R;

/**
 * @author Ping E Lee
 * @email itonglinux@qq.com
 * @date 2023-08-19
 */
public class MainServices {
  public void replaceFragemnt(FragmentActivity activity, Fragment fragment) {
    FragmentManager fragmentManager = activity.getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.fragmentContainer, fragment);
    // 添加事务到返回栈，以便用户可以通过返回键返回到源 Fragment
    fragmentTransaction.commit();
  }

  public void switchToFragment(FragmentActivity activity, Fragment fragment, ImageView imageView1, ImageView... imageViews) {
    imageView1.setSelected(true);
    for (ImageView imageView : imageViews) {
      imageView.setSelected(false);
    }
//    BlankFragment04 fragment01 = BlankFragment04.newInstance("微信");
    this.replaceFragemnt(activity, fragment);


  }
}
