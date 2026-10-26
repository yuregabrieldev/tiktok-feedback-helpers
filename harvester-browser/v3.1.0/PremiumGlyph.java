package com.harvester.browser.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Original VistaBrowser outline icon set. Lightweight, dependency-free,
 * accessible in every legacy Android WebView screen. No Candy assets copied.
 */
public final class PremiumGlyph extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String kind;
    private final int size;
    private int color;

    private PremiumGlyph(String kind,int color,int size){
        this.kind=kind;this.color=color;this.size=size;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.9f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        setBounds(0,0,size,size);
    }

    public static PremiumGlyph forSymbol(String text,int color,int size){
        if(text==null)return null;
        String k;
        switch(text){
            case "⌂": k="home"; break;
            case "+":case "＋": k="plus";break;
            case "⋮": k="menu";break;
            case "‹": k="back";break;
            case "›": k="forward";break;
            case "☷": k="sliders";break;
            case "⌕": k="search";break;
            case "▶":case "▷":k="play";break;
            case "⟳":k="reload";break;
            case "×":k="close";break;
            case "▤":k="library";break;
            case "◎":k="radar";break;
            case "◈":case "◇":k="diamond";break;
            case "◌":k="private";break;
            case "☆":case "★":k="star";break;
            case "◷":k="history";break;
            case "⇩":k="download";break;
            case "⇧":k="upload";break;
            case "▣":k="window";break;
            case "◉":k="dot";break;
            case "⚠":k="alert";break;
            case "ⓘ":k="info";break;
            case "⌫":k="delete";break;

            default:return null;
        }
        return new PremiumGlyph(k,color,size);
    }

    private void line(Canvas c,float a,float b,float x,float y){
        c.drawLine(a,b,x,y,paint);
    }
    private void circle(Canvas c,float x,float y,float r){
        c.drawCircle(x,y,r,paint);
    }
    private void rect(Canvas c,float l,float t,float r,float b,float radius){
        c.drawRoundRect(new RectF(l,t,r,b),radius,radius,paint);
    }
    private void poly(Canvas c,float... xy){
        Path p=new Path();p.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);
        c.drawPath(p,paint);
    }
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();
        int checkpoint=canvas.save();
        canvas.translate(b.left,b.top);
        canvas.scale(b.width()/24f,b.height()/24f);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.9f);
        switch(kind){
            case "home":
                poly(canvas,3,11,12,3,21,11);
                poly(canvas,5.5f,9,5.5f,21,18.5f,21,18.5f,9);
                poly(canvas,10,21,10,15,14,15,14,21);break;
            case "plus":
                line(canvas,12,4,12,20);line(canvas,4,12,20,12);break;
            case "menu":
                paint.setStyle(Paint.Style.FILL);
                circle(canvas,12,5,1.6f);circle(canvas,12,12,1.6f);circle(canvas,12,19,1.6f);break;
            case "back":poly(canvas,15,5,8,12,15,19);break;
            case "forward":poly(canvas,9,5,16,12,9,19);break;
            case "sliders":
                line(canvas,4,6,20,6);line(canvas,4,12,20,12);line(canvas,4,18,20,18);
                paint.setStyle(Paint.Style.FILL);
                circle(canvas,9,6,2.3f);circle(canvas,16,12,2.3f);circle(canvas,10,18,2.3f);break;
            case "search":
                circle(canvas,10.5f,10.5f,6.5f);line(canvas,15.5f,15.5f,21,21);break;
            case "play":
                Path p=new Path();p.moveTo(7,4);p.lineTo(20,12);p.lineTo(7,20);p.close();
                paint.setStyle(Paint.Style.FILL);canvas.drawPath(p,paint);break;
            case "reload":
                canvas.drawArc(new RectF(4,4,20,20),-60,290,false,paint);
                poly(canvas,15,3.8f,20.2f,4.3f,20.2f,9.6f);break;
            case "close":
                line(canvas,6,6,18,18);line(canvas,18,6,6,18);break;
            case "library":
                rect(canvas,3.5f,4,20.5f,20,2);
                line(canvas,8,8,17,8);line(canvas,8,12,17,12);line(canvas,8,16,14,16);break;
            case "radar":
                circle(canvas,12,12,8.5f);circle(canvas,12,12,4.5f);
                paint.setStyle(Paint.Style.FILL);circle(canvas,12,12,1.6f);break;
            case "diamond":
                poly(canvas,12,2.5f,21,12,12,21.5f,3,12,12,2.5f);break;
            case "private":
                circle(canvas,12,12,8);line(canvas,10,12,14,12);break;
            case "star":
                poly(canvas,12,2.8f,14.9f,8.7f,21.4f,9.6f,16.7f,14.2f,17.8f,20.8f,
                    12,17.7f,6.2f,20.8f,7.3f,14.2f,2.6f,9.6f,9.1f,8.7f,12,2.8f);break;
            case "history":
                circle(canvas,12,12,8.2f);
                poly(canvas,12,6.8f,12,12,16,14.5f);break;
            case "download":
                line(canvas,12,3,12,16);poly(canvas,6.7f,11,12,16.7f,17.3f,11);
                poly(canvas,4,18,4,21,20,21,20,18);break;
            case "upload":
                line(canvas,12,17,12,4);poly(canvas,6.7f,9.5f,12,4,17.3f,9.5f);
                poly(canvas,4,18,4,21,20,21,20,18);break;
            case "window":
                rect(canvas,3,4,21,20,2.5f);
                line(canvas,3,9,21,9);circle(canvas,7,6.5f,.7f);break;
            case "dot":
                circle(canvas,12,12,8.3f);paint.setStyle(Paint.Style.FILL);circle(canvas,12,12,3);break;
            case "alert":
                poly(canvas,12,3,22,21,2,21,12,3);
                line(canvas,12,9,12,14.5f);circle(canvas,12,18,0.5f);break;
            case "info":
                circle(canvas,12,12,9);line(canvas,12,10,12,17);
                paint.setStyle(Paint.Style.FILL);circle(canvas,12,7,1.2f);break;
            case "delete":
                poly(canvas,9,4,21,4,21,20,9,20,3,12,9,4);
                line(canvas,11,9,17,15);line(canvas,17,9,11,15);break;
        }
        canvas.restoreToCount(checkpoint);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth(){return size;}
    @Override public int getIntrinsicHeight(){return size;}
}