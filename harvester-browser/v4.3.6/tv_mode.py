#!/usr/bin/env python3
"""VistaBrowser TV adaptation: preserve phone layout, add TV launch and landscape."""
from pathlib import Path
import re
root=Path("hb-v18/app")
manifest=root/"src/main/AndroidManifest.xml"
m=manifest.read_text(encoding="utf-8")
# Android TV entry point for the existing browser rather than a second app.
launcher='<category android:name="android.intent.category.LAUNCHER" />'
assert launcher in m, "Cannot find VistaBrowser launcher entry"
m=m.replace(launcher,launcher+'\n                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />',1)
features='''    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
'''
assert '<application' in m
m=m.replace('<application',features+'    <application',1)
manifest.write_text(m,encoding="utf-8")
main=root/"src/main/java/com/harvester/browser/MainActivity.java"
src=main.read_text(encoding="utf-8")
needle='super.onCreate(savedInstanceState);'
assert needle in src, "VistaBrowser MainActivity onCreate changed"
src=src.replace(needle,needle+'''
        // Use the television's wide-screen layout while retaining the phone portrait UI.
        if ((getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
                android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
''',1)
main.write_text(src,encoding="utf-8")
g=root/"build.gradle"
build=g.read_text(encoding="utf-8")
assert "versionName '4.3.5'" in build and "versionCode 40" in build
g.write_text(build.replace("versionName '4.3.5'","versionName '4.3.6'")
    .replace("versionCode 40","versionCode 41"),encoding="utf-8")
print("VistaBrowser 4.3.6 Android TV entry + widescreen mode; original scanner intact.")
