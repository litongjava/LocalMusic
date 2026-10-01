package com.litongjava.localmusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.media3.common.*;
import androidx.media3.session.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlaybackIntegrationTest {
    private MediaController controller;
    private Context context;
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            main(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        fail("Timed out waiting for playback state");
    }
    @Before public void connect() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.startActivity(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        CountDownLatch connected = new CountDownLatch(1); AtomicReference<Throwable> error = new AtomicReference<>();
        main(() -> {
            ListenableFuture<MediaController> future = new MediaController.Builder(context,
                    new SessionToken(context, new ComponentName(context, PlaybackService.class))).buildAsync();
            future.addListener(() -> {
                try { controller = future.get(); } catch (Exception e) { error.set(e); }
                connected.countDown();
            }, Runnable::run);
        });
        assertTrue(connected.await(15, TimeUnit.SECONDS)); assertNull(error.get()); assertNotNull(controller);
    }
    @After public void disconnect() {
        if (controller != null) main(() -> { controller.pause(); controller.setRepeatMode(Player.REPEAT_MODE_ALL); controller.setShuffleModeEnabled(false); controller.release(); });
    }
    private MediaItem song(String id) {
        return new MediaItem.Builder().setMediaId(id).setUri("asset:///music/Fool's garden/Lemon Tree.mp3")
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("Lemon Tree").setArtist("Fool's garden").build()).build();
    }
    private void start() throws Exception {
        main(() -> { controller.setMediaItems(Arrays.asList(song("one"), song("two")), 0, 0); controller.prepare(); controller.play(); });
        await(() -> controller.isPlaying());
    }
    @Test public void realMp3DecodesPausesSeeksAndSkips() throws Exception {
        start(); await(() -> controller.getCurrentPosition() > 300);
        main(() -> { assertTrue(controller.getDuration() > 180000); controller.pause(); });
        await(() -> !controller.isPlaying());
        AtomicLong paused = new AtomicLong(); main(() -> paused.set(controller.getCurrentPosition()));
        Thread.sleep(350); main(() -> assertEquals(paused.get(), controller.getCurrentPosition(), 100));
        main(() -> { controller.seekTo(30000); controller.play(); });
        await(() -> controller.isPlaying() && controller.getCurrentPosition() >= 30000);
        main(() -> controller.seekToNextMediaItem()); await(() -> controller.getCurrentMediaItemIndex() == 1);
        main(() -> controller.seekToPreviousMediaItem()); await(() -> controller.getCurrentMediaItemIndex() == 0);
        main(() -> assertNull(controller.getPlayerError()));
    }
    @Test public void externalAudioOpensAndPlaysAfterTemporaryGrantIsRevoked()throws Exception{
        Context test=InstrumentationRegistry.getInstrumentation().getContext();
        android.net.Uri uri=android.net.Uri.parse("content://com.litongjava.localmusic.test.audio/qa-external.wav");
        String id="external:"+LocalLyrics.key(uri.toString());
        java.io.File lrc=new java.io.File(context.getCacheDir(),"qa-external.lrc");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(lrc)){out.write("[00:00]QA lyric one\n[00:10]QA lyric two".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        LocalLyrics.importFile(context,id,android.net.Uri.fromFile(lrc));
        try{
            context.startActivity(new Intent().setComponent(new ComponentName(test.getPackageName(),AudioFixtureActivity.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(()->controller.getCurrentMediaItem()!=null&&controller.getCurrentMediaItem().mediaId.equals(id)&&controller.isPlaying());
            context.startActivity(new Intent("revoke").setComponent(new ComponentName(test.getPackageName(),AudioFixtureActivity.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            main(()->{controller.seekTo(5000);controller.play();});await(()->controller.getCurrentPosition()>5500&&controller.isPlaying());
            try(PersonalLibrary db=new PersonalLibrary(context)){assertNotNull(db.savedTrack(id));}
            clickUi(context.getString(R.string.lyrics_cover));
            clickUi("QA lyric two");await(()->controller.getCurrentPosition()>=10000);
            clickUi(context.getString(R.string.lyrics_back));
        }finally{
            main(()->{controller.pause();controller.clearMediaItems();});
            try(PersonalLibrary db=new PersonalLibrary(context)){Track t=db.savedTrack(id);if(t!=null)new java.io.File(android.net.Uri.parse(t.uri).getPath()).delete();db.getWritableDatabase().delete("tracks","id=?",new String[]{id});}
            context.startActivity(new Intent("revoke").setComponent(new ComponentName(test.getPackageName(),AudioFixtureActivity.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            lrc.delete();new java.io.File(context.getFilesDir(),"lyrics/"+LocalLyrics.key(id)+".lrc").delete();
        }
    }
    private android.view.accessibility.AccessibilityNodeInfo node(android.view.accessibility.AccessibilityNodeInfo root,String text){
        if(root==null)return null;
        if(text.contentEquals(root.getText()==null?"":root.getText())||text.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        for(int i=0;i<root.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=node(root.getChild(i),text);if(found!=null)return found;}return null;
    }
    private void clickUi(String text)throws Exception{
        long deadline=System.currentTimeMillis()+10000;
        do{android.view.accessibility.AccessibilityNodeInfo found=node(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow(),text);
            if(found!=null&&found.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){Thread.sleep(200);return;}Thread.sleep(100);
        }while(System.currentTimeMillis()<deadline);fail("Missing UI: "+text);
    }
    @Test public void repeatOneActuallyLoopsAtEndOfAudio() throws Exception {
        start();
        main(() -> {
            controller.setShuffleModeEnabled(false); controller.setRepeatMode(Player.REPEAT_MODE_ONE);
            controller.seekTo(controller.getDuration() - 600);
            assertTrue(controller.getCurrentPosition() > 180000);
        });
        // Verify the audible timeline wrapping, not a local-player-only transition callback.
        await(() -> controller.getCurrentPosition() < 5000 && controller.getCurrentMediaItemIndex() == 0 && controller.isPlaying());
        main(() -> { assertEquals(0, controller.getCurrentMediaItemIndex()); assertTrue(controller.getCurrentPosition() < 5000); assertTrue(controller.isPlaying()); });
    }
    @Test public void sequentialCompletionAndListWrap() throws Exception {
        start();
        main(() -> { controller.setShuffleModeEnabled(false); controller.setRepeatMode(Player.REPEAT_MODE_OFF); controller.seekTo(1, controller.getDuration() - 500); });
        await(() -> controller.getPlaybackState() == Player.STATE_ENDED);
        main(() -> { controller.setRepeatMode(Player.REPEAT_MODE_ALL); controller.seekTo(1, 189900); controller.play(); });
        await(() -> controller.getCurrentMediaItemIndex() == 0 && controller.isPlaying());
        main(() -> { controller.setShuffleModeEnabled(true); assertTrue(controller.getShuffleModeEnabled()); });
    }
    @Test public void platformMediaControlsWorkWhileActivityIsInBackground() throws Exception {
        start();
        android.app.UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        if (android.os.Build.VERSION.SDK_INT < 29) return;
        automation.adoptShellPermissionIdentity("android.permission.MEDIA_CONTENT_CONTROL");
        try {
            android.media.session.MediaSessionManager manager = (android.media.session.MediaSessionManager)
                    context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            AtomicReference<android.media.session.MediaController> platform = new AtomicReference<>();
            await(() -> {
                for (android.media.session.MediaController candidate : manager.getActiveSessions(null))
                    if (context.getPackageName().equals(candidate.getPackageName())) platform.set(candidate);
                return platform.get() != null;
            });
            await(() -> platformArtwork(platform.get())!=null);
            android.graphics.Bitmap artwork=platformArtwork(platform.get());
            assertEquals(512,artwork.getWidth());assertEquals(512,artwork.getHeight());
            assertNotEquals(artwork.getPixel(20,20),artwork.getPixel(350,160));
            context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AtomicLong position = new AtomicLong(); main(() -> position.set(controller.getCurrentPosition()));
            await(() -> controller.isPlaying() && controller.getCurrentPosition() > position.get() + 500);
            platform.get().getTransportControls().pause(); await(() -> !controller.getPlayWhenReady());
            platform.get().getTransportControls().play(); await(() -> controller.isPlaying());
            platform.get().getTransportControls().skipToNext(); await(() -> controller.getCurrentMediaItemIndex() == 1);
            await(() -> platformArtwork(platform.get())!=null);
            platform.get().getTransportControls().seekTo(10000); await(() -> controller.getCurrentPosition() >= 10000);
        } finally { automation.dropShellPermissionIdentity(); }
    }
    private android.graphics.Bitmap platformArtwork(android.media.session.MediaController controller){
        android.media.MediaMetadata metadata=controller.getMetadata();if(metadata==null)return null;
        android.graphics.Bitmap bitmap=metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART);
        if(bitmap==null)bitmap=metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART);
        if(bitmap==null)bitmap=metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        return bitmap;
    }
}
