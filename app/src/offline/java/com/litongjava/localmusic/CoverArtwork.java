package com.litongjava.localmusic;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.ByteArrayOutputStream;

/** Shared artwork for the record surface and Android's media session. */
public final class CoverArtwork {
    private CoverArtwork() { }
    public static byte[] load(Context context,Uri uri) {
        MediaMetadataRetriever reader=new MediaMetadataRetriever();
        Bitmap bitmap=null;
        try {
            if("asset".equals(uri.getScheme())) {
                try(android.content.res.AssetFileDescriptor fd=context.getAssets().openFd(uri.getPath().replaceFirst("^/",""))) {
                    reader.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
                }
            } else reader.setDataSource(context,uri);
            byte[] embedded=reader.getEmbeddedPicture();
            if(embedded!=null) {
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(embedded,0,embedded.length,options);
                options.inSampleSize=Math.max(1,Math.max(options.outWidth,options.outHeight)/512);options.inJustDecodeBounds=false;
                bitmap=BitmapFactory.decodeByteArray(embedded,0,embedded.length,options);
            }
        } catch(Exception ignored) { }
        finally { try { reader.release(); } catch(Exception ignored) { } }
        if(bitmap==null) {
            bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);canvas.translate(256,256);drawFallback(canvas,256);
        }
        ByteArrayOutputStream output=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,output);bitmap.recycle();
        return output.toByteArray();
    }
    public static void drawFallback(Canvas c,float r){
        Paint p=new Paint(3);Path path=new Path();RectF rect=new RectF();
        Shader sleeve=new LinearGradient(-1,-1,1,1,new int[]{0xFF243A51,0xFF587875,0xFF182331},null,Shader.TileMode.CLAMP);
        c.save();c.scale(r,r);p.setShader(sleeve);c.drawRect(-1,-1,1,1,p);p.setShader(null);
        p.setColor(0xFFD3D5A5);c.drawCircle(.42f,-.38f,.24f,p);p.setColor(0x22283747);c.drawCircle(.32f,-.45f,.27f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.028f);p.setColor(0xFF111F25);
        path.reset();path.moveTo(-.55f,1);path.cubicTo(-.22f,.4f,-.18f,-.3f,.05f,-1);c.drawPath(path,p);
        for(int i=0;i<7;i++){float y=.65f-i*.24f;float x=-.43f+i*.045f;c.drawLine(x,y,x+(i%2==0?.6f:-.5f),y-.23f,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(0xFF718777);
        for(int i=0;i<6;i++){float x=(i%2==0?.2f:-.5f),y=.55f-i*.22f;rect.set(x-.16f,y-.07f,x+.15f,y+.035f);c.drawOval(rect,p);}
        c.restore();
    }
}
