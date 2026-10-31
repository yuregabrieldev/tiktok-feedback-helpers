package com.yure.videoharvester;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.WeakHashMap;

/**
 * Explicitly opt-in, bounded player interaction on the user's own WebView.
 *
 * Important: only clearly-labelled play controls on the visible document are
 * eligible; do not click ads, blind central overlays, external links, purchase
 * buttons, CAPTCHA, login or paywalls. A page may still require manual input.
 * The inspector's existing popup blocker remains in charge of new windows.
 */
public final class VistaPlayInteractor {
    private static final String PREFS="vista_smart_crawler_v1";
    private static final WeakHashMap<WebView,State> STATES=new WeakHashMap<>();
    private static final int MAX_TAPS=3;
    private static final long MIN_GAP_MS=2800L;
    private static final long SESSION_MAX_MS=75000L;

    private static final class State {
        String page="";
        long started;
        long lastTap;
        int taps;
        boolean busy;
    }
    private VistaPlayInteractor(){}

    public static void attempt(Activity activity, WebView webview){
        if(activity==null||webview==null||activity.isFinishing()||activity.isDestroyed())return;
        if(!activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getBoolean("assist_play_buttons",false))return;
        if(webview.getWidth()<40||webview.getHeight()<40)return;
        final State state;
        synchronized(STATES){
            State s=STATES.get(webview);
            if(s==null){s=new State();STATES.put(webview,s);}
            String url=webview.getUrl();
            if(url==null)url="";
            if(!s.page.equals(url)){
                s.page=url;s.taps=0;s.started=SystemClock.elapsedRealtime();
                s.lastTap=0;s.busy=false;
            }
            if(s.busy||s.taps>=MAX_TAPS||SystemClock.elapsedRealtime()-s.started>SESSION_MAX_MS||
                SystemClock.elapsedRealtime()-s.lastTap<MIN_GAP_MS)return;
            s.busy=true;
            state=s;
        }
        // Queries the currently loaded document; inaccessible cross-origin
        // iframes are never treated as if their internal play control is known.
        String js="(function(){try{"+
            "var videos=[...document.querySelectorAll('video')];"+
            "if(videos.some(function(v){return !v.paused&&!v.ended||v.readyState>=3&&v.currentTime>0;}))"+
                "return JSON.stringify({state:'playing'});"+
            "var roots=[document],seen=0;"+
            "for(var i=0;i<roots.length&&seen<20;i++){"+
            "var d=roots[i];var nodes=[...d.querySelectorAll('iframe')];"+
            "for(var j=0;j<nodes.length&&seen<20;j++){seen++;try{"+
            "if(nodes[j].contentDocument)roots.push(nodes[j].contentDocument);"+
            "}catch(e){}}}"+
            "var selectors='button,[role=button],.vjs-big-play-button,.jw-icon-display,"+
                ".jw-icon-playback,.plyr__control,.dplayer-play-icon,.play-button,"+
                ".play-btn,.playbtn,.playButton,.play-icon,.poster-play,#play,[data-plyr=play]';"+
            "var best=null;var score=0;var count=0;"+
            "for(var z=0;z<roots.length;z++){"+
              "var d=roots[z];var choices=d.querySelectorAll(selectors);"+
              "for(var k=0;k<choices.length&&count++<150;k++){"+
                "var el=choices[k];var r=el.getBoundingClientRect();"+
                "if(r.width<20||r.height<15)continue;"+
                "var label=[el.getAttribute('aria-label'),el.getAttribute('title'),"+
                "el.id,typeof el.className==='string'?el.className:'',"+
                "el.textContent||''].join(' ').slice(0,260).toLowerCase();"+
                "if(/advert|banner|popup|sponsor|download|install|login|sign.?in|captcha|subscribe|buy|purchase|payment/.test(label))continue;"+
                "if(!/(?:\\bplay\\b|reproduz|assistir|iniciar|jw-icon-display|vjs-big-play|plyr__control|dplayer-play|playbtn|play-button|play-icon|poster-play)/.test(label))continue;"+
                "var a=el.closest('a');if(a&&a.getAttribute('href')&&"+
                "!/^(#|javascript:void\\(0\\))/i.test(a.getAttribute('href')))continue;"+
                "var st=getComputedStyle(el);if(st.visibility==='hidden'||st.display==='none'||"+
                    "st.pointerEvents==='none'||Number(st.opacity)<0.1)continue;"+
                "var parent=d.defaultView;var x=r.left+r.width/2,y=r.top+r.height/2;"+
                "var root=d;var valid=true;"+
                "while(parent&&parent!==window){"+
                  "var f=parent.frameElement;if(!f){valid=false;break;}"+
                  "var fr=f.getBoundingClientRect();"+
                  "x=fr.left+x*(fr.width/(parent.innerWidth||fr.width));"+
                  "y=fr.top+y*(fr.height/(parent.innerHeight||fr.height));"+
                  "parent=parent.parent;"+
                "}"+
                "if(!valid||x<10||y<10||x>window.innerWidth-10||y>window.innerHeight-10)continue;"+
                "var top=document.elementFromPoint(x,y);"+
                "if(!top)continue;"+
                "if(d===document&&!el.contains(top)&&!top.contains(el))continue;"+
                "var candidateScore=label.includes('play')?6:3;"+
                "if(label.includes('vjs-')||label.includes('jw-')||label.includes('plyr'))candidateScore+=2;"+
                "if(candidateScore>score){score=candidateScore;best={x:x,y:y};}"+
              "}}"+
            "return JSON.stringify(best?{state:'candidate',x:best.x,y:best.y,"+
                "viewportW:window.innerWidth,viewportH:window.innerHeight}:"+
                "{state:'no-safe-play-control'});"+
            "}catch(e){return JSON.stringify({state:'error'});}})()";
        webview.evaluateJavascript(js, encoded->{
            synchronized(STATES){state.busy=false;}
            if(activity.isFinishing()||activity.isDestroyed())return;
            try{
                if(encoded==null||"null".equals(encoded))return;
                JSONObject response=new JSONObject(new JSONArray("["+encoded+"]").getString(0));
                String kind=response.optString("state","error");
                if("playing".equals(kind)){
                    status(activity,"Reprodução encontrada. A procurar URL direta…",state.taps);
                    return;
                }
                if(!"candidate".equals(kind)){
                    status(activity,"Sem botão Play seguro e acessível. Pode exigir interação manual.",state.taps);
                    return;
                }
                double viewW=response.optDouble("viewportW",0);
                double viewH=response.optDouble("viewportH",0);
                double vx=response.optDouble("x",-1),vy=response.optDouble("y",-1);
                if(viewW<20||viewH<20||vx<0||vy<0)return;
                float x=(float)(vx*webview.getWidth()/viewW);
                float y=(float)(vy*webview.getHeight()/viewH);
                if(x<4||y<4||x>=webview.getWidth()-4||y>=webview.getHeight()-4)return;
                // One DOWN/UP pair goes to this app's owned WebView only.
                // Pop-up creation is still governed by its existing guard.
                long down=SystemClock.uptimeMillis();
                MotionEvent a=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,x,y,0);
                MotionEvent b=MotionEvent.obtain(down,down+65,MotionEvent.ACTION_UP,x,y,0);
                try{
                    webview.dispatchTouchEvent(a);
                    webview.dispatchTouchEvent(b);
                }finally{a.recycle();b.recycle();}
                state.taps++;state.lastTap=SystemClock.elapsedRealtime();
                status(activity,"Toque no botão Play ("+state.taps+"/"+MAX_TAPS+
                    "). A aguardar o carregamento…",state.taps);
            }catch(Exception ignored){}
        });
    }

    private static void status(Context ctx,String message,int taps){
        ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putString("play_interactor_status",message)
            .putInt("play_interactor_taps",taps)
            .putLong("play_interactor_updated",System.currentTimeMillis())
            .apply();
    }
}
