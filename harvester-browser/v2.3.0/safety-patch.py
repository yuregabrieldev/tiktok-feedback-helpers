from pathlib import Path

root=Path("hb-v18")
source=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
s=source.read_text()
def once(old,new,reason):
    global s
    count=s.count(old)
    if count!=1:raise RuntimeError(f"{reason}: expected 1 match, found {count}")
    s=s.replace(old,new,1)

# The v2.2 recovery path used loadUrl(pinnedUrl) when a redirect managed to
# reach onPageStarted. This can cause a new HTTP request and form a reload loop.
# Stop the navigation without issuing another HTTP request.
once('''                    String target=pinnedTabUrls.get(tab);
                    w.stopLoading();
                    long now=android.os.SystemClock.uptimeMillis();
                    Long prior=pinRestoreAt.put(tab,now);
                    if(target!=null && (prior==null || now-prior>1600L)){
                        w.loadUrl(target);
                    }
                    pageLockNotice();''',
'''                    // Never reload the pinned page here: loadUrl() may loop when
                    // a server insists on redirecting. Keep the user's data intact.
                    w.stopLoading();
                    pageLockNotice();''',
"remove redirect reload loop")

# Track observed resource requests without generating any extra requests.
# A cooldown is per host and pauses background scans after HTTP rate limit
# or unavailable-origin errors; no automatic retry is attempted.
once('''    private final java.util.concurrent.ConcurrentHashMap<Tab,String> pinnedTabUrls=''',
'''    private final java.util.concurrent.ConcurrentHashMap<Tab,java.util.concurrent.atomic.AtomicInteger>
        observedBrowserRequests=new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String,Long>
        pausedScanHosts=new java.util.concurrent.ConcurrentHashMap<>();
    private static final long HOST_PAUSE_MS=10*60*1000L;
    private final java.util.concurrent.ConcurrentHashMap<Tab,String> pinnedTabUrls=''',
"passive request counter and cooldown")

# Counters are deliberately distinct from scanner-triggered traffic.
once('''            @Override public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest request){
                if(selected==tab){''',
'''            @Override public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest request){
                if(selected==tab){
                    observedBrowserRequests.computeIfAbsent(tab,
                        ignored->new java.util.concurrent.atomic.AtomicInteger())
                        .incrementAndGet();''',
"observe WebView network requests passively")

# Pauses should happen on the response to the main document, not on a
# failed image/advertisement subresource or a cross-origin iframe.
needle='''            @Override public void onReceivedError(WebView w,WebResourceRequest req,WebResourceError err){'''
if needle not in s:raise RuntimeError("HTTP error insertion point missing")
s=s.replace(needle,
'''            @Override public void onReceivedHttpError(WebView w,
                WebResourceRequest request,WebResourceResponse response){
                if(request!=null && request.isForMainFrame() && response!=null){
                    int status=response.getStatusCode();
                    if(status==429 || status==522 || status==524 || status==503){
                        pauseScanHost(tab,status);
                    }
                }
            }
'''+needle,1)

# Limit DOM/iframe scan retries and honor per-host suspension.
once('''        if(!autoScanEnabled || tab==null || tab.web==null || !tab.url.startsWith("https://"))return;''',
'''        if(!autoScanEnabled || tab==null || tab.web==null ||
            !tab.url.startsWith("https://") || scanHostPaused(tab))return;''',
"do not inject active scanner on cooling host")
once('''    private void startAutoAnalysis(Tab tab){''',
'''    private void startAutoAnalysis(Tab tab){
        if(scanHostPaused(tab))return;''',
"no automatic analysis during host backoff")
once('''        long[] delays={1700,5900};''',
'''        // Once after load, never three repetitive scans of the same frame.
        long[] delays={2200};''',
"reduce repeated scans")
once('''            if(tab==selected && tab.web!=null && tabs.contains(tab) && tab.scanEpoch==epoch && autoScanEnabled){''',
'''            if(tab==selected && tab.web!=null && tabs.contains(tab) &&
                tab.scanEpoch==epoch && autoScanEnabled && !scanHostPaused(tab)){''',
"protect queued scanner callbacks")
once('''        return autoScanEnabled && autoInteractionsEnabled && popupBlocked && !tab.privateMode''',
'''        return autoScanEnabled && autoInteractionsEnabled && popupBlocked &&
            !scanHostPaused(tab) && !tab.privateMode''',
"prevent automated play interactions during host cooldown")
once('''            @Override public void onPageFinished(WebView w,String link){
                if(pageLockBlocks(tab,link)){
                    pageLockNotice();
                    return;
                }
                tab.url=link;
                injectPageLock(tab);
                if(!tab.privateMode && link.startsWith("https://"))store.history(link,tab.title);
                if(autoScanEnabled)startAutoAnalysis(tab);
                else capturePage(tab);''',
'''            @Override public void onPageFinished(WebView w,String link){
                if(pageLockBlocks(tab,link)){
                    pageLockNotice();
                    return;
                }
                tab.url=link;
                injectPageLock(tab);
                if(!tab.privateMode && link.startsWith("https://"))store.history(link,tab.title);
                if(autoScanEnabled && !scanHostPaused(tab))startAutoAnalysis(tab);
                else if(!autoScanEnabled)capturePage(tab);''',
"skip scanning HTTP error pages during cooldown")

