from pathlib import Path

root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
s=main.read_text(encoding="utf-8")
start=s.index('        // Saved sources should not contain signed/short-lived credentials.')
end=s.index('        String action=editBeforeSaving?', start)
s=s[:start]+'''        // Do not confuse bookmark import with offline downloading:
        // signed MP4 and CDN links can play NOW even if they later expire.
        // Vista Stream's import editor classifies these as Web bookmarks,
        // saves the clean original page URL (not the signed token) and then
        // reports confirmation. Playback still needs a valid direct stream.
''' + s[end:]
# An explicit '+' click can keep the discovery in Harvester even if VS is not
# currently installed. Never silently auto-save a detected candidate.
needle='''        if(editBeforeSaving){
            new AlertDialog.Builder(this)'''
assert s.count(needle)==1, 'expected missing VS fallback once'
s=s.replace(needle,'''        if(editBeforeSaving){
            // The user explicitly chose +. Save the discovery locally even
            // when the VS metadata editor is unavailable on this device.
            boolean stored=store.add(item);
            if(stored)toast("Guardado no Harvester. Para sincronizar com VS, instala o Video Stream.");
            new AlertDialog.Builder(this)''',1)
main.write_text(s,encoding="utf-8")

gradle=root/"app/build.gradle"
g=gradle.read_text()
assert "versionName '2.8.0'" in g, 'unexpected release version'
g=g.replace("versionName '2.8.0'","versionName '2.9.0'").replace("versionCode 19","versionCode 20")
gradle.write_text(g,encoding="utf-8")
print("Harvester Browser v2.9: save confirmed source references without rejecting temporary streams")
