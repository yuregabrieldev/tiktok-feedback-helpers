package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class PlaybackTestActivity extends Activity {
  static final int WINE=0xff421323, ACCENT=0xff651F36, LIGHT=0xffF4E9ED;
  LinearLayout root, content;
  TextView status, details;
  VideoView video;
  WebView webpage;
  Handler handler=new Handler(Looper.getMainLooper());
  String sourceUrl="",sourceFormat="",title="",testStatus="NOT_TESTED";
  boolean direct=false, confirmed=false, started=false, finishing=false;
  int lastPosition=0;
  long playbackStarted=0;
  Runnable sample;
  int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
  GradientDrawable round(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
  TextView label(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
  Button button(String caption, boolean strong){
    Button b=new Button(this);b.setAllCaps(false);b.setText(caption);b.setTextSize(14);
    b.setTextColor(strong?Color.WHITE:ACCENT);b.setBackground(round(strong?ACCENT:LIGHT));
    return b;
  }
  void addButton(String caption,boolean strong,Runnable action){
    Button b=button(caption,strong);
    b.setOnClickListener(v->action.run());
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));
    p.topMargin=dp(9);content.addView(b,p);
  }
  void report(String message){
    if(status!=null)status.setText(message);
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    sourceUrl=getIntent().getStringExtra("url");
    if(sourceUrl==null)sourceUrl="";
    sourceFormat=getIntent().getStringExtra("format");
    if(sourceFormat==null)sourceFormat="";
    title=getIntent().getStringExtra("title");
    if(title==null)title="Teste de reprodução";
    direct=!"REFERENCIA_IFRAME".equalsIgnoreCase(sourceFormat);
    getWindow().setStatusBarColor(WINE);
    getWindow().setNavigationBarColor(WINE);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(WINE);
    root.setPadding(dp(14),dp(17),dp(14),dp(16));
    TextView heading=label("Teste de reprodução",22,Color.WHITE);heading.setTypeface(null,1);
    root.addView(heading);
    TextView name=label(title,14,0xffECD3DB);name.setMaxLines(3);
    root.addView(name);
    LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(13),dp(13),dp(13),dp(13));card.setBackground(round(Color.WHITE));
    LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,0,1);
    cardParams.topMargin=dp(16);root.addView(card,cardParams);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);card.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
    content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);

    status=label(direct?"Ainda não reproduzido":"Referência do provedor: não validada",15,ACCENT);
    status.setTypeface(null,1);content.addView(status);
    details=label(direct?
      "O teste usará o reprodutor Android diretamente, sem abrir o player do site. Só consideramos reproduzível quando o vídeo realmente avançar.":
      "A captura guardou o endereço do iframe, não uma fonte independente. Abrir e assistir aqui confirma apenas o funcionamento no player original.",12,0xff786E78);
    details.setPadding(0,dp(8),0,dp(12));content.addView(details);
    if(!sourceUrl.startsWith("https://")&&!sourceUrl.startsWith("http://")){
      report("URL inválida");addButton("Voltar",false,()->finish());setContentView(root);return;
    }
    if(direct){
      video=new VideoView(this);
      video.setBackgroundColor(Color.BLACK);
      content.addView(video,new LinearLayout.LayoutParams(-1,dp(250)));
      MediaController controls=new MediaController(this);
      controls.setAnchorView(video);video.setMediaController(controls);
      video.setOnPreparedListener(mp->{
        report("Fonte carregada — pressione Play para verificar");
        mp.setOnInfoListener((player,what,extra)->{
          if(what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){
            confirmPlayback();
          }
          return false;
        });
      });
      video.setOnErrorListener((mp,what,extra)->{
        testStatus="ERROR";
        report("Falha ao reproduzir a fonte diretamente");
        details.setText("O reprodutor Android não conseguiu carregar ou decodificar o vídeo. A URL pode estar expirada, indisponível ou usar um formato não compatível. Código: "+what+"/"+extra);
        return true;
      });
      addButton("▶  Reproduzir e testar",true,()->beginPlayback());
      addButton("⟳  Tentar novamente",false,()->beginPlayback());
      addButton("Guardar resultado deste teste",false,()->finishWithResult());
    }else{
      webpage=new WebView(this);
      webpage.setBackgroundColor(Color.BLACK);
      webpage.getSettings().setJavaScriptEnabled(true);
      webpage.getSettings().setDomStorageEnabled(true);
      webpage.getSettings().setMediaPlaybackRequiresUserGesture(false);
      CookieManager.getInstance().setAcceptCookie(true);
      CookieManager.getInstance().setAcceptThirdPartyCookies(webpage,true);
      webpage.setWebChromeClient(new WebChromeClient(){
        @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean userGesture,Message msg){return false;}
      });
      webpage.setWebViewClient(new WebViewClient(){
        @Override public void onPageFinished(WebView view,String url){
          report("Player original carregado — reprodução independente não verificada");
        }
        @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError error){
          if(req.isForMainFrame())report("Não foi possível abrir o player original");
        }
      });
      content.addView(webpage,new LinearLayout.LayoutParams(-1,dp(340)));
      addButton("Abrir player original",true,()->{
        report("Abrindo player do site…");
        webpage.loadUrl(sourceUrl);
      });
      addButton("Voltar à Biblioteca",false,()->finishWithResult());
      report("Apenas referência do iframe — ainda sem vídeo independente");
      details.setText("Esta entrada foi capturada depois da reprodução no site, mas o endereço de mídia tinha dados temporários de sessão. Não podemos afirmar que funciona na nossa Netflix. Este teste apenas reabre o player de origem.");
    }
    setContentView(root);
  }
  void beginPlayback(){
    if(video==null||sourceUrl.isEmpty())return;
    confirmed=false;started=true;playbackStarted=System.currentTimeMillis();lastPosition=0;testStatus="TESTING";
    report("Conectando ao vídeo…");
    details.setText("Aguarde o carregamento. O teste precisa verificar reprodução real, não apenas metadados.");
    try{
      video.setVideoURI(Uri.parse(sourceUrl));
      video.requestFocus();
      video.start();
      if(sample!=null)handler.removeCallbacks(sample);
      sample=new Runnable(){
        @Override public void run(){
          if(video==null||finishing||confirmed)return;
          try{
            int position=video.getCurrentPosition();
            if(video.isPlaying()&&position>1000&&position>=lastPosition)confirmPlayback();
            lastPosition=position;
          }catch(Exception ignored){}
          if(System.currentTimeMillis()-playbackStarted>25000){
            if(!confirmed){
              testStatus="TIMEOUT";
              report("Reprodução não confirmada — tempo excedido");
              details.setText("Não foi possível confirmar reprodução após 25 segundos. Tente novamente ou use outra fonte autorizada.");
            }
          }else handler.postDelayed(this,1000);
        }
      };
      handler.postDelayed(sample,1000);
    }catch(Exception e){
      testStatus="ERROR";report("Erro ao iniciar reprodução");
      details.setText(e.getMessage());
    }
  }
  void confirmPlayback(){
    if(confirmed)return;
    confirmed=true;testStatus="PLAYED";
    report("✓ Reprodução direta confirmada neste dispositivo");
    details.setText("O vídeo iniciou no reprodutor Android. Este resultado vale para a URL e sessão atuais; não confirma disponibilidade permanente nem direitos de reutilização.");
    if(sample!=null)handler.removeCallbacks(sample);
  }
  void finishWithResult(){
    Intent result=new Intent();
    result.putExtra("testStatus",testStatus);
    result.putExtra("sourceUrlHash",sourceUrl.hashCode());
    result.putExtra("direct",direct);
    setResult(RESULT_OK,result);
    finishing=true;finish();
  }
  @Override public void onBackPressed(){finishWithResult();}
  @Override protected void onDestroy(){
    finishing=true;
    if(sample!=null)handler.removeCallbacks(sample);
    if(webpage!=null){webpage.stopLoading();webpage.destroy();}
    if(video!=null){video.stopPlayback();}
    super.onDestroy();
  }
}
