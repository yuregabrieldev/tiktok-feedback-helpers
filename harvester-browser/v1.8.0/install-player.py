from pathlib import Path
p=Path("app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text()
def one(old,new,label):
    global s
    if s.count(old)!=1: raise RuntimeError(label+": expected 1 match got "+str(s.count(old)))
    s=s.replace(old,new,1)

one('    private void mediaDetails(MediaItem item,boolean saved){',
'''    private void openNativePlayer(MediaItem item){
        if(item==null||item.url==null||!item.url.startsWith("https://")){
            toast("Este vídeo não tem URL HTTPS válido");return;
        }
        Intent intent=new Intent(this,NativePlayerActivity.class);
        intent.putExtra("url",item.url);
        intent.putExtra("title",item.title);
        intent.putExtra("format",item.format);
        startActivity(intent);
    }
    private void setPlayIconAction(LinearLayout row,MediaItem item){
        if(row==null||row.getChildCount()==0)return;
        View icon=row.getChildAt(0);
        icon.setContentDescription("Reproduzir: "+item.title);
        icon.setOnClickListener(v->openNativePlayer(item));
    }
    private void mediaDetails(MediaItem item,boolean saved){''',"add playback helper")

one('            addCastQuickAction(resultRow,item);',
'''            setPlayIconAction(resultRow,item);
            addCastQuickAction(resultRow,item);''',"detected play button")
one('            addCastQuickAction(savedRow,item);',
'''            setPlayIconAction(savedRow,item);
            addCastQuickAction(savedRow,item);''',"library play button")
one('        panelHeading(page.body,item.title);',
'''        addPanelAction(page,"▶","Reproduzir neste dispositivo",()->openNativePlayer(item));
        panelHeading(page.body,item.title);
        panelLine(page.body,"▶","Reproduzir no telemóvel",
            "Player nativo com pausa, avanço e Cast",()->openNativePlayer(item),null);''',"video details playback")
one('        options.add("Testar fonte e obter metadados");',
'''        options.add("Testar fonte e obter metadados");
        options.add("Reproduzir no telemóvel");''',"media actions item")
one('                    case "Testar fonte e obter metadados":testMedia(item,saved);break;',
'''                    case "Testar fonte e obter metadados":testMedia(item,saved);break;
                    case "Reproduzir no telemóvel":openNativePlayer(item);break;''',"media actions switch")
p.write_text(s)

m=Path("app/src/main/AndroidManifest.xml");x=m.read_text()
needle="    </application>"
if x.count(needle)!=1:raise RuntimeError("manifest marker")
x=x.replace(needle,'        <activity android:name=".NativePlayerActivity" android:exported="false" />\n'+needle,1)
m.write_text(x)

dest=Path("app/src/main/java/com/harvester/browser/NativePlayerActivity.java")
dest.write_text(Path("../harvester-browser/v1.8.0/NativePlayerActivity.java").read_text())

g=Path("app/build.gradle");x=g.read_text()
if "versionName '1.7.0'" not in x:raise RuntimeError("expected v1.7")
x=x.replace("versionName '1.7.0'","versionName '1.8.0'").replace("versionCode 8","versionCode 9")
x+='''

// Media3 native player for MP4 / WebM / HLS / DASH streams
dependencies {
    def media3 = "1.11.1"
    implementation "androidx.media3:media3-exoplayer:$media3"
    implementation "androidx.media3:media3-exoplayer-hls:$media3"
    implementation "androidx.media3:media3-exoplayer-dash:$media3"
    implementation "androidx.media3:media3-ui:$media3"
}
'''
g.write_text(x)
print("Native playback integrated into history-safe browser panels")
