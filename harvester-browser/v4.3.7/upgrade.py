#!/usr/bin/env python3
"""VistaBrowser 4.3.7: opt-in HTML5 discovery queue + portable Vista Source."""
from pathlib import Path
import shutil

root = Path("hb-v18/app")
browser = root / "src/main/java/com/harvester/browser/MainActivity.java"
inspector = root / "src/main/java/com/yure/videoharvester/VistaPremiumInspectorActivity.java"
harvester = root / "src/main/java/com/yure/videoharvester"
manifest = root / "src/main/AndroidManifest.xml"
gradle = root / "build.gradle"

for file in ("VistaSourceCollector.java", "VistaSourceBrowserActivity.java"):
    shutil.copy2(Path("harvester-browser/v4.3.7") / file, harvester / file)

code = browser.read_text(encoding="utf-8")
anchor = '        menuHeading(box,"Preferências");'
assert code.count(anchor) == 1, "VistaBrowser preferences anchor changed"
code = code.replace(anchor, '''
        menuHeading(box,"Vista Source");
        profileWide(box,"▣","Crawler · Fonte e manifesto",
            this::openVistaSource,popup);

''' + anchor, 1)
marker = '    private void openVistaIdAccount(){'
assert code.count(marker) == 1, "Vista ID menu anchor changed"
code = code.replace(marker, '''    private void openVistaSource(){
        startActivity(new android.content.Intent(this,
            com.yure.videoharvester.VistaSourceBrowserActivity.class));
    }

''' + marker, 1)
browser.write_text(code, encoding="utf-8")

code = inspector.read_text(encoding="utf-8")
anchor = '    private boolean importingBusy;'
assert code.count(anchor) == 1, "Inspector field anchor changed"
code = code.replace(anchor, anchor + '''
    private final Handler vistaCrawlerHandler=new Handler(Looper.getMainLooper());
    private final Runnable vistaCrawlerTick=new Runnable(){
        @Override public void run(){
            if(isFinishing()||isDestroyed()||browser==null)return;
            if(getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE)
                .getBoolean("enabled",false))
                VistaSourceCollector.scan(VistaPremiumInspectorActivity.this,browser);
            vistaCrawlerHandler.postDelayed(this,4500);
        }
    };
''', 1)
anchor = '        getWindow().getDecorView().setSystemUiVisibility(current);'
assert code.count(anchor) == 1, "Inspector onCreate anchor changed"
code = code.replace(anchor, anchor +
    '\n        vistaCrawlerHandler.postDelayed(vistaCrawlerTick,1600);', 1)
anchor = '    private void styleContainer(ViewGroup root,int depth){'
assert code.count(anchor) == 1, "Inspector cleanup anchor changed"
code = code.replace(anchor, '''    @Override protected void onDestroy(){
        vistaCrawlerHandler.removeCallbacks(vistaCrawlerTick);
        super.onDestroy();
    }

''' + anchor, 1)
inspector.write_text(code, encoding="utf-8")

xml = manifest.read_text(encoding="utf-8")
anchor = '    </application>'
assert xml.count(anchor) == 1
xml = xml.replace(anchor, '''        <activity android:name="com.yure.videoharvester.VistaSourceBrowserActivity"
            android:exported="false" />
''' + anchor, 1)
manifest.write_text(xml, encoding="utf-8")

build = gradle.read_text(encoding="utf-8")
assert "versionName '4.3.6'" in build and "versionCode 41" in build
gradle.write_text(build.replace("versionName '4.3.6'","versionName '4.3.7'")
    .replace("versionCode 41","versionCode 42"), encoding="utf-8")
print("VistaBrowser 4.3.7: automatic opt-in inspector discovery, local review and manifest export.")
