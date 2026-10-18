package com.yure.videoharvester;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import android.net.Uri;
import org.json.*;
import java.nio.charset.StandardCharsets;

public class PlayerInspectorActivity extends Activity {
  WebView browser; TextView info; String snapshot=""; String report=""; int popups=0, requests=0; String lastError="";
  final Handler ui=new Handler(Looper.getMainLooper());
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable bg(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
  void style(Button b,boolean primary){b.setAllCaps(false);b.setTextSize(12);b.setTextColor(primary?Color.WHITE:0xff651F36);b.setBackground(bg(primary?0xff651F36:0xffF4E9ED));}
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    String url=getIntent().getStringExtra("url");if(url==null)url="about:blank";
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff421323);
    TextView title=new TextView(this);title.setText("Inspecionar player • v2.4");title.setTextColor(Color.WHITE);title.setTextSize(19);title.setPadding(dp(12),dp(8),dp(8),dp(5));root.addView(title);
    info=new TextView(this);info.setTextColor(0xffF6E9ED);info.setTextSize(12);info.setPadding(dp(12),0,dp(8),dp(7));info.setText("Aguardando página...");root.addView(info);
    LinearLayout bar=new LinearLayout(this);
    Button inspect=new Button(this);inspect.setText("Capturar DOM");bar.addView(inspect,new LinearLayout.LayoutParams(0,dp(52),1));
    Button save=new Button(this);save.setText("Exportar HTML");bar.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));
    Button summary=new Button(this);summary.setText("Relatório");bar.addView(summary,new LinearLayout.LayoutParams(0,dp(52),1));
    style(inspect,true);style(save,false);style(summary,false);
    root.addView(bar);
    Button copy=new Button(this);copy.setText("Copiar relatório");style(copy,false);root.addView(copy,new LinearLayout.LayoutParams(-1,dp(46)));
    copy.setOnClickListener(v->{if(report.isEmpty()){capture(false);return;}((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Video Harvester - diagnóstico",report));Toast.makeText(this,"Relatório copiado",Toast.LENGTH_SHORT).show();});
    Button record=new Button(this);
    record.setText("Salvar resultado da inspeção");
    style(record,true);
    root.addView(record,new LinearLayout.LayoutParams(-1,dp(49)));
    record.setOnClickListener(v->saveInspectionResult());
    Button sendVideo=new Button(this);
    sendVideo.setText("▶  Capturar mídia após Play → Vídeos");
    style(sendVideo,true);
    root.addView(sendVideo,new LinearLayout.LayoutParams(-1,dp(53)));
    sendVideo.setOnClickListener(v->captureForVideos());

    browser=new WebView(this);browser.setBackgroundColor(Color.WHITE);root.addView(browser,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);getWindow().setStatusBarColor(0xff421323);getWindow().setNavigationBarColor(0xff421323);getWindow().getDecorView().setSystemUiVisibility(0);
    WebSettings s=browser.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);
    s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(true);
    CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(browser,true);
    browser.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onCreateWindow(WebView v,boolean d,boolean gesture,Message msg){popups++;refresh();return false;}
      @Override public void onShowCustomView(View view,CustomViewCallback cb){new AlertDialog.Builder(PlayerInspectorActivity.this).setMessage("Modo tela cheia solicitado. Para continuar a inspeção, mantenha o player nesta janela.").setPositiveButton("OK",(x,y)->cb.onCustomViewHidden()).show();}
    });
    browser.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){requests++;return super.shouldInterceptRequest(v,req);}
      @Override public void onReceivedHttpError(WebView v,WebResourceRequest req,WebResourceResponse res){if(req.isForMainFrame()){lastError="HTTP "+res.getStatusCode();refresh();}}
      @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError e){if(req.isForMainFrame()){lastError=String.valueOf(e.getDescription());refresh();}}
      @Override public void onPageFinished(WebView v,String address){refresh();}
    });
    inspect.setOnClickListener(v->capture(false));
    save.setOnClickListener(v->capture(true));
    summary.setOnClickListener(v->{EditText box=new EditText(this);box.setText(report.isEmpty()?"Use Capturar DOM primeiro.":report);box.setTextIsSelectable(true);box.setSingleLine(false);ScrollView scroll=new ScrollView(this);scroll.addView(box);new AlertDialog.Builder(this).setTitle("Diagnóstico selecionável").setView(scroll).setPositiveButton("Copiar",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Diagnóstico",report));}).setNegativeButton("Fechar",null).show();});
    browser.loadUrl(url);
  }
  void refresh(){ui.post(()->info.setText("URL: "+browser.getUrl()+"\nPopups: "+popups+" • Requisições observadas: "+requests+(lastError.isEmpty()?"":" • "+lastError)));}
  void capture(boolean save){
    String js="(function(){try{const es=[...document.querySelectorAll('video,audio,source,button,[role=button],iframe')].slice(0,250);const info=es.map(e=>({tag:e.tagName,role:e.getAttribute('role'),id:e.id,cls:String(e.className||'').slice(0,160),text:(e.innerText||'').slice(0,120),src:e.getAttribute('src'),state:e.tagName==='VIDEO'?{paused:e.paused,readyState:e.readyState,currentTime:e.currentTime,duration:e.duration,src:e.currentSrc}:null}));return JSON.stringify({url:location.href,title:document.title,controls:info,html:document.documentElement.outerHTML.slice(0,1000000)});}catch(e){return JSON.stringify({error:String(e)})}})()";
    browser.evaluateJavascript(js,result->{
      try{
        String raw=new JSONArray("["+result+"]").getString(0);
        JSONObject obj=new JSONObject(raw);
        snapshot=obj.optString("html","");
        JSONArray controls=obj.optJSONArray("controls");
        report="Página: "+obj.optString("url")+"\nTítulo: "+obj.optString("title")+"\n"+classifyElements(controls)+"\nElementos encontrados: "+(controls==null?0:controls.length())+"\nPopups: "+popups+"\nRequisições observadas: "+requests+"\nErro: "+lastError+"\n\nControles:\n"+(controls==null?"":controls.toString(2));
        if(save){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/html");i.putExtra(Intent.EXTRA_TITLE,"video-harvester-dom.html");startActivityForResult(i,71);}
        else new AlertDialog.Builder(this).setTitle("DOM capturado").setMessage(report.length()>12000?report.substring(0,12000):report).setPositiveButton("OK",null).show();
      }catch(Exception ex){Toast.makeText(this,"Falha ao capturar: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    });
  }
  @Override protected void onActivityResult(int req,int code,Intent data){super.onActivityResult(req,code,data);if(req==71&&code==RESULT_OK&&data!=null){try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(snapshot.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"HTML exportado",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();}}}


  String classifyElements(JSONArray controls){
    int active=0,empty=0,ads=0,other=0;
    if(controls!=null)for(int i=0;i<controls.length();i++){
      JSONObject e=controls.optJSONObject(i);
      if(e==null)continue;
      String tag=e.optString("tag");
      if("VIDEO".equals(tag)){
        JSONObject state=e.optJSONObject("state");
        String src=state==null?"":state.optString("src","");
        int ready=state==null?0:state.optInt("readyState");
        if(src.startsWith("http")&&ready>=2)active++;
        else if(src.isEmpty()&&ready==0)empty++;
      }else if("IFRAME".equals(tag)){
        String src=e.optString("src","");
        if(src.contains("imasdk.googleapis.com")||src.contains("doubleclick.net"))ads++;
        else other++;
      }
    }
    return "Vídeos HTML5 ativos: "+active+" | Elementos VIDEO vazios: "+empty+
      " | Iframes de anúncios: "+ads+" | Outros iframes: "+other+
      "\nO Google IMA é publicidade, não o vídeo principal.";
  }

  void captureForVideos(){
    if(browser==null)return;
    // Observe a media element only after normal playback: no ad clicking or bypass.
    String script="(function(){try{"+
      "const videos=[...document.querySelectorAll('video')];"+
      "const valid=videos.filter(v=>/^https?:/i.test(v.currentSrc||v.src||'')&&v.readyState>=2);"+
      "valid.sort((a,b)=>b.readyState-a.readyState);"+
      "const v=valid[0];if(!v)return JSON.stringify({ok:false,reason:'Aguardando uma fonte de vídeo carregada'});"+
      "const src=v.currentSrc||v.src||'';"+
      "const format=/[.]m3u8(?:[?#]|$)/i.test(src)?'HLS':/[.]mpd(?:[?#]|$)/i.test(src)?'DASH':/[.]webm(?:[?#]|$)/i.test(src)?'WEBM':/[.]mp4(?:[?#]|$)/i.test(src)?'MP4':'HTML5';"+
      "return JSON.stringify({ok:true,url:src,title:document.title||'',format:format,ready:v.readyState,duration:isFinite(v.duration)?Math.round(v.duration):0,paused:v.paused});"+
      "}catch(e){return JSON.stringify({ok:false,reason:String(e)})}})()";
    browser.evaluateJavascript(script,encoded->{
      try{
        JSONObject data=new JSONObject(new JSONArray("["+encoded+"]").getString(0));
        if(!data.optBoolean("ok")){
          new AlertDialog.Builder(this).setTitle("Mídia ainda não disponível")
            .setMessage("Inicie a reprodução normalmente, aguarde o vídeo carregar e tente Capturar novamente.\n\n"+
              data.optString("reason","Nenhuma fonte HTML5 pronta."))
            .setPositiveButton("OK",null).show();
          return;
        }
        String source=data.optString("url","");
        Uri parsed=Uri.parse(source);
        String host=parsed.getHost();
        if(host==null||!("https".equalsIgnoreCase(parsed.getScheme())||"http".equalsIgnoreCase(parsed.getScheme()))){
          Toast.makeText(this,"Fonte de mídia inválida",Toast.LENGTH_LONG).show();return;
        }
        String lower=source.toLowerCase(java.util.Locale.ROOT);
        // Do not persist session-authenticated source URLs. They are not durable.
        boolean temporary=lower.contains("token=")||lower.contains("ip_bind=")||
          lower.contains("signature=")||lower.contains("sig=")||
          lower.contains("expires=")||lower.contains("policy=")||
          lower.contains("proxy.mp4")||lower.contains("&url=")||
          lower.contains("sv=")||lower.contains("auth=");
        Intent outcome=new Intent();
        outcome.putExtra("page",getIntent().getStringExtra("url"));
        outcome.putExtra("sourcePage",getIntent().getStringExtra("sourcePage"));
        outcome.putExtra("captureToVideos",true);
        outcome.putExtra("mediaDetected",true);
        outcome.putExtra("videoCount",1);
        outcome.putExtra("activeVideos",1);
        outcome.putExtra("mediaCount",1);
        outcome.putExtra("readyState",data.optInt("ready",2));
        outcome.putExtra("durationSeconds",data.optInt("duration",0));
        outcome.putExtra("mediaFormat",data.optString("format","HTML5"));
        outcome.putExtra("sourceHost",host);
        outcome.putExtra("sessionBound",temporary);
        outcome.putExtra("mediaUrl",temporary?"":source);
        outcome.putExtra("mediaTitle",getIntent().getStringExtra("mediaTitle"));
        setResult(RESULT_OK,outcome);
        finish();
      }catch(Exception e){
        Toast.makeText(this,"Falha na captura: "+e.getMessage(),Toast.LENGTH_LONG).show();
      }
    });
  }
  void saveInspectionResult(){
    if(browser==null)return;
    String js="(function(){try{const v=[...document.querySelectorAll('video')],s=[...document.querySelectorAll('source')],f=[...document.querySelectorAll('iframe')];const src=x=>(x.currentSrc||x.src||'');const active=v.filter(x=>/^https?:/i.test(src(x))&&x.readyState>=2);const blank=v.filter(x=>!src(x)&&x.readyState===0);const ready=v.reduce((n,x)=>Math.max(n,x.readyState||0),0);const media=active.length+s.filter(x=>/^https?:/i.test(src(x))).length;const ads=f.filter(x=>/imasdk[.]googleapis[.]com|doubleclick[.]net/i.test(x.src||'')).length;return JSON.stringify({videoCount:v.length,activeVideos:active.length,emptyVideos:blank.length,adIframes:ads,mediaCount:media,readyState:ready,playerControls:!!document.querySelector('.video-js,.vjs-big-play-button,video'),mediaDetected:active.length>0});}catch(e){return '{}'}})()";
    browser.evaluateJavascript(js,encoded->{
      try{
        JSONObject data=new JSONObject(new JSONArray("["+encoded+"]").getString(0));
        Intent outcome=new Intent();
        outcome.putExtra("page",getIntent().getStringExtra("url"));
        outcome.putExtra("videoCount",data.optInt("videoCount"));
        outcome.putExtra("activeVideos",data.optInt("activeVideos"));
        outcome.putExtra("emptyVideos",data.optInt("emptyVideos"));
        outcome.putExtra("adIframes",data.optInt("adIframes"));
        outcome.putExtra("mediaCount",data.optInt("mediaCount"));
        outcome.putExtra("readyState",data.optInt("readyState"));
        outcome.putExtra("playerControls",data.optBoolean("playerControls"));
        outcome.putExtra("mediaDetected",data.optBoolean("mediaDetected"));
        setResult(RESULT_OK,outcome);
        finish();
      }catch(Exception ex){Toast.makeText(this,"Não foi possível salvar: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    });
  }
  @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(browser!=null)browser.destroy();super.onDestroy();}
}