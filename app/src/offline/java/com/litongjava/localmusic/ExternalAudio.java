package com.litongjava.localmusic;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;

/** Copies a granted audio URI so its playback does not depend on a temporary grant. */
public final class ExternalAudio {
    public static Track importFile(Context context,Uri uri)throws Exception{
        if(uri==null||!("content".equals(uri.getScheme())||"file".equals(uri.getScheme())))throw new IOException("Unsupported URI");
        String name="file".equals(uri.getScheme())?new File(uri.getPath()).getName():uri.getLastPathSegment(),mime=context.getContentResolver().getType(uri);
        if("content".equals(uri.getScheme()))try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}
        if(name==null)name="Audio";
        if(!LibraryIndexer.isAudio(name)&&(mime==null||!(mime.startsWith("audio/")||mime.equals("application/ogg")||mime.equals("application/x-ogg"))))throw new IOException("Not audio");
        String key=LocalLyrics.key(uri.toString()),ext=LibraryIndexer.isAudio(name)?name.substring(name.lastIndexOf('.')):".audio";
        File dir=new File(context.getFilesDir(),"imported-audio");dir.mkdirs();File file=new File(dir,key+ext);
        android.util.AtomicFile atomic=new android.util.AtomicFile(file);FileOutputStream out=null;
        try(InputStream in=LocalLyrics.open(context,uri)){
            if(in==null)throw new IOException("No stream");out=atomic.startWrite();byte[] buffer=new byte[32768];int n;long total=0;
            while((n=in.read(buffer))!=-1){total+=n;if(total>1024L*1024*1024||Thread.currentThread().isInterrupted())throw new IOException("Import interrupted or too large");out.write(buffer,0,n);}
            if(total==0)throw new IOException("Empty audio");atomic.finishWrite(out);
        }catch(Exception e){if(out!=null)atomic.failWrite(out);throw e;}
        return new Track("external:"+key,Uri.fromFile(file).toString(),name,"external-audio",context.getString(R.string.opened_audio),context.getString(R.string.opened_audio));
    }
}
