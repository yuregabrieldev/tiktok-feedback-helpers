#!/usr/bin/env python3
"""VistaBrowser 4.3.8: visible Smart Crawler and optional HTML5 playback assistant."""
from pathlib import Path
import shutil

app=Path("hb-v18/app")
src=app/"src/main/java/com/yure/videoharvester"
premium=src/"VistaScraperPremiumActivity.java"
inspector=src/"VistaPremiumInspectorActivity.java"
source=src/"VistaSourceBrowserActivity.java"
gradle=app/"build.gradle"
shutil.copy2("harvester-browser/v4.3.8/VistaPlayerAssistant.java",src/"VistaPlayerAssistant.java")

text=premium.read_text(encoding="utf-8")
old='''        hero(iframeBody,"Inspeção de players",
            "Analise os iframes encontrados, filtre a fila e confirme quais fontes realmente funcionam.");'''
assert text.count(old)==1, "premium iframe hero changed"
new='''        hero(iframeBody,"Smart Crawler · Análise de iframes",
            "A análise de iframes não significa que o vídeo foi carregado. " +
            "Ativa o Smart Crawler para observar media HTML5 e tentar iniciar " +
            "o player sem clicar em anúncios, pop-ups ou controlos de acesso.");
        btnFull(iframeBody,"Smart Crawler · Configurar e ver resultados",true,
            ()->startActivity(new android.content.Intent(this,
                VistaSourceBrowserActivity.class)));'''
text=text.replace(old,new,1)
premium.write_text(text,encoding="utf-8")

text=source.read_text(encoding="utf-8")
old='''        panel.addView(automatic);
        button("Rever correspondências pendentes",()->review());'''
assert text.count(old)==1, "source option anchor changed"
new='''        panel.addView(automatic);
        android.widget.CheckBox assist=new android.widget.CheckBox(this);
        assist.setText("Tentar iniciar vídeos HTML5 automaticamente (sem clicar em anúncios)");
        assist.setTextColor(Color.WHITE);
        assist.setChecked(getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE)
            .getBoolean("assist_html5",false));
        assist.setOnCheckedChangeListener((b,on)->{
            getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE).edit()
                .putBoolean("assist_html5",on).apply();
            if(on && !automatic.isChecked()) automatic.setChecked(true);
        });
        panel.addView(assist);
        title("Automação experimental: funciona apenas quando a página expõe " +
            "um player HTML5 acessível. Sem interação com publicidade, CAPTCHA, DRM " +
            "ou iframes de outras origens. Pode ser preciso tocar em Play no site.",12);
        title("Estado da última tentativa:",14);
        title(getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE)
            .getString("assistant_status","Ainda não testado no inspetor."),12);
        button("Rever correspondências pendentes",()->review());'''
text=text.replace(old,new,1)
text=text.replace('title("Vista Source",26);','title("Smart Crawler · Vista Source",26);',1)
source.write_text(text,encoding="utf-8")

text=inspector.read_text(encoding="utf-8")
old='''                VistaSourceCollector.scan(VistaPremiumInspectorActivity.this,browser);
            vistaCrawlerHandler.postDelayed(this,4500);'''
assert text.count(old)==1,"inspector timer anchor changed"
new='''                {
                    VistaPlayerAssistant.attempt(VistaPremiumInspectorActivity.this,browser);
                    VistaSourceCollector.scan(VistaPremiumInspectorActivity.this,browser);
                }
            vistaCrawlerHandler.postDelayed(this,4500);'''
text=text.replace(old,new,1)
inspector.write_text(text,encoding="utf-8")

text=gradle.read_text(encoding="utf-8")
assert "versionName '4.3.7'" in text and "versionCode 42" in text
text=text.replace("versionName '4.3.7'","versionName '4.3.8'",1)
text=text.replace("versionCode 42","versionCode 43",1)
# Signing is only enabled when ALL four secrets are provisioned by the repo
# owner; never create/publish the private key in CI. Debug fallback remains
# for manual testing and is always clearly labelled as such.
marker="android {"
assert marker in text
signing='''android {
    def vbKey = System.getenv("VISTABROWSER_KEYSTORE_PATH")
    def vbStorePass = System.getenv("VISTABROWSER_KEYSTORE_PASSWORD")
    def vbAlias = System.getenv("VISTABROWSER_KEY_ALIAS")
    def vbKeyPass = System.getenv("VISTABROWSER_KEY_PASSWORD")
    if (vbKey && vbStorePass && vbAlias && vbKeyPass) {
        signingConfigs {
            vistaStable {
                storeFile file(vbKey)
                storePassword vbStorePass
                keyAlias vbAlias
                keyPassword vbKeyPass
            }
        }
        buildTypes {
            debug { signingConfig signingConfigs.vistaStable }
        }
    }
'''
text=text.replace(marker,signing,1)
gradle.write_text(text,encoding="utf-8")
print("VistaBrowser 4.3.8: visible Smart Crawler + optional HTML5 assistant and stable signing when configured")
