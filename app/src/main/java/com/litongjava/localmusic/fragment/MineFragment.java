package com.litongjava.localmusic.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.litongjava.android.view.inject.annotation.OnClick;
import com.litongjava.android.view.inject.utils.ViewInjectUtils;
import com.litongjava.localmusic.R;

public class MineFragment extends Fragment {


  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_mine, container, false);
    ViewInjectUtils.injectViewAndOnClick(rootView, this);
    return rootView;
  }

  @OnClick(R.id.settingsTextView)
  public void settingsTextView_OnClick(View view) {
    //Toast.makeText(this.getActivity(), "settingsTextView click", Toast.LENGTH_LONG).show();
    //跳转到SettingsFragment
    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();

    // 替换当前容器中的 Fragment 为目标 Fragment
    transaction.replace(R.id.fragmentContainer, new SettingsFragment());

    // 添加事务到返回栈，以便用户可以通过返回键返回到源 Fragment
    transaction.addToBackStack(null);

    // 提交事务
    transaction.commit();
  }

  @OnClick(R.id.othersTextView)
  public void othersTextView_OnClick(View view) {
    Toast.makeText(this.getActivity(), "othersTextView click", Toast.LENGTH_LONG).show();
  }
}
