package com.litongjava.localmusic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import java.util.*;

public final class DirectoryStore {
    private final Context context;
    public DirectoryStore(Context context) { this.context = context.getApplicationContext(); }
    public List<Uri> get() {
        List<Uri> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences("library", 0).getString("roots", "[]"));
            for (int i = 0; i < array.length(); i++) result.add(Uri.parse(array.getString(i)));
        } catch (Exception ignored) { }
        return result;
    }
    public void add(Uri uri) {
        try { context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }
        catch(SecurityException e){context.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        List<Uri> roots = get();
        if (!roots.contains(uri)) { roots.add(uri); save(roots); }
    }
    public void remove(Uri uri) {
        List<Uri> roots = get();
        roots.remove(uri);
        save(roots);
        try { for(android.content.UriPermission permission:context.getContentResolver().getPersistedUriPermissions())if(permission.getUri().equals(uri)) {
            int flags=(permission.isReadPermission()?Intent.FLAG_GRANT_READ_URI_PERMISSION:0)|(permission.isWritePermission()?Intent.FLAG_GRANT_WRITE_URI_PERMISSION:0);
            context.getContentResolver().releasePersistableUriPermission(uri,flags);
        } }
        catch (SecurityException ignored) { }
    }
    private void save(List<Uri> roots) {
        JSONArray array = new JSONArray();
        for (Uri uri : roots) array.put(uri.toString());
        context.getSharedPreferences("library", 0).edit().putString("roots", array.toString()).apply();
    }
}
