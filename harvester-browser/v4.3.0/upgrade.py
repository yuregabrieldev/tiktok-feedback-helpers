from pathlib import Path
root=Path("hb-v18")
app=root/"app"
pre=app/"src/main/java/com/yure/videoharvester"
browser=app/"src/main/java/com/harvester/browser/MainActivity.java"
manifest=app/"src/main/AndroidManifest.xml"
gradle=app/"build.gradle"

premium=Path("harvester-browser/v4.3.0/VistaScraperPremiumActivity.java").read_text(encoding="utf-8")
premium=premium.replace('getSharedPreferences(PLAY_PREFS,0)','getSharedPreferences("vh25_playback_tests",0)')
(pre/"VistaScraperPremiumActivity.java").write_text(premium,encoding="utf-8")
(pre/"VistaPremiumInspectorActivity.java").write_text(
    Path("harvester-browser/v4.3.0/VistaPremiumInspectorActivity.java").read_text(encoding="utf-8"),
    encoding="utf-8")
s=browser.read_text(encoding="utf-8")
old="com.yure.videoharvester.VistaScraperActivity.class"
assert s.count(old)==1, "Crawler integration activity location changed"
s=s.replace(old,"com.yure.videoharvester.VistaScraperPremiumActivity.class",1)
browser.write_text(s,encoding="utf-8")
m=manifest.read_text(encoding="utf-8")
old='android:name="com.yure.videoharvester.VistaScraperActivity"'
assert m.count(old)==1,"Crawler manifest not found"
m=m.replace(old,'android:name="com.yure.videoharvester.VistaScraperPremiumActivity"',1)
old='''        <activity android:name="com.yure.videoharvester.PlayerInspectorActivityV28"
            android:exported="false" />'''
assert old in m,"original inspector registration missing"
m=m.replace(old,old+'''
        <activity android:name="com.yure.videoharvester.VistaPremiumInspectorActivity"
            android:exported="false" />''',1)
# All launch sites that used original inspector now route to themed subclass;
# retains each original inspector method, including the single-popup override.
changed=[]
for path in pre.glob("*.java"):
    if path.name.startswith("VistaPremiumInspector"):continue
    text=path.read_text(encoding="utf-8")
    needle="PlayerInspectorActivityV28.class"
    if needle in text:
        changed.append(path.name)
        path.write_text(text.replace(needle,"VistaPremiumInspectorActivity.class"),encoding="utf-8")
assert changed, "player inspector launch actions not patched"
manifest.write_text(m,encoding="utf-8")

# Some original scanner screens rely on old static final local tints; preserve
# their algorithm and the navigation implementation, fixing only old
# high-contrast light-mode indicators that were difficult to read.
for name in ("MainActivityV22","MainActivityV24","MainActivityV25"):
    p=pre/(name+".java")
    source=p.read_text(encoding="utf-8")
    if name=="MainActivityV22":
        assert "rounded(0xffF7F1F3,13)" in source
        source=source.replace("rounded(0xffF7F1F3,13)",
            "rounded(com.harvester.browser.ui.Styles.SURFACE,13)")
    if name=="MainActivityV25":
        source=source.replace("0xff8B4C60",
            "com.harvester.browser.ui.Styles.MUTED")
    p.write_text(source,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
assert "versionName '4.2.1'" in g and "versionCode 34" in g
g=g.replace("versionName '4.2.1'","versionName '4.3.0'").replace("versionCode 34","versionCode 35")
gradle.write_text(g,encoding="utf-8")
assert "startActivityForResult(intent,IMPORT_REQUEST)" in premium
assert "MediaStore.Video.Media.IS_PENDING" in premium
assert "getSharedPreferences(PLAY_PREFS" not in premium
assert m.count("VistaScraperPremiumActivity")==1
print("VistaBrowser 4.3: full crawler redesign shell, modern player inspector, source verification and VistaStream editor with optional MediaStore gallery save; inspector launch sites:",changed)
