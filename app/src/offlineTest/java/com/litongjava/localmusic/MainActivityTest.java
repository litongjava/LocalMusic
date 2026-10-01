package com.litongjava.localmusic;

import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh")
public class MainActivityTest {
    private View find(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = find(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
    @Test public void settingsAndPlayerNavigationSurviveActivityLifecycle() {
        // Real MediaSession binding/decoding is covered by the on-device suite.
        Shadows.shadowOf(org.robolectric.RuntimeEnvironment.getApplication()).declareComponentUnbindable(
                new android.content.ComponentName(org.robolectric.RuntimeEnvironment.getApplication(), PlaybackService.class));
        try (ActivityController<MainActivity> owner = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = owner.get(); View root = activity.getWindow().getDecorView();
            assertNotNull(find(root, "本地音乐"));
            find(root, "我的").performClick(); assertNotNull(find(root, "我的音乐空间"));
            assertNotNull(find(root, "我喜欢的音乐")); assertNotNull(find(root, "新建歌单"));
            find(root, "选择一首，开始聆听").performClick(); assertNotNull(find(root, "写笔记"));
            activity.onBackPressed(); assertNotNull(find(root, "我的音乐空间"));
            owner.pause().stop().start().resume();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(find(root, "我的音乐空间"));
        }
    }
}
