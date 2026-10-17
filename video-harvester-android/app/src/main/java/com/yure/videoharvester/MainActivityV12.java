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

public class MainActivityV12 extends Activity {
  final int BG=Color.rgb(16,18,22), CARD=Color.rgb(28,31,38), TXT=Color.WHITE, MUT=Color.rgb(170,175,185), ACC=Color.rgb(99,102,241), GREEN=Color.rgb(5,150,105);
  final Handler ui=new Handler(Looper.getMainLooper());
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final ArrayDeque<Task> siteQueue=new ArrayDeque<>(), frameQueue=new ArrayDeque<>();
  final Set<String> siteSeen=new LinkedHashSet<>(), frameSeen=new LinkedHashSet<>(), queuedSite=new LinkedHashSet<>(), queuedFrame=new LinkedHashSet<>();
  final LinkedHashMap<String,Hit> hits=new LinkedHashMap<>();
  final LinkedHashMap<String,Catalog> catalog=new LinkedHashMap<>();
  final ArrayList<Catalog> library=new ArrayList<>();
  EditText urlBox,limitBox,depthBox; CheckBox subdomains; TextView status,count; LinearLayout results; ProgressBar progress; WebView web;
  boolean scanning=false; int pageLimit=100,maxDepth=4; String rootHost="",currentPage=""; Task current;

