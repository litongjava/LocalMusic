package com.litongjava.localmusic;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;

/** Vector controls independent of the phone's symbol font. */
public final class MusicIcon extends View {
    private final Paint p = new Paint(3);
    private final Path path = new Path();
    private String kind;
    private int tint = 0xFFD3D9E2;
    public MusicIcon(Context context, String kind, String description, OnClickListener action) {
        super(context); this.kind = kind; setContentDescription(description); setFocusable(true); setOnClickListener(action);
        GradientDrawable mask = new GradientDrawable(); mask.setColor(Color.WHITE); mask.setCornerRadius(100);
        setBackground(new RippleDrawable(ColorStateList.valueOf(0x226E7D91), null, mask));
    }
    public void setKind(String value) { if (!value.equals(kind)) { kind = value; invalidate(); } }
    public void setTint(int value) { tint = value; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.save();
        float size = Math.min(getWidth(), getHeight()) * (kind.equals("play") || kind.equals("pause") ? .69f : .53f);
        c.translate((getWidth()-size)/2, (getHeight()-size)/2); c.scale(size/24, size/24);
        p.setColor(tint); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.6f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        path.reset();
        switch (kind) {
            case "play": p.setStyle(Paint.Style.FILL); path.moveTo(6,3); path.lineTo(21,12); path.lineTo(6,21); path.close(); c.drawPath(path,p); break;
            case "pause": p.setStyle(Paint.Style.FILL); c.drawRoundRect(6,3,10,21,1,1,p); c.drawRoundRect(15,3,19,21,1,1,p); break;
            case "next": case "previous":
                if (kind.equals("previous")) { c.translate(24,0); c.scale(-1,1); }
                p.setStyle(Paint.Style.FILL); path.moveTo(5,5); path.lineTo(16,12); path.lineTo(5,19); path.close(); c.drawPath(path,p); c.drawRoundRect(17,5,19,19,.6f,.6f,p); break;
            case "down": path.moveTo(4,8); path.lineTo(12,16); path.lineTo(20,8); c.drawPath(path,p); break;
            case "back": path.moveTo(15,4); path.lineTo(7,12); path.lineTo(15,20); c.drawPath(path,p); break;
            case "more": p.setStyle(Paint.Style.FILL); for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1.5f,p); break;
            case "plus": c.drawLine(12,4,12,20,p); c.drawLine(4,12,20,12,p); break;
            case "search": c.drawCircle(10,10,7,p); c.drawLine(15,15,21,21,p); break;
            case "heart": case "heart-filled":
                if(kind.endsWith("filled"))p.setStyle(Paint.Style.FILL);
                path.moveTo(12,21); path.cubicTo(8,17,1,12,2,7); path.cubicTo(3,1,9,1,12,6); path.cubicTo(15,1,21,1,22,7); path.cubicTo(23,12,16,18,12,21); c.drawPath(path,p); break;
            case "note": c.drawRoundRect(3,4,20,21,3,3,p); c.drawLine(7,9,15,9,p); c.drawLine(7,13,13,13,p); c.drawLine(16,3,21,8,p); break;
            case "queue": p.setStyle(Paint.Style.FILL); path.moveTo(2,3);path.lineTo(7,6);path.lineTo(2,9);path.close();c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);c.drawLine(10,6,22,6,p);c.drawLine(3,13,22,13,p);c.drawLine(3,20,22,20,p);break;
            case "repeat": case "repeat-one": case "sequential":
                path.moveTo(18,3);path.lineTo(21,6);path.lineTo(18,9); c.drawPath(path,p);c.drawLine(6,6,21,6,p);c.drawArc(2,6,10,18,90,90,false,p);
                path.reset();path.moveTo(6,15);path.lineTo(3,18);path.lineTo(6,21);c.drawPath(path,p);c.drawLine(3,18,18,18,p);c.drawArc(14,6,22,18,-90,90,false,p);
                if(kind.equals("repeat-one")){p.setStyle(Paint.Style.FILL);p.setTextSize(8);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("1",10,15,p);} break;
            case "shuffle": c.drawLine(3,5,7,5,p);c.drawLine(7,5,17,19,p);c.drawLine(17,19,22,19,p);c.drawLine(3,19,7,19,p);c.drawLine(7,19,17,5,p);c.drawLine(17,5,22,5,p);c.drawLine(19,2,22,5,p);c.drawLine(19,8,22,5,p);c.drawLine(19,16,22,19,p);break;
            case "timer": c.drawCircle(12,13,9,p);c.drawLine(12,7,12,13,p);c.drawLine(12,13,16,15,p);c.drawLine(9,1,15,1,p);break;
            case "folder": path.moveTo(2,7);path.lineTo(2,4);path.lineTo(9,4);path.lineTo(12,7);path.lineTo(22,7);path.lineTo(22,21);path.lineTo(2,21);path.close();c.drawPath(path,p);break;
            default: c.drawCircle(12,12,9,p);c.drawLine(12,7,12,13,p);c.drawPoint(12,17,p);
        }
        c.restore();
    }
}
