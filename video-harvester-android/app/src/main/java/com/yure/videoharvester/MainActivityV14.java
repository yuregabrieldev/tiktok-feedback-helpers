package com.yure.videoharvester;

import android.content.Intent;
import android.graphics.Color;
import android.widget.*;

public class MainActivityV14 extends MainActivityV13 {
  @Override void buildUi(){
    super.buildUi();
    renameTitle((android.view.ViewGroup)findViewById(android.R.id.content));
  }
  boolean renameTitle(android.view.ViewGroup root){for(int i=0;i<root.getChildCount();i++){android.view.View v=root.getChildAt(i);if(v instanceof TextView && ((TextView)v).getText().toString().startsWith("Video Harvester")){((TextView)v).setText("Video Harvester 1.4");return true;}if(v instanceof android.view.ViewGroup && renameTitle((android.view.ViewGroup)v))return true;}return false;}
  @Override void showHits(){
    results.removeAllViews();
    int shown=0;
    for(Hit h:hits.values()){
      LinearLayout card=card();
      card.addView(tv(h.type+" • depth "+h.depth+" • "+h.detector,13,Color.rgb(165,180,252)));
      TextView address=tv(h.url,13,TXT);address.setTextIsSelectable(true);card.addView(address);
      card.addView(tv("Origem: "+h.page,11,MUT));
      if(h.type.equals("IFRAME")&&h.url.startsWith("http")){
        Button inspect=btn("Abrir e inspecionar iframe");
        inspect.setOnClickListener(v->openInspector(h.url));
        card.addView(inspect);
      }
      results.addView(card,margin());shown++;
      if(shown>=600)break;
    }
    if(shown==0)results.addView(tv("Nenhum resultado ainda.",14,MUT));
  }
  void openInspector(String url){
    Intent i=new Intent(this,PlayerInspectorActivity.class);
    i.putExtra("url",url);
    startActivity(i);
  }
}