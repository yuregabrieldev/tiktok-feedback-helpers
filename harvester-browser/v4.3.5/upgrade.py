#!/usr/bin/env python3
"""VistaBrowser 4.3.5: optional local-only Vista ID linking, no data migration."""
from pathlib import Path
import shutil

app=Path("hb-v18/app")
src=app/"src/main/java/com/harvester/browser"
main=src/"MainActivity.java"
manifest=app/"src/main/AndroidManifest.xml"
gradle=app/"build.gradle"

shutil.copy2("harvester-browser/v4.3.5/VistaIdAccountActivity.java",src/"VistaIdAccountActivity.java")

code=main.read_text(encoding="utf-8")
anchor='''        menuHeading(box,"Preferências");'''
assert code.count(anchor)==1, "VistaBrowser Preferences menu anchor changed"
code=code.replace(anchor, '''        menuHeading(box,"Conta Vista");
        profileWide(box,"◎","Vista ID · opcional",
            this::openVistaIdAccount,popup);

''' + anchor,1)
marker="    private void showAdvancedBrowserSettings(){"
assert code.count(marker)==1, "VistaBrowser advanced settings anchor changed"
code=code.replace(marker,'''    private void openVistaIdAccount(){
        startActivity(new android.content.Intent(this,VistaIdAccountActivity.class));
    }

'''+marker,1)
main.write_text(code,encoding="utf-8")

xml=manifest.read_text(encoding="utf-8")
anchor="    </application>"
assert xml.count(anchor)==1
xml=xml.replace(anchor,'''        <activity android:name="com.harvester.browser.VistaIdAccountActivity"
            android:exported="false" />

'''+anchor,1)
manifest.write_text(xml,encoding="utf-8")

build=gradle.read_text(encoding="utf-8")
assert "versionName '4.3.4'" in build and "versionCode 39" in build
gradle.write_text(build.replace("versionName '4.3.4'","versionName '4.3.5'")
    .replace("versionCode 39","versionCode 40"),encoding="utf-8")

assert "private void showDetected(" in code and "private boolean sendToVistaStream(" in code
assert "private void openVistaIdAccount()" in code
assert "private void showMenu()" in code
print("VistaBrowser 4.3.5 opt-in local Vista ID link installed; memory, library, history and sources untouched.")
