package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.net.Uri;
import org.json.*;
import java.util.*;

public class MainActivityV16 extends MainActivityV15 {
  boolean importedPending=false;
  boolean stateReady=false;
  int skipped=0, batchCount=0;
  @Override void buildUi(){
    super.buildUi();
    renameTitle16((ViewGroup)findViewById(android.R.id.content));
    styleTree((ViewGroup)findViewById(android.R.id.content));
    LinearLayout parent=(LinearLayout)results.getParent();
    Button again=btn("Reanalisar iframes guardados");
    again.setBackground(rounded(0xff344566,13));
    parent.addView(again,parent.indexOfChild(results));
    again.setOnClickListener(v->{if(scanning){toast("Aguarde o scan atual");return;}if(hits.isEmpty()){toast("Importe um backup primeiro");return;}String raw=urlBox.getText().toString().trim();if(raw.isEmpty()){toast("Informe o site original");return;}try{rootHost=Uri.parse(raw).getHost();}catch(Exception ignored){}frameSeen.clear();queuedFrame.clear();frameQueue.clear();for(Hit h:hits.values())if("IFRAME".equals(h.type))enqueueFrame(h.url,1,h.page);scanning=true;status.setText("Reanalisando "+frameQueue.size()+" iframes do backup");nextTask();});
    restoreCrawlState();
    stateReady=true;
    if(restored>0){status.setText("Projeto recuperado: "+restored+" descobertas. Use Continuar scan.");showHits();}
  }
  void renameTitle16(ViewGroup v){for(int i=0;i<v.getChildCount();i++){View child=v.getChildAt(i);if(child instanceof TextView && ((TextView)child).getText().toString().equals("Video Harvester 1.5"))((TextView)child).setText("Video Harvester 1.6");if(child instanceof ViewGroup)renameTitle16((ViewGroup)child);}}
  GradientDrawable rounded(int color,int radius){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;}
  void styleTree(ViewGroup root){
    for(int i=0;i<root.getChildCount();i++){
      View c=root.getChildAt(i);
      if(c instanceof Button){
        Button b=(Button)c;
        String label=b.getText().toString();
        if(label.equals("Exportar JSON")){b.setVisibility(View.GONE);continue;}
        if(label.equals("Exportar JSON completo"))b.setText("Salvar backup");
        if(label.equals("Escanear"))b.setText("Continuar scan");
        int bg=label.equals("Parar")?0xffB64356:label.equals("Adicionar todos")?0xff139B83:label.equals("Importar JSON")?0xff334056:0xff5767E9;
        b.setBackground(rounded(bg,13));b.setTextColor(Color.WHITE);b.setTextSize(13);b.setAllCaps(false);
      }else if(c instanceof EditText){c.setBackground(rounded(0xff222A3D,12));c.setPadding(dp(10),dp(5),dp(10),dp(5));}
      else if(c instanceof ProgressBar){((ProgressBar)c).setProgressTintList(android.content.res.ColorStateList.valueOf(0xff7989FF));}
      if(c instanceof ViewGroup)styleTree((ViewGroup)c);
    }
  }
  @Override void importBackup(JSONObject o)throws JSONException{
    super.importBackup(o);
    importedPending=true;
    JSONArray visited=o.optJSONArray("visitedPages");
    if(visited!=null)for(int i=0;i<visited.length();i++){String url=visited.optString(i);if(!url.isEmpty())siteSeen.add(url);}
    JSONArray pending=o.optJSONArray("pendingPages");
    if(pending!=null)for(int i=0;i<pending.length();i++){String url=pending.optString(i);if(!url.isEmpty()&&!siteSeen.contains(url)&&queuedSite.add(url))siteQueue.add(new Task(url,0,"SITE",""));}
    JSONArray scannedFrames=o.optJSONArray("inspectedFrames");
    if(scannedFrames!=null)for(int i=0;i<scannedFrames.length();i++){String u=scannedFrames.optString(i);if(!u.isEmpty())frameSeen.add(u);}
    status.setText("Importados: "+hits.size()+" resultados, "+siteSeen.size()+" páginas concluídas, "+siteQueue.size()+" pendentes.");
    refresh();
  }
  @Override void showLibrary(){
    if(importedPending){importedPending=false;showHits();return;}
    super.showLibrary();
  }
  @Override JSONObject buildBackup(){
    JSONObject obj=super.buildBackup();
    try{
      JSONArray visited=new JSONArray();for(String url:siteSeen)visited.put(url);
      JSONArray pending=new JSONArray();for(Task t:siteQueue)pending.put(t.url);
      JSONArray iframeDone=new JSONArray();for(String s:frameSeen)iframeDone.put(s);
      obj.put("visitedPages",visited);obj.put("pendingPages",pending);obj.put("inspectedFrames",iframeDone);
      obj.put("version",3);obj.put("scanInProgress",scanning);
    }catch(Exception ignored){}
    return obj;
  }
  @Override void startScan(){
    if(scanning){toast("Scan já está em andamento");return;}
    String raw=urlBox.getText().toString().trim();
    if(raw.isEmpty()){toast("Informe uma URL");return;}
    if(!raw.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))raw="https://"+raw;
    Uri uri;
    try{uri=Uri.parse(raw);rootHost=uri.getHost();if(rootHost==null)throw new Exception();rootHost=rootHost.toLowerCase(Locale.ROOT);
      pageLimit=Math.max(1,Math.min(5000,Integer.parseInt(limitBox.getText().toString().trim())));
      maxDepth=Math.max(0,Math.min(10,Integer.parseInt(depthBox.getText().toString().trim())));
    }catch(Exception e){toast("Endereço ou configuração inválidos");return;}
    batchCount=0;skipped=0;
    // No destructive clear: preserve imported discoveries, catalogue and seen URLs.
    if(siteQueue.isEmpty()){enqueueSite(raw);discoverSitemap(raw);}
    // Queue known uninspected iframe entries, without forcing playback/access gating.
    for(Hit hit:hits.values())if("IFRAME".equals(hit.type)&&!hit.url.isEmpty())enqueueFrame(hit.url,1,hit.page);
    scanning=true;
    status.setText("Continuando: "+siteSeen.size()+" já visitadas, "+siteQueue.size()+" pendentes");
    nextTask();
    if(scanning)try{Intent i=new Intent(this,ScanNotificationService.class);i.setAction("START");if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}
  }
  @Override void enqueueSite(String url){
    try{String n=norm(url);Uri u=Uri.parse(n);String h=u.getHost();if(h==null||!allowed(h))return;
      if(!"https".equalsIgnoreCase(u.getScheme())&&!"http".equalsIgnoreCase(u.getScheme()))return;
      if(siteQueue.size()>15000)return;
      if(!siteSeen.contains(n)&&queuedSite.add(n))siteQueue.add(new Task(n,0,"SITE",""));
    }catch(Exception ignored){}
  }
  @Override void enqueueFrame(String url,int depth,String parent){
    if(url.contains("disqus.com/")||url.contains("chatango.com/")||url.contains("googlesyndication.com/"))return;
    super.enqueueFrame(url,depth,parent);
  }
  @Override void nextTask(){
    if(!scanning)return;
    // Limit counts processed in *this* batch; preserve seen pages across sessions.
    Task t=null;
    while(!siteQueue.isEmpty()&&skipped<20000&&batchCount<pageLimit){
      Task q=siteQueue.poll();queuedSite.remove(q.url);
      if(!siteSeen.contains(q.url)){t=q;break;}skipped++;
    }
    if(t==null){
      while(!frameQueue.isEmpty()){
        Task q=frameQueue.poll();queuedFrame.remove(q.key());
        if(!frameSeen.contains(q.key())){t=q;break;}
      }
    }
    if(t==null){stopScan("Concluído. "+siteSeen.size()+" páginas acumuladas.");showCatalog();return;}
    current=t;currentPage=t.url;
    if("SITE".equals(t.kind)){siteSeen.add(t.url);batchCount++;}else frameSeen.add(t.key());
    status.setText(("SITE".equals(t.kind)?"Página ":"Iframe ") + t.url);
    progress.setProgress((int)Math.min(100,batchCount*100.0/pageLimit));refresh();
    web.loadUrl(t.url);
    if((siteSeen.size()+frameSeen.size())%30==0)saveScan();
  }
  @Override void stopScan(String m){super.stopScan(m);saveCrawlState();}
  void saveCrawlState(){
    if(!stateReady)return;
    JSONObject s=buildBackup();getSharedPreferences("vh16",0).edit().putString("state",s.toString()).apply();
  }
  void restoreCrawlState(){
    try{String raw=getSharedPreferences("vh16",0).getString("state","");
      if(!raw.isEmpty()){JSONObject o=new JSONObject(raw);importBackup(o);importedPending=false;
        String site=o.optString("site");if(!site.isEmpty())urlBox.setText(site);}
    }catch(Exception e){status.setText("Aviso ao restaurar estado: "+e.getMessage());}
  }
  @Override void saveScan(){super.saveScan();saveCrawlState();}
  @Override void refresh(){
    super.refresh();
    if(count!=null)count.setText("Páginas acumuladas: "+siteSeen.size()+" • lote: "+batchCount+"/"+pageLimit+" • pendentes: "+siteQueue.size()+" • iframes: "+hits.values().stream().filter(h->"IFRAME".equals(h.type)).count()+" • fontes: "+catalog.size());
  }
}
