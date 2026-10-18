package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivityV25 extends MainActivityV24 {
  static final int TEST_REQUEST=325;
  private static final String PLAY_PREFS="vh25_playback_tests";

  @Override void buildUi(){
    super.buildUi();
    if(screenTitle!=null)screenTitle.setText("Análise de iframes");
  }

  boolean isIframeReference(Source s){
    if(s==null)return false;
    return "REFERENCIA_IFRAME".equalsIgnoreCase(s.format) ||
      (s.url!=null && s.url.contains("/player3/server.php") && !s.url.toLowerCase(Locale.ROOT).matches(".*[.](mp4|m3u8|mpd)([?].*)?$"));
  }
  String statusFor(Source s){
    if(s==null)return "Sem fonte";
    String result=getSharedPreferences(PLAY_PREFS,0).getString("source_"+s.url.hashCode(),"");
    if(isIframeReference(s)){
      return "ORIGINAL_PLAYED".equals(result)?
        "✓ Reproduziu no player original • sem link independente":
        "Somente player original • link independente indisponível";
    }
    if("PLAYED".equals(result))return "✓ Reprodução direta confirmada neste dispositivo";
    if("ERROR".equals(result))return "Falhou no teste de reprodução";
    if("TIMEOUT".equals(result))return "Não confirmada: tempo esgotado";
    if("TESTING".equals(result))return "Teste interrompido";
    return "Ainda não testado em reprodução direta";
  }
  void testCatalog(Catalog item){
    if(item.sources.isEmpty()){toast("Este título não tem nenhuma fonte");return;}
    if(item.sources.size()==1){startPlaybackTest(item,item.sources.get(0));return;}
    String[] labels=new String[item.sources.size()];
    for(int i=0;i<item.sources.size();i++){
      Source source=item.sources.get(i);
      String host="";
      try{host=Uri.parse(source.url).getHost();}catch(Exception ignored){}
      labels[i]=(isIframeReference(source)?"Player original":"Fonte "+source.format)
        +" • "+(host==null?"origem desconhecida":host)
        +" • "+(i+1);
    }
    new AlertDialog.Builder(this).setTitle("Selecione a fonte a testar")
      .setItems(labels,(dialog,which)->startPlaybackTest(item,item.sources.get(which)))
      .setNegativeButton("Cancelar",null).show();
  }
  void startPlaybackTest(Catalog item,Source source){
    if(source.url==null||source.url.trim().isEmpty()){toast("Não há um endereço de mídia para testar");return;}
    Uri parsed=Uri.parse(source.url);
    if(!"https".equalsIgnoreCase(parsed.getScheme())&&!"http".equalsIgnoreCase(parsed.getScheme())){
      toast("A referência não é um endereço HTTP ou HTTPS válido");return;
    }
    Intent intent=new Intent(this,PlaybackTestActivity.class);
    intent.putExtra("url",source.url);
    intent.putExtra("format",source.format);
    intent.putExtra("title",item.title);
    startActivityForResult(intent,TEST_REQUEST);
  }
  void addSourcesInfo(LinearLayout card,Catalog item){
    int i=0;
    for(Source s:item.sources){
      if(i++>=3){card.addView(small("Mais "+(item.sources.size()-3)+" fontes neste título"));break;}
      LinearLayout.LayoutParams extra=new LinearLayout.LayoutParams(-1,-2);extra.topMargin=dp(6);
      TextView line=tv((isIframeReference(s)?"▣ Referência de player":"▶ "+s.format)+
        "\n"+statusFor(s),12,isIframeReference(s)?0xff8B4C60:WINE);
      card.addView(line,extra);
    }
  }

  @Override void renderVideos(){
    if(videosCards==null)return;
    videosCards.removeAllViews();
    int shown=0;
    for(Catalog entry:catalog.values()){
      if(shown++>=100)break;
      LinearLayout card=panel(WHITE);
      card.addView(heading(entry.title,15));
      card.addView(tv(entry.sources.size()+" fontes candidatas • "+
        (approved(entry)?"APROVADO":"AGUARDA APROVAÇÃO"),12,FAINT));
      addSourcesInfo(card,entry);
      card.addView(gap(9));
      btnFull(card,"▶  Testar reprodução",true,()->testCatalog(entry));
      card.addView(gap(7));
      if(!approved(entry))btnFull(card,"✓  Aprovar para Biblioteca",false,()->approve(entry));
      else btnFull(card,"Abrir Biblioteca",false,()->navigate(3));
      addBlock(videosCards,card);
    }
    if(catalog.isEmpty())sectionCard(videosCards,"Sem candidatos",
      "Os vídeos observados durante a inspeção aparecerão aqui para avaliação.");
  }
  @Override void renderLibrary(){
    if(libraryCards==null)return;
    libraryCards.removeAllViews();
    int count=0;
    for(Catalog entry:library){
      if(count++>=120)break;
      LinearLayout card=panel(WHITE);
      card.addView(heading(entry.title,15));
      card.addView(tv(entry.sources.size()+" referências",12,FAINT));
      addSourcesInfo(card,entry);
      card.addView(gap(9));
      btnFull(card,"▶  Testar reprodução",true,()->testCatalog(entry));
      card.addView(gap(7));
      btnFull(card,"Remover da Biblioteca",false,()->{
        library.remove(entry);
        saveLibrary();saveScan();renderLibrary();refresh();
      });
      addBlock(libraryCards,card);
    }
    if(library.isEmpty())sectionCard(libraryCards,"Biblioteca vazia",
      "Aprove um título na página Vídeos para adicioná-lo à Biblioteca.");
  }

  @Override protected void onActivityResult(int request,int code,Intent data){
    super.onActivityResult(request,code,data);
    if(request!=TEST_REQUEST||code!=RESULT_OK||data==null)return;
    int hash=data.getIntExtra("sourceUrlHash",0);
    String status=data.getStringExtra("testStatus");
    if(hash!=0&&status!=null&&("PLAYED".equals(status)||"ERROR".equals(status)||"TIMEOUT".equals(status)||"ORIGINAL_PLAYED".equals(status))){
      getSharedPreferences(PLAY_PREFS,0).edit().putString("source_"+hash,status).apply();
    }
    if(screen==3)renderLibrary();
    if(screen==2)renderVideos();
    if("PLAYED".equals(status))toast("Reprodução direta confirmada nesta sessão");
    if("ORIGINAL_PLAYED".equals(status))toast("Funcionou no player original, mas não é um link independente");
  }
  @Override JSONObject buildBackup(){
    JSONObject o=super.buildBackup();
    try{
      o.put("version",10);
      JSONArray checks=new JSONArray();
      LinkedHashSet<String> seen=new LinkedHashSet<>();
      for(Catalog item:catalog.values())for(Source s:item.sources)seen.add(s.url);
      for(Catalog item:library)for(Source s:item.sources)seen.add(s.url);
      for(String url:seen){
        int hash=url.hashCode();
        String state=getSharedPreferences(PLAY_PREFS,0).getString("source_"+hash,"");
        if(state.isEmpty())continue;
        JSONObject check=new JSONObject();check.put("urlHash",hash);check.put("state",state);
        checks.put(check);
      }
      o.put("playbackTests",checks);
      o.put("playbackTestsNote","A confirmação é válida somente para o dispositivo e sessão observados; fontes iframe não são consideradas reprodução independente.");
    }catch(Exception ignored){}
    return o;
  }
  @Override void importBackup(JSONObject backup)throws JSONException{
    super.importBackup(backup);
    JSONArray tests=backup.optJSONArray("playbackTests");
    if(tests==null)return;
    android.content.SharedPreferences.Editor edit=getSharedPreferences(PLAY_PREFS,0).edit();
    for(int i=0;i<tests.length();i++){
      JSONObject data=tests.optJSONObject(i);if(data==null)continue;
      String status=data.optString("state","");
      if(status.equals("PLAYED")||status.equals("ORIGINAL_PLAYED")||status.equals("ERROR")||status.equals("TIMEOUT"))
        edit.putString("source_"+data.optInt("urlHash"),status);
    }
    edit.apply();
  }
}
