package com.yure.videoharvester;

import android.content.Intent;
import android.graphics.Color;
import android.widget.*;

public class MainActivityV14 extends MainActivityV13 {
  @Override void buildUi(){
    super.buildUi();
    ((TextView)((LinearLayout)((ScrollView)((LinearLayout)findViewById(android.R.id.content).getChildAt(0)).getChildAt(0)).getChildAt(0)).getChildAt(0)).setText("Video Harvester 1.4");
  }
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