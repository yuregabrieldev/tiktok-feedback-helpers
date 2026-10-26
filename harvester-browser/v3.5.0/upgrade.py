from pathlib import Path
import re

root=Path("hb-v18")
source=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
gradle=root/"app/build.gradle"
s=source.read_text(encoding="utf-8")

def once(old,new,label):
    global s
    n=s.count(old)
    if n!=1:raise RuntimeError(f"{label}: expected one match, found {n}")
    s=s.replace(old,new,1)

# 1. Loading progress is a discreet horizontal progress bar instead of percent
# text. Keep all original page-load callbacks and WebView progress mechanics.
once('private TextView progressIndicator;', 'private ProgressBar progressIndicator;', "progress declaration")

once('''        progressIndicator=Styles.text(this,"",10,false,Styles.ACCENT);
        progressIndicator.setPadding(d(14),0,d(14),0);
        root.addView(progressIndicator,new LinearLayout.LayoutParams(-1,d(12)));''',
'''        progressIndicator=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progressIndicator.setMax(100);
        progressIndicator.setProgress(0);
        progressIndicator.setIndeterminate(false);
        progressIndicator.setProgressTintList(android.content.res.ColorStateList.valueOf(Styles.ACCENT));
        progressIndicator.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF262A24));
        progressIndicator.setVisibility(View.INVISIBLE);
        root.addView(progressIndicator,new LinearLayout.LayoutParams(-1,d(3)));''',
"slim progress bar")

once('''        progressIndicator.setText(selected.progress>=100?"":("A carregar • "+selected.progress+"%"));''',
'''        progressIndicator.setProgress(Math.max(0,Math.min(100,selected.progress)));
        progressIndicator.setVisibility(
            selected.progress>=100 || HOME.equals(selected.url)?View.INVISIBLE:View.VISIBLE);''',
"loading progress callback")

# 2. Top three-dots are strictly the control toggles. Existing tooling, scanner,
# inspector and diagnostics remain reachable via the profile/advanced screens.
a=s.index('    private void showQuickActions(){')
b=s.index('    private void showAdvancedToolsMenu(){',a)
old=s[a:b]
for label in ["Bloquear saída da página atual", "Abrir VistaStream ao iniciar um vídeo",
              "Analisar páginas automaticamente", "Interações em domínios autorizados",
              "Bloquear pop-ups", "Autorizar interações neste domínio"]:
    if label not in old:raise RuntimeError("Missing original control: "+label)

switch_begin=old.index('        autoToggle(box,"Bloquear saída da página atual",')
switch_end=old.index('        menuEntry(box,"◈","Autorizar interações neste domínio",',switch_begin)
switches=old[switch_begin:switch_end]
permission=old[switch_end:old.index('        popup.setHeight(',switch_end)]
# Ensure we retained all original callbacks from settings while removing the extra menu commands.
assert "setPageLockForCurrentTab" in switches
assert "autoOpenPlayerOnPlay=v" in switches
assert "setAutoScanFlag" in switches
assert 'putBoolean("block_popups",blocked)' in switches
assert "this::toggleHostPermission" in permission
quick='''    /** Only original live settings — invoked by the three-dots beside the URL. */
    private void showQuickActions(){
        final PopupWindow popup=new PopupWindow(this);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box=Styles.column(this);
        box.setPadding(d(17),d(17),d(17),d(30));
        box.setBackgroundColor(0xFF191B17);
        scroll.addView(box);
        popup.setContentView(scroll);
        popup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        // The sheet spans from under the address toolbar to the actual bottom,
        // with no 65dp spacer exposing the dock behind the switches.
        popup.setHeight(Math.max(d(350),
            getResources().getDisplayMetrics().heightPixels-safeTop()-d(73)));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF191B17);
        float r=d(25);
        bg.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});
        popup.setBackgroundDrawable(bg);
        popup.setElevation(d(18));
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        View grab=new View(this);
        grab.setBackground(Styles.bg(this,0xFF707367,8));
        LinearLayout.LayoutParams grip=new LinearLayout.LayoutParams(d(40),d(4));
        grip.gravity=Gravity.CENTER_HORIZONTAL;
        grip.bottomMargin=d(15);
        box.addView(grab,grip);
        TextView title=Styles.text(this,"Controlos do navegador",21,true,Styles.TEXT);
        title.setPadding(d(2),d(7),0,d(5));
        box.addView(title);
        TextView hint=Styles.text(this,"Definições da página, proteção e automação",12,false,Styles.MUTED);
        hint.setPadding(d(2),0,0,d(13));
        box.addView(hint);
        menuHeading(box,"Navegação e privacidade");
'''
quick+=switches[:switches.index('        autoToggle(box,"Abrir VistaStream ao iniciar um vídeo",')]
quick+='''        menuHeading(box,"Reprodução");
'''
quick+=switches[switches.index('        autoToggle(box,"Abrir VistaStream ao iniciar um vídeo",'):switches.index('        autoToggle(box,"Analisar páginas automaticamente",')]
quick+='''        menuHeading(box,"Análise e permissões");
'''
quick+=switches[switches.index('        autoToggle(box,"Analisar páginas automaticamente",'):]
quick+=permission
quick+='''        // Hide the underlying browser dock while the top-control sheet is open.
        // Restore it on dismiss; no WebView or navigation state is touched.
        popup.setOnDismissListener(()->{
            if(browserControls!=null && !keyboardVisible)
                browserControls.setVisibility(View.VISIBLE);
        });
        if(browserControls!=null)browserControls.setVisibility(View.INVISIBLE);
        popup.showAtLocation(appFrame,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,0);
    }
'''
s=s[:a]+quick+s[b:]

# Fix local styling leftovers on user/profile panels without removing content.
# The advanced menu is still accessible through Profile -> Advanced Tools.
for marker in ["private void showAdvancedToolsMenu(){","private void showMenu(){",
               "private void showDetected(){","private void showLibrary(){",
               "private void showInspector(){","private void showCrawler(){",
               "private boolean sendToVistaStream(","private void openNativePlayer(",
               "private void renderOperaTabs(){","private void showSearchOverlay(",
               "private void showAppearance(){","VistaBrowserUpdates.check(this,false)"]:
    if marker not in s:raise RuntimeError("Existing functionality missing: "+marker)
for label in ["Bloquear saída da página atual", "Abrir VistaStream ao iniciar um vídeo",
              "Analisar páginas automaticamente", "Interações em domínios autorizados",
              "Bloquear pop-ups", "Autorizar interações neste domínio"]:
    assert label in quick,label
assert "Todas as ferramentas" not in quick
assert "Inspetor web" not in quick
assert "Vídeos encontrados" not in quick
assert "menu.setOnClickListener(v->{hideKeyboard();showQuickActions();});" in s
assert 'profileWide(box,"⋮","Ferramentas avançadas",this::showAdvancedToolsMenu,popup);' in s
source.write_text(s,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
assert "versionName '3.4.0'" in g and "versionCode 25" in g
g=g.replace("versionName '3.4.0'","versionName '3.5.0'").replace("versionCode 25","versionCode 26")
gradle.write_text(g,encoding="utf-8")
print("VistaBrowser 3.5: slim progress bar, controls-only popup, no exposed lower navigation, advanced features preserved")
