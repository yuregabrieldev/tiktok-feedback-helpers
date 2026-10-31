#!/usr/bin/env python3
"""VistaBrowser 4.4.2: integrated Smart Crawler, persistent queue and single Pause/Resume."""
from pathlib import Path

root = Path("hb-v18/app")
src = root/"src/main/java/com/yure/videoharvester"
collector = src/"VistaSourceCollector.java"
premium = src/"VistaScraperPremiumActivity.java"
gradle = root/"build.gradle"

data = collector.read_text(encoding="utf-8")
def patch_once(original, replacement):
    global data
    assert data.count(original)==1, "collector patch anchor changed: "+original[:100]
    data=data.replace(original,replacement,1)

patch_once(
'''    public static void scan(Activity activity, WebView browser,
                            String hintTitle,String parentPage) {''',
'''    public static void scan(Activity activity, WebView browser,
                            String hintTitle,String parentPage) {
        scan(activity,browser,hintTitle,parentPage,null,null);
    }

    public static void scan(Activity activity, WebView browser,
                            String hintTitle,String parentPage,
                            String iframeUrl,Runnable onCaptured) {''')
patch_once('''                for (int i=0; i<urls.length(); i++)
                    offer(activity, urls.optString(i), title, page);''',
'''                if(iframeUrl!=null && !iframeUrl.isEmpty() &&
                    browser.getUrl()!=null &&
                    !iframeUrl.equals(browser.getUrl()))return;
                boolean captured=false;
                for (int i=0; i<urls.length(); i++)
                    captured |= offer(activity, urls.optString(i), title, page);
                if(captured && iframeUrl!=null && !iframeUrl.isEmpty()){
                    android.content.SharedPreferences prefs=activity.getSharedPreferences(
                        PREFS,Context.MODE_PRIVATE);
                    java.util.HashSet<String> done=new java.util.HashSet<>(
                        prefs.getStringSet("captured_iframes",java.util.Collections.emptySet()));
                    if(done.add(iframeUrl))prefs.edit().putStringSet("captured_iframes",done).apply();
                    if(onCaptured!=null)onCaptured.run();
                }''')
patch_once('''    private static synchronized void offer(Context context,String url,String title,String page) {
        if (!eligible(url) || title == null || title.trim().isEmpty()) return;''',
'''    private static synchronized boolean offer(Context context,String url,String title,String page) {
        if (!eligible(url) || title == null || title.trim().isEmpty()) return false;''')
patch_once('''            if(old!=null && url.equals(old.optString("url")))return;''',
'''            if(old!=null && url.equals(old.optString("url")))return true;''')
patch_once('''                .putString("items",next.toString()).apply();
        } catch (Exception ignored) { }
    }''',
'''                .putString("items",next.toString()).apply();
            return true;
        } catch (Exception ignored) { return false; }
    }''')
collector.write_text(data,encoding="utf-8")

data=premium.read_text(encoding="utf-8")
anchor='''        VistaSourceCollector.scan(this,view,slugTitle(parentPage),parentPage);'''
assert data.count(anchor)==1,"iframe collector integration anchor missing"
data=data.replace(anchor,'''        VistaSourceCollector.scan(this,view,slugTitle(parentPage),parentPage,
            task.url,()->{
                if(!scanning || current!=task || scanSerial!=sequence || runMode!=2)return;
                setState(task.url,"MIDIA_DETECTADA");
                saveCrawlState();
                refresh();
                renderInbox();
                final Task finished=task;
                ui.postDelayed(()->{
                    if(scanning && runMode==2 && current==finished &&
                        scanSerial==sequence)nextTask();
                },450);
            });''',1)
