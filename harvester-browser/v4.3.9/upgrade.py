#!/usr/bin/env python3
"""Bounded, opt-in native WebView player interactions for VistaBrowser 4.3.9."""
from pathlib import Path
import shutil

root=Path("hb-v18/app")
src=root/"src/main/java/com/yure/videoharvester"
inspector=src/"VistaPremiumInspectorActivity.java"
ui=src/"VistaSourceBrowserActivity.java"
gradle=root/"build.gradle"

shutil.copy2("harvester-browser/v4.3.9/VistaPlayInteractor.java",src/"VistaPlayInteractor.java")
s=inspector.read_text(encoding="utf-8")
needle='''                    VistaPlayerAssistant.attempt(VistaPremiumInspectorActivity.this,browser);
                    VistaSourceCollector.scan(VistaPremiumInspectorActivity.this,browser);'''
assert s.count(needle)==1,"inspector auto-capture integration changed"
s=s.replace(needle,'''                    VistaPlayerAssistant.attempt(VistaPremiumInspectorActivity.this,browser);
                    VistaPlayInteractor.attempt(VistaPremiumInspectorActivity.this,browser);
                    VistaSourceCollector.scan(VistaPremiumInspectorActivity.this,browser);''',1)
inspector.write_text(s,encoding="utf-8")

s=ui.read_text(encoding="utf-8")
needle='''        panel.addView(assist);
        title("Automação experimental: funciona apenas quando a página expõe " +'''
assert s.count(needle)==1,"player assistant options changed"
s=s.replace(needle,'''        panel.addView(assist);
        android.widget.CheckBox taps=new android.widget.CheckBox(this);
        taps.setText("Tentar tocar no botão Play do player (até 3 vezes)");
        taps.setTextColor(Color.WHITE);
        taps.setChecked(getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE)
            .getBoolean("assist_play_buttons",false));
        taps.setOnCheckedChangeListener((b,on)->{
            getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE).edit()
                .putBoolean("assist_play_buttons",on).apply();
            if(on&&!automatic.isChecked())automatic.setChecked(true);
        });
        panel.addView(taps);
        title("Interações limitadas apenas a botões de reprodução identificados; " +
            "não toca em anúncios nem ultrapassa CAPTCHA, DRM ou limites entre iframes. " +
            "Alguns sites podem exigir gestos manuais.",12);
        title("Última tentativa de abrir player:",14);
        title(getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE)
            .getString("play_interactor_status","Ainda não houve tentativas."),12);
        title("Automação experimental: funciona apenas quando a página expõe " +''',1)
s=s.replace('title("Smart Crawler · Vista Source",26);','title("Smart Crawler · Auto Player",26);',1)
ui.write_text(s,encoding="utf-8")

s=gradle.read_text(encoding="utf-8")
assert "versionName '4.3.8'" in s and "versionCode 43" in s
s=s.replace("versionName '4.3.8'","versionName '4.3.9'",1)
s=s.replace("versionCode 43","versionCode 44",1)
gradle.write_text(s,encoding="utf-8")
print("PASS v4.3.9 bounded Play interactor installed in inspector (opt-in)")
