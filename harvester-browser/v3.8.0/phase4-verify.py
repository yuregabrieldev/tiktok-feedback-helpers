from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text(encoding="utf-8")
# Distinguish domain authorization as an ACTION (not a boolean stored by this UI);
# do not falsely display a disabled status when a site is already authorized.
a='''        boolean authorized=selected!=null
            && selected.url!=null
            && isDomainPermissionEnabled(selected.url);
        vbToggleCard(controls2,"◉","Este domínio",authorized,
            this::toggleHostPermission,popup);'''
b='''        vbToggleCard(controls2,"◉","Este domínio",false,
            this::toggleHostPermission,popup);'''
if a not in s:raise RuntimeError("host tile anchor absent")
s=s.replace(a,b,1)
helper='''    private boolean isDomainPermissionEnabled(String url){
        // The underlying domain authorization toggle owns the persisted grant.
        // Until opened, no permission is assumed.
        return false;
    }
'''
if helper not in s:raise RuntimeError("placeholder not found")
s=s.replace(helper,"",1)
a='''        TextView status=Styles.text(this,enabled?"Ativo":"Desligado",10,false,
            enabled?Styles.ACCENT:Styles.MUTED);'''
b='''        boolean domainAction="Este domínio".equals(name);
        TextView status=Styles.text(this,domainAction?"Gerir":enabled?"Ativo":"Desligado",
            10,false,enabled?Styles.ACCENT:Styles.MUTED);'''
if a not in s:raise RuntimeError("status text not found")
s=s.replace(a,b,1)
a='''            if(!"Este domínio".equals(name))showMenu();'''
b='''            if(!"Este domínio".equals(name) && !"Auto scanner".equals(name)
                && !"Pop-ups".equals(name))showMenu();'''
if a not in s:raise RuntimeError("reopen block not found")
s=s.replace(a,b,1)

# Increase visual breathing room of ALL existing internal tool headers.
a='''        header.setPadding(d(14),d(11),d(13),d(8));
        header.setBackground(Styles.gradient(this,0xFF2D302A,0xFF242721,0));'''
b='''        header.setPadding(d(15),d(15),d(14),d(10));
        header.setBackground(Styles.gradient(this,0xFF242620,0xFF20221E,0));'''
if a not in s:raise RuntimeError("header spacing absent")
s=s.replace(a,b,1)
a='''        TextView name=Styles.text(this,title,22,true,PANEL_TEXT);'''
b='''        TextView name=Styles.text(this,title,25,true,PANEL_TEXT);'''
if a not in s:raise RuntimeError("panel title absent")
s=s.replace(a,b,1)

# Initialize saved theme when returning to the Activity; the chosen app-chrome
# preference persists across browser restarts and external playback round-trips.
anchor='''    @Override protected void onResume(){
        super.onResume();'''
replacement='''    @Override protected void onResume(){
        super.onResume();
        applyBrowserTheme();'''
if s.count(anchor)!=1:raise RuntimeError("onResume anchor conflict")
s=s.replace(anchor,replacement,1)

# If fullscreen gets toggled off, restore the saved status bar icon contrast.
a='''        }else{
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }
    private boolean vbFullscreen='''
# field occurs before toggle, not after; modify exact else.
a='''        }else{
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    /** Main user profile hub'''
# stage 1 ends helper before comment? best match direct
needle='''            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }
'''
if needle in s:s=s.replace(needle,'''            getWindow().getDecorView().setSystemUiVisibility(0);
            applyBrowserTheme();
        }
    }
''',1)

# No dangling method names, duplicate technical panel or duplicated menu entries.
assert 'private void showQuickActions(){' not in s
assert 'private void showPageActions(){' in s
assert 'private void showHomeAppearance(){' in s
assert 'private void renderOperaTabs(){' in s
assert 'private void confirmCloseAllTabs(){' in s
assert 'private void confirmCloseSelectedTabs(){' in s
assert 'private void showVistaStreamConnection(){' in s
assert 'private boolean sendToVistaStream(' in s
assert 'private void showInspector(){' in s
assert 'private void showCrawler(){' in s
assert 'private void showDetected(){' in s
assert 'private void showLibrary(){' in s
assert 'private void showAppearance(){' in s
assert 'this::togglePopupProtection' in s
assert 'this::toggleAutoScanner' in s
assert 'this::toggleHostPermission' in s
assert 'this::showAutoScanStatus' in s
assert 'com.ivor.openstream.harvester.HarvesterPlaybackActivity' in s
assert 'com.ivor.openstream.harvester.HarvesterImportActivity' in s
assert 'VistaBrowserUpdates.check(this,false)' in s

p.write_text(s,encoding="utf-8")
g=Path("hb-v18/app/build.gradle")
gradle=g.read_text(encoding="utf-8")
assert "versionName '3.7.0'" in gradle and "versionCode 28" in gradle
g.write_text(gradle.replace("versionName '3.7.0'","versionName '3.8.0'")
                    .replace("versionCode 28","versionCode 29"),encoding="utf-8")
print("VistaBrowser 3.8: page menu, compact toggle cards, Opera appearance, tabs, system chrome; bridge preserved")
