package com.litongjava.localmusic;

import android.app.UiAutomation;
import android.content.*;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PersonalUiIntegrationTest {
    private final UiAutomation ui=InstrumentationRegistry.getInstrumentation().getUiAutomation();
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String value,boolean description,boolean edit){
        if(node==null)return null;
        if(edit&&"android.widget.EditText".contentEquals(node.getClassName()))return node;
        CharSequence actual=description?node.getContentDescription():node.getText();
        if(!edit&&actual!=null&&value.equalsIgnoreCase(actual.toString()))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo result=find(node.getChild(i),value,description,edit);if(result!=null)return result;}
        return null;
    }
    private AccessibilityNodeInfo waitFor(String value,boolean description,boolean edit)throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.res.Configuration config=new android.content.res.Configuration(context.getResources().getConfiguration());config.setLocale(Locale.CHINESE);
        Context zh=context.createConfigurationContext(config);
        for(java.lang.reflect.Field field:R.string.class.getFields()){int id=field.getInt(null);if(zh.getString(id).equals(value)){value=context.getString(id);break;}}
        long deadline=System.currentTimeMillis()+10000;
        do{AccessibilityNodeInfo node=find(ui.getRootInActiveWindow(),value,description,edit);if(node!=null)return node;Thread.sleep(150);}while(System.currentTimeMillis()<deadline);
        throw new AssertionError("UI element missing: "+value);
    }
    private void click(String value,boolean description)throws Exception{
        AccessibilityNodeInfo node=waitFor(value,description,false);
        while(node!=null){if(node.isClickable()){assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(300);return;}node=node.getParent();}
        fail("Element is not clickable: "+value);
    }
    private void enter(String value)throws Exception{
        AccessibilityNodeInfo input=waitFor("text field",false,true);Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
        assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args));Thread.sleep(250);
    }
    @Test public void createPlaylistFavoriteAndTimestampedNoteThroughRealUi()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();PersonalLibrary db=new PersonalLibrary(context);
        String name="QA-"+System.currentTimeMillis(),noteText="QA note "+System.currentTimeMillis();
        Map<String,String> originalTags=db.tagIndex();String tag="QA-tag-"+System.nanoTime();
        Track lemon=null;for(Track t:new LibraryRepository(context).scan().tracks)if(t.title.equals("Lemon Tree")){lemon=t;break;}
        assertNotNull(lemon);boolean wasFavorite=db.isFavorite(lemon.id), wasAlbumFavorite=db.isAlbumFavorite(lemon.albumId);
        try{
            context.startActivity(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            click(context.getString(R.string.ui_my),false);click("新建歌单",true);enter(name);click("保存",false);
            String playlistId=null;for(PersonalLibrary.Playlist p:db.playlists())if(p.name.equals(name))playlistId=p.id;assertNotNull(playlistId);
            click("首页",true);enter("Lemon");click("Lemon Tree",false);
            click(wasFavorite?"取消收藏歌曲":"收藏歌曲",true);assertEquals(!wasFavorite,db.isFavorite(lemon.id));
            click(context.getString(R.string.ui_song_options),true);click(context.getString(R.string.edit_tags),false);enter(tag);click("保存",false);
            click("收起播放器",true);click("首页",true);enter(tag);click("Lemon Tree",false);
            click("加入歌单",true);click(name,false);assertEquals(1,db.playlistTracks(playlistId).size());
            click("添加播放笔记",true);enter(noteText);click("保存",false);
            PersonalLibrary.Note note=null;for(PersonalLibrary.Note n:db.notes(lemon.id))if(n.body.equals(noteText))note=n;
            assertNotNull(note);assertTrue(note.position>0);assertEquals(lemon.id,note.track.id);
            click("收起播放器",true);click("笔记",true);click(noteText,false);click("播放此处",false);
            waitFor("添加播放笔记",true,false);click("暂停",true);
            click("收起播放器",true);click(context.getString(R.string.ui_my),true);click("我的更多选项",true);click("设置",false);waitFor("＋ 添加音乐目录",false,false);
            click("返回",true);click("合集",true);enter("");click("Fool's garden",false);
            click(context.getString(R.string.album_options),true);
            click(context.getString(wasAlbumFavorite?R.string.album_unsave:R.string.album_save),false);
            assertEquals(!wasAlbumFavorite,db.isAlbumFavorite(lemon.albumId));
        }finally{
            for(PersonalLibrary.Playlist p:db.playlists())if(p.name.equals(name))db.deletePlaylist(p.id);
            for(PersonalLibrary.Note n:db.notes(null))if(n.body.equals(noteText))db.deleteNote(n.id);
            if(db.isFavorite(lemon.id)!=wasFavorite)db.toggleFavorite(lemon);
            if(db.isAlbumFavorite(lemon.albumId)!=wasAlbumFavorite)db.toggleAlbum(lemon.albumId,lemon.album,lemon.source);
            db.setTags(lemon,originalTags.containsKey(lemon.id)?originalTags.get(lemon.id):"");
            db.close();
        }
    }
}
