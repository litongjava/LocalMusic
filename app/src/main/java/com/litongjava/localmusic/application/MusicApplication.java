package com.litongjava.localmusic.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.localmusic.constants.SPConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MusicApplication extends Application {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void onCreate() {
    log.info("onCreate");
    super.onCreate();
    initAop();
    initLog();
  }

  private void initLog() {
    File logDir = new File(this.getBaseContext().getExternalFilesDir(null), "logs");
    log.info("path:{}", logDir.getAbsolutePath());
    if (!logDir.exists()) {
      boolean mkdirs = logDir.mkdirs();
      if (!mkdirs) {
        log.error("Failed to create log dir");
      }
    }

  }

  private void initAop() {
    SharedPreferences sharedPreferences = this.getBaseContext().getSharedPreferences(SPConstants.config_file_name, Context.MODE_PRIVATE);
    log.info("add SharedPreferences:{}", sharedPreferences);
    AopManager.me().addMapping(SharedPreferences.class, sharedPreferences.getClass());
    AopManager.me().addSingletonObject(sharedPreferences);
  }
}