# When a new media link arrives, discovery must remain passive. Guard any
# live event handler while cooldown is active, without blocking normal playback.
once('''        if(!autoScanEnabled || json==null || json.length()>4096)return;''',
'''        if(!autoScanEnabled || scanHostPaused(tab) || json==null || json.length()>4096)return;''',
"ignore scanner events while host cooling")
once('''        if(!autoOpenPlayerOnPlay||!autoScanEnabled||tab==null||''',
'''        if(!autoOpenPlayerOnPlay||!autoScanEnabled||tab==null||scanHostPaused(tab)||''',
"disable automatic open on rate limit")
once('''        detectedTimes.remove(tab);lastPlayPrompt.remove(tab);
        pinnedTabUrls.remove(tab);pinRestoreAt.remove(tab);''',
'''        detectedTimes.remove(tab);lastPlayPrompt.remove(tab);
        observedBrowserRequests.remove(tab);
        pinnedTabUrls.remove(tab);pinRestoreAt.remove(tab);''',
"free counter on tab closing")

# Introduce a simple real-time diagnostic based on request interception.
once('''        panelLine(page.body,"◎","Análise automática",autoScanEnabled?"Ativa":"Inativa",null,null);''',
'''        panelLine(page.body,"◎","Análise automática",
            scanHostPaused(t)?"Pausada por proteção de tráfego":
            (autoScanEnabled?"Ativa":"Inativa"),null,null);
        java.util.concurrent.atomic.AtomicInteger count=observedBrowserRequests.get(t);
        panelLine(page.body,"⇄","Recursos observados pelo WebView",
            String.valueOf(count==null?0:count.get())+
            " · observação passiva, não são pedidos adicionais",null,null);
        panelLine(page.body,"⏳","Proteção de tráfego",
            scanHostPaused(t)?"Suspensão temporária (até 10 min), sem retentativas automáticas":
                "Ativa · nenhuma retentativa automática",null,null);''',
"scanner stats and cooldown UI")

# Helper methods: do not make any network requests.
anchor='''    private boolean pageLockBlocks(Tab tab,String attemptedUrl){'''
if s.count(anchor)!=1:raise RuntimeError("helper insertion marker missing")
helpers='''    private boolean scanHostPaused(Tab tab){
        if(tab==null)return false;
        String host=currentHost(tab);
        if(host==null)return false;
        Long until=pausedScanHosts.get(host);
        if(until==null)return false;
        if(android.os.SystemClock.elapsedRealtime()>=until){
            pausedScanHosts.remove(host,until);
            return false;
        }
        return true;
    }
    private void pauseScanHost(Tab tab,int httpCode){
        if(tab==null)return;
        String host=currentHost(tab);
        if(host==null)return;
        pausedScanHosts.put(host,android.os.SystemClock.elapsedRealtime()+HOST_PAUSE_MS);
        // Disable any already-installed automation script, without reloading.
        for(Tab open:tabs){
            if(open.web==null || !host.equals(currentHost(open)))continue;
            open.scannerNote="Análise suspensa pelo HTTP "+httpCode+
                ". Sem novas tentativas automáticas.";
            try{open.web.evaluateJavascript(
                "window.__hbrAutoScanner&&window.__hbrAutoScanner.configure(false);",null);
            }catch(RuntimeException ignored){}
        }
        if(selected==tab)runOnUiThread(()->toast("HTTP "+httpCode+
            ": scanner pausado neste domínio durante 10 minutos."));
    }
'''
s=s.replace(anchor,helpers+anchor,1)
source.write_text(s)

# Update the version without changing the bundled VistaCast integration.
gradle=root/"app/build.gradle"
g=gradle.read_text()
if "versionName '2.2.0'" not in g:raise RuntimeError("Expected v2.2 base")
g=g.replace("versionName '2.2.0'","versionName '2.3.0'").replace("versionCode 13","versionCode 14")
gradle.write_text(g)
print("v2.3: removed navigation reload loop, added host cooldown & passive request counters")
