package com.litongjava.localmusic;

import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LocaleAndImportTest {
    private final Context context=RuntimeEnvironment.getApplication();
    @Test public void appNameAndInterfaceFollowPhoneLocale(){
        for(String language:new String[]{"en","zh","fr"}){
            Configuration config=new Configuration(context.getResources().getConfiguration());config.setLocale(new Locale(language));Context localized=context.createConfigurationContext(config);
            assertEquals(language.equals("zh")?"本地音乐":"Local Music",localized.getString(R.string.app_name));
            assertEquals(language.equals("zh")?"查看笔记":"View notes",localized.getString(R.string.ui_view_notes));
            assertEquals(language.equals("zh")?"我的":"My Music",localized.getString(R.string.ui_my_music));
        }
    }
    @Test public void viewAndShareOggResolveToTheApp(){
        for(String action:new String[]{Intent.ACTION_VIEW,Intent.ACTION_SEND}){
            Intent intent=new Intent(action).setDataAndType(Uri.parse("content://files/recording.ogg"),"audio/ogg").addCategory(Intent.CATEGORY_DEFAULT);
            assertTrue(context.getPackageManager().queryIntentActivities(intent,0).stream().anyMatch(info->info.activityInfo.name.equals(MainActivity.class.getName())));
        }
    }
    @Test public void importedAudioAndLyricsSurviveRepositoryRecreation()throws Exception{
        File source=new File(context.getCacheDir(),"recording.ogg");try(FileOutputStream out=new FileOutputStream(source)){out.write(new byte[]{'O','g','g','S',1,2,3});}
        Track imported=ExternalAudio.importFile(context,Uri.fromFile(source));assertEquals("recording",imported.title);
        try(PersonalLibrary db=new PersonalLibrary(context)){db.saveImported(imported);}
        assertTrue("source deletion",source.delete());assertTrue(new File(context.getFilesDir(),"imported-audio/"+LocalLyrics.key(Uri.fromFile(source).toString())+".ogg").isFile());
        assertTrue(new LibraryRepository(context).scan().tracks.stream().anyMatch(t->t.id.equals(imported.id)));
        File lrc=new File(context.getCacheDir(),"recording.lrc");try(FileOutputStream out=new FileOutputStream(lrc)){out.write("[00:01.00]Test line\n[00:03.00]Next line".getBytes(StandardCharsets.UTF_8));}
        LocalLyrics.importFile(context,imported.id,Uri.fromFile(lrc));assertTrue(lrc.delete());assertEquals(2,LocalLyrics.load(context,imported).lines.size());
    }
    @Test public void rejectsNonAudioAndRemoteUris()throws Exception{
        for(String uri:new String[]{"https://example.com/song.mp3","file:///private/document.txt"}){
            try{ExternalAudio.importFile(context,Uri.parse(uri));fail("Unsupported input accepted");}catch(IOException expected){}
        }
    }
}
