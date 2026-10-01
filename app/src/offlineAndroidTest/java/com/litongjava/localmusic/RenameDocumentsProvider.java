package com.litongjava.localmusic;

import android.database.*;
import android.os.*;
import android.provider.DocumentsProvider;
import android.provider.DocumentsContract.Document;
import java.io.*;

/** Isolated writable directory for exercising real SAF rename calls. */
public class RenameDocumentsProvider extends DocumentsProvider {
    private File directory;
    @Override public boolean onCreate(){directory=new File(getContext().getCacheDir(),"rename-fixture");directory.mkdirs();return true;}
    private File file(String id){String name=id.substring(id.lastIndexOf('/')+1);if(name.contains("..")||name.contains("\\"))throw new IllegalArgumentException();return new File(directory,name);}
    @Override public Cursor queryRoots(String[] projection){return new MatrixCursor(new String[]{"root_id","document_id","title","flags"});}
    private MatrixCursor cursor(String[] projection){return new MatrixCursor(projection==null?new String[]{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS}:projection);}
    private void row(MatrixCursor c,String id){
        MatrixCursor.RowBuilder row=c.newRow();for(String column:c.getColumnNames()){
            if(column.equals(Document.COLUMN_DOCUMENT_ID))row.add(id);
            else if(column.equals(Document.COLUMN_DISPLAY_NAME))row.add(id.equals("root")?"Rename QA":file(id).getName());
            else if(column.equals(Document.COLUMN_MIME_TYPE))row.add(id.equals("root")?Document.MIME_TYPE_DIR:"audio/wav");
            else if(column.equals(Document.COLUMN_FLAGS))row.add(id.equals("root")?0:Document.FLAG_SUPPORTS_RENAME|Document.FLAG_SUPPORTS_WRITE);
            else row.add(null);
        }
    }
    @Override public Cursor queryDocument(String id,String[] projection){MatrixCursor c=cursor(projection);row(c,id);return c;}
    @Override public Cursor queryChildDocuments(String parent,String[] projection,String sort)throws FileNotFoundException{
        File original=new File(directory,"original.wav");
        if(directory.list().length==0)try(FileOutputStream out=new FileOutputStream(original)){out.write(new byte[]{1,2,3,4});}catch(IOException e){throw new FileNotFoundException();}
        MatrixCursor c=cursor(projection);for(File f:directory.listFiles())row(c,"root/"+f.getName());return c;
    }
    @Override public String renameDocument(String id,String name)throws FileNotFoundException{File source=file(id),dest=file("root/"+name);if(dest.exists()||!source.renameTo(dest))throw new FileNotFoundException();return "root/"+name;}
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)throws FileNotFoundException{return ParcelFileDescriptor.open(file(id),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public boolean isChildDocument(String parent,String child){return parent.equals("root")&&child.startsWith("root/");}
}
