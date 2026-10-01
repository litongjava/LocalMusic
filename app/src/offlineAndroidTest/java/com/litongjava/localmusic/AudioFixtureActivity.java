package com.litongjava.localmusic;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;

/** Runs as the sender APK's UID, like a file manager. */
public class AudioFixtureActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);Uri uri=Uri.parse("content://com.litongjava.localmusic.test.audio/qa-external.wav");
        if("folder".equals(getIntent().getAction())){
            Uri tree=android.provider.DocumentsContract.buildTreeDocumentUri("com.litongjava.localmusic.test.rename","root");
            startActivity(new Intent(Intent.ACTION_MAIN).setData(tree).setComponent(new ComponentName("com.litongjava.localmusic","com.litongjava.localmusic.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));finish();return;
        }
        if("revoke".equals(getIntent().getAction()))revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        else startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"audio/wav").setComponent(new ComponentName("com.litongjava.localmusic","com.litongjava.localmusic.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION));
        finish();
    }
}
