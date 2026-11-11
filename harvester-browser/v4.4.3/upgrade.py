#!/usr/bin/env python3
"""VistaBrowser v4.4.3: context menu on media/iframe long press, preserving tabs."""
from pathlib import Path
import re
import shutil

app=Path("hb-v18/app")
main=app/"src/main/java/com/harvester/browser/MainActivity.java"
helper=app/"src/main/java/com/harvester/browser/VistaMediaContextMenu.java"
gradle=app/"build.gradle"
source=main.read_text(encoding="utf-8")

# The production browser uses a WebView per tab. Install on newly-created
# WebViews rather than the global root, so new tabs and private tabs work too.
pattern=r"\bnew\s+WebView\s*\(\s*(?:this|MainActivity\.this)\s*\)"
wrapped=("VistaMediaContextMenu.attach(MainActivity.this, new WebView(MainActivity.this), "
         "mediaTarget -> newTab(selected!=null && selected.privateMode,mediaTarget))")
updated,count=re.subn(pattern,lambda match: wrapped,source)
assert 1<=count<=6, (
    f"Expected at least one tab WebView constructor, got {count}. "
    "Do not silently publish a version without the long-press hook."
)
assert "VistaMediaContextMenu.attach" in updated
main.write_text(updated,encoding="utf-8")
shutil.copy2("harvester-browser/v4.4.3/VistaMediaContextMenu.java",helper)

build=gradle.read_text(encoding="utf-8")
assert "versionName '4.4.2'" in build and "versionCode 47" in build, (
    "Unexpected base version; refusing a non-monotonic update"
)
build=build.replace("versionName '4.4.2'","versionName '4.4.3'",1)
build=build.replace("versionCode 47","versionCode 48",1)
gradle.write_text(build,encoding="utf-8")

# Fail at build time if the interaction is not wired or the previous
# Smart Crawler queue, source store, and imported VS bridge disappear.
assert (app/"src/main/java/com/yure/videoharvester/VistaSourceCollector.java").exists()
assert "toggleCrawler442" in (app/
    "src/main/java/com/yure/videoharvester/VistaScraperPremiumActivity.java").read_text()
assert "sendToVistaStream" in updated
print(f"PASS VistaBrowser 4.4.3: media menu installed on {count} WebView creation site(s)")
