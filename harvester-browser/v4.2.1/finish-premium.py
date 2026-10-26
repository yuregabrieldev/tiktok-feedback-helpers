from pathlib import Path
root=Path("hb-v18/app/src/main/java/com/yure/videoharvester")
styles="com.harvester.browser.ui.Styles"
maps={
 "0xff421323":f"{styles}.BG",
 "0xff651F36":f"{styles}.ink({styles}.ACCENT)",
 "0xffF4E9ED":f"{styles}.SURFACE",
 "0xffF6E9ED":f"{styles}.TEXT",
 "0xff70606A":f"{styles}.MUTED",
 "0xff805764":f"{styles}.MUTED",
 "0xffE9CCD6":f"{styles}.SURFACE",
 "0xffECD3DB":f"{styles}.MUTED",
 "0xff786E78":f"{styles}.MUTED"
}
targets=["PlayerInspectorActivity","PlayerInspectorActivityV26",
         "PlayerInspectorActivityV27","PlayerInspectorActivityV28"]
for name in targets:
 p=root/(name+".java")
 s=p.read_text()
 for old,new in maps.items():s=s.replace(old,new)
 if name=="PlayerInspectorActivity":
  old="title.setTextColor(Color.WHITE);"
  assert old in s
  s=s.replace(old,f"title.setTextColor({styles}.TEXT);",1)
  # The embedded player UI is not the external website: palette follows VB.
  s=s.replace("browser.setBackgroundColor(Color.WHITE);",f"browser.setBackgroundColor({styles}.SURFACE);")
 p.write_text(s)

p=root/"BrCatalogActivity.java"
s=p.read_text()
old="private final int BG=0xff101319, CARD=0xff222733, TEXT=0xffFFFFFF, MUTED=0xffBFC4CE, ACCENT=0xff7255D8;"
new=(f"private final int BG={styles}.BG, CARD={styles}.SURFACE, "
     f"TEXT={styles}.TEXT, MUTED={styles}.MUTED, ACCENT={styles}.ink({styles}.ACCENT);")
assert old in s
s=s.replace(old,new,1)
s=s.replace("b.setTextColor(Color.WHITE);",f"b.setTextColor({styles}.LIGHT?Color.WHITE:{styles}.BG);",1)
p.write_text(s)

p=root/"PlaybackTestActivity.java"
s=p.read_text()
old="static final int WINE=0xff421323, ACCENT=0xff651F36, LIGHT=0xffF4E9ED;"
new=f"static final int WINE={styles}.BG, ACCENT={styles}.ink({styles}.ACCENT), LIGHT={styles}.SURFACE;"
assert old in s
s=s.replace(old,new)
s=s.replace("b.setTextColor(strong?Color.WHITE:ACCENT);",
            f"b.setTextColor(strong?({styles}.LIGHT?Color.WHITE:{styles}.BG):ACCENT);")
s=s.replace("TextView heading=label(\"Teste de reprodução\",22,Color.WHITE);",
            f"TextView heading=label(\"Teste de reprodução\",22,{styles}.TEXT);")
s=s.replace("card.setBackground(round(Color.WHITE));",
            f"card.setBackground(round({styles}.SURFACE));")
s=s.replace("0xffECD3DB",f"{styles}.MUTED").replace("0xff786E78",f"{styles}.MUTED")
p.write_text(s)

# Keep JS/CSS inspection WebView functionality. Only change container palette.
assert (root/"MainActivityV25.java").exists()
assert (root/"BrCatalogEngine.java").exists()
assert (root/"VistaScraperActivity.java").exists()
g=Path("hb-v18/app/build.gradle")
t=g.read_text()
assert "versionName '4.2.0'" in t and "versionCode 33" in t
g.write_text(t.replace("versionName '4.2.0'","versionName '4.2.1'")
             .replace("versionCode 33","versionCode 34"))
print("VistaBrowser v4.2.1: Inspector, BR catalog and playback-test UIs now inherit the full premium light/dark/accent palette")
