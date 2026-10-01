package com.litongjava.localmusic;

import java.util.Locale;

public final class Track {
    public final String id, uri, title, albumId, album, source;
    public Track(String id, String uri, String name, String albumId, String album, String source) {
        this(id, uri, name, albumId, album, source, false);
    }
    private Track(String id, String uri, String name, String albumId, String album, String source, boolean displayTitle) {
        this.id = id;
        this.uri = uri;
        int dot = name.lastIndexOf('.');
        this.title = !displayTitle && dot > 0 ? name.substring(0, dot) : name;
        this.albumId = albumId;
        this.album = album;
        this.source = source;
    }
    public static Track restore(String id, String uri, String title, String albumId, String album, String source) {
        return new Track(id, uri, title, albumId, album, source, true);
    }
    public boolean matches(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        return title.toLowerCase(Locale.ROOT).contains(q) || album.toLowerCase(Locale.ROOT).contains(q);
    }
}
