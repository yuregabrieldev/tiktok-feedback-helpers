package com.yure.videoharvester;

import android.app.*;
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
  final int BG=Color.rgb(16,18,22), CARD=Color.rgb(28,31,38), TXT=Color.WHITE, MUT=Color.rgb(170,175,185), ACC=Color.rgb(99,102,241), GREEN=Color.rgb(5,150,105);
  final Handler ui=new Handler(Looper.getMainLooper());
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final ArrayDeque<CrawlTask> queue=new ArrayDeque<>();
  final Set<String> seenTasks=new LinkedHashSet<>();
  final LinkedHashMap<String,Hit> hits=new LinkedHashMap<>();
  final LinkedHashMap<String,CatalogItem> catalog=new LinkedHashMap<>();
  final ArrayList<LibraryItem> library=new ArrayList<>();

  EditText urlBox,limitBox,depthBox;
  CheckBox subdomains;
  TextView status,count;
  LinearLayout results;
  ProgressBar progress;
  WebView web;
  boolean scanning=false;
  int limit=100,maxDepth=4;
  String rootHost="",currentPage="";
  CrawlTask currentTask;

  @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();loadLibrary();}
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  TextView tv(String s,int size,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(c);return v;}
  Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(ACC);return b;}

  void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(18),dp(16),dp(30));
    TextView h=tv("Video Harvester 1.1",28,TXT);h.setTypeface(null,1);body.addView(h);
    TextView sub=tv("Crawler de páginas + scraper recursivo de iframes + catálogo organizado.",14,MUT);sub.setPadding(0,dp(4),0,dp(14));body.addView(sub);

    urlBox=new EditText(this);urlBox.setHint("https://exemplo.com/");urlBox.setHintTextColor(MUT);urlBox.setTextColor(TXT);urlBox.setSingleLine(true);urlBox.setInputType(InputType.TYPE_TEXT_VARIATION_URI);urlBox.setBackgroundColor(CARD);urlBox.setPadding(dp(12),0,dp(12),0);body.addView(urlBox,new LinearLayout.LayoutParams(-1,dp(52)));

    LinearLayout opts=new LinearLayout(this);opts.setOrientation(LinearLayout.HORIZONTAL);opts.setPadding(0,dp(8),0,0);
    limitBox=new EditText(this);limitBox.setText("100");limitBox.setHint("Páginas");limitBox.setTextColor(TXT);limitBox.setHintTextColor(MUT);limitBox.setInputType(InputType.TYPE_CLASS_NUMBER);limitBox.setBackgroundColor(CARD);limitBox.setPadding(dp(10),0,dp(10),0);opts.addView(limitBox,new LinearLayout.LayoutParams(dp(95),dp(48)));
    depthBox=new EditText(this);depthBox.setText("4");depthBox.setHint("Depth");depthBox.setTextColor(TXT);depthBox.setHintTextColor(MUT);depthBox.setInputType(InputType.TYPE_CLASS_NUMBER);depthBox.setBackgroundColor(CARD);depthBox.setPadding(dp(10),0,dp(10),0);LinearLayout.LayoutParams dep=new LinearLayout.LayoutParams(dp(80),dp(48));dep.setMargins(dp(7),0,0,0);opts.addView(depthBox,dep);
    subdomains=new CheckBox(this);subdomains.setText("Subdomínios");subdomains.setTextColor(TXT);opts.addView(subdomains,new LinearLayout.LayoutParams(0,dp(48),1));body.addView(opts);

    LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,dp(8),0,0);
    Button start=btn("Escanear");start.setOnClickListener(v->startScan());actions.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));
    Button stop=btn("Parar");stop.setBackgroundColor(Color.rgb(185,28,28));stop.setOnClickListener(v->stopScan("Interrompido"));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1);sp.setMargins(dp(8),0,0,0);actions.addView(stop,sp);body.addView(actions);

    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(6));pp.setMargins(0,dp(10),0,0);body.addView(progress,pp);
    status=tv("Pronto",14,TXT);status.setPadding(0,dp(8),0,0);body.addView(status);count=tv("",13,MUT);count.setPadding(0,dp(3),0,dp(10));body.addView(count);

    LinearLayout viewRow=new LinearLayout(this);viewRow.setOrientation(LinearLayout.HORIZONTAL);
    Button raw=btn("Descobertos");raw.setOnClickListener(v->showHits());viewRow.addView(raw,new LinearLayout.LayoutParams(0,dp(44),1));
    Button cat=btn("Catálogo");cat.setBackgroundColor(Color.rgb(55,65,81));cat.setOnClickListener(v->showCatalog());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(44),1);cp.setMargins(dp(7),0,0,0);viewRow.addView(cat,cp);body.addView(viewRow);

    LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);tools.setPadding(0,dp(8),0,0);
    Button add=btn("Adicionar todos organizados");add.setBackgroundColor(GREEN);add.setOnClickListener(v->addAllCatalog());tools.addView(add,new LinearLayout.LayoutParams(0,dp(46),2));
    Button lib=btn("Biblioteca");lib.setBackgroundColor(Color.rgb(55,65,81));lib.setOnClickListener(v->showLibrary());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);lp.setMargins(dp(7),0,0,0);tools.addView(lib,lp);body.addView(tools);

    Button export=btn("Exportar biblioteca JSON");export.setOnClickListener(v->shareJson());LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(46));ep.setMargins(0,dp(8),0,dp(12));body.addView(export,ep);
    results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);

    web=new WebView(this);web.setVisibility(View.GONE);body.addView(web,new LinearLayout.LayoutParams(1,1));setupWeb();
    scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();
  }

  void setupWeb(){
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(true);s.setUserAgentString(s.getUserAgentString()+" VideoHarvester/1.1");
    web.addJavascriptInterface(new Bridge(),"Harvester");
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onCreateWindow(WebView view,boolean isDialog,boolean isUserGesture,Message resultMsg){
        addHit("POPUP","(nova janela bloqueada)",currentPage,"popup",currentTask==null?0:currentTask.depth,"");
        return false;
      }
    });
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){inspectRequest(r.getUrl().toString());return super.shouldInterceptRequest(v,r);}
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
        String u=r.getUrl().toString();
        if(r.isForMainFrame() && currentTask!=null && !sameUrl(u,currentTask.url)){
          addHit("REDIRECT",u,currentPage,"navigation",currentTask.depth,"");
        }
        return false;
      }
      @Override public void onPageFinished(WebView v,String url){
        currentPage=url;
        String js="(function(){try{const A=u=>{try{return new URL(u,location.href).href}catch(e){return null}};"+
          "try{const ow=window.open;window.open=function(u){try{Harvester.popup(A(u)||String(u||''))}catch(e){};return null}}catch(e){};"+
          "const links=[...document.querySelectorAll('a[href]')].map(x=>A(x.getAttribute('href'))).filter(Boolean);"+
          "const iframes=[...document.querySelectorAll('iframe[src],frame[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);"+
          "const media=[...document.querySelectorAll('video[src],audio[src],source[src],track[src]')].map(x=>A(x.getAttribute('src'))).filter(Boolean);"+
          "const meta={title:document.title||'',h1:(document.querySelector('h1')||{}).innerText||'',og:(document.querySelector('meta[property=\\\"og:title\\\"]')||{}).content||'',img:(document.querySelector('meta[property=\\\"og:image\\\"]')||{}).content||''};"+
          "const text=[...document.scripts].map(x=>x.textContent||'').join('\\n');"+
          "const re=/(https?:\\/\\/[^\\\"'<>\\s]+?(?:\\.m3u8|\\.mpd|\\.mp4|\\.m4v|\\.webm|\\.mov)(?:\\?[^\\\"'<>\\s]*)?)/ig;const inline=[];let m;while((m=re.exec(text))&&inline.length<300)inline.push(m[1]);"+
          "Harvester.dom(JSON.stringify({url:location.href,links,iframes,media,inline,meta}));}catch(e){Harvester.err(String(e))}})();";
        v.evaluateJavascript(js,null);
        ui.postDelayed(new Runnable(){public void run(){next();}},1400);
      }
    });
  }

  void startScan(){
    String raw=urlBox.getText().toString().trim();if(raw.isEmpty()){toast("Informe uma URL");return;}if(!raw.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))raw="https://"+raw;
    try{Uri u=Uri.parse(raw);rootHost=u.getHost();if(rootHost==null)throw new Exception();rootHost=rootHost.toLowerCase(Locale.ROOT);limit=Math.max(1,Math.min(5000,Integer.parseInt(limitBox.getText().toString().trim().isEmpty()?"100":limitBox.getText().toString().trim())));maxDepth=Math.max(0,Math.min(10,Integer.parseInt(depthBox.getText().toString().trim().isEmpty()?"4":depthBox.getText().toString().trim())));}catch(Exception e){toast("URL, limite ou profundidade inválidos");return;}
    queue.clear();seenTasks.clear();hits.clear();catalog.clear();results.removeAllViews();scanning=true;enqueueTask(norm(raw),0,"ROOT","",true);status.setText("Preparando…");discoverSitemap(raw);next();
  }

  void discoverSitemap(String root){io.submit(new Runnable(){public void run(){try{URL b=new URL(root);String sm=b.getProtocol()+"://"+b.getHost()+(b.getPort()>0?":"+b.getPort():"")+"/sitemap.xml";HttpURLConnection c=(HttpURLConnection)new URL(sm).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","Mozilla/5.0 VideoHarvester/1.1");if(c.getResponseCode()>=200&&c.getResponseCode()<300){BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();String line;while((line=r.readLine())!=null&&x.length()<3000000)x.append(line);Matcher m=Pattern.compile("<loc>\\s*(.*?)\\s*</loc>",Pattern.CASE_INSENSITIVE).matcher(x.toString());final ArrayList<String> found=new ArrayList<>();while(m.find()&&found.size()<limit*3)found.add(m.group(1).replace("&amp;","&"));ui.post(new Runnable(){public void run(){for(String z:found)enqueueSitePage(z);refresh();}});}}catch(Exception ignored){}}});}

  void next(){
    if(!scanning)return;
    if(seenTasks.size()>=limit || queue.isEmpty()){stopScan("Concluído");showCatalog();return;}
    CrawlTask n=queue.poll();if(n==null){ui.post(this::next);return;}String key=n.depth+"|"+n.url;if(seenTasks.contains(key)){ui.post(this::next);return;}
    seenTasks.add(key);currentTask=n;currentPage=n.url;status.setText("Analisando "+seenTasks.size()+"/"+limit+" • depth "+n.depth+" • "+n.kind+": "+n.url);progress.setProgress((int)Math.min(100,seenTasks.size()*100.0/limit));refresh();web.loadUrl(n.url);
  }

  void stopScan(String m){scanning=false;try{web.stopLoading();}catch(Exception ignored){}status.setText(m);refresh();}
  String norm(String s){try{return Uri.parse(s.trim()).buildUpon().fragment(null).build().toString();}catch(Exception e){return s;}}
  boolean sameUrl(String a,String b){return norm(a).equals(norm(b));}
  boolean allowedSiteHost(String h){if(h==null)return false;h=h.toLowerCase(Locale.ROOT);return h.equals(rootHost)||(subdomains.isChecked()&&h.endsWith("."+rootHost));}

  void enqueueSitePage(String s){try{String n=norm(s);Uri u=Uri.parse(n);String h=u.getHost(),sc=u.getScheme();if(h==null||!allowedSiteHost(h)||(!"http".equalsIgnoreCase(sc)&&!"https".equalsIgnoreCase(sc)))return;enqueueTask(n,0,"PAGE","",true);}catch(Exception ignored){}}
  void enqueueIframe(String s,String parent,int depth){if(depth>maxDepth)return;try{String n=norm(s);Uri u=Uri.parse(n);String sc=u.getScheme();if(u.getHost()==null||(!"http".equalsIgnoreCase(sc)&&!"https".equalsIgnoreCase(sc)))return;enqueueTask(n,depth,"IFRAME",parent,false);}catch(Exception ignored){}}
  void enqueueTask(String n,int depth,String kind,String parent,boolean siteBound){String k=depth+"|"+n;if(seenTasks.contains(k))return;for(CrawlTask t:queue)if((t.depth+"|"+t.url).equals(k))return;if(seenTasks.size()+queue.size()>=limit*5)return;queue.add(new CrawlTask(n,depth,kind,parent));}

  void inspectRequest(String u){String l=u.toLowerCase(Locale.ROOT),t=null;if(l.contains(".m3u8"))t="HLS";else if(l.contains(".mpd"))t="DASH";else if(l.matches(".*\\.(mp4|m4v)(\\?.*)?$"))t="MP4";else if(l.matches(".*\\.(webm|mov)(\\?.*)?$"))t="VIDEO";if(t!=null)addHit(t,u,currentPage,"network",currentTask==null?0:currentTask.depth,currentTask==null?"":currentTask.parent);}
  void classify(String u,String page,String detector,int depth,String parent){String l=u.toLowerCase(Locale.ROOT);String t=l.contains(".m3u8")?"HLS":l.contains(".mpd")?"DASH":l.contains(".mp4")||l.contains(".m4v")?"MP4":l.contains(".webm")?"WEBM":"MEDIA";addHit(t,u,page,detector,depth,parent);}

  void addHit(String t,String u,String page,String detector,int depth,String parent){if(u==null||u.trim().isEmpty())return;ui.post(new Runnable(){public void run(){String k=t+"|"+u;if(hits.containsKey(k))return;Hit h=new Hit(t,u,page,detector,depth,parent);hits.put(k,h);if(isPlayable(t))catalogize(h);refresh();}});}
  boolean isPlayable(String t){return t.equals("HLS")||t.equals("DASH")||t.equals("MP4")||t.equals("VIDEO")||t.equals("WEBM")||t.equals("MEDIA");}

  class Bridge{
    @JavascriptInterface public void dom(String json){try{JSONObject o=new JSONObject(json);String page=o.optString("url",currentPage);int depth=currentTask==null?0:currentTask.depth;String parent=currentTask==null?"":currentTask.parent;JSONObject meta=o.optJSONObject("meta");if(meta!=null)applyMeta(page,meta);
      JSONArray a=o.optJSONArray("links");if(a!=null && (currentTask==null || currentTask.depth==0))for(int i=0;i<a.length();i++){final String x=a.optString(i);ui.post(new Runnable(){public void run(){enqueueSitePage(x);}});}
      a=o.optJSONArray("iframes");if(a!=null)for(int i=0;i<a.length();i++){String x=a.optString(i);addHit("IFRAME",x,page,"dom",depth,parent);final String fx=x;final int nd=depth+1;ui.post(new Runnable(){public void run(){enqueueIframe(fx,page,nd);}});}
      a=o.optJSONArray("media");if(a!=null)for(int i=0;i<a.length();i++)classify(a.optString(i),page,"dom",depth,parent);
      a=o.optJSONArray("inline");if(a!=null)for(int i=0;i<a.length();i++)classify(a.optString(i),page,"script",depth,parent);
      ui.post(MainActivity.this::refresh);
    }catch(Exception ignored){}}
    @JavascriptInterface public void popup(String u){addHit("POPUP",u,currentPage,"window.open",currentTask==null?0:currentTask.depth,currentTask==null?"":currentTask.parent);}
    @JavascriptInterface public void err(String e){ui.post(new Runnable(){public void run(){status.setText("Aviso: "+e);}});}
  }

  final Map<String,PageMeta> metas=new HashMap<>();
  void applyMeta(String page,JSONObject m){PageMeta pm=new PageMeta();pm.title=firstNonEmpty(m.optString("og"),m.optString("h1"),m.optString("title"));pm.image=m.optString("img");metas.put(norm(page),pm);}
  String firstNonEmpty(String... xs){for(String x:xs)if(x!=null&&!x.trim().isEmpty())return x.trim();return "";}

  void catalogize(Hit h){
    PageMeta pm=metas.get(norm(h.page));String sourceTitle=pm==null?"":pm.title;if(sourceTitle.isEmpty())sourceTitle=titleFromUrl(h.page);
    ParsedTitle p=parseTitle(sourceTitle,h.page);String key=(p.type+"|"+p.title).toLowerCase(Locale.ROOT);CatalogItem item=catalog.get(key);if(item==null){item=new CatalogItem();item.type=p.type;item.title=p.title;item.year=p.year;item.image=pm==null?"":pm.image;catalog.put(key,item);}if(item.image.isEmpty()&&pm!=null)item.image=pm.image;
    Episode ep=new Episode();ep.season=p.season;ep.episode=p.episode;ep.label=p.label;ep.sourcePage=h.page;ep.hit=h;
    String ek=ep.season+"|"+ep.episode+"|"+h.url;boolean exists=false;for(Episode e:item.episodes)if((e.season+"|"+e.episode+"|"+e.hit.url).equals(ek)){exists=true;break;}if(!exists)item.episodes.add(ep);
  }

  String titleFromUrl(String u){try{String path=Uri.parse(u).getLastPathSegment();if(path==null)return "Sem título";path=URLDecoder.decode(path,"UTF-8");path=path.replaceAll("\\.[a-zA-Z0-9]{1,6}$","").replaceAll("[-_]+"," ");return cleanWords(path);}catch(Exception e){return "Sem título";}}
  String cleanWords(String x){x=x.replaceAll("(?i)\\b(1080p|720p|480p|2160p|4k|dublado|legendado|nacional|online|video|filme|serie|série)\\b"," ");x=x.replaceAll("\\s+"," ").trim();return x.isEmpty()?"Sem título":x;}

  ParsedTitle parseTitle(String raw,String url){ParsedTitle p=new ParsedTitle();String s=(raw==null?"":raw).replace('–','-').replace('—','-').trim();String combined=s+" "+url;
    Matcher y=Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(combined);if(y.find())p.year=y.group(1);
    Matcher se=Pattern.compile("(?i)(?:S(?:eason)?\\s*0?(\\d{1,2})\\s*E(?:p(?:isode)?)?\\s*0?(\\d{1,3})|(?:([0-9]{1,2})[ªº]?\\s*Temporada).*?(?:Epis[oó]dio|EP)\\s*0?(\\d{1,3}))").matcher(combined);
    if(se.find()){p.season=parseIntSafe(firstNonEmpty(se.group(1),se.group(3)),1);p.episode=parseIntSafe(firstNonEmpty(se.group(2),se.group(4)),0);p.type="series";}
    else {Matcher ep=Pattern.compile("(?i)(?:Epis[oó]dio|EP)\\s*[-:#]?\\s*0?(\\d{1,3})").matcher(combined);if(ep.find()){p.episode=parseIntSafe(ep.group(1),0);p.type="series";}else p.type="movie";Matcher season=Pattern.compile("(?i)(\\d{1,2})[ªº]?\\s*Temporada|Season\\s*(\\d{1,2})").matcher(combined);if(season.find()){p.season=parseIntSafe(firstNonEmpty(season.group(1),season.group(2)),1);p.type="series";}}
    String title=s.replaceAll("(?i)S(?:eason)?\\s*0?\\d{1,2}\\s*E(?:p(?:isode)?)?\\s*0?\\d{1,3}"," ");title=title.replaceAll("(?i)\\d{1,2}[ªº]?\\s*Temporada"," ");title=title.replaceAll("(?i)(?:Epis[oó]dio|EP)\\s*[-:#]?\\s*0?\\d{1,3}"," ");title=title.replaceAll("(?i)\\b(1080p|720p|480p|2160p|4k|dublado|legendado|nacional|online)\\b"," ");title=title.replaceAll("\\s*[-|:]\\s*$","").replaceAll("\\s+"," ").trim();if(title.isEmpty())title=titleFromUrl(url);p.title=title;p.label=p.type.equals("series")?"T"+p.season+" E"+p.episode:title;return p;
  }
  int parseIntSafe(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}

  void showHits(){results.removeAllViews();for(Hit h:hits.values())addHitCard(h);if(hits.isEmpty())results.addView(tv("Nenhum resultado ainda.",14,MUT));status.setText("Descobertos: "+hits.size());}
  void addHitCard(Hit h){LinearLayout c=card();TextView t=tv(h.type+" • "+h.detector+" • depth "+h.depth,13,Color.rgb(165,180,252));t.setTypeface(null,1);c.addView(t);TextView u=tv(h.url,13,TXT);u.setTextIsSelectable(true);u.setPadding(0,dp(4),0,0);c.addView(u);TextView p=tv("Página: "+h.page,11,MUT);p.setPadding(0,dp(3),0,dp(6));c.addView(p);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button copy=btn("Copiar");copy.setOnClickListener(v->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("URL",h.url));toast("Copiado");});row.addView(copy,new LinearLayout.LayoutParams(0,dp(40),1));if(h.type.equals("IFRAME")){Button deep=btn("Escanear iframe");deep.setBackgroundColor(GREEN);deep.setOnClickListener(v->{scanning=true;enqueueIframe(h.url,h.page,Math.min(maxDepth,h.depth+1));next();});LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(40),1);ap.setMargins(dp(6),0,0,0);row.addView(deep,ap);}c.addView(row);addCardToResults(c);}

  void showCatalog(){results.removeAllViews();ArrayList<CatalogItem> list=new ArrayList<>(catalog.values());Collections.sort(list,new Comparator<CatalogItem>(){public int compare(CatalogItem a,CatalogItem b){return a.title.compareToIgnoreCase(b.title);}});for(CatalogItem item:list)addCatalogCard(item);if(list.isEmpty())results.addView(tv("Nenhum vídeo resolvido ainda. Os iframes encontrados serão analisados recursivamente.",14,MUT));status.setText("Catálogo: "+list.size()+" títulos");}
  void addCatalogCard(CatalogItem item){LinearLayout c=card();TextView title=tv((item.type.equals("series")?"📺 ":"🎬 ")+item.title+(item.year.isEmpty()?"":" ("+item.year+")"),17,TXT);title.setTypeface(null,1);c.addView(title);TreeMap<Integer,Integer> seasons=new TreeMap<>();for(Episode e:item.episodes){int s=e.season;seasons.put(s,seasons.containsKey(s)?seasons.get(s)+1:1);}StringBuilder info=new StringBuilder(item.episodes.size()+" fonte(s)");if(item.type.equals("series")){info.append(" • ");boolean first=true;for(Map.Entry<Integer,Integer> en:seasons.entrySet()){if(!first)info.append(" • ");info.append("T").append(en.getKey()).append(": ").append(en.getValue());first=false;}}TextView meta=tv(info.toString(),13,MUT);meta.setPadding(0,dp(4),0,dp(8));c.addView(meta);Button add=btn("Adicionar título à biblioteca");add.setBackgroundColor(GREEN);add.setOnClickListener(v->{addCatalogToLibrary(item);toast("Adicionado organizado");});c.addView(add,new LinearLayout.LayoutParams(-1,dp(42)));addCardToResults(c);}

  LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackgroundColor(CARD);return c;}
  void addCardToResults(LinearLayout c){LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(8));results.addView(c,cp);}

  void addCatalogToLibrary(CatalogItem item){String key=(item.type+"|"+item.title).toLowerCase(Locale.ROOT);LibraryItem li=null;for(LibraryItem x:library)if(x.key.equals(key)){li=x;break;}if(li==null){li=new LibraryItem();li.key=key;li.type=item.type;li.title=item.title;li.year=item.year;li.image=item.image;library.add(li);}for(Episode e:item.episodes){LibrarySource s=new LibrarySource();s.season=e.season;s.episode=e.episode;s.label=e.label;s.url=e.hit.url;s.format=e.hit.type;s.sourcePage=e.sourcePage;s.detector=e.hit.detector;boolean exists=false;for(LibrarySource q:li.sources)if(q.url.equals(s.url)){exists=true;break;}if(!exists)li.sources.add(s);}saveLibrary();refresh();}
  void addAllCatalog(){for(CatalogItem item:catalog.values())addCatalogToLibrary(item);toast("Catálogo adicionado à biblioteca");showLibrary();}

  void showLibrary(){stopScan("Biblioteca");results.removeAllViews();for(LibraryItem li:library){LinearLayout c=card();TextView t=tv((li.type.equals("series")?"📺 ":"🎬 ")+li.title+(li.year.isEmpty()?"":" ("+li.year+")"),17,TXT);t.setTypeface(null,1);c.addView(t);TreeMap<Integer,Integer> seasons=new TreeMap<>();for(LibrarySource s:li.sources){seasons.put(s.season,seasons.containsKey(s.season)?seasons.get(s.season)+1:1);}StringBuilder b=new StringBuilder(li.sources.size()+" fonte(s)");if(li.type.equals("series"))for(Map.Entry<Integer,Integer> e:seasons.entrySet())b.append(" • T").append(e.getKey()).append(": ").append(e.getValue());c.addView(tv(b.toString(),13,MUT));addCardToResults(c);}if(library.isEmpty())results.addView(tv("Biblioteca vazia.",14,MUT));status.setText("Biblioteca: "+library.size()+" títulos");}

  JSONArray libJson(){JSONArray a=new JSONArray();try{for(LibraryItem li:library){JSONObject o=new JSONObject();o.put("type",li.type);o.put("title",li.title);o.put("year",li.year);o.put("image",li.image);JSONArray ss=new JSONArray();for(LibrarySource s:li.sources){JSONObject z=new JSONObject();z.put("season",s.season);z.put("episode",s.episode);z.put("label",s.label);z.put("format",s.format);z.put("url",s.url);z.put("source_page",s.sourcePage);z.put("detector",s.detector);ss.put(z);}o.put("sources",ss);a.put(o);}}catch(Exception ignored){}return a;}
  void saveLibrary(){getSharedPreferences("vh",0).edit().putString("library_v2",libJson().toString()).apply();}
  void loadLibrary(){try{JSONArray a=new JSONArray(getSharedPreferences("vh",0).getString("library_v2","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);LibraryItem li=new LibraryItem();li.type=o.optString("type","movie");li.title=o.optString("title","Sem título");li.year=o.optString("year");li.image=o.optString("image");li.key=(li.type+"|"+li.title).toLowerCase(Locale.ROOT);JSONArray ss=o.optJSONArray("sources");if(ss!=null)for(int j=0;j<ss.length();j++){JSONObject z=ss.getJSONObject(j);LibrarySource s=new LibrarySource();s.season=z.optInt("season",1);s.episode=z.optInt("episode",0);s.label=z.optString("label");s.format=z.optString("format");s.url=z.optString("url");s.sourcePage=z.optString("source_page");s.detector=z.optString("detector");li.sources.add(s);}library.add(li);}}catch(Exception ignored){}refresh();}
  void shareJson(){try{String data=libJson().toString(2);Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/json");i.putExtra(Intent.EXTRA_TEXT,data);startActivity(Intent.createChooser(i,"Exportar biblioteca"));}catch(Exception e){toast("Falha ao exportar");}}
  void refresh(){if(count!=null)count.setText(seenTasks.size()+" páginas/iframes • "+hits.size()+" achados • "+catalog.size()+" títulos • biblioteca "+library.size()+" • fila "+queue.size());}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override protected void onDestroy(){scanning=false;io.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}

  static class CrawlTask{final String url,kind,parent;final int depth;CrawlTask(String u,int d,String k,String p){url=u;depth=d;kind=k;parent=p;}}
  static class Hit{final String type,url,page,detector,parent;final int depth;Hit(String t,String u,String p,String d,int dep,String par){type=t;url=u;page=p;detector=d;depth=dep;parent=par;}}
  static class PageMeta{String title="",image="";}
  static class ParsedTitle{String type="movie",title="Sem título",year="",label="";int season=1,episode=0;}
  static class Episode{int season=1,episode=0;String label="",sourcePage="";Hit hit;}
  static class CatalogItem{String type="movie",title="",year="",image="";final ArrayList<Episode> episodes=new ArrayList<>();}
  static class LibrarySource{int season=1,episode=0;String label="",format="",url="",sourcePage="",detector="";}
  static class LibraryItem{String key="",type="movie",title="",year="",image="";final ArrayList<LibrarySource> sources=new ArrayList<>();}
}
