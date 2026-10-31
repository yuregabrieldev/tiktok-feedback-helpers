package com.yure.videoharvester;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebSettings;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Optional HTML5-only assistant. Attempts the media element's own play() after
 * page load; does NOT tap ad banners, navigate external popups, bypass a
 * CAPTCHA, defeat DRM, or pretend a script click is a trusted user gesture.
 */
public final class VistaPlayerAssistant {
    private static final String PREFS="vista_smart_crawler_v1";
    private VistaPlayerAssistant(){}

    public static void attempt(Context context, WebView webview){
        if(webview==null || !context.getSharedPreferences(PREFS,0)
            .getBoolean("assist_html5",false))return;
        try {
            webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        } catch(Exception ignored){}
        final String script="(function(){try{let attempted=0,frames=0;"+
            "function walk(d){if(!d||frames>24)return;"+
            "d.querySelectorAll('video').forEach(function(v){"+
            "if(attempted>=5 || !v.paused || v.ended || !v.isConnected || v.__vistaTried)return;"+
            "if(!v.src&&!v.currentSrc&&!v.querySelector('source'))return;"+
            "if(!v.getClientRects().length)return;v.__vistaTried=true;"+
            "attempted++;try{let p=v.play();if(p&&p.catch)p.catch(function(){});}catch(e){}"+
            "});d.querySelectorAll('iframe').forEach(function(f){frames++;"+
            "if(frames>24)return;try{walk(f.contentDocument);}catch(e){}});}"+
            "walk(document);return JSON.stringify({attempted:attempted,frames:frames});"+
            "}catch(e){return JSON.stringify({attempted:0,error:String(e)});}})()";
        webview.evaluateJavascript(script, encoded->{
            try{
                JSONObject o=new JSONObject(new JSONArray("["+encoded+"]").getString(0));
                int count=o.optInt("attempted",0);
                String status=count>0 ?
                    "Tentativa de reprodução HTML5 enviada a "+count+" player(s). "+
                    "Alguns sites exigem uma interação manual válida." :
                    "Sem elemento HTML5 pronto. Pode ser necessária interação no player "+
                    "ou o iframe pode bloquear acesso a partir desta página.";
                context.getSharedPreferences(PREFS,0).edit()
                    .putString("assistant_status",status)
                    .putLong("assistant_checked_at",System.currentTimeMillis()).apply();
            }catch(Exception ignored){}
        });
    }
}
