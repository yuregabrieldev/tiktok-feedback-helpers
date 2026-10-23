from pathlib import Path
root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
src=main.read_text()
def once(old,new,label):
    global src
    count=src.count(old)
    if count!=1: raise RuntimeError(f"{label}: expected one replacement, found {count}")
    src=src.replace(old,new,1)

once('    private boolean popupBlocked=true;',
'''    // Lock belongs to the individual tab, not every page in the browser.
    private final java.util.concurrent.ConcurrentHashMap<Tab,String> pinnedTabUrls=
        new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Tab,Long> pinRestoreAt=
        new java.util.concurrent.ConcurrentHashMap<>();
    private long lastPinMessageAt;
    private boolean popupBlocked=true;''',
"pin state")

once('''            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest req){
                String target=req.getUrl().toString();''',
'''            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest req){
                String target=req.getUrl().toString();
                // Block only top-level page changes. Video/iframe/network requests still work.
                if(req.isForMainFrame() && pageLockBlocks(tab,target)){
                    pageLockNotice();
                    return true;
                }''',
"mainframe navigation interception")

once('''            @Override public void onPageStarted(WebView w,String link,android.graphics.Bitmap favicon){
                tab.url=link;tab.dom="";tab.originalHtml="";''',
'''            @Override public void onPageStarted(WebView w,String link,android.graphics.Bitmap favicon){
                // Fallback for WebView navigation callbacks not intercepted above
                // (including certain POSTs and redirects): cancel and restore the pin.
                if(pageLockBlocks(tab,link)){
                    String target=pinnedTabUrls.get(tab);
                    w.stopLoading();
                    long now=android.os.SystemClock.uptimeMillis();
                    Long prior=pinRestoreAt.put(tab,now);
                    if(target!=null && (prior==null || now-prior>1600L)){
                        w.loadUrl(target);
                    }
                    pageLockNotice();
                    return;
                }
                tab.url=link;tab.dom="";tab.originalHtml="";''',
"uninterceptable navigation fallback")

once('''            @Override public void onPageFinished(WebView w,String link){
                tab.url=link;''',
'''            @Override public void onPageFinished(WebView w,String link){
                if(pageLockBlocks(tab,link)){
                    pageLockNotice();
                    return;
                }
                tab.url=link;
                injectPageLock(tab);''',
"pin injection after page finishes")

once('''                if(message.startsWith("__HBR_NET__"))recordResource(tab,message.substring(11),"JS");''',
'''                if(message.startsWith("__HBR_PIN__")){
                    pageLockNotice();
                    return true;
                }
                if(message.startsWith("__HBR_NET__"))recordResource(tab,message.substring(11),"JS");''',
"native message for blocked document navigation")

once('''                if(!isUserGesture || popupBlocked || tabs.size()>=16){''',
'''                if(!isUserGesture || popupBlocked || pinnedTabUrls.containsKey(tab) || tabs.size()>=16){''',
"no popup from fixed tab")

once('''        menuHeading(box,"Navegação");''',
'''        menuHeading(box,"Navegação");
        autoToggle(box,"Bloquear saída da página atual",
            selected!=null && pinnedTabUrls.containsKey(selected),
            value->setPageLockForCurrentTab(value));
        menuEntry(box,"▣","Informações do bloqueio de página",()->{
            new AlertDialog.Builder(this)
                .setTitle("Bloquear saída da página atual")
                .setMessage("Quando ativado, o Harvester mantém esta aba no endereço atual e impede links, redirecionamentos e mudanças de rota que abram outra página na mesma aba. Elementos dentro da página, vídeos, scripts e pedidos de rede continuam a funcionar. Para navegar a outro endereço, desative o bloqueio no menu ⋮. O bloqueio é individual por aba.")
                .setPositiveButton("Entendi",null).show();
        },popup);''',
"new settings menu")

