package com.litongjava.localmusic;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/** Playback belongs to the service, never to the Activity lifecycle. */
public final class PlaybackService extends MediaSessionService {
    private MediaSession session;
    private ExoPlayer player;
    private final java.util.concurrent.ExecutorService artworkWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.util.LruCache<String,byte[]> artworkCache=new android.util.LruCache<>(8);
    private int artworkVersion;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sleep = () -> { if (player != null) player.pause(); };
    public static final String ACTION_SLEEP = "com.litongjava.localmusic.SLEEP";
    public static long sleepDeadline;

    @Override public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        int repeat = getSharedPreferences("playback", 0).getInt("repeat", Player.REPEAT_MODE_ALL);
        player.setRepeatMode(repeat);
        player.setShuffleModeEnabled(getSharedPreferences("playback", 0).getBoolean("shuffle", false));
        player.addListener(new Player.Listener() {
            @Override public void onMediaItemTransition(MediaItem item,int reason) { publishArtwork(item); }
            @Override public void onRepeatModeChanged(int mode) {
                getSharedPreferences("playback", 0).edit().putInt("repeat", mode).apply();
            }
            @Override public void onShuffleModeEnabledChanged(boolean enabled) {
                getSharedPreferences("playback", 0).edit().putBoolean("shuffle", enabled).apply();
            }
        });
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        session = new MediaSession.Builder(this, player).setSessionActivity(open).build();
    }
    private void publishArtwork(MediaItem item) {
        if(item==null||item.localConfiguration==null){artworkVersion++;return;}
        String key=item.mediaId+"\n"+item.localConfiguration.uri;
        int version=++artworkVersion;
        if(item.mediaMetadata.artworkData!=null)return;
        artworkWorker.submit(()->{
            byte[] image=artworkCache.get(key);
            if(image==null){image=CoverArtwork.load(getApplicationContext(),item.localConfiguration.uri);artworkCache.put(key,image);}
            final byte[] data=image;
            handler.post(()->{
                if(session==null||version!=artworkVersion)return;
                MediaItem current=player.getCurrentMediaItem();
                if(current==null||current.localConfiguration==null||!key.equals(current.mediaId+"\n"+current.localConfiguration.uri))return;
                // Replacing metadata for the same URI preserves position and playback state.
                MediaMetadata metadata=current.mediaMetadata.buildUpon().setArtworkData(data,MediaMetadata.PICTURE_TYPE_FRONT_COVER).build();
                player.replaceMediaItem(player.getCurrentMediaItemIndex(),current.buildUpon().setMediaMetadata(metadata).build());
            });
        });
    }
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return session; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SLEEP.equals(intent.getAction())) {
            handler.removeCallbacks(sleep);
            long delay = intent.getLongExtra("minutes", 0) * 60_000L;
            sleepDeadline = delay > 0 ? android.os.SystemClock.elapsedRealtime() + delay : 0;
            if (delay > 0) handler.postDelayed(sleep, delay);
        }
        return super.onStartCommand(intent, flags, startId);
    }
    @Override public void onTaskRemoved(Intent rootIntent) {
        if (!player.getPlayWhenReady() || player.getMediaItemCount() == 0 || player.getPlaybackState() == Player.STATE_ENDED) stopSelf();
    }
    @Override public void onDestroy() {
        artworkVersion++;artworkWorker.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        sleepDeadline = 0;
        if (session != null) { player.release(); session.release(); session = null; }
        super.onDestroy();
    }
}
