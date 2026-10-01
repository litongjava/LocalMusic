package com.litongjava.localmusic;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class PersonalLibraryTest {
    PersonalLibrary db;
    Track one = new Track("one","content://music/one","Song.No.1.mp3","album-a","Album","Music");
    Track two = new Track("two","content://music/two","Second.mp3","album-b","Album","Other");
    @Before public void open(){db=new PersonalLibrary(RuntimeEnvironment.getApplication());}
    @After public void close(){db.close();}
    @Test public void renameKeepsTagsFavoritesPlaylistAndNotes(){
        String playlist=db.createPlaylist("Rename test");db.addToPlaylist(playlist,Collections.singletonList(one));db.toggleFavorite(one);db.addNote(one,1234,"Keep this note");
        db.setTags(one,"Work, work, 访谈, #重要");assertEquals("Work, 访谈, 重要",db.tagIndex().get(one.id));
        Track updated=new Track("new-document-id","content://music/new","New name.mp3",one.albumId,one.album,one.source);
        db.renamed(one,updated);assertNull(db.savedTrack(one.id));assertEquals(updated.id,db.playlistTracks(playlist).get(0).id);
        assertTrue(db.isFavorite(updated.id));assertEquals(1234,db.notes(updated.id).get(0).position);assertEquals("New name",db.notes(updated.id).get(0).track.title);
        db.close();db=new PersonalLibrary(RuntimeEnvironment.getApplication());assertEquals("Work, 访谈, 重要",db.tagIndex().get(updated.id));
        db.setTags(updated,"");assertFalse(db.tagIndex().containsKey(updated.id));
    }
    @Test public void invalidTagsDoNotEraseExistingTags(){
        db.setTags(one,"Existing");try{db.setTags(one,String.join("",Collections.nCopies(41,"x")));fail();}catch(IllegalArgumentException expected){}
        assertEquals("Existing",db.tagIndex().get(one.id));
    }
    @Test public void playlistsPersistDeduplicateAndRename(){
        String id=db.createPlaylist("  夜晚散步  ");db.addToPlaylist(id,Arrays.asList(one,two,one));
        db.close();db=new PersonalLibrary(RuntimeEnvironment.getApplication());
        assertEquals("夜晚散步",db.playlists().get(0).name);assertEquals(2,db.playlists().get(0).count);
        assertEquals("Song.No.1",db.playlistTracks(id).get(0).title);
        db.renamePlaylist(id,"午后");assertEquals("午后",db.playlists().get(0).name);
        db.removeFromPlaylist(id,one.id);assertEquals(two.id,db.playlistTracks(id).get(0).id);
    }
    @Test public void deletingPlaylistKeepsFavoritesAndNotes(){
        String id=db.createPlaylist("Mix");db.addToPlaylist(id,Collections.singletonList(one));db.toggleFavorite(one);
        db.addNote(one,12500,"这一段很喜欢");db.deletePlaylist(id);
        assertTrue(db.playlists().isEmpty());assertEquals(1,db.favorites().size());assertEquals(1,db.notes(null).size());
    }
    @Test public void favoriteSnapshotCanBeUpdatedWithoutBreakingReferences(){
        String id=db.createPlaylist("Mix");db.addToPlaylist(id,Collections.singletonList(one));assertTrue(db.toggleFavorite(one));
        db.addNote(one,3000,"note");assertEquals(1,db.playlistTracks(id).size());
        assertFalse(db.toggleFavorite(one));assertTrue(db.favorites().isEmpty());assertEquals(1,db.notes(null).size());
    }
    @Test public void albumFavoritesUseIdentityNotDisplayName(){
        assertTrue(db.toggleAlbum("a","Same name","Music"));assertTrue(db.toggleAlbum("b","Same name","Other"));
        assertEquals(2,db.albums().size());assertFalse(db.toggleAlbum("a","Same name","Music"));assertTrue(db.isAlbumFavorite("b"));
    }
    @Test public void notesPreserveTimestampTrackAndBodyAcrossRestart(){
        String id=db.addNote(one,73512,"  听到这里，记下一点想法。  ");db.close();db=new PersonalLibrary(RuntimeEnvironment.getApplication());
        PersonalLibrary.Note note=db.notes(one.id).get(0);assertEquals(73512,note.position);assertEquals(one.uri,note.track.uri);
        assertEquals("听到这里，记下一点想法。",note.body);assertTrue(db.notes(two.id).isEmpty());
        db.editNote(id,"修改之后");assertEquals("修改之后",db.notes(null).get(0).body);assertEquals(73512,db.notes(null).get(0).position);
        db.deleteNote(id);assertTrue(db.notes(null).isEmpty());
    }
    @Test public void blankInputIsRejected(){
        try{db.createPlaylist("  ");fail();}catch(IllegalArgumentException expected){}
        try{db.addNote(one,0," \n ");fail();}catch(IllegalArgumentException expected){}
        assertTrue(db.playlists().isEmpty());assertTrue(db.notes(null).isEmpty());
    }
    @Test public void duplicateNamesAreRejected(){
        db.createPlaylist("Mix");try{db.createPlaylist("mix");fail();}catch(android.database.sqlite.SQLiteConstraintException expected){}
        assertEquals(1,db.playlists().size());
    }
    @Test public void addingToRemovedPlaylistDoesNotCreateOrphanRows(){
        String id=db.createPlaylist("Mix");db.deletePlaylist(id);
        try{db.addToPlaylist(id,Collections.singletonList(one));}catch(android.database.sqlite.SQLiteConstraintException expected){}
        assertTrue(db.playlistTracks(id).isEmpty());
    }
}
