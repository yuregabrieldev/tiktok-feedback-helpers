from pathlib import Path
import re

root=Path("hb-v18")
browser=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
player=root/"app/src/main/java/com/harvester/browser/NativePlayerActivity.java"
service=root/"app/src/main/java/com/harvester/browser/PlaybackService.java"
js=root/"app/src/main/assets/autoplayer.js"
gradle=root/"app/build.gradle"
src=browser.read_text()

def once(old,new,label):
    global src
    n=src.count(old)
    if n!=1: raise RuntimeError(f"{label}: expected 1 match, got {n}")
    src=src.replace(old,new,1)

# Start the player ONLY after a real play event in the current top-level document.
once('    private boolean popupBlocked=true;',
'''    private boolean autoOpenPlayerOnPlay=true;
    private final java.util.concurrent.ConcurrentHashMap<Tab,
        java.util.concurrent.ConcurrentHashMap<String,Long>> detectedTimes=
            new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Tab,Long> lastPlayPrompt=
            new java.util.concurrent.ConcurrentHashMap<>();
    private boolean popupBlocked=true;''',"automatic media state")
once('        popupBlocked=settings.getBoolean("block_popups",true);',
'''        popupBlocked=settings.getBoolean("block_popups",true);
        autoOpenPlayerOnPlay=settings.getBoolean("auto_open_player_on_play",true);''',"restore user setting")
once('s.setSupportMultipleWindows(true);',
'''        s.setSupportMultipleWindows(true);
        // Lower pressure from content-heavy websites and inactive renderers.
        s.setOffscreenPreRaster(false);''',"webview settings")
once('        web.setDownloadListener((link,ua,disposition,mime,len)->download(link,ua,disposition,mime));',
'''        web.setDownloadListener((link,ua,disposition,mime,len)->download(link,ua,disposition,mime));
        // Complementary detection for players inside cross-origin iframes:
        // observe new media requests AFTER a real touch; never click or inspect frames.
        web.setOnTouchListener((view,motion)->{
            if(motion.getAction()!=android.view.MotionEvent.ACTION_UP ||
               !autoOpenPlayerOnPlay || selected!=tab)return false;
            final long touchedAt=System.currentTimeMillis();
            final int epoch=tab.scanEpoch;
            autoHandler.postDelayed(()->{
                if(selected!=tab || tab.web==null || tab.scanEpoch!=epoch ||
                   !tabs.contains(tab) || !autoOpenPlayerOnPlay)return;
                java.util.Map<String,Long> times=detectedTimes.get(tab);
                if(times==null)return;
                boolean fresh=false;
                for(Long recorded:times.values()){
                    if(recorded!=null && recorded>=touchedAt-300 &&
                       recorded<=System.currentTimeMillis()){fresh=true;break;}
                }
                if(fresh)onNativePlaybackRequested(tab,"");
            },3800);
        });''',"iframe touch and network fallback")
once('        web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND,true);',
'        web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND,false);',
"render process memory priority")
once('        web.setLayerType(View.LAYER_TYPE_HARDWARE,null);',
'        web.setLayerType(View.LAYER_TYPE_NONE,null);',
"avoid an additional WebView hardware layer")
once('            @Override public void onLoadResource(WebView w,String link){recordResource(tab,link,"RESOURCE");}',
'''            @Override public void onLoadResource(WebView w,String link){
                if(selected==tab)recordResource(tab,link,"RESOURCE");
            }''',"reduce background tab processing")
once('                tab.scanEpoch++;tab.iframeCount=0;tab.blobCount=0;tab.drmCount=0;',
'''                tab.scanEpoch++;
                detectedTimes.remove(tab);lastPlayPrompt.remove(tab);
                tab.iframeCount=0;tab.blobCount=0;tab.drmCount=0;''',"clear playback on navigation")
once('                String link=request.getUrl().toString();recordResource(tab,link,request.getMethod());return null;',
'''                if(selected==tab){
                    String link=request.getUrl().toString();
                    recordResource(tab,link,request.getMethod());
                }
                return null;''',"throttle background web requests")
once('        if(added && autoScanEnabled && autoSaveEnabled && !tab.privateMode && !io.isShutdown())',
'''        if(added)detectedTimes.computeIfAbsent(tab,k->new java.util.concurrent.ConcurrentHashMap<>())
            .put(item.normalizedKey(),System.currentTimeMillis());
        if(added && autoScanEnabled && autoSaveEnabled && !tab.privateMode && !io.isShutdown())''',"timestamp detected streams")
once('            }else if("drm".equals(kind)){',
'''            }else if("play".equals(kind)){
                String currentlyPlaying=event.optString("url","");
                runOnUiThread(()->onNativePlaybackRequested(tab,currentlyPlaying));
            }else if("drm".equals(kind)){''',"detect play event")
once('        long[] delays={1500,5000,14000};',
'        long[] delays={1700,5900};',"reduce delayed DOM captures")
once('            if(tab.web!=null && tabs.contains(tab) && tab.scanEpoch==epoch && autoScanEnabled){',
'            if(tab==selected && tab.web!=null && tabs.contains(tab) && tab.scanEpoch==epoch && autoScanEnabled){',
"avoid scans on background tabs")
once('        menuEntry(box,"◎","Estado do Auto Player Scanner",this::showAutoScanStatus,popup);',
'''        menuEntry(box,"◎","Estado do Auto Player Scanner",this::showAutoScanStatus,popup);
        autoToggle(box,"Abrir VistaCast ao iniciar um vídeo",autoOpenPlayerOnPlay,v->{
            autoOpenPlayerOnPlay=v;
            getSharedPreferences("harvester-settings",MODE_PRIVATE).edit()
                .putBoolean("auto_open_player_on_play",v).apply();
        });''',"auto open toggle")
