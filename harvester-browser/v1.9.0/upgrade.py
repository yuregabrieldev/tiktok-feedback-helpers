from pathlib import Path
p=Path("app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text()

def one(old,new,label):
    global s
    n=s.count(old)
    if n!=1: raise RuntimeError(f"{label}: expected one, got {n}")
    s=s.replace(old,new,1)

# SDK initialization and old modal Cast paths were coupled to the browsing activity.
# Keep Cast entirely inside the dedicated native Player activity.
one("        initCast();setupUi();newTab(false,HOME);",
    "        setupUi();newTab(false,HOME);",
    "do not initialize Cast on every browser launch")

one('        menuEntry(box,"▣","Google Cast — dispositivos e controlo",this::showCastControls,popup);',
'''        menuEntry(box,"▣","Reprodutor e Google Cast",this::openPlayerCastHub,popup);
        menuEntry(box,"▷","Testar player nativo",this::openDemoPlayer,popup);''',
    "Cast menu")

# Route all old Cast entry points to the safe player Activity, not the old selector.
one("    private void requestCastAt(MediaItem item,long resumeMs) {",
'''    private void requestCastAt(MediaItem item,long resumeMs) {
        if(item!=null) {
            openNativePlayer(item,resumeMs);
            return;
        }''',"Cast uses native player")

one('    private void openNativePlayer(MediaItem item){',
'''    private void openNativePlayer(MediaItem item){openNativePlayer(item,0L);}
    private void openNativePlayer(MediaItem item,long resumeMs){''',"player overload")

one('        intent.putExtra("format",item.format);',
'''        intent.putExtra("format",item.format);
        intent.putExtra("position_ms",Math.max(0L,resumeMs));''',"send playback position")

needle='    private void openNativePlayer(MediaItem item){openNativePlayer(item,0L);}'
if needle not in s:raise RuntimeError("new player insertion point missing")
helpers='''    private void openDemoPlayer(){
        Intent intent=new Intent(this,NativePlayerActivity.class);
        intent.putExtra("url",NativePlayerActivity.TEST_VIDEO);
        intent.putExtra("title","Big Buck Bunny — teste público");
        intent.putExtra("format","mp4");
        startActivity(intent);
    }
    private void openPlayerCastHub(){
        ArrayList<MediaItem> eligible=new ArrayList<>();
        if(selected!=null) synchronized(selected.detected){
            for(MediaItem item:selected.detected.values())
                if(CastSourcePolicy.mayOfferCast(item))eligible.add(item);
        }
        if(eligible.isEmpty()) {
            new AlertDialog.Builder(this)
               .setTitle("Reprodutor e Google Cast")
               .setMessage("Não encontrei uma fonte direta compatível nesta aba. "
                   +"Podes testar o player e a ligação à TV com um vídeo público. "
                   +"O Google Cast oficial não usa emparelhamento por PIN personalizado.")
               .setPositiveButton("Testar player", (dialog,which)->openDemoPlayer())
               .setNegativeButton("Cancelar",null).show();
            return;
        }
        if(eligible.size()==1){openNativePlayer(eligible.get(0));return;}
        String[] labels=new String[eligible.size()+1];
        for(int i=0;i<eligible.size();i++){
            MediaItem item=eligible.get(i);
            labels[i]=item.format.toUpperCase(java.util.Locale.ROOT)+" • "+shortDomain(item.url);
        }
        labels[eligible.size()]="Vídeo de demonstração público";
        new AlertDialog.Builder(this).setTitle("Escolher vídeo para o player")
            .setItems(labels,(dialog,index)->{
                if(index==eligible.size())openDemoPlayer();
                else openNativePlayer(eligible.get(index));
            }).setNegativeButton("Cancelar",null).show();
    }
'''
s=s.replace(needle,helpers+needle,1)

# The whole card now opens the native player. Details remain available on long press.
one('            addCastQuickAction(resultRow,item);',
'''            resultRow.setOnClickListener(v->openNativePlayer(item));
            resultRow.setOnLongClickListener(v->{mediaDetails(item,false);return true;});
            addCastQuickAction(resultRow,item);''',
    "detected row opens player")
one('            addCastQuickAction(savedRow,item);',
'''            savedRow.setOnClickListener(v->openNativePlayer(item));
            savedRow.setOnLongClickListener(v->{mediaDetails(item,true);return true;});
            addCastQuickAction(savedRow,item);''',
    "saved row opens player")

# Explicit prominent Player affordance in the details screen.
one('        addPanelAction(page,"▶","Reproduzir neste dispositivo",()->openNativePlayer(item));',
'''        addPanelAction(page,"▶","Abrir player nativo",()->openNativePlayer(item));
        addPanelAction(page,"▣","Ver detalhes do Cast no player",()->openNativePlayer(item));''',
    "player detail actions")

# A cast quick action always opens the player with the selected URL.
one('        button.setOnClickListener(v->requestCast(item));',
    '        button.setOnClickListener(v->openNativePlayer(item));',
    "per-media Cast player shortcut")

p.write_text(s)
p=Path("app/src/main/java/com/harvester/browser/NativePlayerActivity.java")
source=Path("../harvester-browser/v1.9.0/NativePlayerActivity.java")
if not source.exists():raise RuntimeError("New native player missing")
p.write_text(source.read_text())
manifest=Path("app/src/main/AndroidManifest.xml")
xml=manifest.read_text()
if 'android:name=".NativePlayerActivity"' not in xml:raise RuntimeError("NativePlayerActivity not in manifest")
manifest.write_text(xml)

gradle=Path("app/build.gradle")
g=gradle.read_text()
if "versionName '1.8.0'" not in g:raise RuntimeError("unexpected base version")
gradle.write_text(g.replace("versionName '1.8.0'","versionName '1.9.0'").replace("versionCode 9","versionCode 10"))
print("v1.9 native Player and isolated Cast integrated successfully")