anchor='''    @Override void createIframePage(){'''
assert data.count(anchor)==1,"premium iframe page anchor missing"
methods='''    // One durable source of truth: a media URL only counts as captured once
    // it has actually been saved by VistaSourceCollector. Merely visiting an
    // iframe must never remove it from the queue.
    private TextView crawlerStatus442, queueToggle442;
    private final String crawlerPrefs442="vista_smart_crawler_v1";
    private Set<String> captured442(){
        return new HashSet<>(getSharedPreferences(crawlerPrefs442,0)
            .getStringSet("captured_iframes",Collections.emptySet()));
    }
    private void crawlerSetting442(String name,boolean enabled){
        getSharedPreferences(crawlerPrefs442,0).edit().putBoolean(name,enabled).apply();
    }

    private Task dwellTask442;
    private long dwellStart442=0L;
    private boolean dwellScheduled442=false;
    private static final long DWELL_MS_442=13500L;

    @Override void nextTask(){
        // The inherited WebView auto-advances after only 5.3 s. Keep the
        // iframe visible long enough for recognized play interactions,
        // unless a direct video has already been captured.
        if(scanning && runMode==2 && current!=null &&
            current==dwellTask442 && !captured442().contains(current.url)){
            long elapsed=android.os.SystemClock.elapsedRealtime()-dwellStart442;
            if(elapsed < DWELL_MS_442){
                if(!dwellScheduled442){
                    dwellScheduled442=true;
                    final Task task=current;
                    final int seq=scanSerial;
                    ui.postDelayed(()->{
                        dwellScheduled442=false;
                        if(scanning&&runMode==2&&current==task&&scanSerial==seq)
                            nextTask();
                    },Math.max(300L,DWELL_MS_442-elapsed));
                }
                return;
            }
        }
        super.nextTask();
        dwellTask442=current;
        dwellStart442=android.os.SystemClock.elapsedRealtime();
        dwellScheduled442=false;
    }

    @Override void reanalyzeFrames(){
        if(scanning){toast("A análise já está em execução");return;}
        if(inbox.isEmpty())stageAllFrames(false);
        Set<String> done=captured442();
        onlyFrames=true;runMode=2;frameQueue.clear();queuedFrame.clear();frameSeen.clear();
        current=null;
        int queued=0;
        for(Map.Entry<String,String> entry:inbox.entrySet()){
            if(entry.getKey().isEmpty() || done.contains(entry.getKey()))continue;
            int previous=frameQueue.size();
            enqueueFrame(entry.getKey(),1,entry.getValue());
            if(frameQueue.size()>previous){
                queued++;setState(entry.getKey(),"PENDENTE");
            }
        }
        if(queued==0){
            onlyFrames=false;runMode=0;refresh();
            toast("Não existem novos iframes pendentes para analisar");
            return;
        }
        getSharedPreferences(crawlerPrefs442,0).edit().putBoolean("paused",false).apply();
        scanning=true;
        attachWeb(iframeWebHost,220);navigate(1);
        if(status!=null)status.setText("Smart Crawler • "+queued+" iframes pendentes");
        nextTask();
        launchScanService();
        refresh();
    }

    @Override void stopScan(String message){
        int mode=runMode;
        LinkedHashMap<String,String> snapshot=new LinkedHashMap<>(inbox);
        super.stopScan(message);
        if(mode==2){
            Set<String> done=captured442();
            for(Map.Entry<String,String> e:snapshot.entrySet()){
                if(done.contains(e.getKey()))inbox.remove(e.getKey());
                else inbox.put(e.getKey(),e.getValue());
            }
            saveCrawlState();
            renderInbox();
            refresh();
        }
    }

    private void toggleCrawler442(){
        if(scanning){
            if(runMode!=2){toast("Está a decorrer um scan de páginas");return;}
            getSharedPreferences(crawlerPrefs442,0).edit().putBoolean("paused",true).apply();
            stopScan("Análise pausada; progresso guardado");
        } else reanalyzeFrames();
        refresh();
    }

    private void installIntegratedCrawler442(){
        // All opt-ins live beside the real WebView preview, not in a
        // disconnected Smart Crawler activity.
        if(previewPanel!=null){
            place(previewPanel,t("Smart Crawler • Captura automática",16,true,Styles.TEXT),-2,11);
            String[][] toggles={
                {"enabled","Analisar automaticamente os iframes da fila"},
                {"assist_html5","Tentar iniciar vídeos HTML5 acessíveis"},
                {"assist_play_buttons","Tentar botões Play identificados (até 3 vezes)"}
            };
            for(String[] option:toggles){
                CheckBox cb=new CheckBox(this);
                cb.setText(option[1]);
                cb.setTextSize(12);cb.setTextColor(Styles.TEXT);
                cb.setChecked(getSharedPreferences(crawlerPrefs442,0)
                    .getBoolean(option[0],false));
                cb.setOnCheckedChangeListener((button,checked)->crawlerSetting442(option[0],checked));
                previewPanel.addView(cb);
            }
            crawlerStatus442=t("Sem fontes capturadas nesta análise",12,false,Styles.MUTED);
            place(previewPanel,crawlerStatus442,-2,8);
            place(previewPanel,tap("▣","Rever, exportar ou enviar fontes",()->{
                startActivity(new Intent(this,VistaSourceBrowserActivity.class));
            },false),50,12);
        }
        if(dashboard22==null)return;
        for(int i=0;i<dashboard22.getChildCount();i++){
            View view=dashboard22.getChildAt(i);
            if(!(view instanceof LinearLayout))continue;
            LinearLayout card=(LinearLayout)view;
            if(card.getChildCount()==0 || !(card.getChildAt(0) instanceof TextView))continue;
            if(!"Ações da fila".equals(((TextView)card.getChildAt(0)).getText().toString()))continue;
            for(int index=card.getChildCount()-1;index>=0;index--){
                View child=card.getChildAt(index);
                String desc=String.valueOf(child.getContentDescription());
                if("Iniciar análise".equals(desc)||"Parar análise".equals(desc))
                    card.removeViewAt(index);
            }
            queueToggle442=t("▶   Iniciar análise",14,true,onAccent());
            queueToggle442.setGravity(Gravity.CENTER);
            queueToggle442.setBackground(frame(accent(),17,0));
            queueToggle442.setContentDescription("Pausar ou retomar análise");
            queueToggle442.setOnClickListener(v->toggleCrawler442());
            int insert=Math.min(4,card.getChildCount());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(53));
            lp.topMargin=dp(10);
            card.addView(queueToggle442,insert,lp);
            break;
        }
    }

    @Override void refresh(){
        super.refresh();
        if(crawlerStatus442!=null){
            int stored=VistaSourceCollector.entries(this).length();
            int captured=captured442().size();
            crawlerStatus442.setText(captured+" iframes com vídeo capturado • "+
                stored+" fontes guardadas • "+inbox.size()+" pendentes");
            if(valueMedia22!=null)valueMedia22.setText(String.valueOf(captured));
        }
        if(queueToggle442!=null){
            boolean paused=getSharedPreferences(crawlerPrefs442,0)
                .getBoolean("paused",false);
            queueToggle442.setText(scanning && runMode==2?"Ⅱ   Pausar análise":
                paused?"▶   Retomar análise":"▶   Iniciar análise");
        }
    }

'''
data=data.replace(anchor,methods+anchor,1)
anchor='''        super.createIframePage();'''
assert data.count(anchor)==1,"page hook missing"
data=data.replace(anchor,'''        super.createIframePage();
        installIntegratedCrawler442();''',1)