once('        intent.putExtra("position_ms",Math.max(0L,resumeMs));',
'''        intent.putExtra("position_ms",Math.max(0L,resumeMs));
        // Prevent browser video and native player from running in parallel.
        if(selected!=null && selected.web!=null){
            try{selected.web.evaluateJavascript(
                "(function(){document.querySelectorAll('video').forEach(function(v){try{v.pause();}catch(e){}});})()",null);
            }catch(Exception ignored){}
        }''',"pause browser video before native player")
once('    @Override protected void onDestroy(){',
'''    @Override protected void onPause(){
        if(selected!=null && selected.web!=null)
            try{selected.web.onPause();}catch(RuntimeException ignored){}
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(selected!=null && selected.web!=null)
            try{selected.web.onResume();}catch(RuntimeException ignored){}
    }
    @Override protected void onDestroy(){''',"browser activity lifecycle")
once('        tab.web=null;tab.scanEpoch++;',
'''        tab.web=null;tab.scanEpoch++;
        detectedTimes.remove(tab);lastPlayPrompt.remove(tab);''',"clean state on closing")
anchor='    private void showAutoScanStatus(){'
if anchor not in src:raise RuntimeError("Unable to find scanner status entry")
methods='''    /** The browser is never displaced merely because an MP4 URL was found.
     * Playback requests originate from an actual video play event in the top document.
     * Sources from cross-origin frames may not be visible to the WebView API. */
    private void onNativePlaybackRequested(Tab tab,String reportedUrl){
        if(!autoOpenPlayerOnPlay||!autoScanEnabled||tab==null||
           selected!=tab||tab.web==null||!tabs.contains(tab))return;
        final int pageEpoch=tab.scanEpoch;
        long now=System.currentTimeMillis();
        Long previous=lastPlayPrompt.put(tab,now);
        if(previous!=null&&now-previous<9000)return;
        autoHandler.postDelayed(()->{
            if(isFinishing()||tab!=selected||!tabs.contains(tab)||
               tab.web==null||tab.scanEpoch!=pageEpoch)return;
            java.util.LinkedHashMap<String,MediaItem> options=new java.util.LinkedHashMap<>();
            String url=MediaScanner.normalize(reportedUrl,tab.url);
            if(url!=null&&MediaItem.hasVideoExtension(url)){
                MediaItem direct=new MediaItem(url,tab.url,tab.title,"","play-event");
                options.put(direct.normalizedKey(),direct);
                recordDetected(tab,direct);
            }
            java.util.Map<String,Long> timestamps=detectedTimes.get(tab);
            synchronized(tab.detected){
                for(MediaItem item:tab.detected.values()){
                    if(item==null||item.url==null||!item.url.startsWith("https://"))continue;
                    if(!MediaItem.hasVideoExtension(item.url))continue;
                    long seen=timestamps==null?0:timestamps.getOrDefault(item.normalizedKey(),0L);
                    // Keep the exact HTML5 source even when its request was earlier.
                    if(seen>=now-14000 ||
                       (url!=null&&item.normalizedKey().equals(url)))
                        options.put(item.normalizedKey(),item);
                }
                // A player can buffer its sole source before the click.
                if(options.isEmpty()&&tab.detected.size()==1){
                    MediaItem only=tab.detected.values().iterator().next();
                    if(MediaItem.hasVideoExtension(only.url))options.put(only.normalizedKey(),only);
                }
            }
            if(options.isEmpty()){
                toast("Reprodução detetada, mas a fonte do player não ficou acessível.");
                return;
            }
            java.util.ArrayList<MediaItem> found=new java.util.ArrayList<>(options.values());
            if(found.size()==1){
                openNativePlayer(found.get(0));
                return;
            }
            String[] labels=new String[found.size()];
            for(int i=0;i<found.size();i++){
                MediaItem item=found.get(i);
                labels[i]=item.format.toUpperCase(java.util.Locale.ROOT)+" • "+shortDomain(item.url);
            }
            new AlertDialog.Builder(this)
                .setTitle("Escolher vídeo para o VistaCast")
                .setItems(labels,(dialog,which)->openNativePlayer(found.get(which)))
                .setNegativeButton("Continuar no navegador",null)
                .show();
        },1700);
    }
'''
src=src.replace(anchor,methods+anchor,1)
browser.write_text(src)

# Native DOM listener. Never clicks buttons, changes autoplay, or inspects other origins.
script=js.read_text()
script+='''

;(() => {
  if(window.__harvesterRealPlayListener)return;
  window.__harvesterRealPlayListener=true;
  let lastGesture=0;
  for(const kind of ['pointerdown','touchstart','keydown']){
    document.addEventListener(kind,()=>{lastGesture=Date.now()},true);
  }
  document.addEventListener('play',(e)=>{
    try{
      const video=e.target;
      if(!video||String(video.tagName).toLowerCase()!=='video')return;
      if(Date.now()-lastGesture>8000)return;
      const sources=[video.currentSrc,video.src,
        ...Array.from(video.querySelectorAll('source')).map(x=>x.src)];
      const src=sources.find(x=>/^https:\\/\\//i.test(x))||'';
      console.log('__HBR_AUTO__'+JSON.stringify({type:'play',url:src}));
    }catch(_){}
  },true);
})();
'''
js.write_text(script)
g=gradle.read_text()
if "versionName '2.0.0'" not in g:raise RuntimeError("v2.0 base unavailable")
g=g.replace("versionName '2.0.0'","versionName '2.1.0'").replace("versionCode 11","versionCode 12")
gradle.write_text(g)
print("Browser auto-activation, source chooser and WebView memory safeguards integrated")
