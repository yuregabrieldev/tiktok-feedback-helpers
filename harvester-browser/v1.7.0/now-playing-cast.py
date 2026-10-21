from pathlib import Path

p=Path('app/src/main/java/com/harvester/browser/MainActivity.java')
s=p.read_text()

def one(old,new,label):
    global s
    k=s.count(old)
    if k!=1: raise RuntimeError(f'{label}: expected exactly one occurrence, got {k}')
    s=s.replace(old,new,1)

one('    private MediaItem castSelection;', '''    private MediaItem castSelection;
    private long castResumePositionMs=0L;''', 'Cast position field')

one('        menuEntry(box,"▣","Google Cast — dispositivos e controlo",this::showCastControls,popup);', '''        menuEntry(box,"▣","Google Cast — dispositivos e controlo",this::showCastControls,popup);
        menuEntry(box,"▶","Transmitir vídeo atual para TV",this::castCurrentPlayback,popup);''', 'Cast current playback menu action')

one('        panelHeading(page.body,"Google Cast");', '''        panelHeading(page.body,"Google Cast");
        panelLine(page.body,"▶","Transmitir vídeo em reprodução", "Detetar a fonte ativa nesta aba e continuar na televisão", this::castCurrentPlayback,null);''', 'Cast active playback control')

one('        if(castContext==null) {', '''        if(android.os.Build.VERSION.SDK_INT>=33 &&
           checkSelfPermission("android.permission.NEARBY_WIFI_DEVICES")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            panelLine(page.body,"◎","Permitir dispositivos próximos",
                "Necessário para procurar televisões em algumas versões Android",
                ()->requestPermissions(new String[]{"android.permission.NEARBY_WIFI_DEVICES"},719),null);
        }
        if(castContext==null) {''', 'Nearby permissions messaging')

one('    private void requestCast(MediaItem item) {', '''    private void requestCast(MediaItem item) { requestCastAt(item,0L); }
    private void requestCastAt(MediaItem item,long resumeMs) {
        castResumePositionMs=Math.max(0L,resumeMs);''', 'Resume Cast position')

one('            remote.load(new MediaLoadRequestData.Builder().setMediaInfo(info).setAutoplay(true).build());', '''            MediaLoadRequestData.Builder load=new MediaLoadRequestData.Builder()
                .setMediaInfo(info).setAutoplay(true);
            if(castResumePositionMs>0L) load.setCurrentTime(castResumePositionMs);
            remote.load(load.build());
            castResumePositionMs=0L;''', 'Send playback position')

