package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivityV24 extends MainActivityV22 {
  private final ArrayList<String> pickerAddresses=new ArrayList<>();
  private final ArrayList<String> pickerLabels=new ArrayList<>();
  private int discoveryPageSize=12;

  @Override void buildUi(){
    resultLimit=12;
    super.buildUi();
    if(screenTitle!=null)screenTitle.setText("Análise de iframes");
  }

  @Override void createIframePage(){
    super.createIframePage();
    if(dashboard22==null)return;
    // The manual inspector is now reachable by selecting any iframe from the saved queue.
    for(int i=0;i<dashboard22.getChildCount();i++){
      View child=dashboard22.getChildAt(i);
      if(!(child instanceof LinearLayout))continue;
      LinearLayout card=(LinearLayout)child;
      if(card.getChildCount()==0||!(card.getChildAt(0) instanceof TextView))continue;
      String heading=((TextView)card.getChildAt(0)).getText().toString();
      if(heading.equals("Ações da fila")){
        card.addView(gap(8));
        btnFull(card,"▣  Selecionar iframe para inspecionar",false,()->showQueuePicker());
        break;
      }
    }
  }

  @Override void renderInbox(){
    if(iframeQueueCards==null)return;
    iframeQueueCards.removeAllViews();
    int shown=0;
    LinkedHashSet<String> seen=new LinkedHashSet<>();
    for(Map.Entry<String,String> row:inbox.entrySet()){
      if(shown>=5)break;
      String address=row.getKey();
      if(!seen.add(address))continue;
      addQueuePreview(address,row.getValue());
      shown++;
    }
    if(shown==0){
      for(Hit h:hits.values()){
        if(shown>=5)break;
        if(eligible(h)&&playerLike(h.url)&&seen.add(h.url)){
          addQueuePreview(h.url,h.page);shown++;
        }
      }
    }
    if(shown==0)iframeQueueCards.addView(small("Nenhum iframe encontrado. Importe um JSON ou execute um scan."));
    int more=Math.max(0,inbox.size()-shown);
    if(more>0)iframeQueueCards.addView(small("Mais "+more+" na fila; use Selecionar iframe para encontrar qualquer um."));
    if(shown>0){
      iframeQueueCards.addView(gap(7));
      btnFull(iframeQueueCards,"Ver lista e pesquisar iframes",false,()->showQueuePicker());
    }
  }
  void addQueuePreview(String url,String parent){
    LinearLayout panel=panel(BLUSH);panel.setElevation(0);
    panel.addView(tv(shortDescription(url),12,INK));
    FrameState state=observations.get(url);
    panel.addView(tv(state==null?"Pendente":state.status.replace('_',' '),11,FAINT));
    btnFull(panel,"Inspecionar este iframe",false,()->openInspector(url));
    iframeQueueCards.addView(panel);
    iframeQueueCards.addView(gap(7));
  }
  String shortDescription(String url){
    try{
      Uri u=Uri.parse(url);
      String id=u.getQueryParameter("vid");
      String host=u.getHost();
      String path=u.getPath();
      String label=(host==null?"":host)+(path==null?"":path);
      if(id!=null&&!id.isEmpty())label+="  •  "+id;
      return label.length()>100?label.substring(0,100)+"…":label;
    }catch(Exception e){return url.length()>100?url.substring(0,100):url;}
  }

  void showQueuePicker(){
    if(scanning){toast("Pare a análise atual antes de abrir o inspetor manual");return;}
    LinkedHashMap<String,String> options=new LinkedHashMap<>(inbox);
    for(Hit h:hits.values()){
      if(eligible(h)&&!options.containsKey(h.url))options.put(h.url,h.page);
    }
    if(options.isEmpty()){toast("Nenhum iframe guardado para inspecionar");return;}
    pickerAddresses.clear();pickerLabels.clear();
    for(Map.Entry<String,String> entry:options.entrySet()){
      pickerAddresses.add(entry.getKey());
      pickerLabels.add(shortDescription(entry.getKey()));
    }
    LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);
    view.setPadding(dp(12),dp(5),dp(12),0);
    EditText search=new EditText(this);
    search.setSingleLine(true);
    search.setHint("Pesquisar endereço, título ou ID do vídeo");
    view.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
    ListView list=new ListView(this);
    final ArrayList<Integer> filtered=new ArrayList<>();
    final ArrayList<String> labels=new ArrayList<>();
    ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);
    list.setAdapter(adapter);
    view.addView(list,new LinearLayout.LayoutParams(-1,dp(340)));
    Runnable filter=()->{
      String needle=search.getText().toString().trim().toLowerCase(Locale.ROOT);
      filtered.clear();labels.clear();
      for(int i=0;i<pickerAddresses.size();i++){
        String link=pickerAddresses.get(i);
        String label=pickerLabels.get(i);
        if(!needle.isEmpty()&&!link.toLowerCase(Locale.ROOT).contains(needle)&&!label.toLowerCase(Locale.ROOT).contains(needle))continue;
        filtered.add(i);labels.add(label);
        if(filtered.size()>=90)break;
      }
      adapter.notifyDataSetChanged();
    };
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int st,int count,int after){}
      public void onTextChanged(CharSequence s,int st,int before,int count){filter.run();}
      public void afterTextChanged(Editable e){}
    });
    filter.run();
    AlertDialog dialog=new AlertDialog.Builder(this)
      .setTitle("Selecionar iframe para inspecionar")
      .setView(view).setNegativeButton("Fechar",null).create();
    list.setOnItemClickListener((parent,v,position,id)->{
      if(position<0||position>=filtered.size())return;
      String url=pickerAddresses.get(filtered.get(position));
      dialog.dismiss();openInspector(url);
    });
    dialog.show();
  }

  @Override void renderDiscoveries(){
    if(results==null)return;
    results.removeAllViews();
    ArrayList<Hit> main=new ArrayList<>(),others=new ArrayList<>();
    for(Hit hit:hits.values()){
      if(unwanted(hit))continue;
      if("IFRAME".equals(hit.type)&&playerLike(hit.url))main.add(hit);
      else if("IFRAME".equals(hit.type)||isMedia(hit.type))others.add(hit);
    }
    int shown=0;
    for(Hit hit:main){
      if(shown>=resultLimit)break;
      addDiscoveryCard(hit);shown++;
    }
    for(Hit hit:others){
      if(shown>=resultLimit)break;
      addDiscoveryCard(hit);shown++;
    }
    int total=main.size()+others.size();
    if(total>shown){
      Button more=action("Mostrar mais "+Math.min(discoveryPageSize,total-shown)+" de "+(total-shown),false);
      more.setOnClickListener(v->{resultLimit+=discoveryPageSize;renderDiscoveries();});
      results.addView(more,new LinearLayout.LayoutParams(-1,dp(46)));
    }
    if(total==0)results.addView(small("Nenhuma descoberta ainda."));
    if(helper!=null)helper.setText("Exibindo "+shown+" de "+total+" resultados • lista em páginas para carregar mais rápido");
  }
  void addDiscoveryCard(Hit hit){
    LinearLayout card=panel(BLUSH);card.setElevation(0);
    boolean frame="IFRAME".equals(hit.type);
    card.addView(tv(frame?(playerLike(hit.url)?"PLAYER CANDIDATO":"IFRAME ANINHADO"):hit.type,12,WINE));
    TextView address=tv(shortDescription(hit.url),12,INK);address.setMaxLines(3);
    card.addView(address);
    card.addView(tv("Origem: "+shortDescription(hit.page),10,FAINT));
    if(frame){
      FrameState f=observations.get(hit.url);
      card.addView(tv("Estado: "+(f==null?"Pendente":f.status),11,FAINT));
      card.addView(gap(6));
      btnFull(card,"Inspecionar e capturar mídia",true,()->openInspector(hit.url));
    }
    results.addView(card);results.addView(gap(7));
  }

  String originPage(String iframe){
    for(Hit h:hits.values())if("IFRAME".equals(h.type)&&iframe.equals(h.url)&&h.page!=null&&!h.page.isEmpty())return h.page;
    String p=inbox.get(iframe);
    return p==null||p.isEmpty()?iframe:p;
  }
  @Override void openInspector(String url){
    if(scanning){toast("Pare o scan para inspecionar manualmente");return;}
    String parent=originPage(url);
    String title=slugTitle(parent);
    if(title.isEmpty()||"server".equalsIgnoreCase(title)) {
      try{
        String id=Uri.parse(url).getQueryParameter("vid");
        title=(id==null||id.isEmpty())?"Vídeo encontrado":("Player "+id);
      }catch(Exception e){title="Vídeo encontrado";}
    }
    inspectedUrl=url;
    Intent i=new Intent(this,PlayerInspectorActivityV27.class);
    i.putExtra("url",url);
    i.putExtra("sourcePage",parent);
    i.putExtra("mediaTitle",title);
    startActivityForResult(i,INSPECT_REQUEST);
  }

  @Override protected void onActivityResult(int request,int code,Intent data){
    super.onActivityResult(request,code,data);
    if(request!=INSPECT_REQUEST||code!=RESULT_OK||data==null||
       !data.getBooleanExtra("captureToVideos",false))return;
    String iframe=data.getStringExtra("page");
    if(iframe==null||iframe.isEmpty())iframe=inspectedUrl;
    if(iframe==null||iframe.isEmpty())return;
    String origin=data.getStringExtra("sourcePage");
    if(origin==null||origin.isEmpty())origin=originPage(iframe);
    String title=data.getStringExtra("mediaTitle");
    if(title==null||title.trim().isEmpty())title=slugTitle(origin);
    if(title==null||title.trim().isEmpty())title="Vídeo "+Math.abs(iframe.hashCode());
    title=title.trim();
    if(title.length()>90)title=title.substring(0,90);
    String fmt=data.getStringExtra("mediaFormat");
    if(fmt==null||fmt.isEmpty())fmt="HTML5";
    String stable=data.getStringExtra("mediaUrl");
    boolean temporary=data.getBooleanExtra("sessionBound",true);
    if(stable==null||stable.isEmpty())temporary=true;
    String url=temporary?iframe:stable;
    String host=data.getStringExtra("sourceHost");
    if(host==null)host="origem desconhecida";
    String format=temporary?"REFERENCIA_IFRAME":fmt;
    String key="movie|"+title.toLowerCase(Locale.ROOT)+"|";
    Catalog c=catalog.get(key);
    if(c==null){
      c=new Catalog();c.key=key;c.title=title;c.type="movie";
      catalog.put(key,c);
    }
    boolean exists=false;
    for(Source src:c.sources)if(url.equals(src.url)){exists=true;break;}
    if(!exists){
      Source source=new Source();
      source.url=url;source.page=origin;source.format=format;
      source.label=temporary?
        "Fonte observada em "+host+"; requer sessão. Referência, não link reproduzível.":
        "Mídia HTML5 observada em "+host+"; validar autorização e disponibilidade.";
      c.sources.add(source);
    }
    saveScan();saveCrawlState();refresh();
    navigate(2);
    toast(temporary?"Referência temporária enviada para Vídeos":"Fonte encontrada enviada para Vídeos");
  }

  @Override void renderVideos(){
    super.renderVideos();
    // Make the distinction between a player reference and a verified playable link visible.
    if(videosCards==null)return;
    for(int i=0;i<videosCards.getChildCount();i++){
      View view=videosCards.getChildAt(i);
      if(!(view instanceof LinearLayout))continue;
      LinearLayout card=(LinearLayout)view;
      for(int j=0;j<card.getChildCount();j++){
        View v=card.getChildAt(j);
        if(v instanceof TextView){
          TextView t=(TextView)v;
          if(t.getText().toString().contains("referências candidatas")){
            // Keep source metadata and review status distinct from playback claims.
            t.setText(t.getText()+" • confira se a fonte é temporária");
            break;
          }
        }
      }
    }
  }
}
