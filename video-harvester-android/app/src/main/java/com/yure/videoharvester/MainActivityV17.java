package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivityV17 extends MainActivityV16 {
  static final int WINE=0xff651F36, WINE2=0xff87384F, INK=0xff22212A, FAINT=0xff786E78,
       CANVAS=0xffF8F5F6, WHITE=0xffffffff, BLUSH=0xffF4E9ED, BORDER=0xffE6DDE0;
  TextView pageStat,frameStat,sourceStat,queueStat,section,helper;
  boolean onlyFrames=false;
  int visibleHits=65;
  long lastBackupMillis=0;
  String activeTab="descobertos";
  LinearLayout body;

  @Override void buildUi(){
    // Stand-alone presentation. Scan, import, catalog, and storage remain inherited.
    stateReady=false;
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(CANVAS);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
    body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(17),dp(23),dp(17),dp(38));scroll.addView(body);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));

    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    TextView brand=tv("◉",25,WHITE);brand.setGravity(Gravity.CENTER);brand.setBackground(rounded(WINE,16));
    top.addView(brand,new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.VERTICAL);
    heading.setPadding(dp(12),0,0,0);
    TextView title=tv("Video Harvester",23,INK);title.setTypeface(null,1);heading.addView(title);
    heading.addView(tv("Studio  •  versão 1.7",12,FAINT));top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
    TextView dot=tv("●",20,WINE);top.addView(dot);body.addView(top);
    body.addView(gap(18));

    LinearLayout summary=panel(WHITE);
    summary.addView(heading("Visão geral",18));
    summary.addView(tv("Descoberta, análise e biblioteca no mesmo projeto.",12,FAINT));
    summary.addView(gap(13));
    LinearLayout r1=new LinearLayout(this);
    pageStat=stat(r1,"PÁGINAS","0");
    frameStat=stat(r1,"IFRAMES","0");
    summary.addView(r1);summary.addView(gap(10));
    LinearLayout r2=new LinearLayout(this);
    sourceStat=stat(r2,"FONTES CANDIDATAS","0");
    queueStat=stat(r2,"PENDENTES","0");
    summary.addView(r2);
    body.addView(summary);body.addView(gap(13));

    LinearLayout settings=panel(WHITE);
    settings.addView(heading("Novo scan / Continuar",17));settings.addView(gap(9));
    settings.addView(tv("Endereço do site",12,FAINT));
    urlBox=field("https://exemplo.com",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);settings.addView(urlBox,new LinearLayout.LayoutParams(-1,dp(52)));
    settings.addView(gap(10));
    LinearLayout opts=new LinearLayout(this);opts.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout first=new LinearLayout(this);first.setOrientation(LinearLayout.VERTICAL);
    first.addView(tv("Páginas por lote",11,FAINT));
    limitBox=field("",InputType.TYPE_CLASS_NUMBER);limitBox.setText("400");first.addView(limitBox,new LinearLayout.LayoutParams(-1,dp(47)));
    opts.addView(first,new LinearLayout.LayoutParams(0,-2,1));
    LinearLayout second=new LinearLayout(this);second.setOrientation(LinearLayout.VERTICAL);second.setPadding(dp(8),0,0,0);
    second.addView(tv("Profundidade",11,FAINT));depthBox=field("",InputType.TYPE_CLASS_NUMBER);depthBox.setText("4");second.addView(depthBox,new LinearLayout.LayoutParams(-1,dp(47)));
    opts.addView(second,new LinearLayout.LayoutParams(0,-2,1));
    settings.addView(opts);
    subdomains=new CheckBox(this);subdomains.setText("Incluir subdomínios");subdomains.setTextColor(INK);subdomains.setButtonTintList(ColorStateList.valueOf(WINE));settings.addView(subdomains);
    LinearLayout acts=new LinearLayout(this);
    Button go=action("▶  Iniciar / Continuar",true);go.setOnClickListener(v->startScan());
    acts.addView(go,new LinearLayout.LayoutParams(0,dp(54),2));
    Button stop=action("Parar",false);stop.setOnClickListener(v->stopScan("Interrompido pelo usuário"));
    LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(0,dp(54),1);stopParams.setMargins(dp(8),0,0,0);acts.addView(stop,stopParams);settings.addView(acts);
    settings.addView(gap(10));
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(WINE));settings.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));
    status=tv("Pronto para analisar",12,INK);status.setPadding(0,dp(10),0,0);status.setMaxLines(3);settings.addView(status);
    count=tv("",11,FAINT);count.setPadding(0,dp(6),0,0);settings.addView(count);
    body.addView(settings);body.addView(gap(13));

    LinearLayout backup=panel(WHITE);backup.addView(heading("Projeto e backups",17));
    backup.addView(tv("Importe backups anteriores sem perder resultados.",12,FAINT));backup.addView(gap(9));
    LinearLayout bRow=new LinearLayout(this);
    Button export=action("↓  Exportar JSON",true);export.setOnClickListener(v->exportFull());
    Button imp=action("↑  Importar JSON",false);imp.setOnClickListener(v->importFile());
    bRow.addView(export,new LinearLayout.LayoutParams(0,dp(48),1));
    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,dp(48),1);ip.setMargins(dp(7),0,0,0);bRow.addView(imp,ip);
    backup.addView(bRow);backup.addView(gap(7));
    Button rescan=action("↻  Reanalisar iframes guardados",false);
    rescan.setOnClickListener(v->reanalyzeFrames());
    backup.addView(rescan,new LinearLayout.LayoutParams(-1,dp(48)));
    body.addView(backup);body.addView(gap(15));

    section=heading("Descobertas",19);body.addView(section);body.addView(gap(9));
    LinearLayout nav=new LinearLayout(this);
    navButton(nav,"Descobertos",()->showHits());
    navButton(nav,"Catálogo",()->showCatalog());
    navButton(nav,"Biblioteca",()->showLibrary());
    body.addView(nav);body.addView(gap(10));
    helper=tv("Os links identificados não são necessariamente fontes reproduzíveis.",12,FAINT);body.addView(helper);
    body.addView(gap(9));
    results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);

    web=new WebView(this);web.setVisibility(View.GONE);body.addView(web,new LinearLayout.LayoutParams(1,1));
    setupWeb();setContentView(root);
    refresh();loadScan();restoreCrawlState();stateReady=true;
    if(hits.size()>0)showHits();else showWelcome();
  }
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(CANVAS);getWindow().setNavigationBarColor(CANVAS);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);if(hits.size()>0)status.setText("Projeto restaurado • "+hits.size()+" resultados disponíveis");}
  View gap(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return v;}
  LinearLayout panel(int color){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(15),dp(15),dp(15),dp(15));p.setBackground(rounded(color,20));p.setElevation(dp(2));return p;}
  TextView heading(String s,int size){TextView t=tv(s,size,INK);t.setTypeface(null,1);return t;}
  EditText field(String hint,int input){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setHintTextColor(0xffA89DA3);e.setTextSize(15);e.setTextColor(INK);e.setInputType(input);e.setPadding(dp(12),0,dp(12),0);e.setBackground(rounded(CANVAS,12));return e;}
  Button action(String s,boolean filled){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(filled?WHITE:WINE);b.setBackground(rounded(filled?WINE:BLUSH,14));return b;}
  TextView stat(LinearLayout line,String label,String val){
    LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(13),dp(12),dp(13),dp(12));c.setBackground(rounded(CANVAS,14));
    TextView main=tv(val,23,WINE);main.setTypeface(null,1);c.addView(main);c.addView(tv(label,10,FAINT));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(77),1);if(line.getChildCount()>0)lp.setMargins(dp(9),0,0,0);
    line.addView(c,lp);return main;
  }
  void navButton(LinearLayout p,String label,Runnable handler){
    Button b=action(label,false);b.setOnClickListener(v->handler.run());
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);
    if(p.getChildCount()>0)lp.setMargins(dp(6),0,0,0);p.addView(b,lp);
  }
  @Override void refresh(){
    super.refresh();
    if(pageStat!=null)pageStat.setText(String.valueOf(siteSeen.size()));
    if(frameStat!=null)frameStat.setText(String.valueOf(hits.values().stream().filter(h->"IFRAME".equals(h.type)).count()));
    if(sourceStat!=null)sourceStat.setText(String.valueOf(catalog.size()));
    if(queueStat!=null)queueStat.setText(String.valueOf(siteQueue.size()+frameQueue.size()));
  }
  @Override void showHits(){activeTab="descobertos";section.setText("Descobertas");
    helper.setText("Players, mídia, comentários e erros. Use Reanalisar para os dados importados.");
    results.removeAllViews();
    int index=0;
    for(Hit h:hits.values()){
      if(index++>=visibleHits)break;
      LinearLayout card=panel(WHITE);
      TextView tag=tv(h.type+"  ·  nível "+h.depth,11,WINE);tag.setTypeface(null,1);card.addView(tag);
      TextView url=tv(h.url,12,INK);url.setMaxLines(4);url.setTextIsSelectable(true);card.addView(url);
      card.addView(tv("Origem: "+h.page,11,FAINT));
      if("IFRAME".equals(h.type)&&h.url.startsWith("http")){
        card.addView(gap(6));Button inspect=action("Inspecionar player ↗",false);
        inspect.setOnClickListener(v->openInspector(h.url));card.addView(inspect,new LinearLayout.LayoutParams(-1,dp(43)));
      }
      results.addView(card);results.addView(gap(9));
    }
    if(index>visibleHits){
      Button more=action("Mostrar mais resultados ("+Math.min(hits.size()-visibleHits,100)+")",false);
      more.setOnClickListener(v->{visibleHits+=100;showHits();});results.addView(more,new LinearLayout.LayoutParams(-1,dp(50)));
    }
    if(hits.isEmpty())showWelcome();
  }
  void showWelcome(){if(results==null)return;results.removeAllViews();LinearLayout card=panel(WHITE);
    card.addView(heading("Seu projeto começa aqui",16));card.addView(tv("Faça uma varredura ou importe um backup para consultar os resultados anteriores.",13,FAINT));results.addView(card);
  }
  @Override void showCatalog(){
    activeTab="catalogo";section.setText("Catálogo");helper.setText("Fontes candidatas precisam de validação e autorização de uso.");
    results.removeAllViews();int count=0;
    for(Catalog c:catalog.values()){if(count++>=180)break;
      LinearLayout card=panel(WHITE);card.addView(heading(c.title,15));
      card.addView(tv((c.type.equals("series")?"Série":"Filme")+(c.year.isEmpty()?"":" · "+c.year)+" · "+c.sources.size()+" fontes candidatas",12,FAINT));
      results.addView(card);results.addView(gap(8));
    }
    if(catalog.isEmpty())showEmpty("Nenhuma fonte catalogada","A presença de iframes não garante fontes reproduzíveis.");
  }
  @Override void showLibrary(){
    if(importedPending){importedPending=false;showHits();return;}
    activeTab="biblioteca";section.setText("Biblioteca");helper.setText("Itens salvos ficam separados do histórico de descoberta.");
    results.removeAllViews();
    for(Catalog c:library){LinearLayout card=panel(WHITE);card.addView(heading(c.title,15));card.addView(tv(c.sources.size()+" fontes registradas",12,FAINT));results.addView(card);results.addView(gap(8));}
    if(library.isEmpty())showEmpty("Biblioteca vazia","No Catálogo, adicione apenas fontes que você tenha permissão para utilizar.");
  }
  void showEmpty(String title,String desc){LinearLayout card=panel(WHITE);card.addView(heading(title,15));card.addView(tv(desc,12,FAINT));results.addView(card);}
  void reanalyzeFrames(){
    if(scanning){toast("Pare o scan atual primeiro");return;}
    String raw=urlBox.getText().toString().trim();if(raw.isEmpty()){toast("Informe o endereço do projeto");return;}
    try{rootHost=Uri.parse(raw).getHost();}catch(Exception ignored){}
    if(rootHost==null){toast("URL inválida");return;}
    rootHost=rootHost.toLowerCase(Locale.ROOT);
    frameQueue.clear();queuedFrame.clear();frameSeen.clear();onlyFrames=true;int added=0;
    for(Hit h:hits.values()){if(!"IFRAME".equals(h.type))continue;int before=frameQueue.size();enqueueFrame(h.url,1,h.page);if(frameQueue.size()>before)added++;}
    if(added==0){onlyFrames=false;toast("Nenhum iframe de player elegível");return;}
    status.setText("Analisando "+added+" iframes salvos…");scanning=true;nextTask();
  }
  @Override void startScan(){onlyFrames=false;super.startScan();}
  @Override void stopScan(String message){onlyFrames=false;super.stopScan(message);}
  @Override void nextTask(){
    if(!scanning)return;
    if(!onlyFrames){super.nextTask();return;}
    Task t=null;
    while(!frameQueue.isEmpty()){Task x=frameQueue.poll();queuedFrame.remove(x.key());if(!frameSeen.contains(x.key())){t=x;break;}}
    if(t==null){stopScan("Reanálise concluída");showHits();return;}
    current=t;currentPage=t.url;frameSeen.add(t.key());status.setText("Iframe: "+t.url);refresh();web.loadUrl(t.url);
  }
  @Override void saveScan(){
    long now=android.os.SystemClock.elapsedRealtime();
    if(scanning && now-lastBackupMillis<9000)return;
    lastBackupMillis=now;super.saveScan();
  }
  @Override JSONObject buildBackup(){
    JSONObject obj=super.buildBackup();
    try{
      obj.put("version",4);
      JSONArray references=new JSONArray();
      for(Hit h:hits.values())if("IFRAME".equals(h.type)){
        JSONObject r=new JSONObject();r.put("type","iframe_reference");r.put("page",h.page);r.put("iframe",h.url);r.put("status","reference_only");r.put("playback_url",JSONObject.NULL);references.put(r);
      }
      // Never promise URL renewal without an authorized provider integration.
      for(Hit h:hits.values())if("MEDIA_CANDIDATE".equals(h.type)||"MP4".equals(h.type)||"HLS".equals(h.type)||"DASH".equals(h.type)){
        JSONObject r=new JSONObject();r.put("type","media_candidate");r.put("page",h.page);
        r.put("format",h.type);r.put("status","requires_validation_and_rights");
        r.put("source_host",Uri.parse(h.url).getHost());
        r.put("requires_session",h.url.contains("ip_bind=")||h.url.contains("token=")||h.url.contains("signature="));
        r.put("playback_url",JSONObject.NULL);references.put(r);
      }
      obj.put("playbackReferences",references);
      obj.put("resolverVersion",1);
    }catch(Exception ignored){}
    return obj;
  }
  @Override void setupWeb(){
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);s.setMediaPlaybackRequiresUserGesture(true);
    s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    s.setUserAgentString(WebSettings.getDefaultUserAgent(this));
    android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
    web.addJavascriptInterface(new Bridge(),"Harvester");
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onCreateWindow(WebView v,boolean d,boolean g,Message m){addHit("POPUP","blocked://popup",currentPage,"popup",current==null?0:current.depth);return false;}
    });
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
        String url=request.getUrl().toString();
        // Network hits are candidates; a WebView request alone doesn't prove playback.
        if(url.toLowerCase(java.util.Locale.ROOT).matches("^https?://.+[.](m3u8|mpd|mp4|m4v|webm)([?].*)?$"))
          addHit("MEDIA_CANDIDATE",url,currentPage,"network",current==null?0:current.depth);
        return super.shouldInterceptRequest(view,request);
      }
      @Override public void onReceivedHttpError(WebView v,WebResourceRequest req,WebResourceResponse response){
        if(req.isForMainFrame()){int code=response.getStatusCode();addHit("HTTP_"+code,req.getUrl().toString(),currentPage,"http",0);ui.post(()->status.setText("Resposta HTTP "+code));}
      }
      @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError e){
        if(req.isForMainFrame()){String msg=String.valueOf(e.getDescription());ui.post(()->status.setText("Falha de carregamento: "+msg));}
      }
      @Override public void onPageFinished(WebView v,String url){
        if(!scanning||current==null)return;
        final Task task=current;
        final String marker=task.key();
        // Two observations catch ordinary deferred DOM media without opening ad gates.
        Runnable read=()->{if(!scanning||current==null||!current.key().equals(marker))return;
          v.evaluateJavascript(EXTRACT_JS,null);
        };
        ui.postDelayed(read,140);
        ui.postDelayed(read,"IFRAME".equals(task.kind)?1750:740);
        ui.postDelayed(()->{if(scanning&&current!=null&&current.key().equals(marker))nextTask();},"IFRAME".equals(task.kind)?2700:1260);
      }
    });
  }
  static final String EXTRACT_JS="(function(){try{\nconst urls=new Set(),iframes=new Set(),media=new Set();\nfunction abs(v,b){if(!v||!String(v).trim())return null;try{const u=new URL(v,b||location.href);return /^https?:$/.test(u.protocol)?u.href:null}catch(e){return null}}\nfunction add(set,v,base){const u=abs(v,base);if(u)set.add(u)}\nfunction inspect(doc,level){\n if(!doc||level>2)return;\n const base=/^https?:/i.test(doc.baseURI||'')?doc.baseURI:location.href;\n for(const x of doc.querySelectorAll('video,audio,source')){\n   add(media,x.currentSrc,base);add(media,x.getAttribute('src'),base);add(media,x.src,base);\n }\n for(const f of doc.querySelectorAll('iframe,frame')){\n   add(iframes,f.getAttribute('src'),base);\n   if(level<2)try{if(f.contentDocument)inspect(f.contentDocument,level+1)}catch(e){}\n }\n}\ninspect(document,0);\nfor(const a of document.querySelectorAll('a[href]'))add(urls,a.getAttribute('href'),location.href);\nconst code=[...document.scripts].map(x=>x.textContent||'').join('\\n').slice(0,250000);\nconst re=/(https?:\\/\\/[^\"'<>\\\\s]+?\\.(?:m3u8|mpd|mp4|m4v|webm|mov)(?:\\?[^\"'<>\\\\s]*)?)/ig;\nconst inline=[];let m;while((m=re.exec(code))&&inline.length<50)inline.push(m[1]);\nconst meta={title:document.title||'',h1:(document.querySelector('h1')||{}).innerText||'',og:(document.querySelector('meta[property=\"og:title\"]')||{}).content||'',img:(document.querySelector('meta[property=\"og:image\"]')||{}).content||''};\nHarvester.dom(JSON.stringify({url:location.href,links:[...urls],iframes:[...iframes],media:[...media],inline,meta}));\n}catch(e){Harvester.err(String(e))}})();";
}
