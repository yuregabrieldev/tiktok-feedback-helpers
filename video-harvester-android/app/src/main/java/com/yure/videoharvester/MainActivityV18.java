package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivityV18 extends MainActivityV17 {
  private static final int DARK_WINE=0xff421323;
  private static final int WINE_SURFACE=0xff5C2035;
  private static final int LIGHT_TEXT=0xffF6E9ED;
  TextView playerSummary;
  int playerSteps=0;

  @Override void buildUi() {
    super.buildUi();
    // Keep the working native crawler and JSON importer, replace only styling/actions.
    body.setBackgroundColor(DARK_WINE);
    if(body.getParent() instanceof View)((View)body.getParent()).setBackgroundColor(DARK_WINE);
    if(body.getParent()!=null && body.getParent().getParent() instanceof View)
      ((View)body.getParent().getParent()).setBackgroundColor(DARK_WINE);
    if(body.getChildAt(0) instanceof LinearLayout) {
      LinearLayout top=(LinearLayout)body.getChildAt(0);
      if(top.getChildCount()>0 && top.getChildAt(0) instanceof TextView){
        TextView logo=(TextView)top.getChildAt(0);
        logo.setTextColor(DARK_WINE);logo.setBackground(rounded(WHITE,16));
      }
      if(top.getChildCount()>1 && top.getChildAt(1) instanceof LinearLayout){
        LinearLayout names=(LinearLayout)top.getChildAt(1);
        if(names.getChildCount()>0 && names.getChildAt(0) instanceof TextView)
          ((TextView)names.getChildAt(0)).setTextColor(WHITE);
        if(names.getChildCount()>1 && names.getChildAt(1) instanceof TextView){
          TextView version=(TextView)names.getChildAt(1);
          version.setText("Studio  •  versão 1.8");
          version.setTextColor(LIGHT_TEXT);
        }
      }
      if(top.getChildCount()>2 && top.getChildAt(2) instanceof TextView)
        ((TextView)top.getChildAt(2)).setTextColor(WHITE);
    }
    section.setTextColor(WHITE);helper.setTextColor(LIGHT_TEXT);
    LinearLayout backup=findBackupPanel();
    if(backup!=null){
      Button reanalyze=null;
      for(int i=0;i<backup.getChildCount();i++){
        View v=backup.getChildAt(i);
        if(v instanceof Button && ((Button)v).getText().toString().contains("Reanalisar")){
          reanalyze=(Button)v;break;
        }
      }
      if(reanalyze!=null){
        reanalyze.setText("▶  Analisar todos os players");
        reanalyze.setTextColor(WHITE);
        reanalyze.setBackground(rounded(WINE,14));
        reanalyze.setOnClickListener(v->reanalyzeFrames());
      }
      Button store=action("✓  Guardar referências no projeto",false);
      store.setOnClickListener(v->{
        saveCrawlState();superSaveNow();
        toast("Referências guardadas. Exporte JSON para criar uma cópia.");
      });
      backup.addView(gap(7));
      backup.addView(store,new LinearLayout.LayoutParams(-1,dp(46)));
      playerSummary=tv("",12,FAINT);
      playerSummary.setPadding(0,dp(8),0,0);
      backup.addView(playerSummary);
    }
    refresh();
    if(hits.size()>0)showHits();
  }
  LinearLayout findBackupPanel(){
    for(int i=0;i<body.getChildCount();i++){
      View v=body.getChildAt(i);
      if(!(v instanceof LinearLayout))continue;
      LinearLayout l=(LinearLayout)v;
      if(l.getChildCount()>0 && l.getChildAt(0) instanceof TextView
          && ((TextView)l.getChildAt(0)).getText().toString().equals("Projeto e backups"))
        return l;
    }
    return null;
  }
  void superSaveNow(){getSharedPreferences("vh15",0).edit().putString("scan",buildBackup().toString()).apply();}
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(DARK_WINE);
    getWindow().setNavigationBarColor(DARK_WINE);
    getWindow().getDecorView().setSystemUiVisibility(0);
  }
  boolean siteMatches(String page){
    try{
      String h=Uri.parse(page).getHost();
      if(h!=null)return h.equalsIgnoreCase("redecanais.by")||h.toLowerCase(Locale.ROOT).endsWith(".redecanais.by");
    }catch(Exception ignored){}
    return false;
  }
  boolean unwanted(Hit h){
    if(!"IFRAME".equals(h.type)||!siteMatches(h.page))return false;
    String u=h.url.toLowerCase(Locale.ROOT);
    return u.contains("disqus.com/embed/")||u.contains("chatango.com/")
       ||u.contains("imasdk.googleapis.com/");
  }
  boolean playerLike(String link){
    try {
      Uri u=Uri.parse(link);
      String path=u.getPath();
      if(path==null)return false;
      path=path.toLowerCase(Locale.ROOT);
      return path.startsWith("/player3/")||path.startsWith("/player/")
          ||path.startsWith("/embed/")||path.equals("/embed.php")
          ||path.contains("/videoplayer/")||path.contains("/iframe-player/");
    }catch(Exception ex){return false;}
  }
  boolean eligible(Hit h){return "IFRAME".equals(h.type)&&!unwanted(h)&&h.url.startsWith("http");}
  @Override void addHit(String t,String u,String p,String d,int depth){
    if(u==null||u.trim().isEmpty())return;
    if("IFRAME".equals(t) && unwanted(new Hit(t,u,p,d,depth)))return;
    super.addHit(t,u,p,d,depth);
  }
  @Override void enqueueFrame(String url,int depth,String parent){
    if(url==null||unwanted(new Hit("IFRAME",url,parent,"queue",depth)))return;
    super.enqueueFrame(url,depth,parent);
  }
  @Override void refresh(){
    super.refresh();
    if(frameStat!=null){
      int eligibleCount=0;
      for(Hit h:hits.values())if(eligible(h)&&playerLike(h.url))eligibleCount++;
      frameStat.setText(String.valueOf(eligibleCount));
      LinearLayout parent=(LinearLayout)frameStat.getParent();
      if(parent.getChildCount()>1 && parent.getChildAt(1) instanceof TextView)
        ((TextView)parent.getChildAt(1)).setText("PLAYERS");
    }
    if(playerSummary!=null) {
      int total=0, inspected=0, withMedia=0;
      for(Hit h:hits.values()){
        if(!eligible(h)||!playerLike(h.url))continue;
        total++;
        if(frameSeen.contains("1|"+h.url))inspected++;
        for(Hit media:hits.values())if(media.page.equals(h.url)&&isMedia(media.type)){withMedia++;break;}
      }
      playerSummary.setText("Players relevantes: "+total+"  •  Analisados: "+inspected+
          "  •  Com mídia candidata: "+withMedia+"\nSalvar referências não baixa os arquivos de vídeo.");
    }
  }
  boolean isMedia(String kind){
    return "MP4".equals(kind)||"HLS".equals(kind)||"DASH".equals(kind)
         ||"MEDIA_CANDIDATE".equals(kind)||"MEDIA".equals(kind)||"WEBM".equals(kind);
  }
  @Override void showHits(){
    activeTab="descobertos";section.setText("Descobertas");
    section.setTextColor(WHITE);
    helper.setTextColor(LIGHT_TEXT);
    helper.setText("Players primeiro. Comentários Disqus não entram na análise deste site.");
    results.removeAllViews();
    ArrayList<Hit> ordered=new ArrayList<>();
    for(Hit h:hits.values())if(!unwanted(h)&&"IFRAME".equals(h.type)&&playerLike(h.url))ordered.add(h);
    for(Hit h:hits.values())if(!unwanted(h)&&!("IFRAME".equals(h.type)&&playerLike(h.url)))ordered.add(h);
    int show=Math.min(visibleHits,ordered.size());
    for(int i=0;i<show;i++){
      Hit h=ordered.get(i);
      LinearLayout card=panel(WHITE);
      String type="IFRAME".equals(h.type)?(playerLike(h.url)?"PLAYER CANDIDATO":"OUTRO IFRAME"):h.type;
      TextView tag=tv(type+"  •  nível "+h.depth,11,WINE);
      tag.setTypeface(null,1);card.addView(tag);
      TextView address=tv(h.url,12,INK);address.setMaxLines(4);address.setTextIsSelectable(true);
      card.addView(address);
      card.addView(tv("Origem: "+h.page,11,FAINT));
      if(eligible(h)){
        card.addView(gap(8));
        Button inspect=action("Inspecionar iframe ↗",false);
        inspect.setOnClickListener(v->openInspector(h.url));
        card.addView(inspect,new LinearLayout.LayoutParams(-1,dp(43)));
      }
      results.addView(card);results.addView(gap(9));
    }
    if(ordered.size()>show){
      Button more=action("Mostrar mais ("+Math.min(ordered.size()-show,100)+")",false);
      more.setOnClickListener(v->{visibleHits+=100;showHits();});
      results.addView(more,new LinearLayout.LayoutParams(-1,dp(46)));
    }
    if(ordered.size()==0)showWelcome();
  }
  @Override void showCatalog(){super.showCatalog();section.setTextColor(WHITE);helper.setTextColor(LIGHT_TEXT);}
  @Override void showLibrary(){super.showLibrary();section.setTextColor(WHITE);helper.setTextColor(LIGHT_TEXT);}

  @Override void reanalyzeFrames(){
    if(scanning){toast("Pare a análise atual antes de iniciar outra");return;}
    String root=urlBox.getText().toString().trim();
    if(root.isEmpty()){toast("Informe o site do projeto");return;}
    if(!root.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))root="https://"+root;
    try{rootHost=Uri.parse(root).getHost();}catch(Exception ignored){}
    if(rootHost==null){toast("URL inválida");return;}
    rootHost=rootHost.toLowerCase(Locale.ROOT);
    frameQueue.clear();queuedFrame.clear();frameSeen.clear();playerSteps=0;
    int queued=0;
    for(int phase=0;phase<2;phase++){
      for(Hit h:hits.values()){
        if(!eligible(h)||playerLike(h.url)!=(phase==0))continue;
        int before=frameQueue.size();enqueueFrame(h.url,1,h.page);
        if(frameQueue.size()>before)queued++;
      }
    }
    if(queued==0){toast("Não há iframes para analisar");return;}
    onlyFrames=true;scanning=true;
    saveCrawlState();
    status.setText("Analisando automaticamente "+queued+" iframes. O progresso é guardado no projeto.");
    nextTask();
    try{
      Intent svc=new Intent(this,ScanNotificationService.class);svc.setAction("START");
      if(Build.VERSION.SDK_INT>=26)startForegroundService(svc);else startService(svc);
    }catch(Exception ignored){}
  }
  @Override void nextTask(){
    if(scanning&&onlyFrames){
      playerSteps++;
      if(playerSteps%10==0){saveCrawlState();superSaveNow();}
    }
    super.nextTask();
  }
  @Override JSONObject buildBackup(){
    JSONObject backup=super.buildBackup();
    try{
      backup.put("version",5);
      JSONArray raw=backup.optJSONArray("discovered");
      if(raw!=null){
        JSONArray clean=new JSONArray();
        for(int i=0;i<raw.length();i++){
          JSONObject entry=raw.optJSONObject(i);if(entry==null)continue;
          Hit hit=new Hit(entry.optString("type"),entry.optString("url"),entry.optString("page"),
              entry.optString("detector"),entry.optInt("depth"));
          if(!unwanted(hit))clean.put(entry);
        }
        backup.put("discovered",clean);
      }
      JSONArray references=backup.optJSONArray("playbackReferences");
      if(references!=null){
        JSONArray cleanReferences=new JSONArray();
        for(int j=0;j<references.length();j++){
          JSONObject reference=references.optJSONObject(j);if(reference==null)continue;
          if("iframe_reference".equals(reference.optString("type"))
              && unwanted(new Hit("IFRAME",reference.optString("iframe"),reference.optString("page"),"backup",0)))continue;
          cleanReferences.put(reference);
        }
        backup.put("playbackReferences",cleanReferences);
      }
      JSONArray players=new JSONArray();
      for(Hit hit:hits.values()){
        if(!eligible(hit)||!playerLike(hit.url))continue;
        JSONObject p=new JSONObject();
        p.put("iframe",hit.url);p.put("source_page",hit.page);
        boolean checked=frameSeen.contains("1|"+hit.url);
        p.put("status",checked?"inspected":"pending");
        p.put("lastScanRecorded",checked?System.currentTimeMillis():JSONObject.NULL);
        p.put("playbackReferenceOnly",true);
        players.put(p);
      }
      backup.put("players",players);
      backup.put("storedVideoFiles",0);
    }catch(Exception ignored){}
    return backup;
  }
}
