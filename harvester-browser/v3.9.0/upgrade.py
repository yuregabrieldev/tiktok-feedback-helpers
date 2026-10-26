from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text(encoding="utf-8")
begin='''        menuHeading(box,"Scanner e diagnósticos");'''
end='''        menuHeading(box,"Preferências");'''
if s.count(begin)!=1 or s.count(end)!=1:
    raise RuntimeError("Could not identify duplicated long tools sections")
start=s.index(begin)
finish=s.index(end,start)
old=s[start:finish]
required=(
    'this::showAutoScanStatus','this::showCrashDiagnostics',
    'this::openDemoPlayer','this::castCurrentPlayback',
    'this::showExportOptions','this::showCrawlExports',
    'this::confirmClearHistory'
)
for action in required:
    if action not in old:raise RuntimeError("Lost access to "+action)

# Replace the two oversized tool sections with one calm, dedicated entry.
s=s[:start]+'''        menuHeading(box,"Ferramentas");
        profileWide(box,"⚙","Configurações avançadas",
            this::showAdvancedBrowserSettings,popup);

'''+s[finish:]

# Real, independent screen, not a nested modal containing duplicated items.
advanced=r'''
    /** Scanner, diagnostics and exports: one advanced hub, not repeated in Profile. */
    private void showAdvancedBrowserSettings(){
        BrowserPage page=newPage("Configurações avançadas");
        panelHint(page.body,
            "Ferramentas técnicas e diagnósticos. As funções de navegação do dia a dia continuam no menu principal.");
        panelHeading(page.body,"Scanner e diagnósticos");
        panelLine(page.body,"◎","Estado do Auto Player Scanner",
            "Consultar análise automática e estado do scanner",
            this::showAutoScanStatus,null);
        panelDivider(page.body);
        panelLine(page.body,"⚠","Diagnóstico de falhas",
            "Verificar erros e relatórios do navegador",
            this::showCrashDiagnostics,null);
        panelDivider(page.body);
        panelLine(page.body,"▶","Testar player nativo",
            "Reprodução de teste e diagnóstico de vídeo",
            this::openDemoPlayer,null);
        panelDivider(page.body);
        panelLine(page.body,"▣","Transmitir vídeo atual para TV",
            "Utilizar a transmissão do vídeo selecionado",
            this::castCurrentPlayback,null);
        panelHeading(page.body,"Exportação e dados");
        panelLine(page.body,"⇧","Exportar biblioteca",
            "Guardar os dados da biblioteca local",
            this::showExportOptions,null);
        panelDivider(page.body);
        panelLine(page.body,"▣","Exportar rastreio ZIP",
            "Guardar os resultados de rastreio e análise",
            this::showCrawlExports,null);
        panelDivider(page.body);
        panelLine(page.body,"⌫","Limpar histórico",
            "Eliminar registos locais de navegação",
            this::confirmClearHistory,null);
    }
'''
anchor='''    private void shareCurrentPage(){'''
if s.count(anchor)!=1:raise RuntimeError("shareCurrentPage anchor missing")
s=s.replace(anchor,advanced+anchor,1)

profile=s.split('private void showMenu(){',1)[1].split('private void showAdvancedBrowserSettings(){',1)[0]
advanced_section=s.split('private void showAdvancedBrowserSettings(){',1)[1].split('private void shareCurrentPage(){',1)[0]
assert profile.count("Configurações avançadas")==1
for title in ("Scanner e diagnósticos","Exportação e dados",
              "Estado do Auto Player Scanner","Diagnóstico de falhas",
              "Testar player nativo","Transmitir vídeo atual para TV",
              "Exportar biblioteca","Exportar rastreio ZIP","Limpar histórico"):
    assert title not in profile,title
    assert title in advanced_section,title
for entry in ('Favoritos','Transferências','Histórico','Biblioteca',
              'Explorar e reproduzir','Proteção e automação',
              'Verificar atualizações','Acerca do VistaBrowser'):
    assert entry in profile,entry
for legacy in ('private void showPageActions(){',
               'private void showAppearance(){',
               'private void renderOperaTabs(){',
               'private boolean sendToVistaStream(',
               'private void showDetected(){',
               'private void showLibrary(){',
               'private void showInspector(){',
               'private void showCrawler(){',
               'private void showQuickActions(){'):
    if legacy=='private void showQuickActions(){':
        assert legacy not in s
    else:
        assert legacy in s,legacy

p.write_text(s,encoding="utf-8")
g=Path("hb-v18/app/build.gradle")
gradle=g.read_text(encoding="utf-8")
assert "versionName '3.8.0'" in gradle and "versionCode 29" in gradle
g.write_text(gradle.replace("versionName '3.8.0'","versionName '3.9.0'")
                   .replace("versionCode 29","versionCode 30"),encoding="utf-8")
print("VistaBrowser 3.9: 7 advanced actions moved to one dedicated screen without duplication")
