package com.yure.videoharvester;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivityV20 extends MainActivityV19 {
  final LinkedHashMap<String,PlayerData> playerData=new LinkedHashMap<>();
  TextView playerClassifier;
  static class PlayerData {
    int allVideos=0, activeVideos=0, emptyVideos=0, adIframes=0, nestedPlayers=0, ready=0;
    boolean playerTechnology=false, videoSource=false, sessionLinked=false;
    String format="desconhecido", provider="";
    long observedAt=0;
    JSONObject toJson(String url)throws JSONException {
      JSONObject obj=new JSONObject();
      obj.put("iframe",url);
      obj.put("videos",allVideos);obj.put("activeVideos",activeVideos);
      obj.put("emptyVideos",emptyVideos);obj.put("adIframes",adIframes);
      obj.put("nestedPlayers",nestedPlayers);obj.put("readyState",ready);
      obj.put("html5Player",playerTechnology);obj.put("sourceObserved",videoSource);
      obj.put("sessionLinked",sessionLinked);obj.put("format",format);
      obj.put("providerHost",provider);obj.put("observedAt",observedAt);
      return obj;
    }
  }

  @Override void buildUi(){
    super.buildUi();
    LinearLayout card=panel(WHITE);
    card.addView(heading("Classificador de mídia",16));
    card.addView(tv("Identifica o player HTML5, diferencia vídeos vazios e ignora iframes de anúncios Google IMA.",12,FAINT));
    card.addView(gap(9));
    playerClassifier=tv("",12,INK);
    card.addView(playerClassifier);
    int at=body.indexOfChild(previewPanel);
    body.addView(card,at>=0?at+1:body.getChildCount());
    refresh();
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    refresh();
  }

  // An iframe used by an advertisement bridge is not a video player.
  @Override boolean unwanted(Hit h){
    if(super.unwanted(h))return true;
    if(!"IFRAME".equals(h.type)||h.url==null)return false;
    String host=Uri.parse(h.url).getHost();
    if(host==null)return false;
    host=host.toLowerCase(Locale.ROOT);
    return host.equals("imasdk.googleapis.com")||
      host.equals("doubleclick.net")||host.endsWith(".doubleclick.net")||
      host.equals("googlesyndication.com")||host.endsWith(".googlesyndication.com")||
      host.equals("disqus.com")||host.endsWith(".disqus.com")||
      host.equals("chatango.com")||host.endsWith(".chatango.com");
  }

  @Override void importBackup(JSONObject backup)throws JSONException{
    super.importBackup(backup);
    JSONArray a=backup.optJSONArray("playerDiagnostics");
    if(a!=null)for(int i=0;i<a.length();i++){
      JSONObject o=a.optJSONObject(i);
      if(o==null||o.optString("iframe").isEmpty())continue;
      PlayerData d=playerData.containsKey(o.optString("iframe"))?
          playerData.get(o.optString("iframe")):new PlayerData();
      d.allVideos=o.optInt("videos");
      d.activeVideos=o.optInt("activeVideos");d.emptyVideos=o.optInt("emptyVideos");
      d.adIframes=o.optInt("adIframes");d.nestedPlayers=o.optInt("nestedPlayers");
      d.ready=o.optInt("readyState");d.playerTechnology=o.optBoolean("html5Player");
      d.videoSource=o.optBoolean("sourceObserved");d.sessionLinked=o.optBoolean("sessionLinked");
      d.format=o.optString("format","desconhecido");d.provider=o.optString("providerHost","");
      d.observedAt=o.optLong("observedAt");
      playerData.put(o.optString("iframe"),d);
    }
    if(playerClassifier!=null)refresh();
  }

  @Override JSONObject buildBackup(){
    JSONObject o=super.buildBackup();
    try{
      o.put("version",7);
      JSONArray arr=new JSONArray();
      for(Map.Entry<String,PlayerData> item:playerData.entrySet())arr.put(item.getValue().toJson(item.getKey()));
      o.put("playerDiagnostics",arr);
      o.put("sourceUrlsStored",false);
      o.put("diagnosticsNote","Metadados de observação; não incluem URLs com tokens ou sessão nem garantem reutilização.");
    }catch(Exception ignored){}
    return o;
  }

  @Override void refresh(){
    super.refresh();
    if(playerClassifier==null)return;
    int ready=0,empty=0,ads=0,pending=0,requires=0;
    for(Map.Entry<String,PlayerData> item:playerData.entrySet()){
      PlayerData d=item.getValue();
      if(d.videoSource && d.ready>=2)ready++;
      empty+=d.emptyVideos;ads+=d.adIframes;
    }
    for(Hit h:hits.values()){
      if(!eligible(h)||!playerLike(h.url))continue;
      FrameState f=observations.get(h.url);
      if(f==null||"PENDENTE".equals(f.status)||"ANALISANDO".equals(f.status))pending++;
      if(f!=null&&"INTERACAO_NECESSARIA".equals(f.status))requires++;
    }
    playerClassifier.setText("Players com mídia observada: "+ready+
      "\nElementos VIDEO vazios: "+empty+
      "\nIframes de anúncios identificados: "+ads+
      "\nAguardando análise: "+pending+"  •  Interação necessária: "+requires+
      "\nFontes temporárias não são guardadas como links permanentes.");
  }

  @Override void inspectDom(WebView view,Task task,int sequence,boolean last){
    if(!scanning||current!=task||scanSerial!=sequence)return;
    view.evaluateJavascript(EXTRACT_JS,null);
    if(!"IFRAME".equals(task.kind))return;
    String js="(function(){try{"+
      "const vs=[...document.querySelectorAll('video')];"+
      "const ss=[...document.querySelectorAll('source')];"+
      "const getUrl=v=>(v.currentSrc||v.src||v.getAttribute('src')||'');"+
      "const active=vs.filter(v=>v.readyState>=2 && /^https?:/i.test(getUrl(v)));"+
      "const blank=vs.filter(v=>!/^https?:/i.test(getUrl(v)) && v.readyState===0);"+
      "const sources=[...active.map(getUrl),...ss.map(getUrl).filter(u=>/^https?:/i.test(u))];"+
      "const frames=[...document.querySelectorAll('iframe')];"+
      "const ads=frames.filter(f=>/imasdk[.]googleapis[.]com|doubleclick[.]net|googlesyndication[.]com/i.test(f.src||'')).length;"+
      "const nested=frames.filter(f=>/\\/player3\\/|\\/embed[/.]|\\/player\\//i.test(f.src||'')).length;"+
      "const maxReady=vs.reduce((n,v)=>Math.max(n,v.readyState||0),0);"+
      "const src=sources[0]||'';let provider='';try{provider=new URL(src).hostname}catch(e){}"+
      "const ext=/[.]m3u8(?:[?#]|$)/i.test(src)?'HLS':/[.]mpd(?:[?#]|$)/i.test(src)?'DASH':/[.]mp4(?:[?#]|$)/i.test(src)?'MP4':src?'outro':'nenhum';"+
      "return JSON.stringify({videos:vs.length,active:active.length,blank:blank.length,ads:ads,nested:nested,ready:maxReady,player:!!document.querySelector('.video-js,.vjs-big-play-button,#RedeCanaisPlayer_html5_api'),source:sources.length>0,format:ext,host:provider,session:/[?&](?:ip_bind|token|signature|expires|sv)=/i.test(src)});"+
      "}catch(e){return JSON.stringify({error:String(e)})}})()";
    view.evaluateJavascript(js,encoded->{
      if(!scanning||current!=task||scanSerial!=sequence)return;
      try{
        JSONObject obj=new JSONObject(new JSONArray("["+encoded+"]").getString(0));
        if(obj.has("error"))return;
        PlayerData d=playerData.containsKey(task.url)?playerData.get(task.url):new PlayerData();
        d.allVideos=Math.max(d.allVideos,obj.optInt("videos"));
        d.activeVideos=Math.max(d.activeVideos,obj.optInt("active"));
        d.emptyVideos=Math.max(d.emptyVideos,obj.optInt("blank"));
        d.adIframes=Math.max(d.adIframes,obj.optInt("ads"));
        d.nestedPlayers=Math.max(d.nestedPlayers,obj.optInt("nested"));
        d.ready=Math.max(d.ready,obj.optInt("ready"));
        d.playerTechnology|=obj.optBoolean("player");
        d.videoSource|=obj.optBoolean("source");
        d.sessionLinked|=obj.optBoolean("session");
        if(obj.optBoolean("source")){d.format=obj.optString("format");d.provider=obj.optString("host");}
        d.observedAt=System.currentTimeMillis();
        playerData.put(task.url,d);
        FrameState frame=state(task.url);
        frame.videoCount=Math.max(frame.videoCount,d.allVideos);
        frame.mediaElements=Math.max(frame.mediaElements,d.activeVideos);
        frame.readyState=Math.max(frame.readyState,d.ready);
        frame.playerControls|=d.playerTechnology;
        if(d.activeVideos>0&&d.ready>=2)frame.status="MIDIA_DETECTADA";
        else if(last&&!frame.status.startsWith("HTTP_")&&!"ERRO_CARREGAMENTO".equals(frame.status)){
          frame.status=d.playerTechnology||d.allVideos>0?"INTERACAO_NECESSARIA":"PLAYER_SEM_FONTE";
        }
        frame.updatedAt=System.currentTimeMillis();
        refresh();
      }catch(Exception ignored){}
    });
  }

  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==INSPECT_REQUEST&&result==RESULT_OK&&data!=null){
      String url=data.getStringExtra("page");if(url==null||url.isEmpty())url=inspectedUrl;
      if(url==null||url.isEmpty())return;
      PlayerData d=playerData.containsKey(url)?playerData.get(url):new PlayerData();
      d.allVideos=Math.max(d.allVideos,data.getIntExtra("videoCount",0));
      d.activeVideos=Math.max(d.activeVideos,data.getIntExtra("activeVideos",0));
      d.emptyVideos=Math.max(d.emptyVideos,data.getIntExtra("emptyVideos",0));
      d.adIframes=Math.max(d.adIframes,data.getIntExtra("adIframes",0));
      d.ready=Math.max(d.ready,data.getIntExtra("readyState",0));
      d.videoSource|=data.getBooleanExtra("mediaDetected",false);
      d.playerTechnology|=data.getBooleanExtra("playerControls",false);
      d.observedAt=System.currentTimeMillis();
      playerData.put(url,d);
      saveCrawlState();
      superSaveNow();
      refresh();
    }
  }

  @Override void showHits(){
    super.showHits();
    if(results==null)return;
    // Keep the current UI but include a compact source interpretation card.
    if(!playerData.isEmpty()&&"descobertos".equals(activeTab)){
      LinearLayout card=panel(WHITE);
      card.addView(heading("Diagnóstico HTML5",15));
      card.addView(tv("Um vídeo ativo precisa de uma fonte HTTP e readyState >= 2. "+
        "Iframes de anúncios Google IMA e elementos de vídeo vazios são excluídos do total de mídia.",12,FAINT));
      results.addView(card,0);
    }
  }
}
