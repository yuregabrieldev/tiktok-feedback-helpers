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

public class MainActivityV21 extends MainActivityV20 {
  static final int DARK=0xff421323, PALE=0xffF3DFE7;
  final LinkedHashMap<String,String> inbox=new LinkedHashMap<>();
  LinearLayout root21, toolbar21, scanBody, iframeBody, videosBody, libraryBody, settingsBody;
  LinearLayout scanWebHost, iframeWebHost;
  FrameLayout pages;
  LinearLayout[] screens=new LinearLayout[5];
  TextView screenTitle, scanCount21, inboxCount21, videosCount21, libraryCount21;
  int screen=0, runMode=0, resultLimit=45;
  boolean showIframeDiscoveries=false;
  boolean uiReady=false;
  String screenMessage="";
  Button[] navButtons=new Button[5];

  @Override void buildUi(){
    stateReady=false;uiReady=false;
    root21=new LinearLayout(this);
    root21.setOrientation(LinearLayout.VERTICAL);
    root21.setBackgroundColor(DARK);
    toolbar21=new LinearLayout(this);
    toolbar21.setPadding(dp(13),dp(12),dp(13),dp(10));
    toolbar21.setGravity(Gravity.CENTER_VERTICAL);
    Button menu=action("☰",false);
    menu.setTextColor(DARK);menu.setBackground(rounded(WHITE,12));
    toolbar21.addView(menu,new LinearLayout.LayoutParams(dp(49),dp(49)));
    menu.setOnClickListener(v->showMenu(menu));
    screenTitle=tv("Scan de páginas",21,WHITE);
    screenTitle.setTypeface(null,1);screenTitle.setPadding(dp(13),0,0,0);
    toolbar21.addView(screenTitle,new LinearLayout.LayoutParams(0,-2,1));
    root21.addView(toolbar21);

    pages=new FrameLayout(this);
    root21.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
    for(int i=0;i<5;i++){
      ScrollView scroll=new ScrollView(this);
      scroll.setFillViewport(true);
      scroll.setBackgroundColor(DARK);
      LinearLayout content=new LinearLayout(this);
      content.setOrientation(LinearLayout.VERTICAL);
      content.setPadding(dp(16),dp(13),dp(16),dp(32));
      scroll.addView(content);
      pages.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
      screens[i]=content;
      scroll.setVisibility(View.GONE);
    }
    scanBody=screens[0];iframeBody=screens[1];videosBody=screens[2];
    libraryBody=screens[3];settingsBody=screens[4];
    body=scanBody;

    createScanPage();
    createIframePage();
    createVideosPage();
    createLibraryPage();
    createSettingsPage();

    LinearLayout bottom=new LinearLayout(this);
    bottom.setGravity(Gravity.CENTER_VERTICAL);
    bottom.setBackgroundColor(WHITE);
    bottom.setPadding(dp(3),dp(5),dp(3),dp(5));
    String[] names={"Scan","Iframes","Vídeos","Biblioteca","Ajustes"};
    String[] glyph={"⌕","▣","▶","▤","⚙"};
    for(int i=0;i<5;i++){
      final int index=i;
      Button b=action(glyph[i]+"\n"+names[i],false);
      b.setTextSize(10);b.setPadding(0,0,0,0);
      b.setMinHeight(dp(55));
      b.setOnClickListener(v->navigate(index));
      navButtons[i]=b;
      bottom.addView(b,new LinearLayout.LayoutParams(0,dp(57),1));
    }
    root21.addView(bottom,new LinearLayout.LayoutParams(-1,dp(67)));

    // The single WebView is moved between pages. The scan engine is preserved.
    web=new WebView(this);
    web.setBackgroundColor(WHITE);
    web.setVisibility(View.VISIBLE);
    iframeWebHost.addView(web,new LinearLayout.LayoutParams(-1,dp(220)));
    setupWeb();
    setContentView(root21);
    uiReady=true;
    navigate(0);
    refresh();
    loadScan();
    restoreCrawlState();
    stateReady=true;
    restoreInbox();
    refresh();
    renderInbox();
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(DARK);
    getWindow().setNavigationBarColor(WHITE);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  void showMenu(View anchor){
    PopupMenu popup=new PopupMenu(this,anchor);
    String[] names={"Scan de páginas","Análise de iframes","Vídeos para aprovação","Biblioteca","Configurações","Descobertas e players"};
    for(int i=0;i<names.length;i++)popup.getMenu().add(0,i,0,names[i]);
    popup.setOnMenuItemClickListener(item->{
      if(item.getItemId()==5)openIframeDiscoveries();
      else navigate(item.getItemId());
      return true;
    });
    popup.show();
  }
  TextView small(String s){TextView t=tv(s,12,FAINT);return t;}
  void addBlock(LinearLayout parent,View content){parent.addView(content);parent.addView(gap(12));}
  LinearLayout sectionCard(LinearLayout parent,String heading,String description){
    LinearLayout card=panel(WHITE);
    card.addView(heading(heading,17));
    if(description!=null&&!description.isEmpty()){card.addView(gap(6));card.addView(small(description));}
    card.addView(gap(10));
    addBlock(parent,card);
    return card;
  }
  void btnFull(LinearLayout parent,String label,boolean emphasized,Runnable fn){
    Button b=action(label,emphasized);
    b.setOnClickListener(v->fn.run());
    parent.addView(b,new LinearLayout.LayoutParams(-1,dp(47)));
  }

  void createScanPage(){
    LinearLayout summary=sectionCard(scanBody,"Páginas e descobertas",
      "Esta página percorre o site e descobre links, iframes e mídias. A análise aprofundada dos players fica na próxima página.");
    scanCount21=tv("0 páginas",14,WINE);summary.addView(scanCount21);
    LinearLayout form=sectionCard(scanBody,"Novo scan / Continuar","Informe aqui o site somente quando desejar percorrer páginas.");
    form.addView(small("Endereço do site"));
    urlBox=field("https://exemplo.com",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
    form.addView(urlBox,new LinearLayout.LayoutParams(-1,dp(50)));
    form.addView(gap(9));
    LinearLayout controls=new LinearLayout(this);
    LinearLayout a=new LinearLayout(this);a.setOrientation(LinearLayout.VERTICAL);
    a.addView(small("Páginas por lote"));
    limitBox=field("",InputType.TYPE_CLASS_NUMBER);limitBox.setText("400");
    a.addView(limitBox,new LinearLayout.LayoutParams(-1,dp(45)));
    controls.addView(a,new LinearLayout.LayoutParams(0,-2,1));
    LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);
    b.setPadding(dp(9),0,0,0);b.addView(small("Profundidade"));
    depthBox=field("",InputType.TYPE_CLASS_NUMBER);depthBox.setText("4");
    b.addView(depthBox,new LinearLayout.LayoutParams(-1,dp(45)));
    controls.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    form.addView(controls);
    subdomains=new CheckBox(this);subdomains.setTextColor(INK);subdomains.setText("Incluir subdomínios");
    subdomains.setButtonTintList(ColorStateList.valueOf(WINE));form.addView(subdomains);
    btnFull(form,"▶  Iniciar / Continuar páginas",true,()->startScan());
    form.addView(gap(8));
    btnFull(form,"■  Parar",false,()->stopScan("Interrompido pelo usuário"));
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(WINE));
    form.addView(gap(8));form.addView(progress,new LinearLayout.LayoutParams(-1,dp(7)));
    status=tv("Pronto para descobrir páginas.",12,INK);status.setMaxLines(4);
    status.setPadding(0,dp(8),0,0);form.addView(status);
    count=small("");form.addView(count);
    LinearLayout send=sectionCard(scanBody,"Encaminhar descobertas",
      "Agrupe os iframes já encontrados e envie para a fila de análise. Não é necessário rescaneá-los.");
    frameStat=tv("0 players",20,WINE);send.addView(frameStat);
    btnFull(send,"→  Enviar iframes para análise",true,()->{stageAllFrames(true);navigate(1);});
    send.addView(gap(8));
    btnFull(send,"Abrir vídeos encontrados",false,()->navigate(2));
    LinearLayout live=sectionCard(scanBody,"Prévia da página analisada",
      "O WebView funciona nesta página enquanto você percorre links.");
    scanWebHost=new LinearLayout(this);scanWebHost.setOrientation(LinearLayout.VERTICAL);
    live.addView(scanWebHost);
  }

