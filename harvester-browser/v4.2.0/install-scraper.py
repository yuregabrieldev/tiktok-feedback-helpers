from pathlib import Path
import re
root=Path("hb-v18")
app=root/"app"
source=app/"src/main/java/com/harvester/browser/MainActivity.java"
manifest=app/"src/main/AndroidManifest.xml"
gradle=app/"build.gradle"
scraper=app/"src/main/java/com/yure/videoharvester"
if not (scraper/"MainActivityV25.java").exists():
    raise SystemExit("The complete verified Simples Scraper source tree must be vendored before integration")

# The source is the complete original v3.0.1 inheritance stack,
# not an API redirect or a UI imitation.
expected=[
    "MainActivity","MainActivityV12","MainActivityV13","MainActivityV14",
    "MainActivityV15","MainActivityV16","MainActivityV17","MainActivityV18",
    "MainActivityV19","MainActivityV20","MainActivityV21","MainActivityV22",
    "MainActivityV24","MainActivityV25",
    "PlayerInspectorActivity","PlayerInspectorActivityV26","PlayerInspectorActivityV27",
    "PlayerInspectorActivityV28","PlaybackTestActivity","BrCatalogActivity",
    "BrCatalogEngine","BrTitleParser","HarvesterVistaBridge","ScanNotificationService"
]
for name in expected:
    if not (scraper/(name+".java")).exists():raise AssertionError("missing Simples Scraper functionality "+name)
# These constants need to be updated from the active VistaBrowser theme.
for name,needle,sub in [
    ("MainActivityV17","static final int WINE=","static int WINE="),
    ("MainActivityV21","static final int DARK=","static int DARK="),
    ("MainActivityV22","static final int SUCCESS_GREEN=","static int SUCCESS_GREEN=")
]:
    p=scraper/(name+".java")
    data=p.read_text()
    if data.count(needle)!=1:raise AssertionError(name+" constants changed")
    p.write_text(data.replace(needle,sub,1))
# Manifest contains internal activities only. The external launcher remains
# VistaBrowser; legacy Simples Scraper remains independently installable.
m=manifest.read_text()
marker='''    </application>'''
if m.count(marker)!=1:raise AssertionError("manifest app footer missing")
component='''        <!-- Simples Scraper v3.0.1 embedded in VistaBrowser: no extra APK. -->
        <activity android:name="com.yure.videoharvester.VistaScraperActivity"
            android:exported="false" android:windowSoftInputMode="adjustResize" />
        <activity android:name="com.yure.videoharvester.PlayerInspectorActivity"
            android:exported="false" />
        <activity android:name="com.yure.videoharvester.PlayerInspectorActivityV26"
            android:exported="false" />
        <activity android:name="com.yure.videoharvester.PlayerInspectorActivityV27"
            android:exported="false" />
        <activity android:name="com.yure.videoharvester.PlayerInspectorActivityV28"
            android:exported="false" />
        <activity android:name="com.yure.videoharvester.PlaybackTestActivity"
            android:exported="false" />
        <activity android:name="com.yure.videoharvester.BrCatalogActivity"
            android:exported="false" />
        <service android:name="com.yure.videoharvester.ScanNotificationService"
            android:exported="false" android:foregroundServiceType="dataSync" />
'''
m=m.replace(marker,component+marker)
perm='''    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />'''
if m.count(perm)!=1:raise AssertionError("foreground permission missing")
m=m.replace(perm,perm+'''
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />''')
manifest.write_text(m)

g=gradle.read_text()
if "org.jsoup:jsoup:1.18.3" not in g:
    g+="\n// Simples Scraper v3.0.1 HTML / DOM catalog adapters\n"
    g+="dependencies { implementation 'org.jsoup:jsoup:1.18.3' }\n"
assert "versionName '4.1.0'" in g and "versionCode 32" in g
g=g.replace("versionName '4.1.0'","versionName '4.2.0'").replace("versionCode 32","versionCode 33")
gradle.write_text(g)

