from pathlib import Path
premium=Path("hb-v18/app/src/main/java/com/yure/videoharvester/VistaScraperPremiumActivity.java")
assert premium.exists()
premium.write_text(Path("harvester-browser/v4.3.0/VistaScraperPremiumActivity.java").read_text().replace('getSharedPreferences(PLAY_PREFS,0)','getSharedPreferences("vh25_playback_tests",0)'))
# V4.3 build workflow copies the premium class during its upgrade. We update
# that class with a patch to avoid changing compiled engine internals.
# Changes are already authored into v4.3.0/VistaScraperPremiumActivity.java on
# this repair branch, so overwrite the compiled copy after the 4.3 step.
from subprocess import run
run(["python3","harvester-browser/v4.3.1/fix-browser-vista-handshake.py"],check=True)
gradle=Path("hb-v18/app/build.gradle")
g=gradle.read_text()
assert "versionName '4.3.0'" in g and "versionCode 35" in g
gradle.write_text(g.replace("versionName '4.3.0'","versionName '4.3.1'")
                   .replace("versionCode 35","versionCode 36"))
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text()
assert "CONFIRMATION_RECEIVER" in s
assert "VistaScraperPremiumActivity.class" in s
assert "SAVED_WEB_ONLY" in s
x=premium.read_text()
assert "toolbar21.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(84)))" in x
assert "titleGroup.addView(screenTitle,new LinearLayout.LayoutParams(-1,dp(39)))" in x
assert "playSourceInVista(Catalog item)" in x
assert "CONFIRMATION_RECEIVER" in x
print("VistaBrowser 4.3.1: fixed scanner gap, transient VS playback/Cast, robust success receipts, web-only distinction.")
