package com.litongjava.localmusic;

import android.content.Context;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class AudioRenamerTest {
    @Test public void invalidNamesAndAssetsAreRejected()throws Exception{
        for(String name:new String[]{"","../file","a/b","a\\b",".","bad:name","a\nname"}){
            try{AudioRenamer.validate(name);fail(name);}catch(IllegalArgumentException expected){}
        }
        Track asset=new Track("asset:one","asset:///music/song.mp3","song.mp3","a","a","a");
        try{AudioRenamer.rename(RuntimeEnvironment.getApplication(),asset,"New");fail();}catch(IOException expected){}
    }
}