# Restore the 4.1 technical crawler as an optional legacy tool.
s=source.read_text()
anchor="    private void showCrawler(){"
if s.count(anchor)!=1:raise AssertionError("original crawler missing")
s=s.replace(anchor,"    private void showDomainCrawlerLegacy(){",1)
# One premium VistaBrowser tool hub routes to all native internal SS screens.
hub=r'''
    /** Built-in Simples Scraper v3.0.1, reached from the VistaBrowser Crawler tool. */
    private void openScraperTool(String page){
        String url=selected!=null&&selected.url!=null&&
            selected.url.startsWith("https://")?selected.url:"";
        try{
            Intent intent=new Intent(this,com.yure.videoharvester.VistaScraperActivity.class);
            intent.putExtra("vistabrowser_scraper_page",page);
            intent.putExtra("vistabrowser_source_url",url);
            startActivity(intent);
        }catch(Exception e){toast("Não foi possível abrir o scanner: "+e.getMessage());}
    }
    private void showCrawler(){
        BrowserPage page=newPage("Crawler e Scanner");
        panelHint(page.body,
            "O Simples Scraper está integrado no VistaBrowser. Analise páginas, iframes e fontes, reveja resultados e mantenha o progresso sem instalar outro APK.");
        panelHeading(page.body,"Descoberta e análise");
        panelLine(page.body,"⌕","Scan completo de websites",
            "Páginas, links, sitemaps, media, subdomínios e profundidade controlada",
            ()->openScraperTool("scan"),null);
        panelDivider(page.body);
        panelLine(page.body,"◎","Análise de iframes",
            "Fila persistente, análise recursiva, progresso e inspeção de players",
            ()->openScraperTool("iframes"),null);
        panelDivider(page.body);
        panelLine(page.body,"▤","Descobertas e players",
            "Ver todos os iframes, URLs e resultados do rastreio",
            ()->openScraperTool("discoveries"),null);

        panelHeading(page.body,"Vídeos e biblioteca");
        panelLine(page.body,"▶","Vídeos encontrados",
            "Classificar fontes, aprovar títulos, testar reprodução ou enviar ao VistaStream",
            ()->openScraperTool("videos"),null);
        panelDivider(page.body);
        panelLine(page.body,"▣","Biblioteca do scanner",
            "Consultar e gerir títulos aprovados pelo Simples Scraper",
            ()->openScraperTool("library"),null);
        panelDivider(page.body);
        panelLine(page.body,"⌕","Catálogo de fontes BR",
            "Adaptadores brasileiros opcionais para filmes, séries e animes",
            ()->openScraperTool("brcatalog"),null);

        panelHeading(page.body,"Projeto e compatibilidade");
        panelLine(page.body,"☷","Configurações e backups do scanner",
            "Guardar progresso, escolher profundidade, exportar e importar JSON",
            ()->openScraperTool("settings"),null);
        panelDivider(page.body);
        panelLine(page.body,"◈","Crawler técnico do VistaBrowser",
            "Manter rastreio anterior, limites, robots.txt e arquivo HTML/CSS/JS",
            this::showDomainCrawlerLegacy,null);
        panelDivider(page.body);
        panelLine(page.body,"⇧","Rastreios antigos para ZIP",
            "Exportar ou eliminar arquivos criados pelo crawler técnico",
            this::showCrawlExports,null);
        panelHint(page.body,
            "As bibliotecas e os backups do scanner mantêm o respetivo formato. Links candidatos não significam reprodução confirmada; não são contornados DRM ou controlos de acesso.");
    }
'''
s=s.replace("    private void showDomainCrawlerLegacy(){",hub+"    private void showDomainCrawlerLegacy(){",1)
assert s.count("private void showCrawler(){")==1
assert s.count("private void showDomainCrawlerLegacy(){")==1
assert "private boolean sendToVistaStream(" in s
assert "private void showAutoScanStatus(){" in s
source.write_text(s)
print("VistaBrowser 4.2: full verified Simples Scraper sources, all internal Android Activities, complete tool hub, legacy crawler preserved")
