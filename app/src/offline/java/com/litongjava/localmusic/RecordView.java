package com.litongjava.localmusic;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Vinyl, sleeve art and tonearm drawn at device resolution. */
public final class RecordView extends View {
    private final Paint p = new Paint(3);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private Bitmap artwork;
    private boolean playing;
    private float angle;
    private long lastFrame;
    private float armProgress;
    private Shader vinyl;
    public RecordView(Context context) { super(context); setContentDescription(context.getString(R.string.lyrics_cover)); }
    public void setPlaying(boolean value) { if (playing != value) { playing = value; lastFrame = 0; invalidate(); } }
    public void setArtwork(Bitmap bitmap) { artwork = bitmap; invalidate(); }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        vinyl = new SweepGradient(0,0,new int[]{0xFF070809,0xFF242729,0xFF090A0B,0xFF2A2C2C,0xFF070809},null);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx=getWidth()/2f, r=Math.min(getWidth()*.46f,getHeight()*.365f), cy=getHeight()*.60f;
        c.save();c.translate(cx,cy);
        p.setStyle(Paint.Style.FILL);p.setColor(0x253B536B);c.drawCircle(0,0,r+12,p);
        p.setColor(0xFF4A596A);c.drawCircle(0,0,r+2,p);
        p.setShader(vinyl);c.drawCircle(0,0,r,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);
        for(float v=r*.60f;v<r-3;v+=3){p.setColor(((int)v%2==0)?0xFF252728:0xFF17191A);c.drawCircle(0,0,v,p);}
        p.setColor(0xFF07090A);p.setStrokeWidth(3);c.drawCircle(0,0,r*.61f,p);p.setStyle(Paint.Style.FILL);
        c.save();c.rotate(angle);path.reset();path.addCircle(0,0,r*.59f,Path.Direction.CW);c.clipPath(path);
        if(artwork!=null){rect.set(-r*.59f,-r*.59f,r*.59f,r*.59f);c.drawBitmap(artwork,null,rect,p);}
        else CoverArtwork.drawFallback(c,r*.59f);
        c.restore();p.setColor(0xFFD5D8D7);c.drawCircle(0,0,r*.025f,p);p.setColor(0xFF0D131B);c.drawCircle(0,0,r*.012f,p);c.restore();
        float target = playing ? 1f : 0f;
        armProgress += Math.max(-.06f, Math.min(.06f, target-armProgress));
        float ax=cx, ay=Math.max(12,cy-r*1.48f);
        p.setColor(0xFF27384D);c.drawCircle(ax,ay,17,p);p.setColor(0xFFF2F3F4);c.drawCircle(ax,ay,9,p);
        p.setColor(0xFFBAC6D2);c.drawCircle(ax,ay,4,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setStrokeCap(Paint.Cap.ROUND);
        // Pivot the entire arm: the playing cartridge lands inside the vinyl's outer groove.
        c.save(); c.rotate(armProgress*24f, ax, ay);
        path.reset();path.moveTo(ax+4,ay+6);path.cubicTo(ax+r*.29f,ay+r*.48f,ax+r*.35f,ay+r*.50f,ax+r*.77f,ay+r*.65f);c.drawPath(path,p);
        c.save();c.translate(ax+r*.77f,ay+r*.65f);c.rotate(19);p.setStyle(Paint.Style.FILL);p.setColor(0xFFECF0F4);rect.set(-3,-7,19,8);c.drawRoundRect(rect,3,3,p);
        p.setColor(0xFF97A6B3);rect.set(8,-4,16,-2);c.drawRect(rect,p);rect.set(8,2,16,4);c.drawRect(rect,p);c.restore();
        c.restore();
        if(isShown() && (playing || Math.abs(target-armProgress)>.001f)){long now=android.os.SystemClock.uptimeMillis();if(playing&&lastFrame!=0)angle=(angle+(now-lastFrame)*.010f)%360;lastFrame=now;postInvalidateOnAnimation();}
    }
}
