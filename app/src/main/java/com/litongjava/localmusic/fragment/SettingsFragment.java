package com.litongjava.localmusic.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.litongjava.android.view.inject.annotation.OnClick;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.R;

public class SettingsFragment extends Fragment {

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
    ViewInjectUtils.injectViewAndOnClick(rootView, this);
    return rootView;
  }

  @OnClick(R.id.MusicPathTextView)
  public void onClick_MusicPathTextView(View view) {
    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.fragmentContainer, new MusicPathFragment());
    // 添加事务到返回栈，以便用户可以通过返回键返回到源 Fragment
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();

  }
}