  void createIframePage(){
    LinearLayout intro=sectionCard(iframeBody,"Fila de análise independente",
      "Analise os iframes importados ou descobertos sem precisar inserir novamente o link original do site.");
    inboxCount21=tv("0 iframes na fila",15,WINE);intro.addView(inboxCount21);
    btnFull(intro,"＋  Adicionar descobertos à fila",false,()->stageAllFrames(true));
    intro.addView(gap(8));
    btnFull(intro,"▶  Analisar fila de iframes",true,()->reanalyzeFrames());
    intro.addView(gap(8));
    btnFull(intro,"Descobertas e players →",false,()->openIframeDiscoveries());
    LinearLayout detail=sectionCard(iframeBody,"Análise recursiva e estados",
      "Busca iframes aninhados até a profundidade configurada, registra o parentesco e não trata anúncios como mídia.");
    pageStat=tv("0 páginas",12,FAINT);detail.addView(pageStat);
    playerSummary=small("");detail.addView(playerSummary);
    qualityReport=small("");detail.addView(qualityReport);
    playerClassifier=small("");detail.addView(playerClassifier);
    LinearLayout live=sectionCard(iframeBody,"WebView do player",
      "Monitoramento do DOM e HTML5; interações obrigatórias não são automatizadas.");
    previewPanel=live;
    iframeWebHost=new LinearLayout(this);
    iframeWebHost.setOrientation(LinearLayout.VERTICAL);
    live.addView(iframeWebHost);
    live.addView(gap(7));
    btnFull(live,"Inspecionar próximo player manualmente",false,()->openPendingInspector());
    LinearLayout queue=sectionCard(iframeBody,"Iframes pendentes",
      "Os endereços podem vir de um scan anterior ou de um backup JSON.");
    queueStat=tv("",13,WINE);queue.addView(queueStat);
    LinearLayout pendingContainer=new LinearLayout(this);pendingContainer.setOrientation(LinearLayout.VERTICAL);
    queue.addView(pendingContainer);
    iframeQueueCards=pendingContainer;
    LinearLayout nested=sectionCard(iframeBody,"Descobertas e players",
      "Inclui a relação entre iframe pai e filho e o acesso ao inspetor individual.");
    section=heading("Descobertas e players",16);nested.addView(section);
    helper=small("A análise recursiva respeita a origem, sessão e políticas de acesso.");nested.addView(helper);
    nested.addView(gap(6));
    results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);nested.addView(results);
    btnFull(nested,"Atualizar descobertas",false,()->renderDiscoveries());
  }
  LinearLayout iframeQueueCards;

  void createVideosPage(){
    LinearLayout card=sectionCard(videosBody,"Vídeos encontrados",
      "Revise as fontes candidatas antes de aprovar a entrada na Biblioteca. Um link temporário não é uma fonte permanente.");
    sourceStat=tv("0 fontes candidatas",14,WINE);card.addView(sourceStat);
    videosCount21=small("");card.addView(videosCount21);
    btnFull(card,"✓  Aprovar todos para Biblioteca",true,()->approveAll());
    card.addView(gap(8));
    btnFull(card,"Atualizar vídeos",false,()->renderVideos());
    videosCards=new LinearLayout(this);videosCards.setOrientation(LinearLayout.VERTICAL);
    videosBody.addView(videosCards);
  }
  LinearLayout videosCards;
  void createLibraryPage(){
    LinearLayout card=sectionCard(libraryBody,"Sua Biblioteca",
      "Somente os títulos que você aprovar na seção Vídeos serão exibidos aqui.");
    libraryCount21=tv("0 títulos",14,WINE);card.addView(libraryCount21);
    btnFull(card,"Atualizar Biblioteca",false,()->renderLibrary());
    libraryCards=new LinearLayout(this);libraryCards.setOrientation(LinearLayout.VERTICAL);
    libraryBody.addView(libraryCards);
  }
  LinearLayout libraryCards;
  void createSettingsPage(){
    LinearLayout cfg=sectionCard(settingsBody,"Configurações do projeto",
      "O endereço do site só é exigido para iniciar um novo scan de páginas.");
    btnFull(cfg,"Definir profundidade da análise",false,()->showDepthDialog());
    cfg.addView(gap(8));
    btnFull(cfg,"Guardar progresso agora",true,()->{saveScan();saveCrawlState();toast("Progresso guardado");});
    LinearLayout backup=sectionCard(settingsBody,"Backup e restauração",
      "Exporte o catálogo, a Biblioteca, as páginas, a fila de iframes e os resultados de análise.");
    btnFull(backup,"↓  Exportar JSON completo",true,()->exportFull());
    backup.addView(gap(8));
    btnFull(backup,"↑  Importar JSON",false,()->importFile());
    backup.addView(gap(7));
    backup.addView(small("Endereços que contêm tokens de sessão podem ser temporários. Revise os dados antes de partilhar backups."));
  }

  void showDepthDialog(){
    final EditText field=new EditText(this);
    field.setInputType(InputType.TYPE_CLASS_NUMBER);
    field.setText(String.valueOf(maxDepth));
    new AlertDialog.Builder(this).setTitle("Profundidade máxima dos iframes (1–10)")
      .setView(field).setPositiveButton("Guardar",(dialog,which)->{
        try{
          maxDepth=Math.max(1,Math.min(10,Integer.parseInt(field.getText().toString())));
          depthBox.setText(String.valueOf(maxDepth));
          toast("Profundidade: "+maxDepth);
        }catch(Exception e){toast("Valor inválido");}
      }).setNegativeButton("Cancelar",null).show();
  }

  void navigate(int next){
    if(next<0||next>=5)return;
    screen=next;showIframeDiscoveries=false;
    final String[] names={"Scan de páginas","Análise de iframes","Vídeos","Biblioteca","Configurações"};
    screenTitle.setText(names[next]);
    for(int i=0;i<5;i++){
      ((View)screens[i].getParent()).setVisibility(i==next?View.VISIBLE:View.GONE);
      if(navButtons[i]!=null){
        navButtons[i].setTextColor(i==next?WHITE:WINE);
        navButtons[i].setBackground(rounded(i==next?WINE:BLUSH,12));
      }
    }
    if(next==0){if(!scanning||runMode==1)attachWeb(scanWebHost,180);}
    if(next==1){attachWeb(iframeWebHost,220);renderInbox();renderDiscoveries();}
    if(next==2)renderVideos();
    if(next==3)renderLibrary();
    refresh();
  }
  void attachWeb(LinearLayout destination,int height){
    if(web==null||destination==null)return;
    if(web.getParent()==destination)return;
    if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);
    destination.addView(web,new LinearLayout.LayoutParams(-1,dp(height)));
  }
  void openIframeDiscoveries(){
    navigate(1);
    showIframeDiscoveries=true;
    renderDiscoveries();
    if(results!=null&&results.getParent()!=null){
      // The discoveries list is inside its own iframe page rather than the scan form.
      ((View)results.getParent()).requestFocus();
    }
  }

  @Override void startScan(){
    if(scanning){toast("Outra análise já está em execução");return;}
    String raw=urlBox.getText().toString().trim();
    if(raw.isEmpty()){toast("Informe um site para descobrir páginas");return;}
    if(!raw.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))raw="https://"+raw;
    try{
      Uri u=Uri.parse(raw);rootHost=u.getHost();
      if(rootHost==null)throw new IllegalArgumentException();
      rootHost=rootHost.toLowerCase(Locale.ROOT);
      pageLimit=Math.max(1,Math.min(5000,Integer.parseInt(limitBox.getText().toString().trim())));
      maxDepth=Math.max(1,Math.min(10,Integer.parseInt(depthBox.getText().toString().trim())));
    }catch(Exception e){toast("Site ou parâmetros inválidos");return;}
    runMode=1;onlyFrames=false;batchCount=0;skipped=0;
    if(siteQueue.isEmpty()){enqueueSite(raw);discoverSitemap(raw);}
    if(siteQueue.isEmpty()){toast("Nenhuma página nova na fila");return;}
    attachWeb(scanWebHost,180);navigate(0);
    scanning=true;status.setText("Percorrendo páginas e descobrindo referências…");
    nextTask();
    launchScanService();
  }
  void launchScanService(){
    try{
      Intent svc=new Intent(this,ScanNotificationService.class).setAction("START");
      if(Build.VERSION.SDK_INT>=26)startForegroundService(svc);else startService(svc);
    }catch(Exception ignored){}
  }
  @Override void reanalyzeFrames(){
    if(scanning){toast("Finalize o processo atual primeiro");return;}
    if(inbox.isEmpty())stageAllFrames(false);
    if(inbox.isEmpty()){toast("Nenhum iframe na fila. Importe ou descubra iframes primeiro.");return;}
    onlyFrames=true;runMode=2;frameQueue.clear();queuedFrame.clear();frameSeen.clear();
    current=null;
    int queued=0;
    for(Map.Entry<String,String> entry:inbox.entrySet()){
      if(entry.getKey().isEmpty())continue;
      int n=frameQueue.size();
      enqueueFrame(entry.getKey(),1,entry.getValue());
      if(frameQueue.size()>n){queued++;setState(entry.getKey(),"PENDENTE");}
    }
    if(queued==0){onlyFrames=false;runMode=0;toast("Não há iframes válidos para analisar");return;}
    scanning=true;
    attachWeb(iframeWebHost,220);navigate(1);
    status.setText("Análise de "+queued+" iframes e seus descendentes");
    nextTask();
    launchScanService();
  }

  @Override void nextTask(){
    if(!scanning)return;
    if(runMode==1&&siteQueue.isEmpty()){
      stopScan("Páginas concluídas. Encaminhe os iframes descobertos.");
      return;
    }
    super.nextTask();
  }
  @Override void stopScan(String message){
    final int mode=runMode;
    runMode=0;
    super.stopScan(message);
    if(mode==2){
      ArrayList<String> done=new ArrayList<>();
      for(String u:inbox.keySet())if(frameSeen.contains("1|"+u))done.add(u);
      for(String u:done)inbox.remove(u);
      saveCrawlState();
    }
    if(uiReady){refresh();renderInbox();if(mode==2)renderVideos();}
  }
  @Override void refresh(){
    super.refresh();
    if(!uiReady)return;
    int frames=0,media=0;
    for(Hit h:hits.values())if("IFRAME".equals(h.type)&&!unwanted(h))frames++;
    for(Catalog c:catalog.values())media+=c.sources.size();
    if(scanCount21!=null)scanCount21.setText(siteSeen.size()+" páginas visitadas • "+hits.size()+" descobertas");
    if(frameStat!=null)frameStat.setText(frames+" iframes relevantes");
    if(inboxCount21!=null)inboxCount21.setText(inbox.size()+" na fila • "+frameSeen.size()+" inspecionados no último lote");
    if(sourceStat!=null)sourceStat.setText(catalog.size()+" títulos candidatos • "+media+" referências de mídia");
    if(libraryCount21!=null)libraryCount21.setText(library.size()+" títulos aprovados");
    if(queueStat!=null)queueStat.setText("Pendentes: "+inbox.size()+" • em execução: "+frameQueue.size());
  }

  int stageAllFrames(boolean inform){
    int count=0;
    for(Hit h:hits.values()){
      if(!eligible(h))continue;
      if(!inbox.containsKey(h.url)){inbox.put(h.url,h.page);count++;}
    }
    saveCrawlState();
    refresh();renderInbox();
    if(inform)toast(count+" novos iframes enviados à análise");
    return count;
  }
  @Override void importBackup(JSONObject obj)throws JSONException{
    super.importBackup(obj);
    JSONArray a=obj.optJSONArray("iframeInbox");
    if(a!=null)for(int i=0;i<a.length();i++){
      JSONObject entry=a.optJSONObject(i);
      if(entry==null)continue;
      String url=entry.optString("url"),parent=entry.optString("parent");
      if(!url.isEmpty()&&!inbox.containsKey(url))inbox.put(url,parent);
    }
    if(uiReady){refresh();renderInbox();}
  }
  void restoreInbox(){
    try{
      String state=getSharedPreferences("vh16",0).getString("state","");
      if(!state.isEmpty()){
        JSONArray a=new JSONObject(state).optJSONArray("iframeInbox");
        if(a!=null)for(int i=0;i<a.length();i++){
          JSONObject item=a.optJSONObject(i);
          if(item!=null&&!item.optString("url").isEmpty())inbox.put(item.optString("url"),item.optString("parent"));
        }
      }
    }catch(Exception ignored){}
  }
  @Override JSONObject buildBackup(){
    JSONObject obj=super.buildBackup();
    try{
      obj.put("version",8);
      JSONArray a=new JSONArray();
      for(Map.Entry<String,String> e:inbox.entrySet()){
        JSONObject item=new JSONObject();item.put("url",e.getKey());item.put("parent",e.getValue());a.put(item);
      }
      obj.put("iframeInbox",a);
      obj.put("uiSections",new JSONArray(new String[]{"scan","iframes","videos","library","settings"}));
      obj.put("iframeAnalysisIndependent",true);
    }catch(Exception ignored){}
    return obj;
  }

  void renderInbox(){
    if(iframeQueueCards==null)return;
    iframeQueueCards.removeAllViews();
    int shown=0;
    for(Map.Entry<String,String> e:inbox.entrySet()){
      if(shown++>=24)break;
      TextView t=tv("• "+e.getKey(),11,INK);t.setTextIsSelectable(true);
      t.setPadding(0,dp(5),0,dp(5));iframeQueueCards.addView(t);
    }
    if(inbox.isEmpty())iframeQueueCards.addView(small("Fila vazia. Use “Enviar iframes para análise” após o scan, ou importe um backup."));
    if(inbox.size()>24)iframeQueueCards.addView(small("Mais "+(inbox.size()-24)+" entradas na fila."));
  }
  void renderDiscoveries(){
    if(results==null)return;
    results.removeAllViews();
    ArrayList<Hit> ordered=new ArrayList<>();
    for(Hit h:hits.values())if("IFRAME".equals(h.type)&&eligible(h)&&playerLike(h.url))ordered.add(h);
    for(Hit h:hits.values())if(!unwanted(h)&&!("IFRAME".equals(h.type)&&playerLike(h.url)))ordered.add(h);
    int shown=0;
    for(Hit h:ordered){
      if(shown++>=resultLimit)break;
      LinearLayout box=panel(BLUSH);box.setElevation(0);
      boolean frame="IFRAME".equals(h.type);
      box.addView(tv((frame?(playerLike(h.url)?"PLAYER":"IFRAME") :h.type)+" • nível "+h.depth,11,WINE));
      TextView link=tv(h.url,11,INK);link.setMaxLines(3);link.setTextIsSelectable(true);box.addView(link);
      box.addView(tv("Pai: "+h.page,10,FAINT));
      if(frame){
        FrameState f=observations.get(h.url);
        box.addView(tv("Análise: "+(f==null?"PENDENTE":f.status),10,FAINT));
        Button manual=action("Inspecionar individualmente",false);
        manual.setOnClickListener(v->openInspector(h.url));
        box.addView(manual,new LinearLayout.LayoutParams(-1,dp(41)));
      }
      results.addView(box);results.addView(gap(7));
    }
    if(shown>resultLimit||ordered.size()>resultLimit){
      Button more=action("Mostrar mais descobertas",false);
      more.setOnClickListener(v->{resultLimit+=75;renderDiscoveries();});
      results.addView(more);
    }
    if(ordered.isEmpty())results.addView(small("Nenhum iframe ou vídeo descoberto ainda."));
  }
  @Override void showHits(){if(uiReady){navigate(1);renderDiscoveries();}}
  @Override void showCatalog(){if(uiReady)navigate(2);}
  @Override void showLibrary(){if(uiReady)navigate(3);}

  boolean approved(Catalog c){
    for(Catalog l:library)if(l.key.equals(c.key))return true;
    return false;
  }
  void approve(Catalog c){
    if(approved(c)){toast("Já está na Biblioteca");return;}
    library.add(c);saveLibrary();saveScan();toast("Adicionado à Biblioteca");
    renderVideos();refresh();
  }
  void approveAll(){
    if(catalog.isEmpty()){toast("Nenhum vídeo encontrado");return;}
    for(Catalog c:catalog.values())if(!approved(c))library.add(c);
    saveLibrary();saveScan();toast("Títulos adicionados à Biblioteca");
    renderVideos();refresh();
  }
  void renderVideos(){
    if(videosCards==null)return;
    videosCards.removeAllViews();
    int shown=0;
    for(Catalog c:catalog.values()){
      if(shown++>=120)break;
      LinearLayout card=panel(WHITE);
      card.addView(heading(c.title,15));
      card.addView(small(c.sources.size()+" referências candidatas • "+(approved(c)?"APROVADO":"AGUARDA APROVAÇÃO")));
      int n=0;
      for(Source source:c.sources){
        if(n++>=3)break;
        String host=Uri.parse(source.url).getHost();
        card.addView(tv("• "+source.format+" • "+(host==null?"origem desconhecida":host),11,FAINT));
      }
      card.addView(gap(9));
      if(!approved(c))btnFull(card,"✓  Aprovar para Biblioteca",true,()->approve(c));
      else btnFull(card,"Ver na Biblioteca",false,()->navigate(3));
      addBlock(videosCards,card);
    }
    if(catalog.isEmpty()){
      LinearLayout empty=sectionCard(videosCards,"Sem vídeos candidatos",
        "As referências de mídia detectadas durante o scan ou a análise de iframes aparecerão aqui para aprovação.");
      btnFull(empty,"Abrir Análise de iframes",true,()->navigate(1));
    }
  }
  void renderLibrary(){
    if(libraryCards==null)return;
    libraryCards.removeAllViews();
    for(Catalog c:library){
      LinearLayout card=panel(WHITE);
      card.addView(heading(c.title,15));
      card.addView(small(c.sources.size()+" referências"));
      btnFull(card,"Remover da Biblioteca",false,()->{
        library.remove(c);saveLibrary();saveScan();renderLibrary();refresh();
      });
      addBlock(libraryCards,card);
    }
    if(library.isEmpty())sectionCard(libraryCards,"Biblioteca vazia",
      "Aprove títulos na seção Vídeos para adicioná-los aqui.");
  }
  @Override void openPendingInspector(){
    if(scanning){toast("Pare a análise antes de inspecionar manualmente");return;}
    navigate(1);
    super.openPendingInspector();
  }
}
