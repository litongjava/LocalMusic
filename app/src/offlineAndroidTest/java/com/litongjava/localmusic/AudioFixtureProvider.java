package com.litongjava.localmusic;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.*;

/** Silent WAV fixture served with a real temporary content-URI grant. */
public class AudioFixtureProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "audio/wav";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME});c.addRow(new Object[]{"qa-external.wav"});return c;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        File file=new File(getContext().getCacheDir(),"qa-external.wav");
        try(FileOutputStream out=new FileOutputStream(file)){
            int size=8000*2*30;ByteBuffer header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
            header.put("RIFF".getBytes("US-ASCII")).putInt(size+36).put("WAVEfmt ".getBytes("US-ASCII")).putInt(16).putShort((short)1).putShort((short)1).putInt(8000).putInt(16000).putShort((short)2).putShort((short)16).put("data".getBytes("US-ASCII")).putInt(size);
            out.write(header.array());out.write(new byte[size]);
        }catch(IOException e){throw new FileNotFoundException(e.getMessage());}
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] args){return 0;}
    @Override public int update(Uri uri,ContentValues v,String s,String[] args){return 0;}
}
