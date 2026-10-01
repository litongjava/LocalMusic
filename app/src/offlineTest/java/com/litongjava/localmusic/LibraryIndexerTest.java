package com.litongjava.localmusic;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LibraryIndexerTest {
    private static class Node implements LibraryIndexer.Node {
        final String id, name; final boolean dir; final List<LibraryIndexer.Node> children = new ArrayList<>();
        boolean fail;
        Node(String id, String name, boolean dir, Node... children) {
            this.id = id; this.name = name; this.dir = dir; this.children.addAll(Arrays.asList(children));
        }
        public String id() { return id; }
        public String uri() { return "content://test/" + id; }
        public String name() { return name; }
        public boolean directory() { return dir; }
        public List<LibraryIndexer.Node> children() throws Exception {
            if (fail) throw new SecurityException("Revoked permission");
            return children;
        }
    }
    private Node song(String id, String name) { return new Node(id, name, false); }
    private Node dir(String id, String name, Node... children) { return new Node(id, name, true, children); }
    private LibraryIndexer.Result scan(Node... roots) { return new LibraryIndexer().scan(Arrays.asList(roots)); }

    @Test public void directChildrenBecomeAlbumsAndDeepTracksKeepTopAlbum() {
        LibraryIndexer.Result result = scan(dir("root", "Music", dir("album", "Fool's garden",
                song("one", "Lemon Tree.mp3"), dir("deep", "Disc 2", song("two", "Track.FLAC")))));
        assertEquals(2, result.tracks.size());
        for (Track track : result.tracks) { assertEquals("Fool's garden", track.album); assertEquals("album", track.albumId); }
    }
    @Test public void rootAudioGetsRootAlbum() {
        Track track = scan(dir("root", "Music", song("one", "hello.wav"))).tracks.get(0);
        assertEquals("Music", track.album); assertEquals("hello", track.title);
    }
    @Test public void overlappingDirectoriesNeverDuplicateTracks() {
        Node album = dir("album", "Same", song("song", "one.mp3"));
        assertEquals(1, scan(dir("root", "Music", album), album, album).tracks.size());
    }
    @Test public void sameAlbumNameInDifferentRootsStaysSeparate() {
        List<Track> tracks = scan(dir("a", "One", dir("a/album", "Shared", song("a/1", "One.mp3"))),
                dir("b", "Two", dir("b/album", "Shared", song("b/1", "Two.mp3")))).tracks;
        assertEquals(2, tracks.size()); assertNotEquals(tracks.get(0).albumId, tracks.get(1).albumId);
    }
    @Test public void imagesLyricsAndDirectoriesAreNotQueued() {
        assertEquals(1, scan(dir("root", "Music", song("1", "audio.MP3"), song("2", "cover.jpg"),
                song("3", "audio.lrc"), dir("4", "fake.mp3"))).tracks.size());
    }
    @Test public void unreadableRootDoesNotHideHealthyRoots() {
        Node bad = dir("bad", "Revoked"); bad.fail = true;
        LibraryIndexer.Result result = scan(bad, dir("ok", "Available", song("s", "Song.ogg")));
        assertEquals(1, result.tracks.size()); assertEquals(1, result.warnings.size());
    }
    @Test public void failedNestedDirectoryDoesNotAbortSiblingTracks() {
        Node bad = dir("bad", "Lost"); bad.fail = true;
        LibraryIndexer.Result result = scan(dir("root", "Music", dir("album", "Album", bad, song("s", "Song.opus"))));
        assertEquals(1, result.tracks.size()); assertEquals(1, result.warnings.size());
    }
    @Test public void directoryCyclesTerminate() {
        Node album = dir("album", "Album", song("s", "Song.m4a")); album.children.add(album);
        assertEquals(1, scan(dir("root", "Music", album)).tracks.size());
    }
    @Test public void emptyAndNonAudioLibrariesAreEmpty() {
        assertTrue(scan(dir("root", "Music", song("r", "README.txt"))).tracks.isEmpty());
        assertTrue(scan().tracks.isEmpty());
    }
    @Test public void searchMatchesTitleOrAlbumIgnoringCaseAndWhitespace() {
        Track track = new Track("1", "file:///song", "Lemon Tree.mp3", "a", "Fool's garden", "Built in");
        assertTrue(track.matches(" LEMON ")); assertTrue(track.matches("GARDEN"));
        assertFalse(track.matches("other")); assertTrue(track.matches(""));
    }
    @Test public void sortingIsStableAndCaseInsensitive() {
        List<Track> tracks = scan(dir("root", "Music", song("z", "Zoo.mp3"), song("a", "apple.mp3"))).tracks;
        assertEquals("apple", tracks.get(0).title); assertEquals("Zoo", tracks.get(1).title);
    }
    @Test public void supportedExtensionsAreExplicit() {
        for (String suffix : new String[]{"mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "oga", "amr", "3gp"})
            assertTrue(LibraryIndexer.isAudio("some name." + suffix));
        assertFalse(LibraryIndexer.isAudio("mp3")); assertFalse(LibraryIndexer.isAudio("sample.mp3.jpg"));
    }
}
