package com.litongjava.localmusic;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.IOException;

public final class AudioRenamer {
    public static String validate(String base) {
        String name=base.trim();
        if(name.isEmpty()||name.equals(".")||name.equals("..")||name.length()>120||java.util.regex.Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]").matcher(name).find())throw new IllegalArgumentException();
        return name;
    }
    public static Track rename(Context context,Track track,String base)throws Exception {
        base=validate(base);Uri old=Uri.parse(track.uri);String filename;
        if("file".equals(old.getScheme()))filename=new File(old.getPath()).getName();
        else if(DocumentsContract.isDocumentUri(context,old)) {
            try(Cursor c=context.getContentResolver().query(old,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_FLAGS},null,null,null)) {
                if(c==null||!c.moveToFirst()||(c.getLong(1)&DocumentsContract.Document.FLAG_SUPPORTS_RENAME)==0)throw new IOException("Read only");
                filename=c.getString(0);
            }
        } else throw new IOException("Read only");
        int dot=filename.lastIndexOf('.');String name=base+(dot>0?filename.substring(dot):"");
        if(name.equals(filename))return track;
        Uri renamed;String id=track.id;
        if("file".equals(old.getScheme())) {
            File source=new File(old.getPath()),target=new File(source.getParentFile(),name);
            if(target.exists()||!source.renameTo(target))throw new IOException("Rename failed or name exists");
            renamed=Uri.fromFile(target);
        } else {
            // Check sibling names before asking a provider that might overwrite a collision.
            String doc=DocumentsContract.getDocumentId(old);int slash=doc.lastIndexOf('/');
            String parent=slash>=0?doc.substring(0,slash):doc.contains(":")?doc.substring(0,doc.indexOf(':')+1):null;
            if(parent==null)throw new IOException("Cannot check parent");
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(old,parent);
            try(Cursor c=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)) {
                if(c==null)throw new IOException("Cannot read parent");
                while(c.moveToNext())if(name.equalsIgnoreCase(c.getString(0)))throw new IOException("Name exists");
            }
            renamed=DocumentsContract.renameDocument(context.getContentResolver(),old,name);
            if(renamed==null)throw new IOException("Rename failed");
            id=renamed.getAuthority()+":"+DocumentsContract.getDocumentId(renamed);
        }
        return new Track(id,renamed.toString(),name,track.albumId,track.album,track.source);
    }
}
