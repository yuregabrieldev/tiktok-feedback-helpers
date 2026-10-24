from pathlib import Path
path=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=path.read_text()
def one(before,after,why):
    global s
    n=s.count(before)
    if n!=1: raise RuntimeError(f"{why}: expected 1, found {n}")
    s=s.replace(before,after,1)

# Harvester remains a FULL independent browser. External app handles playback.
# Only fall back to the legacy VistaCast player when Vista is not installed.
one('''    private void openNativePlayer(MediaItem item,long resumeMs){
        if(item==null''',
'''    private void openNativePlayer(MediaItem item,long resumeMs){
        if(sendToVistaStream(item,false))return;
        if(item==null''',
"send detected playback to official Vista Stream")

# The existing details screen supports source inspection; add editable import.
one('''        addPanelAction(page,"▶","Abrir player nativo",()->openNativePlayer(item));''',
'''        addPanelAction(page,"▶","Reproduzir no Vista Stream",()->openNativePlayer(item));
        addPanelAction(page,"＋","Adicionar ao Vista Stream · editar metadados",
            ()->sendToVistaStream(item,true));''',
"metadata save command from media details")

one('''        options.add("Testar fonte e obter metadados");''',
'''        options.add("Testar fonte e obter metadados");
        options.add("Adicionar ao Vista Stream (filme / série)");''',
"more options import")
one('''                    case "Testar fonte e obter metadados":testMedia(item,saved);break;''',
'''                    case "Testar fonte e obter metadados":testMedia(item,saved);break;
                    case "Adicionar ao Vista Stream (filme / série)":
                        sendToVistaStream(item,true);break;''',
"more options action")

# Place bridge above native player so it is available for every playback path:
# autoscanner, found videos, saved videos and test sources.
anchor='''    private void openNativePlayer(MediaItem item){openNativePlayer(item,0L);}'''
if s.count(anchor)!=1:raise RuntimeError("Cannot find main player routing")
bridge=r'''    /**
     * Cross-application protocol:
     * - PLAY starts the Vista Stream official Media3 player.
     * - IMPORT opens Vista Stream's editable movie/series metadata dialog,
     *   and saves only after user confirmation.
     * Nothing is uploaded to a third-party backend and no cookies/passwords pass.
     */
    private boolean sendToVistaStream(MediaItem item,boolean editBeforeSaving) {
        if(item==null||item.url==null ||
            (!item.url.startsWith("https://")&&!item.url.startsWith("http://"))){
            toast("Esta fonte não possui um URL HTTP válido.");
            return false;
        }
        String action=editBeforeSaving?
            "com.vistastream.tv.action.IMPORT":"com.vistastream.tv.action.PLAY";
        String destination=editBeforeSaving?
            "com.ivor.openstream.harvester.HarvesterImportActivity":
            "com.ivor.openstream.harvester.HarvesterPlaybackActivity";
        String mime=null;
        if("hls".equalsIgnoreCase(item.format))mime="application/vnd.apple.mpegurl";
        else if("dash".equalsIgnoreCase(item.format))mime="application/dash+xml";
        else if("webm".equalsIgnoreCase(item.format))mime="video/webm";
        else if("mkv".equalsIgnoreCase(item.format))mime="video/x-matroska";
        else if("mp4".equalsIgnoreCase(item.format))mime="video/mp4";
        String page=(item.pageUrl!=null&&!item.pageUrl.isEmpty())?
            item.pageUrl:(selected==null?"":selected.url);
        String userAgent="";
        if(selected!=null&&selected.web!=null)
            try{userAgent=selected.web.getSettings().getUserAgentString();}
            catch(RuntimeException ignored){}
        // Try the debug APK first, then a non-debug release installation.
        for(String packageName:new String[]{"com.vistastream.tv.debug","com.vistastream.tv"}){
            Intent intent=new Intent(action);
            intent.setClassName(packageName,destination);
            intent.putExtra("com.vistastream.tv.extra.URL",item.url);
            intent.putExtra("com.vistastream.tv.extra.TITLE",item.title);
            intent.putExtra("com.vistastream.tv.extra.MIME_TYPE",mime);
            if(page!=null&&(page.startsWith("https://")||page.startsWith("http://"))){
                intent.putExtra("com.vistastream.tv.extra.PAGE_URL",page);
                intent.putExtra("com.vistastream.tv.extra.REFERER",page);
            }
            if(userAgent!=null&&!userAgent.isEmpty())
                intent.putExtra("com.vistastream.tv.extra.USER_AGENT",
                    userAgent.length()>350?userAgent.substring(0,350):userAgent);
            if(item.poster!=null && item.poster.startsWith("https://"))
                intent.putExtra("com.vistastream.tv.extra.POSTER",item.poster);
            intent.putExtra("com.vistastream.tv.extra.KIND","video");
            if(item.title!=null)intent.putExtra("com.vistastream.tv.extra.SERIES","");
            try{
                startActivity(intent);
                return true;
            }catch(android.content.ActivityNotFoundException ignored){
                // The receiver is missing for this installed package version.
            }catch(SecurityException ignored){
                // This build is not permitted to start the external Activity.
            }
        }
        if(editBeforeSaving){
            new AlertDialog.Builder(this)
                .setTitle("Vista Stream não disponível")
                .setMessage("Para adicionar filmes e séries com metadados, instala a versão atualizada do Vista Stream.")
                .setPositiveButton("Abrir página de versões",(dialog,which)->{
                    try{startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/yuregabriell/Vista-Stream-TV/releases")));}
                    catch(RuntimeException ignored){}
                }).setNegativeButton("Cancelar",null).show();
        }else {
            toast("Vista Stream não encontrado. A abrir o player local.");
        }
        return false;
    }
'''
s=s.replace(anchor,bridge+anchor,1)
path.write_text(s)

# Android 11 package visibility for safe package discovery and Android intents.
# Explicit setClassName can work without <queries>, but declare packages for
# future capability checks without granting new Android permissions.
manifest=Path("hb-v18/app/src/main/AndroidManifest.xml")
m=manifest.read_text()
if '<queries>' not in m:
    m=m.replace('<application', '''<queries>
        <package android:name="com.vistastream.tv.debug"/>
        <package android:name="com.vistastream.tv"/>
    </queries>
    <application''',1)
manifest.write_text(m)

gradle=Path("hb-v18/app/build.gradle")
g=gradle.read_text()
if "versionName '2.3.0'" not in g:raise RuntimeError("Expected Harvester Browser v2.3 base")
g=g.replace("versionName '2.3.0'","versionName '2.4.0'").replace("versionCode 14","versionCode 15")
gradle.write_text(g)
print("Harvester Browser v2.4 playback -> Vista Stream, metadata -> Vista editor; browser stays separate.")
