from pathlib import Path
root=Path("hb-v18")
source=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
gradle=root/"app/build.gradle"
manifest=root/"app/src/main/AndroidManifest.xml"
s=source.read_text(encoding="utf-8")

def one(before,after,label):
    global s
    n=s.count(before)
    if n!=1:raise RuntimeError(f"{label}: wanted one occurrence, found {n}")
    s=s.replace(before,after,1)

# Fix the cross-app protocol broken by the historical global text-rename.
# VistaStream's exported Java/Kotlin activity names must not be rebranded.
one('''"com.ivor.openstream.harvester.VistaBrowserImportActivity"''',
    '''"com.ivor.openstream.harvester.HarvesterImportActivity"''',
    "VistaStream import receiver")
one('''"com.ivor.openstream.harvester.VistaBrowserPlaybackActivity"''',
    '''"com.ivor.openstream.harvester.HarvesterPlaybackActivity"''',
    "VistaStream playback receiver")
one('''for(String packageName:new String[]{"com.vistastream.tv.debug","com.vistastream.tv"})''',
    '''for(String packageName:new String[]{"com.vistastream.tv","com.vistastream.tv.debug"})''',
    "prefer production VistaStream package")

# A real capability check, not a fake connected toggle: Android explicit
# Activities are exported by the real VistaStream Manifest. No shared login.
helper=r'''
    private String availableVistaStreamPackage(boolean requireImport){
        android.content.pm.PackageManager pm=getPackageManager();
        String receiver=requireImport?
            "com.ivor.openstream.harvester.HarvesterImportActivity":
            "com.ivor.openstream.harvester.HarvesterPlaybackActivity";
        for(String pkg:new String[]{"com.vistastream.tv","com.vistastream.tv.debug"}){
            try{
                android.content.pm.ActivityInfo info=pm.getActivityInfo(
                    new android.content.ComponentName(pkg,receiver),0);
                if(info!=null && info.exported && info.enabled)return pkg;
            }catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
            catch(RuntimeException ignored){}
        }
        return null;
    }
    private void openVistaStreamHome(){
        String pkg=availableVistaStreamPackage(false);
        if(pkg==null){showVistaStreamConnection();return;}
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null)
                launch=new Intent(Intent.ACTION_MAIN)
                    .setClassName(pkg,"com.ivor.openstream.MainActivity");
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        }catch(android.content.ActivityNotFoundException | SecurityException e){
            toast("Não foi possível abrir o VistaStream. Verifique a versão instalada.");
        }
    }
    private void showVistaStreamConnection(){
        String player=availableVistaStreamPackage(false);
        String importer=availableVistaStreamPackage(true);
        String message;
        if(player!=null && importer!=null){
            message="VistaStream detetado neste dispositivo.\\n\\n"+
                "Reprodução: disponível\\n"+
                "Importação para a biblioteca: disponível\\n\\n"+
                "Ao escolher um vídeo, o VistaBrowser abre o player do VistaStream; "+
                "o botão + abre o editor de metadados para confirmação. "+
                "Não precisa de ligar uma conta.";
        }else if(player!=null){
            message="VistaStream detetado, mas a atividade de importação "+
                "não está disponível nesta versão. Atualize o VistaStream.";
        }else{
            message="Não foi encontrada uma instalação compatível do VistaStream. "+
                "Instale ou atualize o APK para ativar a reprodução e a biblioteca.";
        }
        AlertDialog.Builder dialog=new AlertDialog.Builder(this)
            .setTitle(player!=null?"VistaStream detetado":"Ligar ao VistaStream")
            .setMessage(message)
            .setNegativeButton("Fechar",null);
        if(player!=null)
            dialog.setPositiveButton("Abrir VistaStream",(d,w)->openVistaStreamHome());
        else
            dialog.setPositiveButton("Ver releases",(d,w)->{
                try{startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/yuregabriell/Vista-Stream-TV/releases")));}
                catch(RuntimeException ex){toast("Não foi possível abrir o GitHub.");}
            });
        dialog.show();
    }
'''
one('''    private boolean sendToVistaStream(MediaItem item,boolean editBeforeSaving) {''',
helper+'''    private boolean sendToVistaStream(MediaItem item,boolean editBeforeSaving) {''',
"add direct app discovery and check")

# Profile tiles: protection and scanning actually toggle their settings.
# VistaStream opens its capability screen and the real app, rather than
# masquerading as an always-connected auto-play switch.
before='''        profileStatus(statusRow,"◈","Proteção",
            popupBlocked?"Ativada":"Desativada",popup,
            this::showQuickActions);
        profileStatus(statusRow,"◎","Scanner",
            autoScanEnabled?"Ativo":"Inativo",popup,
            this::showQuickActions);
        profileStatus(statusRow,"▶","VistaStream",
            autoOpenPlayerOnPlay?"Ligado":"Desligado",popup,
            this::showQuickActions);'''
after='''        profileStatus(statusRow,"◈","Proteção",
            popupBlocked?"Ativada":"Desativada",popup,
            this::togglePopupProtection);
        profileStatus(statusRow,"◎","Scanner",
            autoScanEnabled?"Ativo":"Inativo",popup,
            this::toggleAutoScanner);
        profileStatus(statusRow,"▶","VistaStream",
            availableVistaStreamPackage(false)!=null?"Detetado":"Não ligado",popup,
            this::showVistaStreamConnection);'''
one(before,after,"profile status actions")

