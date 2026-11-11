package com.harvester.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.Locale;

/**
 * Context actions for a media element actually touched in the main browser.
 * Uses a read-only, top-document DOM hit test. No JavaScript bridge is exposed
 * to websites, and cross-origin iframe contents are never inspected.
 */
public final class VistaMediaContextMenu {
    public interface NewTab { void open(String url); }

    private static final class Selection {
        final String url, page, kind;
        final long touch;
        Selection(String url, String page, String kind, long touch) {
            this.url=url; this.page=page; this.kind=kind; this.touch=touch;
        }
    }

    private VistaMediaContextMenu() { }

    public static WebView attach(Activity activity, WebView browser, NewTab tabs) {
        // Installing on the UI queue lets the tab finish configuring its WebView.
        browser.post(() -> {
            final Selection[] found={null};
            final long[] sequence={0L};
            browser.setOnTouchListener((view,event) -> {
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN){
                    final long when=SystemClock.uptimeMillis();
                    sequence[0]=when; found[0]=null;
                    String page=browser.getUrl();
                    if(!webUrl(page) || browser.getWidth()<=0 || browser.getHeight()<=0)
                        return false;
                    float px=Math.max(0f,Math.min(1f,event.getX()/browser.getWidth()));
                    float py=Math.max(0f,Math.min(1f,event.getY()/browser.getHeight()));
                    browser.evaluateJavascript(probe(px,py), json -> {
                        if(sequence[0]!=when || !page.equals(browser.getUrl()))return;
                        try {
                            Object decoded=new JSONTokener(json).nextValue();
                            if(!(decoded instanceof String))return;
                            JSONObject hit=new JSONObject((String)decoded);
                            String target=hit.optString("url","");
                            String kind=hit.optString("kind","");
                            if(webUrl(target) && ("video".equals(kind)||"iframe".equals(kind)))
                                found[0]=new Selection(target,page,kind,when);
                        }catch(Exception ignored){ /* non-media DOM, retain native selection */ }
                    });
                }else if(action==MotionEvent.ACTION_CANCEL){
                    found[0]=null; sequence[0]=-1L;
                }
                // Do not swallow taps, scrolling, native selection or player events.
                return false;
            });
            browser.setOnLongClickListener(view -> {
                Selection media=found[0];
                if(media==null || SystemClock.uptimeMillis()-media.touch>1700L)
                    return false; // Normal Android text/link context menu.
                found[0]=null;
                show(activity,media,tabs);
                return true;
            });
        });
        return browser;
    }

    private static String probe(float fx,float fy) {
        return "(function(){try{"+
            "var x=innerWidth*"+Float.toString(fx)+",y=innerHeight*"+Float.toString(fy)+";"+
            "function media(e){if(!e)return null;"+
            "var tag=(e.tagName||'').toLowerCase();"+
            "if(tag==='video'){var u=e.currentSrc||e.src||'';"+
            "if((!u||u.indexOf('blob:')===0)&&e.querySelector){"+
            "var s=e.querySelector('source[src]');if(s)u=s.src||u;}"+
            "return u?{kind:'video',url:u}:null;}"+
            "if(tag==='iframe')return e.src?{kind:'iframe',url:e.src}:null;"+
            "return null;}"+
            "var e=document.elementFromPoint(x,y),r=null,i=0;"+
            "while(e&&i++<8){r=media(e);if(r)break;e=e.parentElement;}"+
            "if(!r){var all=document.querySelectorAll('video,iframe');"+
            "for(i=0;i<Math.min(all.length,120);i++){"+
            "var b=all[i].getBoundingClientRect();"+
            "if(x>=b.left&&x<=b.right&&y>=b.top&&y<=b.bottom){"+
            "r=media(all[i]);if(r)break;}}}"+
            "return r?JSON.stringify(r):null;}catch(e){return null;}})()";
    }

    private static boolean webUrl(String raw) {
        if(raw==null||raw.length()>8192)return false;
        try {
            Uri url=Uri.parse(raw);
            String scheme=url.getScheme();
            return ("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme))
                && url.getHost()!=null && !url.getHost().isEmpty();
        }catch(Exception e){return false;}
    }

    private static boolean directMedia(Selection selected) {
        if("video".equals(selected.kind))return true;
        String path=Uri.parse(selected.url).getPath();
        if(path==null)return false;
        String lower=path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4")||lower.endsWith(".m3u8")||
            lower.endsWith(".mpd")||lower.endsWith(".webm")||
            lower.endsWith(".mkv")||lower.endsWith(".mov")||
            lower.endsWith(".ts");
    }

    private static void show(Activity activity,Selection media,NewTab tabs) {
        String[] actions={"Abrir em nova guia","Copiar link","Reproduzir no VistaStream"};
        new AlertDialog.Builder(activity)
            .setTitle("iframe".equals(media.kind)?"Opções do iframe":"Opções do vídeo")
            .setItems(actions,(dialog,which) -> {
                if(which==0){ tabs.open(media.url); return; }
                if(which==1){
                    ClipboardManager clips=(ClipboardManager)activity.getSystemService(
                        Context.CLIPBOARD_SERVICE);
                    if(clips!=null)clips.setPrimaryClip(
                        ClipData.newPlainText("VistaBrowser · ligação",media.url));
                    Toast.makeText(activity,"Link copiado",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!directMedia(media)){
                    new AlertDialog.Builder(activity)
                        .setTitle("Fonte direta não encontrada")
                        .setMessage("Este iframe é uma página Web, não um endereço de vídeo. " +
                            "Abra-o na nova guia ou use o Smart Crawler para identificar " +
                            "uma fonte reproduzível antes de a enviar ao VistaStream.")
                        .setPositiveButton("Entendi",null).show();
                    return;
                }
                playInVistaStream(activity,media);
            })
            .setNegativeButton("Cancelar",null).show();
    }

    private static void playInVistaStream(Activity activity,Selection media) {
        Uri uri=Uri.parse(media.url);
        // Route only to VistaStream packages: never dispatch arbitrary links
        // to an unrelated external app.
        for(String pkg:new String[]{"com.vistastream.tv","com.vistastream.tv.debug"}){
            Intent play=new Intent("com.vistastream.tv.action.PLAY_URL");
            play.setData(uri); play.setPackage(pkg);
            play.putExtra("com.vistastream.tv.extra.URL",media.url);
            play.putExtra("com.vistastream.tv.extra.PAGE_URL",media.page);
            play.putExtra("com.vistastream.tv.extra.REFERER",media.page);
            play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { activity.startActivity(play); return; }
            catch(ActivityNotFoundException|SecurityException ignored) { }
            Intent fallback=new Intent(Intent.ACTION_VIEW);
            fallback.setDataAndType(uri,"video/*");
            fallback.setPackage(pkg);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { activity.startActivity(fallback); return; }
            catch(ActivityNotFoundException|SecurityException ignored) { }
        }
        new AlertDialog.Builder(activity)
            .setTitle("Player VistaStream indisponível")
            .setMessage("O VistaStream instalado não anunciou uma ação para receber " +
                "este vídeo diretamente. O link continua disponível em Copiar link.")
            .setPositiveButton("OK",null).show();
    }
}
