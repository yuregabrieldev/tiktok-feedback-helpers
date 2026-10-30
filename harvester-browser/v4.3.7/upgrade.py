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


# Resume a completed DownloadManager APK transfer after process death or
# after the user has approved "install unknown apps". Never lose its ID
# before Android successfully opens the installer.
updater = root / "src/main/java/com/harvester/browser/VistaBrowserUpdates.java"
updates = updater.read_text(encoding="utf-8")
old = '        if(!manual && System.currentTimeMillis()-prefs.getLong("checked_at",0)<INTERVAL)return;'
assert updates.count(old)==1, "VistaBrowser updater throttle anchor changed"
new = """        if(!manual){
            long id=prefs.getLong("pending_id",-1L);
            if(id>0){
                DownloadManager mgr=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
                if(mgr!=null){
                    try(Cursor cursor=mgr.query(new DownloadManager.Query().setFilterById(id))){
                        if(cursor!=null && cursor.moveToFirst()){
                            int state=cursor.getInt(cursor.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_STATUS));
                            if(state==DownloadManager.STATUS_SUCCESSFUL){
                                validateAndInstall(activity,mgr,id,
                                    prefs.getString("pending_digest",""));
                                return;
                            }
                            if(state==DownloadManager.STATUS_PENDING ||
                                state==DownloadManager.STATUS_RUNNING ||
                                state==DownloadManager.STATUS_PAUSED)return;
                        }
                    }catch(Exception ignored){}
                }
                prefs.edit().remove("pending_id").remove("pending_digest").apply();
            }
            if(System.currentTimeMillis()-prefs.getLong("checked_at",0)<INTERVAL)return;
        }"""
updates = updates.replace(old,new,1)
old='''        activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .remove("pending_id").remove("pending_digest").apply();
        if(Build.VERSION.SDK_INT>=26'''
assert updates.count(old)==1, "VistaBrowser installer permission anchor changed"
updates = updates.replace(old,'''        if(Build.VERSION.SDK_INT>=26''',1)
anchor='''            activity.startActivity(installer);'''
assert updates.count(anchor)==1, "VistaBrowser installer action anchor changed"
updates=updates.replace(anchor,anchor+'''
            activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .remove("pending_id").remove("pending_digest").apply();''',1)
updater.write_text(updates,encoding="utf-8")

build = gradle.read_text(encoding="utf-8")
assert "versionName '4.3.6'" in build and "versionCode 41" in build
gradle.write_text(build.replace("versionName '4.3.6'","versionName '4.3.7'")
    .replace("versionCode 41","versionCode 42"), encoding="utf-8")
print("VistaBrowser 4.3.7: automatic opt-in inspector discovery, local review and manifest export.")
