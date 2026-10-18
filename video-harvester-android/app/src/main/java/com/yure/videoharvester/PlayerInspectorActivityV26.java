package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class PlayerInspectorActivityV26 extends PlayerInspectorActivity {
  TextView downloadSummary;
  TextView scriptsSummary;
  String downloadHost="", downloadFormat="";
  boolean hasDownload=false, temporary=false;

  @Override public void onCreate(Bundle bundle){
    super.onCreate(bundle);
    ViewGroup container=(ViewGroup)browser.getParent();
    TextView heading=(TextView)container.getChildAt(0);
    heading.setText("Inspeção HTML5 • v2.6");
    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(12),dp(9),dp(12),dp(9));
    panel.setBackground(bg(0xffF4E9ED));

    downloadSummary=new TextView(this);
    downloadSummary.setTextSize(12);
    downloadSummary.setTextColor(0xff651F36);
    downloadSummary.setText("Nenhum download iniciado nesta inspeção.");
    panel.addView(downloadSummary);

    Button register=new Button(this);
    register.setText("Guardar diagnóstico do Download em Vídeos");
    style(register,true);
    panel.addView(register,new LinearLayout.LayoutParams(-1,dp(45)));
    register.setOnClickListener(v->registerDownload());

    scriptsSummary=new TextView(this);
    scriptsSummary.setTextSize(11);
    scriptsSummary.setTextColor(0xff70606A);
    scriptsSummary.setText("Scripts e estado do player ainda não verificados.");
    panel.addView(scriptsSummary);

    Button scripts=new Button(this);
    scripts.setText("Ver scripts carregados e estado HTML5");
    style(scripts,false);
    panel.addView(scripts,new LinearLayout.LayoutParams(-1,dp(42)));
    scripts.setOnClickListener(v->inspectScripts());
    container.addView(panel,container.indexOfChild(browser));

    // Observe the metadata of a download initiated by the user.
    // Do not fetch, persist or display potentially signed URLs.
    browser.setDownloadListener((url,userAgent,disposition,mime,contentLength)->{
      Uri uri=Uri.parse(url);
      String host=uri.getHost();
      String path=uri.getPath();
      String ext=(path==null?"":path).toLowerCase(Locale.ROOT);
      boolean media=ext.endsWith(".mp4")||ext.endsWith(".m3u8")||
          ext.endsWith(".webm")||ext.endsWith(".mpd")||
          (mime!=null&&mime.toLowerCase(Locale.ROOT).startsWith("video/"));
      if(!media){
        runOnUiThread(()->downloadSummary.setText("Download iniciado, mas sem formato de vídeo identificado."));
        return;
      }
      downloadHost=host==null?"Desconhecido":host;
      downloadFormat=ext.endsWith(".m3u8")?"HLS":ext.endsWith(".mpd")?"DASH":
        ext.endsWith(".webm")?"WEBM":"MP4";
      Set<String> params=uri.getQueryParameterNames();
      temporary=ext.contains("proxy.")||
        params.contains("ip_bind")||params.contains("signature")||
        params.contains("token")||params.contains("expires")||
        params.contains("auth")||params.contains("sv");
      hasDownload=true;
      runOnUiThread(()->downloadSummary.setText(
        "Download de vídeo observado\nServidor: "+downloadHost+
        "\nFormato: "+downloadFormat+
        "\nPossível vínculo à sessão: "+(temporary?"Sim":"Não confirmado")+
        "\nURL completa e assinatura não armazenadas."));
    });
  }
  void registerDownload(){
    if(!hasDownload){
      Toast.makeText(this,"Inicie normalmente um download no player antes de guardar o diagnóstico",Toast.LENGTH_LONG).show();
      return;
    }
    Intent outcome=new Intent();
    outcome.putExtra("page",getIntent().getStringExtra("url"));
    outcome.putExtra("sourcePage",getIntent().getStringExtra("sourcePage"));
    outcome.putExtra("mediaTitle",getIntent().getStringExtra("mediaTitle"));
    outcome.putExtra("captureToVideos",true);
    outcome.putExtra("downloadObserved",true);
    outcome.putExtra("mediaDetected",true);
    outcome.putExtra("mediaFormat",downloadFormat);
    outcome.putExtra("sourceHost",downloadHost);
    outcome.putExtra("sessionBound",true);
    outcome.putExtra("mediaUrl","");
    outcome.putExtra("readyState",0);
    setResult(RESULT_OK,outcome);
    finish();
  }
  void inspectScripts(){
    String js="(function(){try{const names=[...document.scripts].map(s=>{"+
      "if(!s.src)return 'inline';try{const u=new URL(s.src);return u.hostname+u.pathname;}catch(e){return 'script';}});"+
      "const videos=[...document.querySelectorAll('video')];"+
      "return JSON.stringify({scripts:[...new Set(names)].slice(0,30),"+
      "html5Videos:videos.length,readyStates:videos.map(v=>v.readyState),"+
      "videojs:!!document.querySelector('.video-js,#RedeCanaisPlayer_html5_api')});"+
      "}catch(e){return JSON.stringify({error:'Falha ao ler o DOM'})}})()";
    browser.evaluateJavascript(js,result->{
      try{
        JSONObject obj=new JSONObject(new JSONArray("["+result+"]").getString(0));
        JSONArray scripts=obj.optJSONArray("scripts");
        scriptsSummary.setText("Scripts: "+(scripts==null?0:scripts.length())+
          " • Vídeos HTML5: "+obj.optInt("html5Videos"));
        new AlertDialog.Builder(this).setTitle("Scripts e player")
          .setMessage(obj.toString(2)).setPositiveButton("Fechar",null).show();
      }catch(Exception e){
        Toast.makeText(this,"Não foi possível ler os scripts",Toast.LENGTH_LONG).show();
      }
    });
  }
}