  @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();loadLibrary();}
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  TextView tv(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
  Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(ACC);return b;}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(18),dp(16),dp(30));
    TextView h=tv("Video Harvester 1.2",28,TXT);h.setTypeface(null,1);body.addView(h);
    TextView sub=tv("Crawler principal completo + scraper separado de iframes + catálogo.",14,MUT);sub.setPadding(0,dp(4),0,dp(14));body.addView(sub);
    urlBox=new EditText(this);urlBox.setHint("https://exemplo.com/");urlBox.setHintTextColor(MUT);urlBox.setTextColor(TXT);urlBox.setSingleLine(true);urlBox.setInputType(InputType.TYPE_TEXT_VARIATION_URI);urlBox.setBackgroundColor(CARD);urlBox.setPadding(dp(12),0,dp(12),0);body.addView(urlBox,new LinearLayout.LayoutParams(-1,dp(52)));
    LinearLayout opts=new LinearLayout(this);opts.setOrientation(LinearLayout.HORIZONTAL);opts.setPadding(0,dp(8),0,0);
    limitBox=new EditText(this);limitBox.setText("400");limitBox.setTextColor(TXT);limitBox.setHintTextColor(MUT);limitBox.setInputType(InputType.TYPE_CLASS_NUMBER);limitBox.setBackgroundColor(CARD);limitBox.setPadding(dp(10),0,dp(10),0);opts.addView(limitBox,new LinearLayout.LayoutParams(dp(90),dp(48)));
    depthBox=new EditText(this);depthBox.setText("4");depthBox.setTextColor(TXT);depthBox.setInputType(InputType.TYPE_CLASS_NUMBER);depthBox.setBackgroundColor(CARD);depthBox.setPadding(dp(10),0,dp(10),0);LinearLayout.LayoutParams dpp=new LinearLayout.LayoutParams(dp(70),dp(48));dpp.setMargins(dp(7),0,0,0);opts.addView(depthBox,dpp);
    subdomains=new CheckBox(this);subdomains.setText("Subdomínios");subdomains.setTextColor(TXT);opts.addView(subdomains,new LinearLayout.LayoutParams(0,dp(48),1));body.addView(opts);
    LinearLayout a=new LinearLayout(this);a.setOrientation(LinearLayout.HORIZONTAL);a.setPadding(0,dp(8),0,0);Button start=btn("Escanear");start.setOnClickListener(v->startScan());a.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));Button stop=btn("Parar");stop.setBackgroundColor(Color.rgb(185,28,28));stop.setOnClickListener(v->stopScan("Interrompido"));LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(0,dp(48),1);stp.setMargins(dp(8),0,0,0);a.addView(stop,stp);body.addView(a);
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);body.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));
    status=tv("Pronto",14,TXT);status.setPadding(0,dp(8),0,0);body.addView(status);count=tv("",13,MUT);count.setPadding(0,dp(3),0,dp(10));body.addView(count);
    LinearLayout views=new LinearLayout(this);views.setOrientation(LinearLayout.HORIZONTAL);Button raw=btn("Descobertos");raw.setOnClickListener(v->showHits());views.addView(raw,new LinearLayout.LayoutParams(0,dp(44),1));Button cat=btn("Catálogo");cat.setBackgroundColor(Color.rgb(55,65,81));cat.setOnClickListener(v->showCatalog());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(44),1);cp.setMargins(dp(7),0,0,0);views.addView(cat,cp);body.addView(views);
    LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);tools.setPadding(0,dp(8),0,0);Button add=btn("Adicionar todos");add.setBackgroundColor(GREEN);add.setOnClickListener(v->addAll());tools.addView(add,new LinearLayout.LayoutParams(0,dp(46),2));Button lib=btn("Biblioteca");lib.setBackgroundColor(Color.rgb(55,65,81));lib.setOnClickListener(v->showLibrary());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);lp.setMargins(dp(7),0,0,0);tools.addView(lib,lp);body.addView(tools);
    Button exp=btn("Exportar JSON");exp.setOnClickListener(v->shareJson());LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(46));ep.setMargins(0,dp(8),0,dp(12));body.addView(exp,ep);
    results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);
    web=new WebView(this);web.setVisibility(View.GONE);body.addView(web,new LinearLayout.LayoutParams(1,1));setupWeb();sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();
  }

  void setupWeb(){
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(true);s.setUserAgentString(s.getUserAgentString()+" VideoHarvester/1.2");
    web.addJavascriptInterface(new Bridge(),"Harvester");
    web.setWebChromeClient(new WebChromeClient(){@Override public boolean onCreateWindow(WebView v,boolean d,boolean g,Message m){addHit("POPUP","blocked://popup",currentPage,"popup",current==null?0:current.depth);return false;}});
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){inspectRequest(r.getUrl().toString());return super.shouldInterceptRequest(v,r);}
      @Override public void onPageFinished(WebView v,String url){currentPage=url;String js="(function(){try{const A=u=>{try{return new URL(u,location.href).href}catch(e){return null}};const links=[...document.querySelectorAll('a[href]')].map(x=>A(x.getAttribute('href'))).filter(Boolean);const iframes=[...document.querySelectorAll('iframe[src],frame[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);const media=[...document.querySelectorAll('video[src],audio[src],source[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);const text=[...document.scripts].map(x=>x.textContent||'').join('\\n');const re=/(https?:\\/\\/[^\\\"'<>\\s]+?(?:\\.m3u8|\\.mpd|\\.mp4|\\.m4v|\\.webm|\\.mov)(?:\\?[^\\\"'<>\\s]*)?)/ig;const inline=[];let m;while((m=re.exec(text))&&inline.length<300)inline.push(m[1]);const meta={title:document.title||'',h1:(document.querySelector('h1')||{}).innerText||'',og:(document.querySelector('meta[property=\\\"og:title\\\"]')||{}).content||'',img:(document.querySelector('meta[property=\\\"og:image\\\"]')||{}).content||''};Harvester.dom(JSON.stringify({url:location.href,links,iframes,media,inline,meta}));}catch(e){Harvester.err(String(e))}})();";v.evaluateJavascript(js,null);ui.postDelayed(()->nextTask(),1200);}
    });
  }

  void startScan(){
    String raw=urlBox.getText().toString().trim();if(raw.isEmpty()){toast("Informe uma URL");return;}if(!raw.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))raw="https://"+raw;
    try{Uri u=Uri.parse(raw);rootHost=u.getHost();if(rootHost==null)throw new Exception();rootHost=rootHost.toLowerCase(Locale.ROOT);pageLimit=Math.max(1,Math.min(5000,Integer.parseInt(limitBox.getText().toString().trim())));maxDepth=Math.max(0,Math.min(10,Integer.parseInt(depthBox.getText().toString().trim())));}catch(Exception e){toast("URL ou limite inválido");return;}
    siteQueue.clear();frameQueue.clear();siteSeen.clear();frameSeen.clear();queuedSite.clear();queuedFrame.clear();hits.clear();catalog.clear();results.removeAllViews();scanning=true;enqueueSite(raw);discoverSitemap(raw);status.setText("Preparando crawler principal…");nextTask();
  }

  void discoverSitemap(String root){io.submit(()->{try{URL b=new URL(root);String sm=b.getProtocol()+"://"+b.getHost()+(b.getPort()>0?":"+b.getPort():"")+"/sitemap.xml";HttpURLConnection c=(HttpURLConnection)new URL(sm).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","Mozilla/5.0 VideoHarvester/1.2");if(c.getResponseCode()>=200&&c.getResponseCode()<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();String line;while((line=r.readLine())!=null&&x.length()<4000000)x.append(line);Matcher m=Pattern.compile("<loc>\\s*(.*?)\\s*</loc>",Pattern.CASE_INSENSITIVE).matcher(x.toString());ArrayList<String> f=new ArrayList<>();while(m.find()&&f.size()<pageLimit*4)f.add(m.group(1).replace("&amp;","&"));ui.post(()->{for(String z:f)enqueueSite(z);refresh();});}}catch(Exception ignored){}});}

  void nextTask(){
    if(!scanning)return;
    Task t=null;
    while(!siteQueue.isEmpty() && siteSeen.size()<pageLimit){Task q=siteQueue.poll();queuedSite.remove(q.url);if(!siteSeen.contains(q.url)){t=q;break;}}
    if(t==null){while(!frameQueue.isEmpty()){Task q=frameQueue.poll();queuedFrame.remove(q.key());if(!frameSeen.contains(q.key())){t=q;break;}}}
    if(t==null){stopScan("Concluído: páginas e iframes analisados");showCatalog();return;}
    current=t;currentPage=t.url;if(t.kind.equals("SITE"))siteSeen.add(t.url);else frameSeen.add(t.key());
    status.setText((t.kind.equals("SITE")?"Página ":"Iframe depth "+t.depth+" ")+t.url);
    progress.setProgress((int)Math.min(100,siteSeen.size()*100.0/pageLimit));refresh();web.loadUrl(t.url);
  }

  void enqueueSite(String s){try{String n=norm(s);Uri u=Uri.parse(n);String h=u.getHost(),sc=u.getScheme();if(h==null||!allowed(h)||(!"http".equalsIgnoreCase(sc)&&!"https".equalsIgnoreCase(sc)))return;if(siteSeen.size()+siteQueue.size()>=pageLimit*3)return;if(!siteSeen.contains(n)&&queuedSite.add(n))siteQueue.add(new Task(n,0,"SITE",""));}catch(Exception ignored){}}
  void enqueueFrame(String s,int depth,String parent){if(depth>maxDepth)return;try{String n=norm(s);Uri u=Uri.parse(n);String sc=u.getScheme();if(u.getHost()==null||(!"http".equalsIgnoreCase(sc)&&!"https".equalsIgnoreCase(sc)))return;Task t=new Task(n,depth,"IFRAME",parent);if(!frameSeen.contains(t.key())&&queuedFrame.add(t.key()))frameQueue.add(t);}catch(Exception ignored){}}
  boolean allowed(String h){h=h.toLowerCase(Locale.ROOT);return h.equals(rootHost)||(subdomains.isChecked()&&h.endsWith("."+rootHost));}
  String norm(String s){return Uri.parse(s.trim()).buildUpon().fragment(null).build().toString();}
  void stopScan(String m){scanning=false;try{web.stopLoading();}catch(Exception ignored){}status.setText(m);refresh();}

  class Bridge{
    @JavascriptInterface public void dom(String json){try{JSONObject o=new JSONObject(json);String page=o.optString("url",currentPage);int depth=current==null?0:current.depth;JSONObject meta=o.optJSONObject("meta");String title="";String img="";if(meta!=null){title=first(meta.optString("og"),meta.optString("h1"),meta.optString("title"));img=meta.optString("img");}
      JSONArray a=o.optJSONArray("links");if(a!=null&&current!=null&&current.kind.equals("SITE"))for(int i=0;i<a.length();i++)enqueueSite(a.optString(i));
      a=o.optJSONArray("iframes");if(a!=null)for(int i=0;i<a.length();i++){String f=a.optString(i);addHit("IFRAME",f,page,"dom",depth);enqueueFrame(f,depth+1,page);}
      a=o.optJSONArray("media");if(a!=null)for(int i=0;i<a.length();i++)classify(a.optString(i),page,"dom",depth,title,img);
      a=o.optJSONArray("inline");if(a!=null)for(int i=0;i<a.length();i++)classify(a.optString(i),page,"script",depth,title,img);
      refresh();
    }catch(Exception ignored){}}
    @JavascriptInterface public void err(String e){ui.post(()->status.setText("Aviso: "+e));}
  }

  void inspectRequest(String u){String l=u.toLowerCase(Locale.ROOT),t=null;if(l.contains(".m3u8"))t="HLS";else if(l.contains(".mpd"))t="DASH";else if(l.matches(".*\\.(mp4|m4v)(\\?.*)?$"))t="MP4";else if(l.matches(".*\\.(webm|mov)(\\?.*)?$"))t="VIDEO";if(t!=null){addHit(t,u,currentPage,"network",current==null?0:current.depth);catalogize(t,u,currentPage,"","",current==null?0:current.depth);}}
  void classify(String u,String page,String detector,int depth,String title,String img){String l=u.toLowerCase(Locale.ROOT);String t=l.contains(".m3u8")?"HLS":l.contains(".mpd")?"DASH":l.contains(".mp4")||l.contains(".m4v")?"MP4":l.contains(".webm")?"WEBM":"MEDIA";addHit(t,u,page,detector,depth);catalogize(t,u,page,title,img,depth);}
  void addHit(String t,String u,String p,String d,int depth){if(u==null||u.trim().isEmpty())return;ui.post(()->{String k=t+"|"+u;if(!hits.containsKey(k)){hits.put(k,new Hit(t,u,p,d,depth));refresh();}});}

  void catalogize(String fmt,String u,String page,String title,String img,int depth){ui.post(()->{String raw=first(title,slugTitle(page),"Sem título");Info info=parseInfo(raw,page);String key=info.type+"|"+info.title.toLowerCase(Locale.ROOT)+"|"+info.year;Catalog c=catalog.get(key);if(c==null){c=new Catalog();c.key=key;c.type=info.type;c.title=info.title;c.year=info.year;c.image=img;catalog.put(key,c);}Source s=new Source();s.season=info.season;s.episode=info.episode;s.label=info.label;s.format=fmt;s.url=u;s.page=page;s.depth=depth;for(Source x:c.sources)if(x.url.equals(u))return;c.sources.add(s);refresh();});}
  Info parseInfo(String text,String page){String x=(text+" "+page).replace('_',' ').replace('-',' ');Info i=new Info();Matcher se=Pattern.compile("(?i)(?:S(?:eason)?\\s*(\\d{1,2})\\s*E(?:pisode)?\\s*(\\d{1,3})|(?:temporada|season)\\s*(\\d{1,2}).{0,30}(?:epis[oó]dio|episode|ep)\\s*(\\d{1,3})|(?:epis[oó]dio|episode|ep)\\s*(\\d{1,3}))").matcher(x);if(se.find()){i.type="series";if(se.group(1)!=null){i.season=num(se.group(1),1);i.episode=num(se.group(2),0);}else if(se.group(3)!=null){i.season=num(se.group(3),1);i.episode=num(se.group(4),0);}else{i.season=1;i.episode=num(se.group(5),0);}}else{i.type="movie";i.season=0;i.episode=0;}
    Matcher y=Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(x);if(y.find())i.year=y.group(1);String clean=x.replaceAll("(?i)S(?:eason)?\\s*\\d{1,2}\\s*E(?:pisode)?\\s*\\d{1,3}"," ").replaceAll("(?i)(temporada|season|epis[oó]dio|episode|ep)\\s*\\d{1,3}"," ").replaceAll("(?i)\\b(480p|720p|1080p|2160p|4k|dublado|legendado|nacional)\\b"," ").replaceAll("https?://\\S+"," ").replaceAll("\\s+"," ").trim();if(clean.length()>80)clean=clean.substring(0,80).trim();i.title=clean.isEmpty()?"Sem título":clean;i.label=i.type.equals("series")?"S"+i.season+"E"+i.episode:"Filme";return i;}
  int num(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}
  String slugTitle(String page){try{String p=Uri.parse(page).getLastPathSegment();if(p==null)return "";return p.replaceAll("\\.[A-Za-z0-9]{1,5}$","").replaceAll("[-_]+"," ");}catch(Exception e){return "";}}
  String first(String...v){for(String s:v)if(s!=null&&!s.trim().isEmpty())return s.trim();return "";}

  void showHits(){results.removeAllViews();for(Hit h:hits.values()){LinearLayout c=card();c.addView(tv(h.type+" • depth "+h.depth+" • "+h.detector,13,Color.rgb(165,180,252)));TextView u=tv(h.url,13,TXT);u.setTextIsSelectable(true);c.addView(u);c.addView(tv("Página: "+h.page,11,MUT));results.addView(c,margin());}if(hits.isEmpty())results.addView(tv("Nenhum resultado ainda.",14,MUT));}
  void showCatalog(){results.removeAllViews();for(Catalog c:catalog.values()){LinearLayout v=card();TextView t=tv((c.type.equals("series")?"SÉRIE • ":"FILME • ")+c.title+(c.year.isEmpty()?"":" ("+c.year+")"),16,TXT);t.setTypeface(null,1);v.addView(t);TreeMap<Integer,Integer> seasons=new TreeMap<>();for(Source s:c.sources)if(c.type.equals("series"))seasons.put(s.season,seasons.getOrDefault(s.season,0)+1);v.addView(tv(c.type.equals("series")?(seasons.toString()+" • "+c.sources.size()+" fontes"):(c.sources.size()+" fontes encontradas"),12,MUT));results.addView(v,margin());}if(catalog.isEmpty())results.addView(tv("Nenhum vídeo catalogado ainda.",14,MUT));}
  LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackgroundColor(CARD);return c;}
  LinearLayout.LayoutParams margin(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));return p;}

  void addAll(){for(Catalog c:catalog.values()){boolean exists=false;for(Catalog l:library)if(l.key.equals(c.key)){exists=true;break;}if(!exists)library.add(c);}saveLibrary();toast("Catálogo adicionado à biblioteca");showLibrary();}
  void showLibrary(){results.removeAllViews();for(Catalog c:library){LinearLayout v=card();v.addView(tv((c.type.equals("series")?"SÉRIE • ":"FILME • ")+c.title,16,TXT));v.addView(tv(c.sources.size()+" fontes",12,MUT));results.addView(v,margin());}if(library.isEmpty())results.addView(tv("Biblioteca vazia.",14,MUT));}
  JSONArray libJson(){JSONArray out=new JSONArray();try{for(Catalog c:library){JSONObject o=new JSONObject();o.put("type",c.type);o.put("title",c.title);o.put("year",c.year);o.put("image",c.image);JSONArray ss=new JSONArray();for(Source s:c.sources){JSONObject z=new JSONObject();z.put("season",s.season);z.put("episode",s.episode);z.put("label",s.label);z.put("format",s.format);z.put("url",s.url);z.put("source_page",s.page);ss.put(z);}o.put("sources",ss);out.put(o);}}catch(Exception ignored){}return out;}
  void saveLibrary(){getSharedPreferences("vh12",0).edit().putString("library",libJson().toString()).apply();}
  void loadLibrary(){try{JSONArray a=new JSONArray(getSharedPreferences("vh12",0).getString("library","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Catalog c=new Catalog();c.type=o.optString("type","movie");c.title=o.optString("title","Sem título");c.year=o.optString("year","");c.image=o.optString("image","");c.key=c.type+"|"+c.title.toLowerCase(Locale.ROOT)+"|"+c.year;JSONArray ss=o.optJSONArray("sources");if(ss!=null)for(int j=0;j<ss.length();j++){JSONObject z=ss.getJSONObject(j);Source s=new Source();s.season=z.optInt("season");s.episode=z.optInt("episode");s.label=z.optString("label");s.format=z.optString("format");s.url=z.optString("url");s.page=z.optString("source_page");c.sources.add(s);}library.add(c);}}catch(Exception ignored){}refresh();}
  void shareJson(){Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/json");i.putExtra(Intent.EXTRA_TEXT,libJson().toString());startActivity(Intent.createChooser(i,"Exportar biblioteca"));}
  void refresh(){if(count!=null)count.setText("Páginas "+siteSeen.size()+"/"+pageLimit+" • fila site "+siteQueue.size()+" • iframes "+frameSeen.size()+" • fila iframe "+frameQueue.size()+" • vídeos "+catalog.size());}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override protected void onDestroy(){scanning=false;io.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}

  static class Task{String url,kind,parent;int depth;Task(String u,int d,String k,String p){url=u;depth=d;kind=k;parent=p;}String key(){return depth+"|"+url;}}
  static class Hit{String type,url,page,detector;int depth;Hit(String t,String u,String p,String d,int x){type=t;url=u;page=p;detector=d;depth=x;}}
  static class Info{String type="movie",title="Sem título",year="",label="";int season=0,episode=0;}
  static class Source{int season,episode,depth;String label="",format="",url="",page="";}
  static class Catalog{String key="",type="movie",title="",year="",image="";ArrayList<Source> sources=new ArrayList<>();}
}
