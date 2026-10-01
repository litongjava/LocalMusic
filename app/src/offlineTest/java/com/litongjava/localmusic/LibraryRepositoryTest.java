package com.litongjava.localmusic;

import android.content.Context;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LibraryRepositoryTest {
    @Test public void packagedSongIsDiscoverableAndReadableOffline() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        LibraryIndexer.Result result = new LibraryRepository(context).scan();
        assertEquals(1, result.tracks.size()); assertTrue(result.warnings.isEmpty());
        Track song = result.tracks.get(0);
        assertEquals("Lemon Tree", song.title); assertEquals("Fool's garden", song.album);
        assertEquals("asset", Uri.parse(song.uri).getScheme());
        try (java.io.InputStream input = context.getAssets().open(Uri.parse(song.uri).getPath().substring(1))) {
            byte[] header = new byte[3]; assertEquals(3, input.read(header)); assertEquals("ID3", new String(header, "US-ASCII"));
        }
    }
    @Test public void packagedLemonTreeLyricsHaveAnOfflineTimeline() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        Track song=new LibraryRepository(context).scan().tracks.get(0);
        Lyrics lyrics=LocalLyrics.load(context,song);
        assertTrue(lyrics.timed);assertTrue(lyrics.lines.size()>30);
        assertEquals(0,lyrics.lines.get(0).time);
        assertTrue(lyrics.active(15000)>0);
    }
    @Test public void missingProviderProducesWarningWhileBundledMusicSurvives() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("library", 0).edit()
                .putString("roots", "[\"content://missing.provider/tree/primary%3AMusic\"]").commit();
        LibraryIndexer.Result result = new LibraryRepository(context).scan();
        assertEquals(1, result.tracks.size()); assertEquals(1, result.warnings.size());
    }
    @Test public void storedDirectoriesSurviveNewStoreInstanceAndRemoval() {
        Context context = RuntimeEnvironment.getApplication();
        String uri = "content://test/tree/primary%3AMusic";
        context.getSharedPreferences("library", 0).edit().putString("roots", "[\"" + uri + "\"]").commit();
        assertEquals(Uri.parse(uri), new DirectoryStore(context).get().get(0));
        new DirectoryStore(context).remove(Uri.parse(uri));
        assertTrue(new DirectoryStore(context).get().isEmpty());
    }
}
