package com.yure.videoharvester;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivityV22 extends MainActivityV21 {
  static final int SUCCESS_GREEN=0xff16844B;
  static final int GREEN_LIGHT=0xffEAF7EE;
  static final int RED_WARNING=0xffA7404E;
  LinearLayout dashboard22, discovery22, breakdownGrid22;
  TextView percent22, processed22, scanPercent22, currentStatus22, iframeHeader22;
  TextView valueMedia22,valueWait22,valuePending22,valueErrors22;
  TextView classifier22, lastItem22;
  ProgressBar iframeProgress22;
  boolean detailMode22=false;
  boolean ui22=false;
  boolean simplePlayback22=false;

  @Override void buildUi(){
    ui22=false;
    super.buildUi();
    // Android 15+ may draw behind the status bar: reserve the actual inset.
    if(root21!=null&&toolbar21!=null){
      root21.setOnApplyWindowInsetsListener((v,insets)->{
        int top=insets.getSystemWindowInsetTop();
        toolbar21.setPadding(dp(13),top+dp(12),dp(13),dp(10));
        return insets;
      });
      root21.requestApplyInsets();
    }
    // Highlight measured progress instead of using lengthy paragraphs.
    if(progress!=null){
      progress.setProgressTintList(ColorStateList.valueOf(SUCCESS_GREEN));
      progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xffE4EEE5));
      if(progress.getParent() instanceof LinearLayout){
        LinearLayout parent=(LinearLayout)progress.getParent();
        scanPercent22=tv("0% do lote",12,SUCCESS_GREEN);
        scanPercent22.setPadding(0,dp(7),0,dp(2));
        parent.addView(scanPercent22,parent.indexOfChild(progress));
      }
    }
    ui22=true;
    navigate(1);
    refresh();
  }
  @Override public void onCreate(Bundle bundle){
    super.onCreate(bundle);
    if(screenTitle!=null)screenTitle.setText("Análise de iframes");
  }

  @Override void createIframePage(){
    // This page intentionally does not display inherited multi-line legacy summaries.
    dashboard22=new LinearLayout(this);
    dashboard22.setOrientation(LinearLayout.VERTICAL);
    iframeBody.addView(dashboard22);

    LinearLayout overview=sectionCard(dashboard22,"Progresso da análise",
      "A percentagem representa os iframes com diagnóstico concluído, não os vídeos reproduzíveis.");
    LinearLayout headline=new LinearLayout(this);headline.setGravity(Gravity.CENTER_VERTICAL);
    percent22=tv("0%",30,SUCCESS_GREEN);percent22.setTypeface(null,1);
    headline.addView(percent22);
    processed22=tv("0 de 0 analisados",12,FAINT);
    processed22.setPadding(dp(14),0,0,0);
    headline.addView(processed22,new LinearLayout.LayoutParams(0,-2,1));
    overview.addView(headline);
    overview.addView(gap(8));
    iframeProgress22=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    iframeProgress22.setMax(100);
    iframeProgress22.setProgressTintList(ColorStateList.valueOf(SUCCESS_GREEN));
    iframeProgress22.setProgressBackgroundTintList(ColorStateList.valueOf(GREEN_LIGHT));
    overview.addView(iframeProgress22,new LinearLayout.LayoutParams(-1,dp(10)));
    overview.addView(gap(12));

    LinearLayout upper=new LinearLayout(this);
    valueMedia22=miniStat(upper,"Mídia observada",SUCCESS_GREEN);
    valueWait22=miniStat(upper,"Interação exigida",WINE);
    overview.addView(upper);overview.addView(gap(8));
    LinearLayout lower=new LinearLayout(this);
    valuePending22=miniStat(lower,"Pendentes",WINE);
    valueErrors22=miniStat(lower,"Erros / sem fonte",RED_WARNING);
    overview.addView(lower);

    LinearLayout actions=sectionCard(dashboard22,"Ações da fila",
      "Analise as descobertas importadas sem preencher novamente o endereço do site.");
    inboxCount21=tv("0 na fila",14,WINE);
    actions.addView(inboxCount21);
    actions.addView(gap(8));
    btnFull(actions,"▶  Iniciar análise",true,()->reanalyzeFrames());
    actions.addView(gap(7));
    btnFull(actions,"＋  Enviar descobertos à fila",false,()->stageAllFrames(true));
    actions.addView(gap(7));
    btnFull(actions,"■  Parar análise",false,()->stopScan("Análise interrompida"));
    actions.addView(gap(7));
    btnFull(actions,"Ver descobertas e inspecionar →",false,()->openIframeDiscoveries());

    LinearLayout statusCard=sectionCard(dashboard22,"Estado atual",
      "O player pode carregar sem disponibilizar mídia. Esses estados são contabilizados separadamente.");
    currentStatus22=tv("Aguardando análise",14,INK);
    currentStatus22.setMaxLines(3);
    statusCard.addView(currentStatus22);
    statusCard.addView(gap(5));
    lastItem22=tv("Nenhuma página em processamento",11,FAINT);
    lastItem22.setMaxLines(2);lastItem22.setTextIsSelectable(true);
    statusCard.addView(lastItem22);

    LinearLayout preview=sectionCard(dashboard22,"Prévia do player",
      "Apenas para acompanhar a página atual. Não clica em anúncios ou controlos de acesso.");
    previewPanel=preview;
    iframeWebHost=new LinearLayout(this);
    iframeWebHost.setOrientation(LinearLayout.VERTICAL);
    preview.addView(iframeWebHost);
    preview.addView(gap(8));
    btnFull(preview,"Inspeção manual opcional",false,()->openPendingInspector());

    LinearLayout metrics=sectionCard(dashboard22,"Resultados encontrados",
      "Separação entre conteúdo HTML5, frames de publicidade e elementos vazios.");
    classifier22=tv("Ainda sem diagnósticos",12,INK);
    metrics.addView(classifier22);

    LinearLayout queueCard=sectionCard(dashboard22,"Fila resumida",
      "Mostramos poucos itens por vez; a lista completa está em Descobertas e players.");
    queueStat=tv("",12,WINE);
    queueCard.addView(queueStat);
    iframeQueueCards=new LinearLayout(this);
    iframeQueueCards.setOrientation(LinearLayout.VERTICAL);
    queueCard.addView(iframeQueueCards);
    btnFull(queueCard,"Abrir todas as descobertas",false,()->openIframeDiscoveries());

    discovery22=new LinearLayout(this);
    discovery22.setOrientation(LinearLayout.VERTICAL);
    discovery22.setVisibility(View.GONE);
    iframeBody.addView(discovery22);
    LinearLayout discoveryMenu=sectionCard(discovery22,"Descobertas e players",
      "Abra um iframe para inspecionar manualmente ou ver seu endereço de origem.");
    btnFull(discoveryMenu,"← Voltar ao painel de análise",false,()->showDashboard22());
    section=heading("Resultados",16);
    section.setTextColor(WHITE);
    discovery22.addView(section);
    discovery22.addView(gap(5));
    helper=tv("Players e mídias detectados. Anúncios não contam como vídeos.",12,PALE);
    discovery22.addView(helper);
    discovery22.addView(gap(10));
    results=new LinearLayout(this);
    results.setOrientation(LinearLayout.VERTICAL);
    discovery22.addView(results);
    btnFull(discovery22,"Mostrar mais descobertas",false,()->{resultLimit+=70;renderDiscoveries();});

    // Non-null placeholders are deliberately not attached to the UI.
    // The modern widgets above replace the old duplicated status paragraphs.
    pageStat=null;
    playerSummary=null;
    qualityReport=null;
    playerClassifier=null;
    inboxCount21.setText("0 iframes disponíveis");
  }

  TextView miniStat(LinearLayout parent,String label,int color){
    LinearLayout cell=new LinearLayout(this);
    cell.setOrientation(LinearLayout.VERTICAL);
    cell.setPadding(dp(11),dp(10),dp(8),dp(10));
    cell.setBackground(rounded(0xffF7F1F3,13));
    TextView value=tv("0",22,color);value.setTypeface(null,1);
    cell.addView(value);
    TextView title=tv(label,11,FAINT);
    title.setMaxLines(2);cell.addView(title);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(73),1);
    if(parent.getChildCount()>0)p.setMargins(dp(9),0,0,0);
    parent.addView(cell,p);return value;
  }

  void showDashboard22(){
    detailMode22=false;
    if(dashboard22!=null)dashboard22.setVisibility(View.VISIBLE);
    if(discovery22!=null)discovery22.setVisibility(View.GONE);
    if(screenTitle!=null)screenTitle.setText("Análise de iframes");
  }
  @Override void openIframeDiscoveries(){
    navigate(1);
    detailMode22=true;
    dashboard22.setVisibility(View.GONE);
    discovery22.setVisibility(View.VISIBLE);
    screenTitle.setText("Descobertas e players");
    renderDiscoveries();
  }
  @Override void navigate(int index){
    super.navigate(index);
    if(index==1)showDashboard22();
    refresh();
  }

  @Override void createSettingsPage(){
    super.createSettingsPage();
    LinearLayout mode=sectionCard(settingsBody,"Automação de reprodução",
      "Em páginas com vídeo HTML5 direto, a aplicação pode tentar iniciar a reprodução quando a fonte já está disponível. Não clica em anúncios, CAPTCHA ou bloqueios.");
    simplePlayback22=getSharedPreferences("vh22",0).getBoolean("simplePlayback",false);
    CheckBox toggle=new CheckBox(this);
    toggle.setText("Tentar Play automático em HTML5 direto e autorizado");
    toggle.setTextSize(13);toggle.setTextColor(INK);
    toggle.setButtonTintList(ColorStateList.valueOf(WINE));
    toggle.setChecked(simplePlayback22);
    toggle.setOnCheckedChangeListener((button,isChecked)->{
      simplePlayback22=isChecked;
      getSharedPreferences("vh22",0).edit().putBoolean("simplePlayback",isChecked).apply();
      if(web!=null)web.getSettings().setMediaPlaybackRequiresUserGesture(!isChecked);
    });
    mode.addView(toggle);
    mode.addView(tv("Esta opção não automatiza etapas de acesso ou publicidade. Funciona apenas em vídeos HTML5 diretos.",11,FAINT));
  }

  @Override void setupWeb(){
    super.setupWeb();
    simplePlayback22=getSharedPreferences("vh22",0).getBoolean("simplePlayback",false);
    web.getSettings().setMediaPlaybackRequiresUserGesture(!simplePlayback22);
  }

  @Override void inspectDom(android.webkit.WebView view,Task task,int sequence,boolean last){
    if(simplePlayback22&&scanning&&current==task&&scanSerial==sequence&&"IFRAME".equals(task.kind)){
      String simpleScript="(function(){try{"+
        "if(document.querySelector('.video-js,.vjs-big-play-button,#rcEmbedCaptureShield,.captcha_button,[data-rc-embed-capture],iframe[src*=imasdk],iframe[src*=doubleclick]'))return 'gated_or_player_library';"+
        "const v=document.querySelector('video[controls][src]');"+
        "if(!v||!/^https?:/i.test(v.currentSrc||v.src||'')||v.readyState<2)return 'no_direct_media';"+
        "if(v.paused){v.play().catch(()=>{});return 'requested_play'}return 'already_playing';"+
        "}catch(e){return 'not_supported'}})()";
      view.evaluateJavascript(simpleScript,null);
    }
    super.inspectDom(view,task,sequence,last);
  }
  @Override void showHits(){
    if(!ui22)return;
    openIframeDiscoveries();
  }
  @Override void renderInbox(){
    if(iframeQueueCards==null)return;
    iframeQueueCards.removeAllViews();
    int display=0;
    for(Map.Entry<String,String> e:inbox.entrySet()){
      if(display++>=4)break;
      LinearLayout line=new LinearLayout(this);
      line.setPadding(dp(9),dp(8),dp(9),dp(8));
      line.setBackground(rounded(BLUSH,9));
      String path=e.getKey();
      try {
        android.net.Uri u=android.net.Uri.parse(path);
        path=(u.getHost()==null?"":u.getHost())+(u.getPath()==null?"":u.getPath());
      }catch(Exception ignored){}
      TextView t=tv(path,11,INK);t.setMaxLines(2);line.addView(t);
      iframeQueueCards.addView(line);iframeQueueCards.addView(gap(4));
    }
    if(inbox.isEmpty())iframeQueueCards.addView(tv("Sem itens pendentes. Envie iframes encontrados à análise.",12,FAINT));
    if(inbox.size()>4)iframeQueueCards.addView(tv("E mais "+(inbox.size()-4)+" entradas disponíveis.",11,FAINT));
  }
  @Override void refresh(){
    super.refresh();
    if(!ui22||percent22==null)return;
    LinkedHashSet<String> relevant=new LinkedHashSet<>();
    for(Hit h:hits.values())if(eligible(h)&&playerLike(h.url))relevant.add(h.url);
    for(String url:inbox.keySet()){
      if(playerLike(url))relevant.add(url);
    }
    int total=relevant.size(), media=0,wait=0,pending=0,errors=0,completed=0;
    for(String url:relevant){
      FrameState f=observations.get(url);
      String state=f==null?"PENDENTE":f.status;
      if("MIDIA_DETECTADA".equals(state)){media++;completed++;}
      else if("INTERACAO_NECESSARIA".equals(state)){wait++;completed++;}
      else if("PENDENTE".equals(state)||"ANALISANDO".equals(state)){pending++;}
      else {errors++;completed++;}
    }
    int pct=total==0?0:Math.min(100,(int)Math.round(completed*100.0/total));
    percent22.setText(pct+"%");
    processed22.setText(completed+" / "+total+" com resultado");
    iframeProgress22.setProgress(pct);
    valueMedia22.setText(String.valueOf(media));
    valueWait22.setText(String.valueOf(wait));
    valuePending22.setText(String.valueOf(pending));
    valueErrors22.setText(String.valueOf(errors));
    if(currentStatus22!=null){
      currentStatus22.setText(scanning? (runMode==2?"Analisando os iframes da fila":"Descobrindo páginas") :
          (total==0?"Nenhum iframe para analisar":completed==total?"Análise encerrada":"Pronto para continuar"));
    }
    if(lastItem22!=null){
      String u=(scanning&&current!=null)?current.url:"";
      if(u.isEmpty())lastItem22.setText("Selecione Iniciar análise para processar a fila.");
      else lastItem22.setText(u);
    }
    if(classifier22!=null){
      int empty=0, ads=0,nested=0,observed=0;
      for(PlayerData d:playerData.values()){
        empty+=d.emptyVideos;ads+=d.adIframes;nested+=d.nestedPlayers;
        if(d.videoSource&&d.ready>=2)observed++;
      }
      classifier22.setText("Players com fonte HTML5 observada: "+observed+
        "\nElementos VIDEO vazios: "+empty+
        "\nIframes de publicidade: "+ads+
        "\nPlayers aninhados identificados: "+nested+
        "\nOs links de sessão não são considerados permanentes.");
    }
    if(scanPercent22!=null){
      int used=Math.min(pageLimit,batchCount);
      int pagePct=pageLimit<=0?0:(int)Math.round(used*100.0/pageLimit);
      scanPercent22.setText(pagePct+"% da meta de "+pageLimit+" páginas neste lote");
    }
  }
}
