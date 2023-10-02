package com.litongjava.localmusic.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.litongjava.android.view.inject.annotation.FindViewById;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.R;

public class BlankFragment04 extends Fragment {

  private static final String ARG_TEXT = "text_content";
  private String textContent;

  private View rootView = null;

  @FindViewById(R.id.textView01)
  private TextView textView01;

  public static BlankFragment04 newInstance(String text) {
    BlankFragment04 fragment = new BlankFragment04();
    Bundle args = new Bundle();
    args.putString(ARG_TEXT, text);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      textContent = getArguments().getString(ARG_TEXT);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    if (rootView == null) {
      rootView = inflater.inflate(R.layout.fragment_blank04, container, false);
    }
    ViewInjectUtils.injectViewAndOnClick(rootView,this);
    initView();
    return rootView;
  }

  private void initView() {
    textView01.setText(textContent);
  }
}