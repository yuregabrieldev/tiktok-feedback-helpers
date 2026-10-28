#!/usr/bin/env python3
"""VistaBrowser TV adaptation: preserve phone layout, add TV launch and landscape."""
from pathlib import Path
import re
root=Path("hb-v18/app")
manifest=root/"src/main/AndroidManifest.xml"
m=manifest.read_text(encoding="utf-8")
# Android TV entry point for the existing browser rather than a second app.
# Launch a TV-only subclass in landscape; keep the phone's original activity unchanged.
assert '    </application>' in m
m=m.replace('    </application>', '''        <activity
            android:name="com.harvester.browser.VistaTvEntryActivity"
            android:screenOrientation="landscape"
            android:exported="true"
            android:label="VistaBrowser">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LEANBACK_LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>''',1)
features='''    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
'''
assert '<application' in m
m=m.replace('<application',features+'    <application',1)
manifest.write_text(m,encoding="utf-8")
# Java activity inherits the same premium browser; a TV-specific manifest entry
# controls landscape without rotating the phone edition.
tv=root/"src/main/java/com/harvester/browser/VistaTvEntryActivity.java"
tv.write_text('''package com.harvester.browser;

/** Reuses the complete VistaBrowser implementation in a landscape TV activity. */
public class VistaTvEntryActivity extends MainActivity { }
''',encoding="utf-8")
g=root/"build.gradle"
build=g.read_text(encoding="utf-8")
assert "versionName '4.3.5'" in build and "versionCode 40" in build
g.write_text(build.replace("versionName '4.3.5'","versionName '4.3.6'")
    .replace("versionCode 40","versionCode 41"),encoding="utf-8")
print("VistaBrowser 4.3.6 Android TV entry + widescreen mode; original scanner intact.")
