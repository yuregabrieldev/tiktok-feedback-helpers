package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivityV15 extends MainActivityV14 {
  private static final int EXPORT=201,IMPORT=202;
  String exportData="";
  int restored=0;
  boolean restoring=false;
  @Override void buildUi(){
    super.buildUi();
    renameV15((ViewGroup)findViewById(android.R.id.content));
    LinearLayout parent=(LinearLayout)results.getParent();
    int pos=parent.indexOfChild(results);
    LinearLayout row=new LinearLayout(this);
    Button backup=btn("Exportar JSON completo");
    Button importBtn=btn("Importar JSON");
    importBtn.setBackgroundColor(0xff46505e);
    row.addView(backup,new LinearLayout.LayoutParams(0,dp(48),1));
    row.addView(importBtn,new LinearLayout.LayoutParams(0,dp(48),1));
    parent.addView(row,pos);
    backup.setOnClickListener(v->exportFull());
    importBtn.setOnClickListener(v->importFile());
    parent.addView(tv("Backups incluem biblioteca, catálogo e URLs descobertas. Scans em segundo plano são experimentais.",12,MUT),pos+1);
    loadScan();
  }
  boolean renameV15(ViewGroup v){for(int i=0;i<v.getChildCount();i++){View c=v.getChildAt(i);if(c instanceof TextView&&((TextView)c).getText().toString().equals("Video Harvester 1.4")){((TextView)c).setText("Video Harvester 1.5");return true;}if(c instanceof ViewGroup&&renameV15((ViewGroup)c))return true;}return false;}
  @Override void shareJson(){exportFull();}
  JSONObject buildBackup(){
    JSONObject o=new JSONObject();
    try{
      o.put("format","video-harvester-backup");
      o.put("version",2);
      o.put("createdAt",System.currentTimeMillis());
      o.put("library",libJson());
      JSONArray discovered=new JSONArray();
      for(Hit h:hits.values()){
        JSONObject z=new JSONObject();z.put("type",h.type);z.put("url",h.url);z.put("page",h.page);z.put("detector",h.detector);z.put("depth",h.depth);
        discovered.put(z);
      }
      o.put("discovered",discovered);
      JSONArray cats=new JSONArray();
      for(Catalog c:catalog.values())cats.put(catalogJson(c));
      o.put("catalog",cats);
      o.put("site",urlBox.getText().toString());
    }catch(Exception e){}
    return o;
  }
  JSONObject catalogJson(Catalog c)throws JSONException {
    JSONObject o=new JSONObject();o.put("type",c.type);o.put("title",c.title);o.put("year",c.year);o.put("image",c.image);
    JSONArray arr=new JSONArray();for(Source s:c.sources){JSONObject x=new JSONObject();x.put("url",s.url);x.put("page",s.page);x.put("format",s.format);x.put("label",s.label);x.put("season",s.season);x.put("episode",s.episode);arr.put(x);}o.put("sources",arr);return o;
  }
  void exportFull(){
    exportData=buildBackup().toString();
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE,"video-harvester-backup.json");
    startActivityForResult(intent,EXPORT);
  }
  void importFile(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
    i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});
    startActivityForResult(i,IMPORT);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(result!=RESULT_OK||data==null||data.getData()==null)return;
    if(request==EXPORT){
      try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException("Arquivo não disponível");out.write(exportData.getBytes(StandardCharsets.UTF_8));toast("JSON salvo com sucesso");}
      catch(Exception e){new AlertDialog.Builder(this).setMessage("Erro ao salvar JSON: "+e.getMessage()).setPositiveButton("OK",null).show();}
    }else if(request==IMPORT){
      try(InputStream in=getContentResolver().openInputStream(data.getData())){
        if(in==null)throw new IOException("Arquivo indisponível");
        ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;
        while((n=in.read(chunk))!=-1){if(buf.size()+n>12_000_000)throw new IOException("JSON acima de 12 MB");buf.write(chunk,0,n);}
        String json=buf.toString("UTF-8");JSONObject obj;
        if(json.trim().startsWith("[")){obj=new JSONObject();obj.put("library",new JSONArray(json));}
        else obj=new JSONObject(json);
        importBackup(obj);saveLibrary();saveScan();
        toast("JSON importado e mesclado");showLibrary();
      }catch(Exception e){new AlertDialog.Builder(this).setMessage("Falha ao importar: "+e.getMessage()).setPositiveButton("OK",null).show();}
    }
  }
  void importBackup(JSONObject o)throws JSONException{
    JSONArray arr=o.optJSONArray("library");
    if(arr!=null)for(int i=0;i<arr.length();i++)mergeCatalog(arr.optJSONObject(i),true);
    arr=o.optJSONArray("catalog");
    if(arr!=null)for(int i=0;i<arr.length();i++)mergeCatalog(arr.optJSONObject(i),false);
    arr=o.optJSONArray("discovered");
    if(arr!=null)for(int i=0;i<arr.length();i++){
      JSONObject z=arr.optJSONObject(i);if(z==null)continue;
      String u=z.optString("url");if(u.isEmpty())continue;
      String type=z.optString("type","IFRAME");String key=type+"|"+u;
      if(!hits.containsKey(key))hits.put(key,new Hit(type,u,z.optString("page"),z.optString("detector","import"),z.optInt("depth")));
    }
    refresh();
  }
  void mergeCatalog(JSONObject o,boolean toLibrary){
    if(o==null)return;
    String title=o.optString("title","").trim();if(title.isEmpty())return;
    String type=o.optString("type","movie"),year=o.optString("year","");
    String key=type+"|"+title.toLowerCase(Locale.ROOT)+"|"+year;
    Catalog item=null;
    if(toLibrary){for(Catalog v:library)if(v.key.equals(key)){item=v;break;}}
    else item=catalog.get(key);
    if(item==null){item=new Catalog();item.key=key;item.title=title;item.type=type;item.year=year;item.image=o.optString("image");if(toLibrary)library.add(item);else catalog.put(key,item);}
    JSONArray sources=o.optJSONArray("sources");if(sources==null)return;
    for(int j=0;j<sources.length();j++){
      JSONObject z=sources.optJSONObject(j);if(z==null)continue;String url=z.optString("url");if(url.isEmpty())continue;
      boolean exists=false;for(Source prev:item.sources)if(prev.url.equals(url)){exists=true;break;}
      if(exists)continue;
      Source s=new Source();s.url=url;s.page=z.optString("page",z.optString("source_page"));s.format=z.optString("format");s.label=z.optString("label");s.season=z.optInt("season");s.episode=z.optInt("episode");item.sources.add(s);
    }
  }
  void saveScan(){if(restoring)return;getSharedPreferences("vh15",0).edit().putString("scan",buildBackup().toString()).apply();}
  void loadScan(){
    try{String s=getSharedPreferences("vh15",0).getString("scan","");if(s.isEmpty())return;
      JSONObject obj=new JSONObject(s);restoring=true;
      JSONArray a=obj.optJSONArray("discovered");if(a!=null)for(int i=0;i<a.length();i++){
        JSONObject v=a.optJSONObject(i);if(v==null)continue;String t=v.optString("type"),u=v.optString("url");if(!u.isEmpty())hits.put(t+"|"+u,new Hit(t,u,v.optString("page"),v.optString("detector"),v.optInt("depth")));
      }
      a=obj.optJSONArray("catalog");if(a!=null)for(int i=0;i<a.length();i++)mergeCatalog(a.optJSONObject(i),false);
      String site=obj.optString("site");if(!site.isEmpty())urlBox.setText(site);
      restored=hits.size();
      if(restored>0)status.setText("Restaurados "+restored+" resultados do último scan");
      refresh();
    }catch(Exception e){}finally{restoring=false;}
  }
  @Override void startScan(){saveScan();super.startScan();if(scanning)try{Intent i=new Intent(this,ScanNotificationService.class);i.setAction("START");if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception e){toast("Notificação indisponível");}}
  @Override void stopScan(String text){super.stopScan(text);saveScan();try{stopService(new Intent(this,ScanNotificationService.class));}catch(Exception e){}}
  @Override protected void onDestroy(){saveScan();super.onDestroy();}
}