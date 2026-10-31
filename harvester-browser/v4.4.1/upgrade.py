#!/usr/bin/env python3
"""VistaBrowser 4.4.1: signing migration version (requires durable signing secrets)."""
from pathlib import Path
gradle=Path("hb-v18/app/build.gradle")
s=gradle.read_text(encoding="utf-8")
assert "versionName '4.4.0'" in s and "versionCode 45" in s, "Unexpected previous version; refusing to produce a mismatched APK"
s=s.replace("versionName '4.4.0'","versionName '4.4.1'",1)
s=s.replace("versionCode 45","versionCode 46",1)
assert "VISTABROWSER_KEYSTORE_PATH" in s and "signingConfigs.vistaStable" in s, "Persistent signing config missing"
gradle.write_text(s,encoding="utf-8")
print("VistaBrowser 4.4.1: fixed-signature release candidate assembled")