premium.write_text(data,encoding="utf-8")

# Allow additional safe Play attempts while retaining the six-attempt cap
# and the existing cross-origin/ad restrictions.
interactor=src/"VistaPlayInteractor.java"
text=interactor.read_text(encoding="utf-8")
assert "private static final int MAX_TAPS=3;" in text
interactor.write_text(text.replace("private static final int MAX_TAPS=3;",
    "private static final int MAX_TAPS=6;"),encoding="utf-8")

text=premium.read_text(encoding="utf-8")
text=text.replace("Play identificados (até 3 vezes)","Play identificados (até 6 vezes)")
needle="            handler.postDelayed(()->performVistaIframeAssist(view,task,sequence),6500);"
assert text.count(needle)==1
text=text.replace(needle,needle+"""
            handler.postDelayed(()->performVistaIframeAssist(view,task,sequence),8500);
            handler.postDelayed(()->performVistaIframeAssist(view,task,sequence),11300);""",1)
premium.write_text(text,encoding="utf-8")

grad=gradle.read_text(encoding="utf-8")
assert "versionName '4.4.1'" in grad and "versionCode 46" in grad
grad=grad.replace("versionName '4.4.1'","versionName '4.4.2'",1)
grad=grad.replace("versionCode 46","versionCode 47",1)
gradle.write_text(grad,encoding="utf-8")
print("PASS VistaBrowser 4.4.2: single embedded Smart Crawler + pause/resume + captured-skip queue")
