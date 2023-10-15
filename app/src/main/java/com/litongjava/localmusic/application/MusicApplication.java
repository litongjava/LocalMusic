package com.litongjava.localmusic.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.localmusic.constants.SPConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicApplication extends Application {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void onCreate() {
    log.info("onCreate");
    super.onCreate();
    initAop();
  }

  private void initAop() {
    SharedPreferences sharedPreferences = this.getBaseContext().getSharedPreferences(SPConstants.config_file_name, Context.MODE_PRIVATE);
    log.info("add SharedPreferences:{}", sharedPreferences);
    AopManager.me().addMapping(SharedPreferences.class, sharedPreferences.getClass());
    AopManager.me().addSingletonObject(sharedPreferences);
  }
}