start='    private void requestCast(MediaItem item) { requestCastAt(item,0L); }'
if start not in s: raise RuntimeError('Cast insertion point missing')
helper='''    /** Prefer the actively playing HTML5 element; never bypass cross-origin frames or DRM. */
    private void castCurrentPlayback() {
        Tab tab=selected;
        if(tab==null || tab.web==null){toast("Abre primeiro uma página com vídeo");return;}
        final String javascript="(function(){try{"
            +"var found=[];var walk=function(w,n){if(n>2)return;"
            +"try{var vs=w.document.querySelectorAll('video');for(var i=0;i<vs.length;i++){"
            +"var v=vs[i];var u=v.currentSrc||v.src||'';"
            +"if(!u){var el=v.querySelector('source[src]');if(el)u=el.src||'';}"
            +"found.push({src:u,playing:!v.paused&&!v.ended,"
            +"time:isFinite(v.currentTime)?v.currentTime:0,ready:v.readyState});}"
            +"var fs=w.document.querySelectorAll('iframe');"
            +"for(var j=0;j<fs.length;j++){try{walk(fs[j].contentWindow,n+1);}catch(x){}}"
            +"}catch(x){}};walk(window,0);"
            +"found.sort(function(a,b){return (+b.playing)-(+a.playing)||b.ready-a.ready});"
            +"var x=found[0]||{};return JSON.stringify({src:x.src||'',"
            +"position:x.playing?Math.round((x.time||0)*1000):0,"
            +"playing:!!x.playing,videoCount:found.length});"
            +"}catch(e){return JSON.stringify({error:String(e)})}})()";
        try{
            tab.web.evaluateJavascript(javascript,encoded->{
                if(tab!=selected || tab.web==null)return;
                String url="";long ms=0L;
                try{
                    Object value=new org.json.JSONTokener(encoded).nextValue();
                    JSONObject info=new JSONObject(String.valueOf(value));
                    url=info.optString("src","");
                    ms=Math.max(0L,Math.min(12L*60*60*1000,info.optLong("position",0L)));
                }catch(Exception ignored){}
                MediaItem match=null;
                synchronized(tab.detected){
                    for(MediaItem item:tab.detected.values()){
                        if(item.url.equals(url) && CastSourcePolicy.mayOfferCast(item)){
                            match=item;break;
                        }
                    }
                }
                if(match!=null){requestCastAt(match,ms);return;}
                // When currentSrc points directly to an ordinary, accessible media URL.
                if(url.startsWith("https://") && MediaItem.hasVideoExtension(url)){
                    MediaItem active=new MediaItem(url,tab.url,tab.title,"","active-video");
                    if(CastSourcePolicy.mayOfferCast(active)){
                        requestCastAt(active,ms);return;
                    }
                }
                // blob:, restricted iframe or no active <video>: offer observed candidates.
                ArrayList<MediaItem> choices=new ArrayList<>();
                synchronized(tab.detected){for(MediaItem item:tab.detected.values())
                    if(CastSourcePolicy.mayOfferCast(item))choices.add(item);}
                if(choices.isEmpty()){
                    new AlertDialog.Builder(this).setTitle("Vídeo atual não identificado")
                      .setMessage("Não encontrei uma fonte HTTPS compatível na aba atual. "
                          +"Alguns players utilizam iframes externos, URLs blob:, sessões temporárias "
                          +"ou DRM. Inicia o vídeo normalmente e consulta Vídeos encontrados. "
                          +"A ligação à TV pode funcionar, mas não garante reprodução dessa fonte.")
                      .setPositiveButton("Dispositivos Cast",(d,w)->showCastControls())
                      .setNegativeButton("Fechar",null).show();
                    return;
                }
                if(choices.size()==1){requestCastAt(choices.get(0),0L);return;}
                String[] names=new String[choices.size()];
                for(int i=0;i<names.length;i++){
                    MediaItem item=choices.get(i);
                    names[i]=item.format.toUpperCase(Locale.ROOT)+" • "+shortDomain(item.url)
                        +" • "+item.discoveredFrom;
                }
                new AlertDialog.Builder(this).setTitle("Escolher fonte para a TV")
                    .setMessage("Há várias fontes observadas. Seleciona a correspondente ao vídeo em reprodução.")
                    .setItems(names,(dialog,which)->requestCastAt(choices.get(which),0L))
                    .setNegativeButton("Cancelar",null).show();
            });
        }catch(Exception e){toast("Não foi possível analisar a reprodução: "+e.getClass().getSimpleName());}
    }
'''
s=s.replace(start,helper+start,1)
p.write_text(s)

p=Path('app/build.gradle')
s=p.read_text()
if "versionName '1.6.0'" not in s:raise RuntimeError('Expected v1.6 base')
p.write_text(s.replace('versionCode 7','versionCode 8').replace("versionName '1.6.0'","versionName '1.7.0'"))
print('v1.7 active-playback Cast patch applied')

# Default receiver has a valid built-in media notification configuration.
options=Path('app/src/main/java/com/harvester/browser/HarvesterCastOptionsProvider.java')
source=options.read_text()
needle='            .setCastMediaOptions(new CastMediaOptions.Builder().setNotificationOptions(null).build())\n'
if source.count(needle)!=1: raise RuntimeError('Expected v1.5 CastMediaOptions customization')
options.write_text(source.replace(needle,''))
print('Cast receiver options simplified for stability')
