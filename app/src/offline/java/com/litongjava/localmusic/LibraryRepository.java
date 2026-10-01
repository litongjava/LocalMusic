package com.litongjava.localmusic;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.*;

public final class LibraryRepository {
    private final Context context;
    public LibraryRepository(Context context) { this.context = context.getApplicationContext(); }
    public LibraryIndexer.Result scan() {
        List<LibraryIndexer.Node> roots = new ArrayList<>();
        roots.add(new AssetNode("music", context.getString(R.string.builtin_music)));
        List<String> warnings = new ArrayList<>();
        for (Uri uri : new DirectoryStore(context).get()) {
            try {
                String id = DocumentsContract.getTreeDocumentId(uri);
                roots.add(new DocumentNode(uri, id, folderName(uri, id), true));
            } catch (Exception e) { warnings.add(uri + ": " + context.getString(R.string.permission_expired)); }
        }
        LibraryIndexer.Result result = new LibraryIndexer().scan(roots);
        for (int i=0;i<result.warnings.size();i++) {
            String warning=result.warnings.get(i);int colon=warning.indexOf(':');
            result.warnings.set(i,warning.substring(colon+1)+": "+context.getString(warning.startsWith("ROOT:")?R.string.root_unreadable:R.string.folder_unreadable));
        }
        try(PersonalLibrary personal=new PersonalLibrary(context)){personal.refreshAlbumLabels(result.tracks);result.tracks.addAll(personal.importedTracks());}
        result.warnings.addAll(warnings);
        return result;
    }
    private String folderName(Uri tree,String id) {
        Uri document=DocumentsContract.buildDocumentUriUsingTree(tree,id);
        try(Cursor cursor=context.getContentResolver().query(document,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)) {
            if(cursor!=null&&cursor.moveToFirst()){String name=cursor.getString(0);if(name!=null&&!name.trim().isEmpty())return name;}
        }catch(Exception ignored) { }
        String name=id.substring(Math.max(id.lastIndexOf('/'),id.lastIndexOf(':'))+1);
        return name.isEmpty()?label(context,tree):name;
    }
    public static String label(Context context, Uri uri) {
        try {
            String id = DocumentsContract.getTreeDocumentId(uri);
            return id.replace("primary:", context.getString(R.string.internal_storage));
        } catch (Exception e) { return uri.toString(); }
    }
    private final class DocumentNode implements LibraryIndexer.Node {
        final Uri tree;
        final String docId, name;
        final boolean directory;
        DocumentNode(Uri tree, String docId, String name, boolean directory) {
            this.tree = tree; this.docId = docId; this.name = name; this.directory = directory;
        }
        public String id() { return tree.getAuthority() + ":" + docId; }
        public String uri() { return DocumentsContract.buildDocumentUriUsingTree(tree, docId).toString(); }
        public String name() { return name; }
        public boolean directory() { return directory; }
        public List<LibraryIndexer.Node> children() throws Exception {
            List<LibraryIndexer.Node> nodes = new ArrayList<>();
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId);
            String[] projection = { DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE };
            try (Cursor cursor = context.getContentResolver().query(children, projection, null, null, null)) {
                if (cursor == null) throw new IllegalStateException("Unavailable provider");
                while (cursor.moveToNext()) {
                    String childId = cursor.getString(0);
                    String name = cursor.getString(1);
                    nodes.add(new DocumentNode(tree, childId, name == null ? childId : name,
                            DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))));
                }
            }
            return nodes;
        }
    }
    private final class AssetNode implements LibraryIndexer.Node {
        final String path, name;
        AssetNode(String path, String name) { this.path = path; this.name = name; }
        public String id() { return "asset:" + path; }
        public String uri() { return new Uri.Builder().scheme("asset").path("/" + path).build().toString(); }
        public String name() { return name; }
        public boolean directory() { return !LibraryIndexer.isAudio(name); }
        public List<LibraryIndexer.Node> children() throws Exception {
            List<LibraryIndexer.Node> nodes = new ArrayList<>();
            String[] names = context.getAssets().list(path);
            // Some host-side AssetManager implementations return nested zip entries.
            // Normalize to immediate children, as Android's directory API normally does.
            Set<String> children = new LinkedHashSet<>();
            if (names != null) for (String entry : names) children.add(entry.split("/", 2)[0]);
            for (String name : children) nodes.add(new AssetNode(path + "/" + name, name));
            return nodes;
        }
    }
}
