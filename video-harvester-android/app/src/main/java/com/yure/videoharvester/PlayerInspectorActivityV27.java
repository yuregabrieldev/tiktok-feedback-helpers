package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class PlayerInspectorActivityV27 extends PlayerInspectorActivityV26 {
  private boolean nextPopupAllowed=false;
  private long allowUntil=0;
  private int allowedCount=0;
  private TextView popupSummary;
  private AlertDialog popupDialog;
  private WebView popupWeb;
  private int topInset=0;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    final LinearLayout root=(LinearLayout)browser.getParent();
    root.setOnApplyWindowInsetsListener((view,insets)->{
      int top=insets.getSystemWindowInsetTop();
      int bottom=insets.getSystemWindowInsetBottom();
      if(top!=topInset){
        topInset=top;
        root.setPadding(0,top,0,bottom);
      }
      return insets;
    });
    root.requestApplyInsets();
    ((TextView)root.getChildAt(0)).setText("Inspeção HTML5 • v2.7");

    LinearLayout popupPanel=new LinearLayout(this);
    popupPanel.setOrientation(LinearLayout.VERTICAL);
    popupPanel.setPadding(dp(12),dp(9),dp(12),dp(9));
    popupPanel.setBackground(bg(0xffF4E9ED));
    popupSummary=new TextView(this);
    popupSummary.setTextColor(0xff651F36);
    popupSummary.setTextSize(12);
    popupSummary.setText("Pop-ups fechados por padrão. Pode permitir uma única janela, por iniciativa sua.");
    popupPanel.addView(popupSummary);
    Button allow=new Button(this);
    allow.setText("Permitir próxima janela após meu toque");
    style(allow,true);
    popupPanel.addView(allow,new LinearLayout.LayoutParams(-1,dp(43)));
    allow.setOnClickListener(v->{
      nextPopupAllowed=true;
      allowUntil=SystemClock.elapsedRealtime()+30000;
      popupSummary.setText("Próxima janela autorizada por 30 segundos. Toque agora no Download do player. Apenas uma janela poderá abrir.");
    });
    root.addView(popupPanel,root.indexOfChild(browser));
    browser.getSettings().setSupportMultipleWindows(true);
    browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
    if(Build.VERSION.SDK_INT>=26)browser.getSettings().setSafeBrowsingEnabled(true);
    browser.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onCreateWindow(WebView view,boolean isDialog,boolean isUserGesture,Message resultMsg){
        popups++;
        refresh();
        if(!nextPopupAllowed||SystemClock.elapsedRealtime()>allowUntil||!isUserGesture){
          if(nextPopupAllowed&&!isUserGesture)
            popupSummary.setText("Janela não confirmada como gesto do utilizador. Foi bloqueada. Tente novamente ou abra o Download no navegador externo.");
          return false;
        }
        nextPopupAllowed=false;
        allowedCount++;
        return openSinglePopup(resultMsg);
      }
      @Override public void onShowCustomView(View view,CustomViewCallback callback){
        new AlertDialog.Builder(PlayerInspectorActivityV27.this)
          .setMessage("Modo tela cheia solicitado. Pode continuar no player principal.")
          .setPositiveButton("OK",(d,w)->callback.onCustomViewHidden()).show();
      }
    });
  }

  private boolean openSinglePopup(Message resultMsg){
    if(popupDialog!=null&&popupDialog.isShowing())popupDialog.dismiss();
    popupWeb=new WebView(this);
    popupWeb.setBackgroundColor(Color.WHITE);
    WebSettings settings=popupWeb.getSettings();
    settings.setJavaScriptEnabled(false);
    settings.setDomStorageEnabled(false);
    settings.setSupportMultipleWindows(false);
    if(Build.VERSION.SDK_INT>=26)settings.setSafeBrowsingEnabled(true);

    final LinearLayout modal=new LinearLayout(this);
    modal.setOrientation(LinearLayout.VERTICAL);
    TextView address=new TextView(this);
    address.setText("Aguardando endereço da janela...");
    address.setTextColor(0xff651F36);
    address.setTextSize(12);
    address.setPadding(dp(8),dp(8),dp(8),dp(6));
    modal.addView(address);
    modal.addView(popupWeb,new LinearLayout.LayoutParams(-1,dp(360)));

    popupWeb.setDownloadListener((url,ua,contentDisposition,mime,length)->{
      reportPopupDownload(url,mime);
      address.setText("Download observado no pop-up: "+safeHost(url));
    });
    popupWeb.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
        Uri u=request.getUrl();
        if(u==null)return false;
        String scheme=u.getScheme();
        if(!"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme)){
          address.setText("Protocolo externo bloqueado");
          return true;
        }
        address.setText("Destino: "+safeHost(u.toString()));
        if(looksLikeVideo(u.toString())){
          reportPopupDownload(u.toString(),"");
          return true;
        }
        return false;
      }
      @Override public void onPageFinished(WebView view,String url){
        address.setText("Página aberta em: "+safeHost(url));
      }
      @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError e){
        if(req.isForMainFrame())address.setText("Falha ao abrir a janela: "+e.getDescription());
      }
    });
    popupDialog=new AlertDialog.Builder(this)
      .setTitle("Janela solicitada pelo player")
      .setMessage("Esta página é externa. Inspecione o domínio antes de interagir. Uma nova janela não confirma que o link seja mídia.")
      .setView(modal)
      .setPositiveButton("Fechar",(d,w)->closePopup())
      .create();
    popupDialog.setOnDismissListener(d->closePopup());
    popupDialog.show();
    WebView.WebViewTransport transport=(WebView.WebViewTransport)resultMsg.obj;
    transport.setWebView(popupWeb);
    resultMsg.sendToTarget();
    popupSummary.setText("Uma janela aberta manualmente • total permitido: "+allowedCount+
      ". Não abriremos outras até você autorizar novamente.");
    return true;
  }

  private static String safeHost(String url){
    try {
      Uri uri=Uri.parse(url);
      return uri.getHost()==null?"endereço sem domínio":uri.getHost();
    }catch(Exception e){return "endereço desconhecido";}
  }
  private static boolean looksLikeVideo(String url){
    try {
      String path=Uri.parse(url).getPath();
      if(path==null)return false;
      String lower=path.toLowerCase(Locale.ROOT);
      return lower.endsWith(".mp4")||lower.endsWith(".webm")||lower.endsWith(".m3u8")||lower.endsWith(".mpd");
    }catch(Exception e){return false;}
  }
  private void reportPopupDownload(String url,String mime){
    if(!looksLikeVideo(url)&&!(mime!=null&&mime.toLowerCase(Locale.ROOT).startsWith("video/")))return;
    Uri uri=Uri.parse(url);
    String path=uri.getPath();
    String low=path==null?"":path.toLowerCase(Locale.ROOT);
    downloadHost=safeHost(url);
    downloadFormat=low.endsWith(".m3u8")?"HLS":low.endsWith(".mpd")?"DASH":
      low.endsWith(".webm")?"WEBM":"MP4";
    Set<String> parameters=uri.getQueryParameterNames();
    temporary=low.contains("proxy.")||parameters.contains("ip_bind")||
      parameters.contains("signature")||parameters.contains("token")||
      parameters.contains("expires")||parameters.contains("auth")||parameters.contains("sv");
    hasDownload=true;
    downloadSummary.setText("Vídeo observado numa janela autorizada\nServidor: "+downloadHost+
      "\nFormato: "+downloadFormat+"\nSessão/assinatura: "+(temporary?"possível":"não confirmada")+
      "\nO link integral não foi guardado.");
  }

  private void closePopup(){
    if(popupWeb!=null){
      WebView child=popupWeb;
      popupWeb=null;
      child.stopLoading();
      if(child.getParent() instanceof ViewGroup)((ViewGroup)child.getParent()).removeView(child);
      child.destroy();
    }
    popupDialog=null;
  }
  @Override protected void onDestroy(){
    closePopup();
    super.onDestroy();
  }
}
