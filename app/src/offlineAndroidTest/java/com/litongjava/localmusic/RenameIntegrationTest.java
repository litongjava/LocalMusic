package com.litongjava.localmusic;

import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RenameIntegrationTest {
    @Test public void safRenameChangesFileAndPreservesPersonalData()throws Exception{
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();Context test=InstrumentationRegistry.getInstrumentation().getContext();
        Uri tree=DocumentsContract.buildTreeDocumentUri("com.litongjava.localmusic.test.rename","root");
        c.startActivity(new Intent("folder").setComponent(new ComponentName(test.getPackageName(),AudioFixtureActivity.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        boolean granted=false;for(int i=0;i<50&&!granted;i++){try{new DirectoryStore(c).add(tree);granted=true;}catch(SecurityException e){Thread.sleep(100);}}
        assertTrue(granted);PersonalLibrary db=new PersonalLibrary(c);String playlist=null;Track renamed=null,original=null;
        try{
            for(Track t:new LibraryRepository(c).scan().tracks)if(t.id.startsWith("com.litongjava.localmusic.test.rename:"))original=t;
            assertNotNull(original);assertEquals("Rename QA",original.album);
            playlist=db.createPlaylist("Rename QA "+System.nanoTime());db.addToPlaylist(playlist,Collections.singletonList(original));db.toggleFavorite(original);db.addNote(original,1234,"QA rename note");db.setTags(original,"QA-tag, 访谈");
            renamed=AudioRenamer.rename(c,original,"Renamed QA");db.renamed(original,renamed);
            assertNotEquals(original.uri,renamed.uri);assertEquals("Renamed QA",db.playlistTracks(playlist).get(0).title);assertTrue(db.isFavorite(renamed.id));assertEquals("QA-tag, 访谈",db.tagIndex().get(renamed.id));assertEquals(1234,db.notes(renamed.id).get(0).position);
            try(java.io.InputStream in=c.getContentResolver().openInputStream(Uri.parse(renamed.uri))){assertArrayEquals(new byte[]{1,2,3,4},LocalLyrics.read(in,100));}
            assertTrue(new LibraryRepository(c).scan().tracks.stream().anyMatch(t->t.title.equals("Renamed QA")));
        }finally{
            if(playlist!=null)db.deletePlaylist(playlist);
            for(Track t:new Track[]{original,renamed})if(t!=null){db.getWritableDatabase().delete("notes","track_id=?",new String[]{t.id});db.getWritableDatabase().delete("favorites","track_id=?",new String[]{t.id});db.getWritableDatabase().delete("track_tags","track_id=?",new String[]{t.id});db.getWritableDatabase().delete("tracks","id=?",new String[]{t.id});}
            if(renamed!=null)AudioRenamer.rename(c,renamed,"original");
            db.close();new DirectoryStore(c).remove(tree);
        }
    }
}
