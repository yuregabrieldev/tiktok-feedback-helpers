package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivityV19 extends MainActivityV18 {
  static final int INSPECT_REQUEST=219;
  final LinkedHashMap<String, FrameState> observations=new LinkedHashMap<>();
  TextView qualityReport;
  LinearLayout previewPanel;
  int scanSerial=0;
  String inspectedUrl="";
  boolean loadingHistory=false;

  static class FrameState {
    String status="PENDENTE";
    int videoCount=0, mediaElements=0, readyState=0, attempts=0;
    boolean playerControls=false;
    long updatedAt=0L;
    JSONObject json(String url)throws JSONException {
      JSONObject o=new JSONObject();
      o.put("iframe",url);o.put("state",status);o.put("videoElements",videoCount);
      o.put("mediaElements",mediaElements);o.put("readyState",readyState);
      o.put("playerControls",playerControls);o.put("attempts",attempts);o.put("updatedAt",updatedAt);
      return o;
    }
  }

  @Override void buildUi(){
    super.buildUi();
    // A WebView with 1px and GONE may never initialize a video player.
    if(web.getParent() instanceof android.view.ViewGroup)
      ((android.view.ViewGroup)web.getParent()).removeView(web);
    previewPanel=panel(WHITE);
    previewPanel.addView(heading("Área de análise HTML5",17));
    previewPanel.addView(tv("WebView visível durante o scan. Não interage com anúncios ou bloqueios.",12,FAINT));
    previewPanel.addView(gap(8));
    web.setVisibility(View.VISIBLE);
    web.setBackgroundColor(0xffF7F4F5);
    previewPanel.addView(web,new LinearLayout.LayoutParams(-1,dp(220)));
    previewPanel.addView(gap(7));
    Button manual=action("Inspecionar próximo player pendente",false);
    manual.setOnClickListener(v->openPendingInspector());
    previewPanel.addView(manual,new LinearLayout.LayoutParams(-1,dp(46)));
    LinearLayout backup=findBackupPanel();
    int index=backup==null?body.indexOfChild(section):body.indexOfChild(backup)+1;
    if(index<0)index=body.indexOfChild(section);
    body.addView(previewPanel,index+1);
    qualityReport=tv("",12,FAINT);
    qualityReport.setPadding(0,dp(8),0,0);
    previewPanel.addView(qualityReport);
    if(backup!=null){
      for(int i=0;i<backup.getChildCount();i++){
        View v=backup.getChildAt(i);
        if(v instanceof Button && ((Button)v).getText().toString().contains("Analisar todos")){
          ((Button)v).setText("▶  Analisar todos os iframes de vídeo");
        }
      }
    }
    refresh();
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    restoreObservations();
    refresh();
  }

  void restoreObservations(){
    // Restore metadata saved in the last backup without reopening every iframe.
    try {
      String raw=getSharedPreferences("vh16",0).getString("state","");
      if(raw.isEmpty())return;
      JSONObject o=new JSONObject(raw);
      readObservationArray(o.optJSONArray("iframeAnalysis"));
    }catch(Exception ignored){}
  }
  void readObservationArray(JSONArray list){
    if(list==null)return;
    for(int i=0;i<list.length();i++){
      JSONObject o=list.optJSONObject(i);if(o==null)continue;
      String url=o.optString("iframe");if(url.isEmpty())continue;
      FrameState f=observations.containsKey(url)?observations.get(url):new FrameState();
      f.status=o.optString("state","PENDENTE");
      f.videoCount=o.optInt("videoElements");
      f.mediaElements=o.optInt("mediaElements");
      f.readyState=o.optInt("readyState");
      f.playerControls=o.optBoolean("playerControls");
      f.attempts=o.optInt("attempts");
      f.updatedAt=o.optLong("updatedAt");
      observations.put(url,f);
    }
  }
  @Override void importBackup(JSONObject o)throws JSONException {
    super.importBackup(o);
    readObservationArray(o.optJSONArray("iframeAnalysis"));
    if(qualityReport!=null)refresh();
  }
  FrameState state(String url){
    FrameState f=observations.get(url);
    if(f==null){f=new FrameState();observations.put(url,f);}
    return f;
  }
  void setState(String url,String value){
    if(url==null||url.isEmpty())return;
    FrameState f=state(url);f.status=value;f.updatedAt=System.currentTimeMillis();
    refresh();
  }
  @Override JSONObject buildBackup(){
    JSONObject o=super.buildBackup();
    try {
      o.put("version",6);
      JSONArray a=new JSONArray();
      for(Map.Entry<String,FrameState> entry:observations.entrySet())
        a.put(entry.getValue().json(entry.getKey()));
      o.put("iframeAnalysis",a);
      o.put("analysisEngine","webview-html5-observation-1");
    }catch(Exception ignored){}
    return o;
  }

  @Override void refresh(){
    super.refresh();
    if(qualityReport==null)return;
    int ok=0,wait=0,pending=0,error=0,seen=0;
    for(Hit h:hits.values()){
      if(!eligible(h)||!playerLike(h.url))continue;
      seen++;
      FrameState s=observations.get(h.url);
      if(s==null||"PENDENTE".equals(s.status)||"ANALISANDO".equals(s.status))pending++;
      else if("MIDIA_DETECTADA".equals(s.status))ok++;
      else if("INTERACAO_NECESSARIA".equals(s.status)||"PLAYER_SEM_FONTE".equals(s.status))wait++;
      else error++;
    }
    qualityReport.setText("Players: "+seen+"  •  Mídia candidata: "+ok+
      "  •  Interação necessária: "+wait+"  •  Pendentes: "+pending+"  •  Erros/timeout: "+error);
  }
  @Override void reanalyzeFrames(){
    // Mark the start of a new observation pass while keeping saved references.
    for(Hit h:hits.values())if(eligible(h)&&playerLike(h.url))setState(h.url,"PENDENTE");
    super.reanalyzeFrames();
  }
  void openPendingInspector(){
    if(scanning){toast("Pare o scan automático antes de inspecionar manualmente");return;}
    for(Hit h:hits.values()){
      if(!eligible(h)||!playerLike(h.url))continue;
      FrameState f=observations.get(h.url);
      if(f==null||!"MIDIA_DETECTADA".equals(f.status)){
        openInspector(h.url);return;
      }
    }
    toast("Nenhum player pendente encontrado");
  }
  @Override void openInspector(String url){
    if(scanning){toast("Interrompa o scan antes de abrir o inspetor");return;}
    inspectedUrl=url;
    Intent i=new Intent(this,PlayerInspectorActivity.class);i.putExtra("url",url);
    startActivityForResult(i,INSPECT_REQUEST);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==INSPECT_REQUEST && result==RESULT_OK && data!=null){
      String url=data.getStringExtra("page");
      if(url==null||url.isEmpty())url=inspectedUrl;
      FrameState f=state(url);
      f.videoCount=data.getIntExtra("videoCount",f.videoCount);
      f.mediaElements=data.getIntExtra("mediaCount",f.mediaElements);
      f.readyState=data.getIntExtra("readyState",f.readyState);
      f.playerControls=data.getBooleanExtra("playerControls",f.playerControls);
      f.status=data.getBooleanExtra("mediaDetected",false)?"MIDIA_DETECTADA":"INTERACAO_NECESSARIA";
      f.updatedAt=System.currentTimeMillis();
      saveCrawlState();superSaveNow();refresh();
      toast("Inspeção guardada no projeto");
      showHits();
    }
  }

  @Override void nextTask(){
    Task previous=current;
    if(scanning && previous!=null && "IFRAME".equals(previous.kind)){
      FrameState f=state(previous.url);
      if("ANALISANDO".equals(f.status)){f.status="TEMPO_ESGOTADO";f.updatedAt=System.currentTimeMillis();}
    }
    super.nextTask();
    if(scanning && current!=null && current!=previous){
      Task task=current;
      int seq=++scanSerial;
      if("IFRAME".equals(task.kind)){
        FrameState f=state(task.url);f.status="ANALISANDO";f.attempts++;f.updatedAt=System.currentTimeMillis();
      }
      ui.postDelayed(()->{
        if(scanning&&current==task&&scanSerial==seq){
          if("IFRAME".equals(task.kind))setState(task.url,"TEMPO_ESGOTADO");
          nextTask();
        }
      },14000);
    }
  }
  void inspectDom(WebView v,Task task,int sequence,boolean last){
    if(!scanning||current!=task||scanSerial!=sequence)return;
    v.evaluateJavascript(EXTRACT_JS,null);
    if(!"IFRAME".equals(task.kind))return;
    String inspect="(function(){try{let vs=[...document.querySelectorAll('video')], ss=[...document.querySelectorAll('source')];let ready=vs.reduce((n,v)=>Math.max(n,v.readyState||0),0);let urls=[...vs.map(v=>v.currentSrc||v.src),...ss.map(s=>s.src)].filter(u=>u&&/^https?:/i.test(u));return JSON.stringify({videos:vs.length,media:urls.length,ready:ready,controls:!!document.querySelector('.video-js,.vjs-big-play-button,video')});}catch(e){return JSON.stringify({error:String(e)})}})()";
    v.evaluateJavascript(inspect,encoded->{
      if(!scanning||current!=task||scanSerial!=sequence)return;
      try{
        String raw=new JSONArray("["+encoded+"]").getString(0);
        JSONObject obj=new JSONObject(raw);
        FrameState f=state(task.url);
        f.videoCount=Math.max(f.videoCount,obj.optInt("videos"));
        f.mediaElements=Math.max(f.mediaElements,obj.optInt("media"));
        f.readyState=Math.max(f.readyState,obj.optInt("ready"));
        f.playerControls|=obj.optBoolean("controls");
        if(f.mediaElements>0 && f.readyState>=2)f.status="MIDIA_DETECTADA";
        else if(last) f.status=(f.playerControls||f.videoCount>0)?"INTERACAO_NECESSARIA":"PLAYER_SEM_FONTE";
        f.updatedAt=System.currentTimeMillis();refresh();
      }catch(Exception ignored){}
    });
  }
  @Override void setupWeb(){
    WebSettings s=web.getSettings();
    s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(true);
    s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);
    s.setUserAgentString(WebSettings.getDefaultUserAgent(this));
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
    web.addJavascriptInterface(new Bridge(),"Harvester");
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onCreateWindow(WebView view,boolean dialog,boolean gesture,Message msg){return false;}
    });
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){
        String url=req.getUrl().toString();
        if(url.toLowerCase(Locale.ROOT).matches("^https?://.+[.](mp4|m4v|m3u8|mpd|webm)([?].*)?$"))
          addHit("MEDIA_CANDIDATE",url,currentPage,"network",current==null?0:current.depth);
        return super.shouldInterceptRequest(v,req);
      }
      @Override public void onReceivedHttpError(WebView v,WebResourceRequest req,WebResourceResponse response){
        if(req.isForMainFrame()&&current!=null){
          setState(current.url,"HTTP_"+response.getStatusCode());
          status.setText("HTTP "+response.getStatusCode()+" • "+current.url);
        }
      }
      @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError e){
        if(req.isForMainFrame()&&current!=null){
          setState(current.url,"ERRO_CARREGAMENTO");
          status.setText("Erro • "+e.getDescription());
        }
      }
      @Override public void onPageFinished(WebView v,String url){
        if(!scanning||current==null)return;
        final Task task=current;
        final int sequence=scanSerial;
        boolean iframe="IFRAME".equals(task.kind);
        ui.postDelayed(()->inspectDom(v,task,sequence,false),250);
        ui.postDelayed(()->inspectDom(v,task,sequence,false),iframe?1900:850);
        if(iframe)ui.postDelayed(()->inspectDom(v,task,sequence,true),4400);
        ui.postDelayed(()->{
          if(scanning&&current==task&&scanSerial==sequence)nextTask();
        },iframe?5300:1700);
      }
    });
  }
  @Override void showHits(){
    super.showHits();
    // Statuses are included in the backup and in the summary above.
    if(section!=null)section.setText("Descobertas e players");
  }
}