# Prevent intentional toolbar URL submission while this tab is fixed.
once('''        selected.web.loadUrl(url);''',
'''        if(pageLockBlocks(selected,url)){
            pageLockNotice();
            return;
        }
        selected.web.loadUrl(url);''',
"typed URL respects lock")

# Cleaning up per-tab state when a tab is closed.
once('''        tab.web=null;tab.scanEpoch++;
        detectedTimes.remove(tab);lastPlayPrompt.remove(tab);''',
'''        tab.web=null;tab.scanEpoch++;
        detectedTimes.remove(tab);lastPlayPrompt.remove(tab);
        pinnedTabUrls.remove(tab);pinRestoreAt.remove(tab);''',
"cleanup per-tab pin")

anchor='    private void showAutoScanStatus(){'
if src.count(anchor)!=1: raise RuntimeError("missing helper insertion point")
helpers=r'''    private boolean pageLockBlocks(Tab tab,String attemptedUrl){
        if(tab==null || attemptedUrl==null)return false;
        String pinned=pinnedTabUrls.get(tab);
        return com.harvester.browser.core.PageNavigationLock.blocks(pinned,attemptedUrl);
    }
    private void pageLockNotice(){
        long now=android.os.SystemClock.uptimeMillis();
        if(now-lastPinMessageAt<2200L)return;
        lastPinMessageAt=now;
        runOnUiThread(()->toast("Página fixa: navegação bloqueada. Desative no menu ⋮."));
    }
    private void setPageLockForCurrentTab(boolean enabled){
        Tab tab=selected;
        if(tab==null||tab.web==null)return;
        if(enabled){
            if(tab.url==null || !tab.url.startsWith("https://")){
                toast("Abra primeiro uma página HTTPS para fixá-la.");
                return;
            }
            pinnedTabUrls.put(tab,tab.url);
            pinRestoreAt.remove(tab);
            toast("Página atual fixada. Links e redirecionamentos serão bloqueados.");
        }else{
            pinnedTabUrls.remove(tab);
            pinRestoreAt.remove(tab);
            toast("Bloqueio desativado. A navegação voltou ao normal.");
        }
        injectPageLock(tab);
    }
    private String readPageLockScript(){
        try(java.io.InputStream input=getAssets().open("pagelock.js");
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream()){
            byte[] buf=new byte[4096];int read;
            while((read=input.read(buf))!=-1)bytes.write(buf,0,read);
            return bytes.toString("UTF-8");
        }catch(Exception error){return "";}
    }
    private void injectPageLock(Tab tab){
        if(tab==null || tab.web==null)return;
        String pinned=pinnedTabUrls.get(tab);
        String script=readPageLockScript();
        if(script.isEmpty())return;
        String config=";window.__harvesterPageLock&&window.__harvesterPageLock.configure("
            +(pinned!=null)+","+org.json.JSONObject.quote(pinned==null?"":pinned)+");";
        try{tab.web.evaluateJavascript(script+config,null);}
        catch(Exception error){android.util.Log.w("HarvesterPageLock","Script injection unavailable",error);}
    }
'''
src=src.replace(anchor,helpers+anchor,1)
main.write_text(src)

asset=root/"app/src/main/assets/pagelock.js"
asset.write_text(Path("harvester-browser/v2.2.0/pagelock.js").read_text())
core=root/"app/src/main/java/com/harvester/browser/core/PageNavigationLock.java"
core.write_text(Path("harvester-browser/v2.2.0/PageNavigationLock.java").read_text())
test=root/"core-tests/PageNavigationLockTests.java"
test.write_text(Path("harvester-browser/v2.2.0/PageNavigationLockTests.java").read_text())
gradle=root/"app/build.gradle"
g=gradle.read_text()
if "versionName '2.1.0'" not in g:raise RuntimeError("The previous v2.1 build was not restored")
g=g.replace("versionName '2.1.0'","versionName '2.2.0'").replace("versionCode 12","versionCode 13")
gradle.write_text(g)
print("v2.2 page pin installed with top-level WebView guard, SPA/anchor guard and menu toggle")
