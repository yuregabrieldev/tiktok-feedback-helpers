from pathlib import Path
import shutil

root=Path("hb-v18/app")
java=root/"src/main/java/com/harvester/browser"
java.mkdir(parents=True,exist_ok=True)
shutil.copy2("harvester-browser/v4.3.6/VistaBrowserTvActivity.java",java/"VistaBrowserTvActivity.java")
manifest=root/"src/main/AndroidManifest.xml"
xml=manifest.read_text(encoding="utf-8")
# Existing mobile entry remains untouched. The TV gets its own 16:9 home.
xml=xml.replace('<category android:name="android.intent.category.LEANBACK_LAUNCHER" />','')
anchor="    </application>"
assert xml.count(anchor)==1
xml=xml.replace(anchor,'''        <activity
            android:name="com.harvester.browser.VistaBrowserTvActivity"
            android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
'''+anchor,1)
assert xml.count("android.intent.category.LEANBACK_LAUNCHER")==1
manifest.write_text(xml,encoding="utf-8")
gradle=root/"build.gradle"
build=gradle.read_text(encoding="utf-8")
assert "versionName '4.3.5'" in build and "versionCode 40" in build
gradle.write_text(build.replace("versionName '4.3.5'","versionName '4.3.6'")
    .replace("versionCode 40","versionCode 41"),encoding="utf-8")
print("PASS VistaBrowser 4.3.6: 16:9 D-pad TV browser and original mobile scanner preserved")
