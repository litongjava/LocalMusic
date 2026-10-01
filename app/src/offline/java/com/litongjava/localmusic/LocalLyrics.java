package com.litongjava.localmusic;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class LocalLyrics {
    private static final int LIMIT=1024*1024;
    public static String key(String id) throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    private static File saved(Context c,String id)throws Exception {File dir=new File(c.getFilesDir(),"lyrics");dir.mkdirs();return new File(dir,key(id)+".lrc");}
    public static void importFile(Context c,String id,Uri uri)throws Exception {
        byte[] bytes;try(InputStream in=c.getContentResolver().openInputStream(uri)){bytes=read(in,LIMIT);}
        String content=decode(bytes);if(Lyrics.parse(content).lines.isEmpty())throw new IOException("Empty lyrics");
        android.util.AtomicFile file=new android.util.AtomicFile(saved(c,id));FileOutputStream out=null;
        try{out=file.startWrite();out.write(content.getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
    public static Lyrics load(Context c,Track track){
        try{File local=saved(c,track.id);if(local.isFile())try(InputStream in=new FileInputStream(local)){return Lyrics.parse(decode(read(in,LIMIT)));}}catch(Exception ignored){}
        Uri uri=Uri.parse(track.uri);
        try{String sidecar=sidecar(c,uri);if(sidecar!=null)return Lyrics.parse(sidecar);}catch(Exception ignored){}
        try(InputStream in=open(c,uri)){return Lyrics.parse(embedded(in));}catch(Exception ignored){return Lyrics.parse("");}
    }
    public static void preserveAfterRename(Context c,String id,Lyrics lyrics)throws Exception {
        if(lyrics.lines.isEmpty())return;
        StringBuilder text=new StringBuilder();
        for(Lyrics.Line line:lyrics.lines){if(lyrics.timed)text.append(String.format(Locale.ROOT,"[%02d:%02d.%03d]",line.time/60000,(line.time/1000)%60,line.time%1000));text.append(line.text).append('\n');}
        android.util.AtomicFile file=new android.util.AtomicFile(saved(c,id));FileOutputStream out=null;
        try{out=file.startWrite();out.write(text.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
    private static String sidecar(Context c,Uri uri)throws Exception{
        String path=uri.getPath();
        if("asset".equals(uri.getScheme())||"file".equals(uri.getScheme())){
            if(path==null||path.lastIndexOf('.')<0)return null;
            String lrc=path.substring(0,path.lastIndexOf('.'))+".lrc";
            try(InputStream in="asset".equals(uri.getScheme())?c.getAssets().open(lrc.replaceFirst("^/","")):new FileInputStream(lrc)){return decode(read(in,LIMIT));}
        }
        if(DocumentsContract.isDocumentUri(c,uri)&&uri.getPathSegments().size()>1&&"tree".equals(uri.getPathSegments().get(0))){
            String id=DocumentsContract.getDocumentId(uri);int slash=id.lastIndexOf('/');if(slash<0)return null;
            String name=id.substring(slash+1);int dot=name.lastIndexOf('.');if(dot<0)return null;String expected=name.substring(0,dot)+".lrc";
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(uri,id.substring(0,slash));
            try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                while(cursor!=null&&cursor.moveToNext())if(expected.equalsIgnoreCase(cursor.getString(1))){
                    try(InputStream in=open(c,DocumentsContract.buildDocumentUriUsingTree(uri,cursor.getString(0)))){return decode(read(in,LIMIT));}
                }
            }
        }
        return null;
    }
    static InputStream open(Context c,Uri uri)throws Exception{
        if("asset".equals(uri.getScheme()))return c.getAssets().open(uri.getPath().replaceFirst("^/",""));
        if("file".equals(uri.getScheme()))return new FileInputStream(uri.getPath());
        return c.getContentResolver().openInputStream(uri);
    }
    static byte[] read(InputStream in,int limit)throws IOException{
        if(in==null)throw new IOException("No stream");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("File too large");out.write(buffer,0,n);}return out.toByteArray();
    }
    static String decode(byte[] bytes){
        if(bytes.length>=2&&((bytes[0]&255)==255&&(bytes[1]&255)==254||(bytes[0]&255)==254&&(bytes[1]&255)==255))return new String(bytes,StandardCharsets.UTF_16);
        String value=new String(bytes,StandardCharsets.UTF_8);return value.indexOf('\uFFFD')>=0?new String(bytes,Charset.forName("GB18030")):value;
    }
    /** ID3v2.3/2.4 USLT: common embedded unsynchronised lyrics. */
    static String embedded(InputStream stream)throws IOException{
        if(stream==null)return "";DataInputStream in=new DataInputStream(stream);byte[] head=new byte[10];in.readFully(head);
        if(head[0]!='I'||head[1]!='D'||head[2]!='3'||head[3]<3||head[3]>4||(head[5]&0xC0)!=0)return "";
        int size=synchsafe(head,6);if(size<0||size>2*LIMIT)return "";byte[] tag=new byte[size];in.readFully(tag);
        for(int p=0;p+10<=tag.length;){
            String id=new String(tag,p,4,StandardCharsets.ISO_8859_1);int n=head[3]==4?synchsafe(tag,p+4):((tag[p+4]&255)<<24)|((tag[p+5]&255)<<16)|((tag[p+6]&255)<<8)|(tag[p+7]&255);
            if(n<=0||n>tag.length-p-10)break;
            if(id.equals("USLT")&&n>=5&&tag[p+9]==0){
                int start=p+10,encoding=tag[start]&255,step=encoding==1||encoding==2?2:1,words=start+4;
                while(words+step<=start+n){boolean zero=tag[words]==0&&(step==1||tag[words+1]==0);words+=step;if(zero)break;}
                Charset charset=encoding==1?StandardCharsets.UTF_16:encoding==2?StandardCharsets.UTF_16BE:encoding==3?StandardCharsets.UTF_8:StandardCharsets.ISO_8859_1;
                if(words<=start+n)return new String(tag,words,start+n-words,charset).replace("\u0000","").trim();
            }
            p+=10+n;
        }return "";
    }
    private static int synchsafe(byte[] data,int p){return (data[p]&127)<<21|(data[p+1]&127)<<14|(data[p+2]&127)<<7|(data[p+3]&127);}
}
