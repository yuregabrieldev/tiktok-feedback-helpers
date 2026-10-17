package com.yure.videoharvester;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
  final int BG=Color.rgb(16,18,22), CARD=Color.rgb(28,31,38), TXT=Color.WHITE, MUT=Color.rgb(170,175,185), ACC=Color.rgb(99,102,241);
  final Handler ui=new Handler(Looper.getMainLooper());
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final ArrayDeque<String> queue=new ArrayDeque<>();
  final Set<String> seen=new LinkedHashSet<>();
  final LinkedHashMap<String,Hit> hits=new LinkedHashMap<>();
  final ArrayList<Hit> library=new ArrayList<>();
  EditText urlBox,limitBox; CheckBox subdomains; TextView status,count; LinearLayout results; ProgressBar progress; WebView web;
  boolean scanning=false; int limit=100; String rootHost="",currentPage="";

  @Override public void onCreate(Bundle b){ super.onCreate(b); buildUi(); loadLibrary(); }
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}  
  TextView tv(String s,int size,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(c);return v;}
  Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(ACC);return b;}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(18),dp(16),dp(30));
    TextView h=tv("Video Harvester",28,TXT);h.setTypeface(null,1);body.addView(h);
    TextView sub=tv("Varra um domínio e encontre iframes e URLs de vídeo expostas pelas páginas.",14,MUT);sub.setPadding(0,dp(4),0,dp(14));body.addView(sub);
    urlBox=new EditText(this);urlBox.setHint("https://exemplo.com/");urlBox.setHintTextColor(MUT);urlBox.setTextColor(TXT);urlBox.setSingleLine(true);urlBox.setInputType(InputType.TYPE_TEXT_VARIATION_URI);urlBox.setBackgroundColor(CARD);urlBox.setPadding(dp(12),0,dp(12),0);body.addView(urlBox,new LinearLayout.LayoutParams(-1,dp(52)));
    LinearLayout opts=new LinearLayout(this);opts.setOrientation(LinearLayout.HORIZONTAL);opts.setPadding(0,dp(8),0,0);
    limitBox=new EditText(this);limitBox.setText("100");limitBox.setTextColor(TXT);limitBox.setInputType(InputType.TYPE_CLASS_NUMBER);limitBox.setBackgroundColor(CARD);limitBox.setPadding(dp(10),0,dp(10),0);opts.addView(limitBox,new LinearLayout.LayoutParams(dp(105),dp(48)));
    subdomains=new CheckBox(this);subdomains.setText("Incluir subdomínios");subdomains.setTextColor(TXT);opts.addView(subdomains,new LinearLayout.LayoutParams(0,dp(48),1));body.addView(opts);
    LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,dp(8),0,0);
    Button start=btn("Escanear");start.setOnClickListener(v->startScan());actions.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));
    Button stop=btn("Parar");stop.setBackgroundColor(Color.rgb(185,28,28));stop.setOnClickListener(v->stopScan("Interrompido"));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1);sp.setMargins(dp(8),0,0,0);actions.addView(stop,sp);body.addView(actions);
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(6));pp.setMargins(0,dp(10),0,0);body.addView(progress,pp);
    status=tv("Pronto",14,TXT);status.setPadding(0,dp(8),0,0);body.addView(status);count=tv("",13,MUT);count.setPadding(0,dp(3),0,dp(10));body.addView(count);
    LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);
    Button add=btn("Adicionar todos");add.setOnClickListener(v->addAll());tools.addView(add,new LinearLayout.LayoutParams(0,dp(46),1));
    Button export=btn("Exportar JSON");export.setOnClickListener(v->shareJson());LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(46),1);ep.setMargins(dp(8),0,0,0);tools.addView(export,ep);body.addView(tools);
    Button lib=btn("Ver biblioteca");lib.setBackgroundColor(Color.rgb(55,65,81));lib.setOnClickListener(v->showLibrary());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));lp.setMargins(0,dp(8),0,dp(12));body.addView(lib,lp);
    results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);
    web=new WebView(this);web.setVisibility(View.GONE);body.addView(web,new LinearLayout.LayoutParams(1,1));setupWeb();
    scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();
  }

  void setupWeb(){
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setUserAgentString(s.getUserAgentString()+" VideoHarvester/1.0");
    web.addJavascriptInterface(new Bridge(),"Harvester");
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){inspectRequest(r.getUrl().toString());return super.shouldInterceptRequest(v,r);}      
      @Override public void onPageFinished(WebView v,String url){currentPage=url;String js="(function(){try{const A=u=>{try{return new URL(u,location.href).href}catch(e){return null}};const links=[...document.querySelectorAll('a[href]')].map(x=>A(x.getAttribute('href'))).filter(Boolean);const iframes=[...document.querySelectorAll('iframe[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);const media=[...document.querySelectorAll('video[src],audio[src],source[src],track[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);const text=[...document.scripts].map(x=>x.textContent||'').join('\\n');const re=/(https?:\\/\\/[^\\\"'<>\\s]+?(?:\\.m3u8|\\.mpd|\\.mp4|\\.m4v|\\.webm|\\.mov)(?:\\?[^\\\"'<>\\s]*)?)/ig;const inline=[];let m;while((m=re.exec(text))&&inline.length<200)inline.push(m[1]);Harvester.dom(JSON.stringify({url:location.href,links,iframes,media,inline}));}catch(e){Harvester.err(String(e))}})();";v.evaluateJavascript(js,null);ui.postDelayed(()->next(),1200);}
    });
  }

  void startScan(){
    String raw=urlBox.getText().toString().trim();if(raw.isEmpty()){toast("Informe uma URL");return;}if(!raw.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))raw="https://"+raw;
    try{Uri u=Uri.parse(raw);rootHost=u.getHost();if(rootHost==null)throw new Exception();rootHost=rootHost.toLowerCase(Locale.ROOT);limit=Math.max(1,Math.min(5000,Integer.parseInt(limitBox.getText().toString().trim().isEmpty()?"100":limitBox.getText().toString().trim())));}catch(Exception e){toast("URL ou limite inválido");return;}
    queue.clear();seen.clear();hits.clear();results.removeAllViews();scanning=true;queue.add(norm(raw));status.setText("Preparando…");discoverSitemap(raw);next();
  }

  void discoverSitemap(String root){io.submit(()->{try{URL b=new URL(root);String sm=b.getProtocol()+"://"+b.getHost()+(b.getPort()>0?":"+b.getPort():"")+"/sitemap.xml";HttpURLConnection c=(HttpURLConnection)new URL(sm).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","Mozilla/5.0 VideoHarvester/1.0");if(c.getResponseCode()>=200&&c.getResponseCode()<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();String line;while((line=r.readLine())!=null&&x.length()<3000000)x.append(line);Matcher m=Pattern.compile("<loc>\\s*(.*?)\\s*</loc>",Pattern.CASE_INSENSITIVE).matcher(x.toString());ArrayList<String> found=new ArrayList<>();while(m.find()&&found.size()<limit*3)found.add(m.group(1).replace("&amp;","&"));ui.post(()->{for(String z:found)enqueue(z);refresh();});}}catch(Exception ignored){}});}

  void next(){
    if(!scanning)return;if(seen.size()>=limit||queue.isEmpty()){stopScan("Concluído");return;}String n=queue.poll();if(n==null||seen.contains(n)){ui.post(this::next);return;}seen.add(n);currentPage=n;status.setText("Analisando "+seen.size()+"/"+limit+": "+n);progress.setProgress((int)Math.min(100,seen.size()*100.0/limit));refresh();web.loadUrl(n);
  }

  void stopScan(String m){scanning=false;try{web.stopLoading();}catch(Exception ignored){}status.setText(m);refresh();}
  String norm(String s){return Uri.parse(s.trim()).buildUpon().fragment(null).build().toString();}
  boolean allowed(String h){h=h.toLowerCase(Locale.ROOT);return h.equals(rootHost)||(subdomains.isChecked()&&h.endsWith("."+rootHost));}
  void enqueue(String s){try{String n=norm(s);Uri u=Uri.parse(n);String h=u.getHost(),sc=u.getScheme();if(h==null||!allowed(h)||(!"http".equalsIgnoreCase(sc)&&!"https".equalsIgnoreCase(sc)))return;if(!seen.contains(n)&&!queue.contains(n)&&seen.size()+queue.size()<limit*5)queue.add(n);}catch(Exception ignored){}}

  void inspectRequest(String u){String l=u.toLowerCase(Locale.ROOT),t=null;if(l.contains(".m3u8"))t="HLS";else if(l.contains(".mpd"))t="DASH";else if(l.matches(".*\\.(mp4|m4v)(\\?.*)?$"))t="MP4";else if(l.matches(".*\\.(webm|mov)(\\?.*)?$"))t="VIDEO";if(t!=null)addHit(t,u,currentPage,"network");}
  void classify(String u,String page,String detector){String l=u.toLowerCase(Locale.ROOT);String t=l.contains(".m3u8")?"HLS":l.contains(".mpd")?"DASH":l.contains(".mp4")||l.contains(".m4v")?"MP4":l.contains(".webm")?"WEBM":"MEDIA";addHit(t,u,page,detector);}
  void addHit(String t,String u,String page,String detector){if(u==null||u.isBlank())return;ui.post(()->{String k=t+"|"+u;if(hits.containsKey(k))return;Hit h=new Hit(t,u,page,detector);hits.put(k,h);addCard(h);refresh();});}

  class Bridge{
    @JavascriptInterface public void dom(String json){try{JSONObject o=new JSONObject(json);String page=o.optString("url",currentPage);JSONArray a=o.optJSONArray("links");if(a!=null)for(int i=0;i<a.length();i++){String x=a.optString(i);ui.post(()->enqueue(x));}a=o.optJSONArray("iframes");if(a!=null)for(int i=0;i<a.length();i++)addHit("IFRAME",a.optString(i),page,"dom");a=o.optJSONArray("media");if(a!=null)for(int i=0;i<a.length();i++)classify(a.optString(i),page,"dom");a=o.optJSONArray("inline");if(a!=null)for(int i=0;i<a.length();i++)classify(a.optString(i),page,"script");ui.post(MainActivity.this::refresh);}catch(Exception ignored){}}
    @JavascriptInterface public void err(String e){ui.post(()->status.setText("Aviso: "+e));}
  }

  void addCard(Hit h){
    LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackgroundColor(CARD);
    TextView t=tv(h.type+" • "+h.detector,13,Color.rgb(165,180,252));t.setTypeface(null,1);c.addView(t);TextView u=tv(h.url,13,TXT);u.setTextIsSelectable(true);u.setPadding(0,dp(4),0,0);c.addView(u);TextView p=tv("Página: "+h.page,11,MUT);p.setPadding(0,dp(3),0,dp(6));c.addView(p);
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button copy=btn("Copiar");copy.setOnClickListener(v->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("URL",h.url));toast("Copiado");});row.addView(copy,new LinearLayout.LayoutParams(0,dp(40),1));Button add=btn("+ Biblioteca");add.setBackgroundColor(Color.rgb(5,150,105));add.setOnClickListener(v->{addLibrary(h);toast("Adicionado");});LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(40),1);ap.setMargins(dp(6),0,0,0);row.addView(add,ap);c.addView(row);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(8));results.addView(c,cp);
  }

  void addLibrary(Hit h){for(Hit x:library)if(x.url.equals(h.url)&&x.type.equals(h.type))return;library.add(h);saveLibrary();refresh();}
  void addAll(){for(Hit h:hits.values())addLibrary(h);toast("Adicionados à biblioteca");showLibrary();}
  void showLibrary(){stopScan("Biblioteca");results.removeAllViews();for(Hit h:library)addCard(h);if(library.isEmpty())results.addView(tv("Biblioteca vazia.",14,MUT));status.setText("Biblioteca: "+library.size()+" itens");}
  JSONArray libJson(){JSONArray a=new JSONArray();try{for(Hit h:library){JSONObject o=new JSONObject();o.put("type",h.type);o.put("url",h.url);o.put("source_page",h.page);o.put("detector",h.detector);a.put(o);}}catch(Exception ignored){}return a;}
  void saveLibrary(){getSharedPreferences("vh",0).edit().putString("library",libJson().toString()).apply();}
  void loadLibrary(){try{JSONArray a=new JSONArray(getSharedPreferences("vh",0).getString("library","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);library.add(new Hit(o.optString("type"),o.optString("url"),o.optString("source_page"),o.optString("detector")));}}catch(Exception ignored){}refresh();}
  void shareJson(){try{String data=libJson().toString(2);Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/json");i.putExtra(Intent.EXTRA_TEXT,data);startActivity(Intent.createChooser(i,"Exportar biblioteca"));}catch(Exception e){toast("Falha ao exportar");}}
  void refresh(){if(count!=null)count.setText(seen.size()+" páginas • "+hits.size()+" resultados • biblioteca "+library.size()+" • fila "+queue.size());}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override protected void onDestroy(){scanning=false;io.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
  static class Hit{final String type,url,page,detector;Hit(String t,String u,String p,String d){type=t;url=u;page=p;detector=d;}}
}
