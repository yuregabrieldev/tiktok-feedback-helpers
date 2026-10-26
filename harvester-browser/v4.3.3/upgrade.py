from pathlib import Path

src=Path("harvester-browser/v4.3.3/VistaPremiumInspectorActivity.java")
target=Path("hb-v18/app/src/main/java/com/yure/videoharvester/VistaPremiumInspectorActivity.java")
assert src.exists() and target.exists()
text=src.read_text(encoding="utf-8")
assert "extends PlayerInspectorActivityV28" in text
assert "@Override void captureForVideos()" in text
assert "com.vistastream.tv.action.IMPORT" in text
assert "com.vistastream.tv.extra.CATALOG_LINKED" in text
assert "BrTitleParser.parse(rawTitle)" in text
assert "BrazilianMediaHints.infer" in text
assert "setResult(RESULT_OK,result)" in text
assert "getIntent().getStringExtra(\"mediaTitle\")" in text
assert "finishConfirmedImport(RESULT_OK,receipt)" in text
assert "mediaUrl\",transientSource?\"\":importUrl" in text
assert "class VistaPremiumInspectorActivity" in text
target.write_text(text,encoding="utf-8")

gradle=Path("hb-v18/app/build.gradle")
data=gradle.read_text()
assert "versionName '4.3.2'" in data
assert "versionCode 37" in data
gradle.write_text(data.replace("versionName '4.3.2'","versionName '4.3.3'")
                      .replace("versionCode 37","versionCode 38"))
manifest=Path("hb-v18/app/src/main/AndroidManifest.xml").read_text()
assert 'android:name="com.yure.videoharvester.VistaPremiumInspectorActivity"' in manifest
print("VistaBrowser v4.3.3: inspector capture now opens metadata preview and VistaStream's catalog matching editor; previous scanner export and diagnostic actions retained")
