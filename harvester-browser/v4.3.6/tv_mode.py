#!/usr/bin/env python3
"""VistaBrowser TV adaptation: preserve phone layout, add TV launch and landscape."""
from pathlib import Path
import re
root=Path("hb-v18/app")
manifest=root/"src/main/AndroidManifest.xml"
m=manifest.read_text(encoding="utf-8")
# Android TV entry point for the existing browser rather than a second app.
# The existing browser's manifest does not use a conventional MAIN/LAUNCHER filter;
# add a separate TV alias without changing its phone entry.
assert '    </application>' in m, "Manifest application end changed"
m=m.replace('    </application>', '''        <activity-alias
            android:name="com.harvester.browser.VistaBrowserTvEntry"
            android:targetActivity="com.harvester.browser.MainActivity"
            android:exported="true"
            android:label="VistaBrowser">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LEANBACK_LAUNCHER"/>
            </intent-filter>
        </activity-alias>
    </application>''',1)
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
