#!/usr/bin/env python3
"""VistaBrowser 4.4.0: drive Smart Crawler on each iframe scan, not only manual inspector."""
from pathlib import Path

root=Path("hb-v18/app")
src=root/"src/main/java/com/yure/videoharvester"
premium=src/"VistaScraperPremiumActivity.java"
gradle=root/"build.gradle"
data=premium.read_text(encoding="utf-8")
anchor='    @Override void createIframePage(){'
assert data.count(anchor)==1, "Iframe UI entry changed"

extra='''    /**
     * Run the same bounded player assistant during automatic iframe analysis.
     * All state is local to the WebView and URL. Attempts do not click adverts
     * or arbitrary central pixels; the scanner's existing popup guard remains.
     * The queue's current task and sequence are checked on delayed callbacks so
     * no action leaks into the next iframe.
     */
    @Override void inspectDom(android.webkit.WebView view, Task task,
                              int sequence, boolean last){
        super.inspectDom(view,task,sequence,last);
        if(!scanning || current!=task || scanSerial!=sequence || runMode!=2 ||
            !"IFRAME".equals(task.kind) ||
            !getSharedPreferences("vista_smart_crawler_v1",0)
                .getBoolean("enabled",false))return;
        performVistaIframeAssist(view,task,sequence);
        if(!last){
            android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(()->performVistaIframeAssist(view,task,sequence),1800);
            handler.postDelayed(()->performVistaIframeAssist(view,task,sequence),3800);
            handler.postDelayed(()->performVistaIframeAssist(view,task,sequence),6500);
        }
    }

    private void performVistaIframeAssist(android.webkit.WebView view,Task task,int sequence){
        if(isFinishing()||isDestroyed()||!scanning||current!=task||
            scanSerial!=sequence||runMode!=2||view==null)return;
        if(!getSharedPreferences("vista_smart_crawler_v1",0)
            .getBoolean("enabled",false))return;
        VistaPlayerAssistant.attempt(this,view);
        VistaPlayInteractor.attempt(this,view);
        VistaSourceCollector.scan(this,view);
    }

'''
data=data.replace(anchor,extra+anchor,1)
old='''            "Analise os iframes encontrados, filtre a fila e confirme quais fontes realmente funcionam.");'''
if old in data:
    data=data.replace(old,'''            "Analisa cada iframe, tenta abrir botões Play identificáveis e procura URLs de mídia. " +
            "Os resultados ainda podem exigir revisão quando o player impõe restrições.");''',1)
premium.write_text(data,encoding="utf-8")
g=gradle.read_text(encoding="utf-8")
assert "versionName '4.3.9'" in g and "versionCode 44" in g
g=g.replace("versionName '4.3.9'","versionName '4.4.0'",1)
g=g.replace("versionCode 44","versionCode 45",1)
gradle.write_text(g,encoding="utf-8")
print("PASS VistaBrowser 4.4.0: bounded Play attempts executed during the iframe scanning queue")