handlers=r'''
    private void togglePopupProtection(){
        popupBlocked=!popupBlocked;
        getSharedPreferences("harvester-settings",MODE_PRIVATE).edit()
            .putBoolean("block_popups",popupBlocked).apply();
        for(Tab tab:tabs){
            if(tab.web!=null){
                try{tab.web.getSettings()
                    .setJavaScriptCanOpenWindowsAutomatically(!popupBlocked);}
                catch(RuntimeException ignored){}
            }
        }
        toast(popupBlocked?"Bloqueio de pop-ups ativado":"Bloqueio de pop-ups desativado");
        showMenu();
    }
    private void toggleAutoScanner(){
        autoScanEnabled=!autoScanEnabled;
        setAutoScanFlag("auto_scan",autoScanEnabled);
        toast(autoScanEnabled?"Scanner automático ativado":"Scanner automático desativado");
        showMenu();
    }
'''
one('''    private void profileStatus(LinearLayout row,String glyph,String label,String state,''',
handlers+'''    private void profileStatus(LinearLayout row,String glyph,String label,String state,''',
"profile card real actions")

# Show an explicit VistaStream shortcut alongside video tools.
one('''        menuHeading(box,"Explorar e reproduzir");
        LinearLayout fourth=Styles.row(this);''',
'''        menuHeading(box,"Explorar e reproduzir");
        profileWide(box,"▶","Ligar e abrir VistaStream",this::showVistaStreamConnection,popup);
        LinearLayout fourth=Styles.row(this);''',"explicit companion app control")

# Allow swipe down from the visible drag handle and from the top of the
# scrollable sheet. Preserve scrolling up/down the long content as normal.
one('''        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box=Styles.column(this);
        box.setPadding(d(17),d(18),d(17),d(30));''',
'''        ScrollView scroll=new ScrollView(this){
            private float startY=0f;
            private boolean canDismiss=false;
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){
                if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){
                    startY=event.getY();
                    canDismiss=getScrollY()==0;
                }
                if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE &&
                   canDismiss && event.getY()-startY>d(38))return true;
                return super.onInterceptTouchEvent(event);
            }
            @Override public boolean onTouchEvent(android.view.MotionEvent event){
                if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE &&
                   canDismiss && event.getY()-startY>d(72)){
                    popup.dismiss();
                    canDismiss=false;
                    return true;
                }
                return super.onTouchEvent(event);
            }
        };
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout box=Styles.column(this);
        box.setPadding(d(17),d(18),d(17),d(30));''',
"Opera profile sheet drag")

one('''        box.addView(grab,gp);

        LinearLayout header=Styles.row(this);''',
'''        box.addView(grab,gp);
        grab.setOnTouchListener(new View.OnTouchListener(){
            private float downY;
            @Override public boolean onTouch(View v,android.view.MotionEvent event){
                if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){
                    downY=event.getRawY();
                    return true;
                }
                if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE &&
                   event.getRawY()-downY>d(48)){
                    popup.dismiss();
                    return true;
                }
                return event.getActionMasked()!=android.view.MotionEvent.ACTION_CANCEL;
            }
        });

        LinearLayout header=Styles.row(this);''',
"swipe grip behavior")

# The address/search overlay previously kept its keyboard open because only
# the URL bar was checked in dispatchTouchEvent. Generalize to any EditText.
one('''    private void hideKeyboard(){
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);
        address.clearFocus();root.requestFocus();
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
        if(event.getAction()==android.view.MotionEvent.ACTION_DOWN && address!=null && address.hasFocus()){
            int[] xy=new int[2];address.getLocationOnScreen(xy);
            float x=event.getRawX(),y=event.getRawY();
            if(x<xy[0]||x>xy[0]+address.getWidth()||y<xy[1]||y>xy[1]+address.getHeight()){
                hideKeyboard();
            }
        }
        return super.dispatchTouchEvent(event);
    }''',
'''    private void hideKeyboard(){
        View current=getCurrentFocus();
        if(current!=null){
            InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(current.getWindowToken(),0);
            current.clearFocus();
        }
        if(root!=null)root.requestFocus();
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
        if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){
            View focus=getCurrentFocus();
            if(focus instanceof EditText && focus.isShown()){
                int[] position=new int[2];focus.getLocationOnScreen(position);
                float x=event.getRawX(),y=event.getRawY();
                if(x<position[0] || x>position[0]+focus.getWidth() ||
                   y<position[1] || y>position[1]+focus.getHeight()){
                    hideKeyboard();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }''',
"keyboard hides on outside taps")

# Ensure the original Android visibility declarations continue to include the
# real VistaStream application packages.
m=manifest.read_text(encoding="utf-8")
for val in ('android:name="com.vistastream.tv"', 'android:name="com.vistastream.tv.debug"'):
    if val not in m:raise RuntimeError("Missing visibility rule "+val)

for name in ["private void showDetected(){","private void showLibrary(){",
    "private void showInspector(){","private void showCrawler(){",
    "private boolean sendToVistaStream(","private void openNativePlayer(",
    "private void showQuickActions(){","private void showMenu(){",
    "private void showSearchOverlay(","private void showAppearance(){",
    "private void showAutoScanStatus(){","private void setAutoScanFlag(",
    "private void profileStatus("]:
    if name not in s:raise RuntimeError("Lost functional method "+name)
assert 'VistaBrowserPlaybackActivity' not in s
assert 'VistaBrowserImportActivity' not in s
assert 'com.ivor.openstream.harvester.HarvesterPlaybackActivity' in s
assert 'com.ivor.openstream.harvester.HarvesterImportActivity' in s
source.write_text(s,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
assert "versionName '3.5.0'" in g and "versionCode 26" in g
g=g.replace("versionName '3.5.0'","versionName '3.6.0'").replace("versionCode 26","versionCode 27")
gradle.write_text(g,encoding="utf-8")
print("VistaBrowser 3.6: correct VistaStream Intent receivers, functional cards, IME outside tap and swipe-down dismissal")
