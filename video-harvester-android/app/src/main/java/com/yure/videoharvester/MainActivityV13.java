package com.yure.videoharvester;

import android.net.Uri;
import android.os.*;
import android.webkit.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class MainActivityV13 extends MainActivityV12 {
  String browserUa="";

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    if(status!=null) status.setText("Pronto • WebView/cookies/VPN compatíveis");
  }

  @Override void setupWeb(){
    WebSettings s=web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setSupportMultipleWindows(true);
    s.setJavaScriptCanOpenWindowsAutomatically(true);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    if(Build.VERSION.SDK_INT>=21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

    browserUa=WebSettings.getDefaultUserAgent(this);
    s.setUserAgentString(browserUa);

    CookieManager cm=CookieManager.getInstance();
    cm.setAcceptCookie(true);
    if(Build.VERSION.SDK_INT>=21) cm.setAcceptThirdPartyCookies(web,true);

    web.addJavascriptInterface(new Bridge(),"Harvester");
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onCreateWindow(WebView v,boolean d,boolean g,Message m){
        addHit("POPUP","blocked://popup",currentPage,"popup",current==null?0:current.depth);
        return false;
      }
    });

    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
        inspectRequest(r.getUrl().toString());
        return super.shouldInterceptRequest(v,r);
      }

      @Override public void onReceivedHttpError(WebView view,WebResourceRequest req,WebResourceResponse res){
        if(req.isForMainFrame()){
          final int code=res.getStatusCode();
          final String u=req.getUrl().toString();
          ui.post(()->status.setText("HTTP "+code+" no WebView: "+u));
          addHit("HTTP_"+code,u,currentPage,"http",current==null?0:current.depth);
        }
      }

      @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError err){
        if(req.isForMainFrame()){
          final String msg=err==null?"erro de rede":String.valueOf(err.getDescription());
          ui.post(()->status.setText("Erro de rede/WebView: "+msg));
        }
      }

      @Override public void onPageFinished(WebView v,String url){
        currentPage=url;
        CookieManager.getInstance().flush();
        String js="(function(){try{"+
          "const A=u=>{try{return new URL(u,location.href).href}catch(e){return null}};"+
          "const body=(document.body&&document.body.innerText||'').slice(0,3000);"+
          "if(/Website Access Blocked|Error HTTP 403|Access denied/i.test(body)){try{Harvester.err('A página carregada no WebView está bloqueada (403/acesso negado). Verifique se a VPN inclui este app.')}catch(e){}}"+
          "const links=[...document.querySelectorAll('a[href]')].map(x=>A(x.getAttribute('href'))).filter(Boolean);"+
          "const iframes=[...document.querySelectorAll('iframe[src],frame[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);"+
          "const media=[...document.querySelectorAll('video[src],audio[src],source[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);"+
          "const text=[...document.scripts].map(x=>x.textContent||'').join('\\n');"+
          "const re=/(https?:\\/\\/[^\\\"'<>\\s]+?(?:\\.m3u8|\\.mpd|\\.mp4|\\.m4v|\\.webm|\\.mov)(?:\\?[^\\\"'<>\\s]*)?)/ig;"+
          "const inline=[];let m;while((m=re.exec(text))&&inline.length<300)inline.push(m[1]);"+
          "const meta={title:document.title||'',h1:(document.querySelector('h1')||{}).innerText||'',og:(document.querySelector('meta[property=\\\"og:title\\\"]')||{}).content||'',img:(document.querySelector('meta[property=\\\"og:image\\\"]')||{}).content||''};"+
          "Harvester.dom(JSON.stringify({url:location.href,links,iframes,media,inline,meta}));"+
          "}catch(e){Harvester.err(String(e))}})();";
        v.evaluateJavascript(js,null);
        ui.postDelayed(()->nextTask(),1700);
      }
    });
  }

  @Override void discoverSitemap(String root){
    io.submit(()->{
      try{
        URL b=new URL(root);
        String sm=b.getProtocol()+"://"+b.getHost()+(b.getPort()>0?":"+b.getPort():"")+"/sitemap.xml";
        HttpURLConnection c=(HttpURLConnection)new URL(sm).openConnection();
        c.setConnectTimeout(7000);
        c.setReadTimeout(9000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent",browserUa==null||browserUa.isEmpty()?"Mozilla/5.0":browserUa);
        c.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        String cookies=CookieManager.getInstance().getCookie(root);
        if(cookies!=null&&!cookies.isEmpty()) c.setRequestProperty("Cookie",cookies);
        int code=c.getResponseCode();
        if(code>=200&&code<300){
          BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));
          StringBuilder x=new StringBuilder();String line;
          while((line=r.readLine())!=null&&x.length()<4000000)x.append(line);
          Matcher m=Pattern.compile("<loc>\\s*(.*?)\\s*</loc>",Pattern.CASE_INSENSITIVE).matcher(x.toString());
          ArrayList<String> f=new ArrayList<>();
          while(m.find()&&f.size()<pageLimit*4)f.add(m.group(1).replace("&amp;","&"));
          ui.post(()->{for(String z:f)enqueueSite(z);refresh();});
        } else {
          ui.post(()->status.setText("Sitemap respondeu HTTP "+code+"; continuando pelos links da página."));
        }
      }catch(Exception e){
        ui.post(()->status.setText("Sitemap indisponível; continuando pelos links internos."));
      }
    });
  }
}
