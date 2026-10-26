from pathlib import Path

source=Path("hb-v18/app/src/main/java/com/yure/videoharvester")
inspector=source/"VistaPremiumInspectorActivity.java"
data=inspector.read_text(encoding="utf-8")

old="        final BrTitleParser.Parsed parsed=BrTitleParser.parse(rawTitle);"
new="""        final VistaMetadataHints.Hint pageHint=VistaMetadataHints.inspect(rawTitle,page);
        // Keep series episode markers parsed from the original filename.
        final BrTitleParser.Parsed original=BrTitleParser.parse(rawTitle);
        final BrTitleParser.Parsed parsed=BrTitleParser.parse(
            original.season>0&&original.episode>0?rawTitle:pageHint.title);"""
assert data.count(old)==1
data=data.replace(old,new,1)
old='            "\\nFormato: "+format+'
new='            (pageHint.year>0?"\\nAno: "+pageHint.year:"")+\n            "\\nFormato: "+format+'
assert data.count(old)==1
data=data.replace(old,new,1)
old='        request.putExtra("com.vistastream.tv.extra.TITLE",parsed.title);'
new='''        request.putExtra("com.vistastream.tv.extra.TITLE",parsed.title);
        final VistaMetadataHints.Hint extraHints=VistaMetadataHints.inspect(title,page);
        if(extraHints.year>0)
            request.putExtra("com.vistastream.tv.extra.YEAR",extraHints.year);'''
assert data.count(old)==1
data=data.replace(old,new,1)
inspector.write_text(data,encoding="utf-8")
(source/"VistaMetadataHints.java").write_text(
    Path("harvester-browser/v4.3.4/VistaMetadataHints.java").read_text(encoding="utf-8"),
    encoding="utf-8")
gradle=Path("hb-v18/app/build.gradle")
build=gradle.read_text(encoding="utf-8")
assert "versionName '4.3.3'" in build and "versionCode 38" in build
gradle.write_text(build.replace("versionName '4.3.3'","versionName '4.3.4'")
    .replace("versionCode 38","versionCode 39"),encoding="utf-8")
print("PASS: v4.3.4 source page slug, year, original episode markers and VistaStream intent")
