package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class PlayerInspectorActivityV28 extends PlayerInspectorActivityV27 {
  static final int WINE=0xff421323;
  static final int PALE=0xffF4E9ED;
  static final int SUCCESS=0xff198754;

  private LinearLayout contentRoot, advancedActions;
  private ScrollView advancedScroll;
  private TextView loadLabel;
  private ProgressBar loading;
  private boolean compactReady=false;
  private boolean advancedOpen=false;
  private Button advancedToggle;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    contentRoot=(LinearLayout)browser.getParent();
    if(contentRoot==null)return;

    // Existing buttons keep their original listeners and behaviour:
    // no reload or new browser instance is needed to change the layout.
    TextView legacyTitle=(TextView)contentRoot.getChildAt(0);
    TextView originalInfo=(TextView)contentRoot.getChildAt(1);
    LinearLayout legacyTools=(LinearLayout)contentRoot.getChildAt(2);
    View copyReport=contentRoot.getChildAt(3);
    View saveInspection=contentRoot.getChildAt(4);
    Button captureMedia=(Button)contentRoot.getChildAt(5);
    LinearLayout legacyDownload=(LinearLayout)contentRoot.getChildAt(6);
    LinearLayout legacyPopup=(LinearLayout)contentRoot.getChildAt(7);
    WebView player=browser;

    TextView downloadText=downloadSummary;
    TextView popupsText=(TextView)legacyPopup.getChildAt(0);
    Button allowPopup=(Button)legacyPopup.getChildAt(1);

    contentRoot.removeAllViews();
    legacyDownload.removeView(downloadText);
    legacyPopup.removeView(popupsText);
    legacyPopup.removeView(allowPopup);

    contentRoot.setPadding(dp(9),contentRoot.getPaddingTop(),dp(9),contentRoot.getPaddingBottom());

    LinearLayout titleBar=new LinearLayout(this);
    titleBar.setGravity(Gravity.CENTER_VERTICAL);
    legacyTitle.setText("Inspeção de vídeo");
    legacyTitle.setTextSize(18);
    legacyTitle.setTypeface(null,1);
    legacyTitle.setPadding(dp(5),dp(4),0,dp(2));
    titleBar.addView(legacyTitle,new LinearLayout.LayoutParams(0,dp(42),1));
    Button reload=new Button(this);
    style(reload,false);reload.setText("⟳");
    reload.setTextSize(19);
    titleBar.addView(reload,new LinearLayout.LayoutParams(dp(49),dp(39)));
    reload.setOnClickListener(v->{
      new AlertDialog.Builder(this)
        .setTitle("Recarregar player?")
        .setMessage("A página será recarregada e o vídeo poderá perder a posição atual.")
        .setPositiveButton("Recarregar",(d,w)->browser.reload())
        .setNegativeButton("Cancelar",null).show();
    });
    contentRoot.addView(titleBar);
    originalInfo.setTextSize(11);
    originalInfo.setSingleLine(true);
    originalInfo.setEllipsize(android.text.TextUtils.TruncateAt.END);
    originalInfo.setPadding(dp(5),0,dp(5),dp(2));
    contentRoot.addView(originalInfo,new LinearLayout.LayoutParams(-1,dp(22)));

    LinearLayout progressRow=new LinearLayout(this);
    progressRow.setGravity(Gravity.CENTER_VERTICAL);
    loadLabel=new TextView(this);
    loadLabel.setTextSize(10);loadLabel.setTextColor(0xffE9CCD6);
    loadLabel.setText("Página: a carregar…");
    progressRow.addView(loadLabel,new LinearLayout.LayoutParams(dp(117),dp(19)));
    loading=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    loading.setMax(100);
    loading.setProgressTintList(ColorStateList.valueOf(SUCCESS));
    loading.setProgressBackgroundTintList(ColorStateList.valueOf(0xff805764));
    progressRow.addView(loading,new LinearLayout.LayoutParams(0,dp(5),1));
    contentRoot.addView(progressRow);

    LinearLayout primaryActions=new LinearLayout(this);
    primaryActions.setGravity(Gravity.CENTER_VERTICAL);
    captureMedia.setText("▶ Capturar vídeo");
    captureMedia.setTextSize(12);
    LinearLayout.LayoutParams captureParams=new LinearLayout.LayoutParams(0,dp(48),1.15f);
    captureParams.setMargins(0,dp(4),dp(5),dp(3));
    primaryActions.addView(captureMedia,captureParams);
    allowPopup.setText("Abrir 1 janela");
    allowPopup.setTextSize(12);
    LinearLayout.LayoutParams popupParams=new LinearLayout.LayoutParams(0,dp(48),1);
    popupParams.setMargins(dp(5),dp(4),0,dp(3));
    primaryActions.addView(allowPopup,popupParams);
    contentRoot.addView(primaryActions);

    popupsText.setTextColor(0xffE9CCD6);
    popupsText.setTextSize(11);
    popupsText.setMaxLines(2);
    popupsText.setEllipsize(android.text.TextUtils.TruncateAt.END);
    popupsText.setPadding(dp(5),dp(2),dp(5),dp(3));
    popupsText.setText("Pop-ups: bloqueados por padrão; permita uma única janela quando precisar.");
    contentRoot.addView(popupsText);

    // Focus on the player rather than showing eight buttons above it.
    LinearLayout.LayoutParams webParams=new LinearLayout.LayoutParams(-1,0,1);
    webParams.setMargins(0,dp(4),0,dp(2));
    contentRoot.addView(player,webParams);

    LinearLayout downloadRow=new LinearLayout(this);
    downloadRow.setGravity(Gravity.CENTER_VERTICAL);
    downloadRow.setBackground(bg(PALE));
    downloadRow.setPadding(dp(8),dp(6),dp(8),dp(6));
    downloadText.setText("Download: nenhum evento observado.");
    downloadText.setTextSize(11);
    downloadText.setMaxLines(2);
    downloadText.setEllipsize(android.text.TextUtils.TruncateAt.END);
    downloadText.setTextColor(0xff651F36);
    downloadText.setOnClickListener(v->new AlertDialog.Builder(this)
      .setTitle("Diagnóstico de Download")
      .setMessage(downloadText.getText().toString())
      .setPositiveButton("Fechar",null).show());
    downloadRow.addView(downloadText,new LinearLayout.LayoutParams(-1,-2));
    contentRoot.addView(downloadRow);

    advancedToggle=new Button(this);
    advancedToggle.setText("Ferramentas avançadas  ▾");
    advancedToggle.setTextSize(12);
    style(advancedToggle,false);
    contentRoot.addView(advancedToggle,new LinearLayout.LayoutParams(-1,dp(43)));

    advancedActions=new LinearLayout(this);
    advancedActions.setOrientation(LinearLayout.VERTICAL);
    advancedActions.setPadding(dp(6),dp(6),dp(6),dp(8));
    advancedActions.setBackground(bg(PALE));
    advancedScroll=new ScrollView(this);
    advancedScroll.setFillViewport(false);
    advancedScroll.addView(advancedActions);
    advancedScroll.setVisibility(View.GONE);
    contentRoot.addView(advancedScroll,new LinearLayout.LayoutParams(-1,dp(225)));

    TextView advancedTitle=new TextView(this);
    advancedTitle.setText("Diagnóstico e exportação — só são executados quando você tocar.");
    advancedTitle.setTextSize(11);advancedTitle.setTextColor(0xff651F36);
    advancedTitle.setPadding(dp(5),0,dp(5),dp(5));
    advancedActions.addView(advancedTitle);

    if(saveInspection instanceof Button)((Button)saveInspection).setText("Guardar resultado da inspeção");
    advancedActions.addView(saveInspection,new LinearLayout.LayoutParams(-1,dp(42)));
    advancedActions.addView(legacyTools,new LinearLayout.LayoutParams(-1,dp(50)));
    advancedActions.addView(copyReport,new LinearLayout.LayoutParams(-1,dp(43)));
    advancedActions.addView(legacyDownload,new LinearLayout.LayoutParams(-1,-2));

    advancedToggle.setOnClickListener(v->{
      advancedOpen=!advancedOpen;
      advancedScroll.setVisibility(advancedOpen?View.VISIBLE:View.GONE);
      advancedToggle.setText(advancedOpen?"Ferramentas avançadas  ▴":"Ferramentas avançadas  ▾");
    });

    compactReady=true;
    refresh();
    onInspectorPageProgress(browser.getProgress());
  }

  @Override void onInspectorPageProgress(int progress){
    if(loading==null||loadLabel==null)return;
    final int p=Math.min(100,Math.max(0,progress));
    loading.setProgress(p);
    loadLabel.setText(p==100?"Página: 100%":"Página: "+p+"%");
  }

  @Override void refresh(){
    if(!compactReady){super.refresh();return;}
    if(info==null||browser==null)return;
    final String host;
    try{
      Uri uri=Uri.parse(browser.getUrl());
      host=uri.getHost()==null?"Player local":uri.getHost();
    }catch(Exception ex){
      ui.post(()->info.setText("Player • "+popups+" janelas solicitadas"));
      return;
    }
    final int attempted=popups, observed=requests;
    final String error=lastError;
    ui.post(()->{
      if(info==null)return;
      info.setText(host+"  •  "+attempted+" pop-ups  •  "+observed+" recursos"+(error.isEmpty()?"":"  •  Erro"));
    });
  }
}
