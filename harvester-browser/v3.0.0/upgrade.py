from pathlib import Path
import re

root = Path("hb-v18")
src = root/"app/src/main/java/com/harvester/browser/MainActivity.java"
s=src.read_text(encoding="utf-8")

def once(a,b,name):
    global s
    n=s.count(a)
    if n != 1: raise RuntimeError(f"{name}: expected one match, got {n}")
    s=s.replace(a,b,1)

# Replace the bottom reload button with an accessible, compact quick-actions launcher.
once('''        navButton(nav,"⟳",()->{hideKeyboard();if(selected!=null)selected.web.reload();});''',
'''        TextView quick=navButton(nav,"☷",()->{hideKeyboard();showQuickActions();});
        quick.setContentDescription("Ações rápidas: atualizar página e opções do scanner");''',
"bottom toolbar")

# Remove the four old menu switches, preserving scanner status/information entries.
once('''        autoToggle(box,"Bloquear saída da página atual",
            selected!=null && pinnedTabUrls.containsKey(selected),
            value->setPageLockForCurrentTab(value));
''','',"move page lock")
once('''        autoToggle(box,"Abrir VistaCast ao iniciar um vídeo",autoOpenPlayerOnPlay,v->{
            autoOpenPlayerOnPlay=v;
            getSharedPreferences("harvester-settings",MODE_PRIVATE).edit()
                .putBoolean("auto_open_player_on_play",v).apply();
        });
''','',"move auto player")
once('''        autoToggle(box,"Analisar páginas automaticamente",autoScanEnabled,v->{
            autoScanEnabled=v;setAutoScanFlag("auto_scan",v);
            toast(v?"Análise automática ativada":"Análise automática desativada");
        });
''','',"move scanner toggle")
once('''        autoToggle(box,"Interações em domínios autorizados",autoInteractionsEnabled,v->{
            autoInteractionsEnabled=v;setAutoScanFlag("auto_interactions",v);
        });
''','',"move domain interactions")

# This existing conventional popup blocker switch also belongs in quick actions.
block_begin=s.index('''        LinearLayout toggleRow=Styles.row(this);toggleRow.setPadding(d(15),d(7),d(15),d(7));''')
block_end=s.index('''        menuEntry(box,"▣","Modo desktop / móvel",''',block_begin)
if 'toggleRow' not in s[block_begin:block_end] or 'popupBlocked=blocked' not in s[block_begin:block_end]:
    raise RuntimeError("Popup blocker original switch structure changed")
s=s[:block_begin]+s[block_end:]

once('''        menuEntry(box,"◈","Autorizar / revogar interações neste domínio",this::toggleHostPermission,popup);
''','',"move permission action")

quick_actions='''    /** Floating quick settings from the bottom navigation, replacing reload. */
    private void showQuickActions(){
        final PopupWindow popup=new PopupWindow(this);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout box=Styles.column(this);
        box.setPadding(d(3),d(7),d(3),d(11));
        box.setBackground(Styles.stroke(this,0xFA25282D,20,0xFF46505E));
        scroll.addView(box);
        popup.setContentView(scroll);
        popup.setWidth(Math.min(d(364),getResources().getDisplayMetrics().widthPixels-d(20)));
        popup.setHeight(Math.min(d(640),(int)(getResources().getDisplayMetrics().heightPixels*.70)));
        popup.setBackgroundDrawable(Styles.bg(this,0xFA25282D,20));
        popup.setElevation(d(18));
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        menuHeading(box,"Ações rápidas");
        menuEntry(box,"⟳","Atualizar página",()->{
            if(selected!=null && selected.web!=null)selected.web.reload();
        },popup);
        autoToggle(box,"Bloquear saída da página atual",
            selected!=null && pinnedTabUrls.containsKey(selected),
            enabled->setPageLockForCurrentTab(enabled));
        autoToggle(box,"Abrir VistaStream ao iniciar um vídeo",autoOpenPlayerOnPlay,v->{
            autoOpenPlayerOnPlay=v;
            getSharedPreferences("harvester-settings",MODE_PRIVATE).edit()
                .putBoolean("auto_open_player_on_play",v).apply();
        });
        autoToggle(box,"Analisar páginas automaticamente",autoScanEnabled,v->{
            autoScanEnabled=v;setAutoScanFlag("auto_scan",v);
            toast(v?"Análise automática ativada":"Análise automática desativada");
        });
        autoToggle(box,"Interações em domínios autorizados",autoInteractionsEnabled,v->{
            autoInteractionsEnabled=v;setAutoScanFlag("auto_interactions",v);
        });
        autoToggle(box,"Bloquear pop-ups",popupBlocked,blocked->{
            popupBlocked=blocked;
            getSharedPreferences("harvester-settings",MODE_PRIVATE).edit()
                .putBoolean("block_popups",blocked).apply();
            for(Tab t:tabs)if(t.web!=null)
                t.web.getSettings().setJavaScriptCanOpenWindowsAutomatically(!blocked);
        });
        menuEntry(box,"◈","Autorizar interações neste domínio",
            this::toggleHostPermission,popup);
        // Keep the sheet above system gesture navigation and away from the keyboard.
        popup.showAtLocation(root,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,d(65));
    }
'''
once('''    private void showMenu(){''',quick_actions+'''    private void showMenu(){''',"quick sheet insertion")
once('''        menuHeading(box,"Navegação");''',
'''        menuHeading(box,"Navegação");
        menuEntry(box,"⇧","Verificar atualizações",()->
            VistaBrowserUpdates.check(this,true),popup);''',"manual updater entry")

# Once-a-day background check only notifies; it never silently downloads or installs.
once('''    @Override protected void onResume(){
        super.onResume();''',
'''    @Override protected void onResume(){
        super.onResume();
        VistaBrowserUpdates.check(this,false);''',"daily update check")

# Rename visible text while keeping package name, database, saved tabs,
# settings keys, and the existing VistaStream integration protocol unchanged.
def rename_literal(m):
    literal=m.group(0)
    literal=literal.replace("Harvester Browser","VistaBrowser")
    literal=literal.replace("Harvester","VistaBrowser")
    literal=literal.replace("VistaCast","VistaStream")
    return literal
s=re.sub(r'"(?:\\.|[^"\\])*"',rename_literal,s)
src.write_text(s,encoding="utf-8")

for path in root.glob("app/src/main/java/com/harvester/browser/*.java"):
    if path==src:continue
    old=path.read_text(encoding="utf-8")
    new=re.sub(r'"(?:\\.|[^"\\])*"',rename_literal,old)
    if new!=old:path.write_text(new,encoding="utf-8")

manifest=root/"app/src/main/AndroidManifest.xml"
m=manifest.read_text(encoding="utf-8")
assert 'android:label="Harvester Browser"' in m
m=m.replace('android:label="Harvester Browser"','android:label="VistaBrowser"',1)
assert 'android.permission.REQUEST_INSTALL_PACKAGES' not in m
m=m.replace('<uses-permission android:name="android.permission.INTERNET" />',
    '<uses-permission android:name="android.permission.INTERNET" />\n'+
    '    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />',1)
manifest.write_text(m,encoding="utf-8")

gradle=root/"app/build.gradle"
g=gradle.read_text(encoding="utf-8")
assert "versionName '2.9.0'" in g and 'versionCode 20' in g
g=g.replace("versionName '2.9.0'","versionName '3.0.0'").replace('versionCode 20','versionCode 21')
gradle.write_text(g,encoding="utf-8")

print("VistaBrowser v3.0: rename + quick actions + in-app updates; Android ID preserved")
