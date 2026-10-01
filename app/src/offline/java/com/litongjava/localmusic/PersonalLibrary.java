package com.litongjava.localmusic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;

/** Offline user data. Track snapshots keep notes and playlists intact across rescans. */
public final class PersonalLibrary extends SQLiteOpenHelper {
    private final Context context;
    public PersonalLibrary(Context context) { super(context.getApplicationContext(), "personal-library.db", null, 2); this.context=context.getApplicationContext(); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tracks (id TEXT PRIMARY KEY, uri TEXT NOT NULL, title TEXT NOT NULL, album_id TEXT NOT NULL, album TEXT NOT NULL, source TEXT NOT NULL)");
        db.execSQL("CREATE TABLE playlists (id TEXT PRIMARY KEY, name TEXT NOT NULL COLLATE NOCASE UNIQUE, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE playlist_tracks (playlist_id TEXT REFERENCES playlists(id) ON DELETE CASCADE, track_id TEXT REFERENCES tracks(id), added INTEGER NOT NULL, PRIMARY KEY(playlist_id,track_id))");
        db.execSQL("CREATE TABLE favorites (track_id TEXT PRIMARY KEY REFERENCES tracks(id), added INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE album_favorites (id TEXT PRIMARY KEY, name TEXT NOT NULL, source TEXT NOT NULL, added INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE notes (id TEXT PRIMARY KEY, track_id TEXT NOT NULL REFERENCES tracks(id), position INTEGER NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX notes_track ON notes(track_id, created)");
        createTags(db);
    }
    private void createTags(SQLiteDatabase db) { db.execSQL("CREATE TABLE IF NOT EXISTS track_tags (track_id TEXT NOT NULL REFERENCES tracks(id), tag TEXT NOT NULL COLLATE NOCASE, PRIMARY KEY(track_id,tag))"); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { if(oldVersion<2)createTags(db); }
    public void setTags(Track track,String input) {
        LinkedHashSet<String> tags=new LinkedHashSet<>();Set<String> normalized=new HashSet<>();
        for(String raw:input.split("[,，;；\\n]")){String tag=raw.trim().replaceFirst("^#","").trim();if(tag.isEmpty())continue;if(tag.length()>40)throw new IllegalArgumentException();if(normalized.add(tag.toLowerCase(Locale.ROOT)))tags.add(tag);}
        if(tags.size()>20)throw new IllegalArgumentException();
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try {remember(db,track);db.delete("track_tags","track_id=?",new String[]{track.id});
            for(String tag:tags){ContentValues v=new ContentValues();v.put("track_id",track.id);v.put("tag",tag);db.insertOrThrow("track_tags",null,v);}db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    public Map<String,String> tagIndex() {
        Map<String,String> result=new HashMap<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT track_id,tag FROM track_tags ORDER BY rowid",null)) {
            while(c.moveToNext()){String id=c.getString(0),existing=result.get(id);result.put(id,(existing==null?"":existing+", ")+c.getString(1));}
        }return result;
    }
    public void renamed(Track old,Track updated) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try {
            remember(db,updated);
            if(!old.id.equals(updated.id)) {
                ContentValues v=new ContentValues();v.put("track_id",updated.id);
                for(String table:new String[]{"playlist_tracks","favorites","notes","track_tags"}) {db.updateWithOnConflict(table,v,"track_id=?",new String[]{old.id},SQLiteDatabase.CONFLICT_IGNORE);db.delete(table,"track_id=?",new String[]{old.id});}
                db.delete("tracks","id=?",new String[]{old.id});
            }db.setTransactionSuccessful();
        }finally {db.endTransaction();}
    }
    private ContentValues snapshot(Track t) {
        ContentValues v = new ContentValues(); v.put("id", t.id); v.put("uri", t.uri); v.put("title", t.title);
        v.put("album_id", t.albumId); v.put("album", t.album); v.put("source", t.source); return v;
    }
    private void remember(SQLiteDatabase db, Track t) {
        ContentValues v = snapshot(t);
        // REPLACE would delete a referenced row before reinserting it.
        if (db.update("tracks", v, "id=?", new String[]{t.id}) == 0) db.insertOrThrow("tracks", null, v);
    }
    public static final class Playlist {
        public final String id, name; public final int count;
        Playlist(String id, String name, int count) { this.id = id; this.name = name; this.count = count; }
    }
    private String validName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 60) throw new IllegalArgumentException(context.getString(R.string.playlist_name_error));
        return trimmed;
    }
    public String createPlaylist(String name) {
        ContentValues v = new ContentValues(); String id = UUID.randomUUID().toString();
        v.put("id", id); v.put("name", validName(name)); v.put("created", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("playlists", null, v); return id;
    }
    public void renamePlaylist(String id, String name) {
        ContentValues v = new ContentValues(); v.put("name", validName(name));
        getWritableDatabase().update("playlists", v, "id=?", new String[]{id});
    }
    public void deletePlaylist(String id) { getWritableDatabase().delete("playlists", "id=?", new String[]{id}); }
    public List<Playlist> playlists() {
        List<Playlist> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT p.id,p.name,COUNT(pt.track_id) FROM playlists p LEFT JOIN playlist_tracks pt ON p.id=pt.playlist_id GROUP BY p.id ORDER BY p.created,p.rowid", null)) {
            while (c.moveToNext()) result.add(new Playlist(c.getString(0), c.getString(1), c.getInt(2)));
        }
        return result;
    }
    public void addToPlaylist(String id, List<Track> tracks) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            for (Track track : tracks) {
                remember(db, track); ContentValues v = new ContentValues(); v.put("playlist_id", id);
                v.put("track_id", track.id); v.put("added", System.currentTimeMillis());
                db.insertWithOnConflict("playlist_tracks", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public void removeFromPlaylist(String id, String trackId) {
        getWritableDatabase().delete("playlist_tracks", "playlist_id=? AND track_id=?", new String[]{id, trackId});
    }
    public List<Track> playlistTracks(String id) {
        return readTracks("SELECT t.* FROM tracks t JOIN playlist_tracks p ON p.track_id=t.id WHERE p.playlist_id=? ORDER BY p.added,p.rowid", new String[]{id});
    }
    public void refreshAlbumLabels(List<Track> tracks) {
        SQLiteDatabase db=getWritableDatabase();Set<String> seen=new HashSet<>();db.beginTransaction();
        try { for(Track track:tracks)if(seen.add(track.albumId)) {
            ContentValues album=new ContentValues();album.put("name",track.album);album.put("source",track.source);
            db.update("album_favorites",album,"id=?",new String[]{track.albumId});
            ContentValues snapshot=new ContentValues();snapshot.put("album",track.album);snapshot.put("source",track.source);
            db.update("tracks",snapshot,"album_id=?",new String[]{track.albumId});
        } db.setTransactionSuccessful(); } finally { db.endTransaction(); }
    }
    public void saveImported(Track track) { remember(getWritableDatabase(),track); }
    public List<Track> importedTracks() { return readTracks("SELECT * FROM tracks WHERE id LIKE 'external:%'",null); }
    public Track savedTrack(String id) {
        List<Track> matches = readTracks("SELECT * FROM tracks WHERE id=?", new String[]{id});
        return matches.isEmpty() ? null : matches.get(0);
    }
    private List<Track> readTracks(String sql, String[] args) {
        List<Track> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) { while (c.moveToNext()) result.add(track(c)); }
        return result;
    }
    private Track track(Cursor c) { return Track.restore(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)); }
    public boolean isFavorite(String id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM favorites WHERE track_id=?", new String[]{id})) { return c.moveToFirst(); }
    }
    public boolean toggleFavorite(Track track) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            if (isFavorite(track.id)) { db.delete("favorites", "track_id=?", new String[]{track.id}); db.setTransactionSuccessful(); return false; }
            remember(db, track); ContentValues v = new ContentValues(); v.put("track_id", track.id); v.put("added", System.currentTimeMillis());
            db.insertOrThrow("favorites", null, v); db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
    public List<Track> favorites() { return readTracks("SELECT t.* FROM tracks t JOIN favorites f ON f.track_id=t.id ORDER BY f.added DESC,f.rowid DESC", null); }
    public boolean isAlbumFavorite(String id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM album_favorites WHERE id=?", new String[]{id})) { return c.moveToFirst(); }
    }
    public boolean toggleAlbum(String id, String name, String source) {
        SQLiteDatabase db = getWritableDatabase();
        if (isAlbumFavorite(id)) { db.delete("album_favorites", "id=?", new String[]{id}); return false; }
        ContentValues v = new ContentValues(); v.put("id", id); v.put("name", name); v.put("source", source); v.put("added", System.currentTimeMillis());
        db.insertOrThrow("album_favorites", null, v); return true;
    }
    public static final class Album {
        public final String id, name, source;
        Album(String id, String name, String source) { this.id = id; this.name = name; this.source = source; }
    }
    public List<Album> albums() {
        List<Album> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,source FROM album_favorites ORDER BY added DESC,rowid DESC", null)) {
            while (c.moveToNext()) result.add(new Album(c.getString(0), c.getString(1), c.getString(2)));
        }
        return result;
    }
    public static final class Note {
        public final String id, body; public final Track track; public final long position, created;
        Note(String id, String body, Track track, long position, long created) {
            this.id = id; this.body = body; this.track = track; this.position = position; this.created = created;
        }
    }
    private String validNote(String body) {
        String trimmed = body.trim();
        if (trimmed.isEmpty() || trimmed.length() > 5000) throw new IllegalArgumentException(context.getString(R.string.note_body_error));
        return trimmed;
    }
    public String addNote(Track track, long position, String body) {
        String text = validNote(body), id = UUID.randomUUID().toString();
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            remember(db, track); ContentValues v = new ContentValues(); v.put("id", id); v.put("track_id", track.id);
            v.put("position", Math.max(0, position)); v.put("body", text); v.put("created", System.currentTimeMillis());
            db.insertOrThrow("notes", null, v); db.setTransactionSuccessful(); return id;
        } finally { db.endTransaction(); }
    }
    public void editNote(String id, String body) {
        ContentValues v = new ContentValues(); v.put("body", validNote(body)); getWritableDatabase().update("notes", v, "id=?", new String[]{id});
    }
    public void deleteNote(String id) { getWritableDatabase().delete("notes", "id=?", new String[]{id}); }
    public List<Note> notes(String trackId) {
        List<Note> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT t.*,n.id,n.body,n.position,n.created FROM notes n JOIN tracks t ON t.id=n.track_id"
                + (trackId == null ? "" : " WHERE n.track_id=?") + " ORDER BY n.created DESC,n.rowid DESC", trackId == null ? null : new String[]{trackId})) {
            while (c.moveToNext()) result.add(new Note(c.getString(6), c.getString(7), track(c), c.getLong(8), c.getLong(9)));
        }
        return result;
    }
}
