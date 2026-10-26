from pathlib import Path
root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
gradle=root/"app/build.gradle"
s=main.read_text(encoding="utf-8")

def one(old,new,label):
    global s
    count=s.count(old)
    if count!=1:raise RuntimeError(f"{label}: expected one match, got {count}")
    s=s.replace(old,new,1)

# HOME: WebView callbacks must never reattach a black about:blank WebView
# over the native homepage. The existing WebView is kept for later navigation.
one('''                if(selected==tab && tab.web.getParent()==null){browserHost.removeAllViews();browserHost.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));}
                synchronizeUi();''',
'''                if(selected==tab && HOME.equals(link)){
                    showHomepage();
                    synchronizeUi();
                    return;
                }
                if(selected==tab && tab.web.getParent()==null){
                    browserHost.removeAllViews();
                    browserHost.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));
                }
                synchronizeUi();''',"WebView onPageStarted and native homepage")
one('''                tab.url=link;
                injectPageLock(tab);
                if(!tab.privateMode && link.startsWith("https://"))store.history(link,tab.title);''',
'''                tab.url=link;
                if(HOME.equals(link)){
                    if(selected==tab)showHomepage();
                    synchronizeUi();
                    return;
                }
                injectPageLock(tab);
                if(!tab.privateMode && link.startsWith("https://"))store.history(link,tab.title);''',"WebView onPageFinished home route")
one('''        try{tab.web.onResume();}catch(Exception ignored){}browserHost.removeAllViews();browserHost.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));''',
'''        try{tab.web.onResume();}catch(Exception ignored){}
        browserHost.removeAllViews();
        if(HOME.equals(tab.url))showHomepage();
        else browserHost.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));''',"tab selection homepage route")
one('''        home.setOnClickListener(v->{hideKeyboard();if(selected!=null){selected.url=HOME;selected.web.loadUrl(HOME);showHomepage();}});''',
'''        home.setOnClickListener(v->{hideKeyboard();goHome();});''',"top home button")
one('''                if(selected!=null&&selected.web!=null){
                    selected.url=HOME;selected.web.loadUrl(HOME);showHomepage();
                }''',
'''                goHome();''',"bottom home button")
one('''    private void focusBrowserSearch(){''',
'''    private void goHome(){
        if(selected==null||selected.web==null)return;
        if(pinnedTabUrls.containsKey(selected)){
            toast("Desative o bloqueio de saída desta página para voltar à Home.");
            return;
        }
        selected.web.stopLoading();
        selected.url=HOME;
        selected.title="Página inicial";
        selected.web.loadUrl(HOME);
        showHomepage();
    }
    private void focusBrowserSearch(){''',"single safe homepage command")

# Popups use the same actual drag-to-dismiss gesture with physical movement.
# Swipe from the grip OR pull the scroll view down while already at scroll=0.
sheet=r'''
    /** Moves the entire PopupWindow decor during a pull-down; settles or closes. */
    private void moveSheet(PopupWindow popup,float distance){
        if(popup==null||popup.getContentView()==null)return;
        View decor=popup.getContentView().getRootView();
        if(decor!=null)decor.setTranslationY(Math.max(0,distance));
    }
    private void finishSheetDrag(PopupWindow popup,float distance){
        if(popup==null||popup.getContentView()==null)return;
        View decor=popup.getContentView().getRootView();
        if(decor==null)return;
        if(distance>d(95)){
            decor.animate().cancel();
            decor.animate().translationY(Math.max(decor.getHeight(),d(700)))
                .setDuration(210)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(()->{decor.setTranslationY(0);popup.dismiss();})
                .start();
        }else{
            decor.animate().cancel();
            decor.animate().translationY(0).setDuration(170).start();
        }
    }
    private class DismissibleSheetScroll extends ScrollView{
        final PopupWindow target;
        float initialY=0f,travel=0f;
        boolean eligible=false;
        DismissibleSheetScroll(PopupWindow popup){
            super(MainActivity.this);
            target=popup;
            setVerticalScrollBarEnabled(false);
            setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN){
                initialY=event.getRawY();travel=0;
                eligible=getScrollY()==0;
            }else if(action==android.view.MotionEvent.ACTION_MOVE && eligible){
                travel=Math.max(0,event.getRawY()-initialY);
                if(travel>d(12))return true;
            }
            return super.onInterceptTouchEvent(event);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent event){
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN){
                initialY=event.getRawY();travel=0;
                eligible=getScrollY()==0;
            }else if(action==android.view.MotionEvent.ACTION_MOVE && eligible){
                travel=Math.max(0,event.getRawY()-initialY);
                if(travel>d(8)){
                    moveSheet(target,travel);
                    return true;
                }
            }else if(action==android.view.MotionEvent.ACTION_UP && eligible && travel>d(8)){
                finishSheetDrag(target,travel);
                eligible=false;travel=0;
                return true;
            }else if(action==android.view.MotionEvent.ACTION_CANCEL && travel>d(0)){
                finishSheetDrag(target,0);
                eligible=false;travel=0;
            }
            return super.onTouchEvent(event);
        }
    }
    private void addSheetGrip(LinearLayout box,PopupWindow popup){
        FrameLayout touch=new FrameLayout(this);
        View pill=new View(this);
        pill.setBackground(Styles.bg(this,0xFF707367,8));
        FrameLayout.LayoutParams pillParams=new FrameLayout.LayoutParams(d(44),d(4),Gravity.CENTER);
        touch.addView(pill,pillParams);
        LinearLayout.LayoutParams whole=new LinearLayout.LayoutParams(-1,d(37));
        whole.bottomMargin=d(5);
        box.addView(touch,whole);
        touch.setOnTouchListener(new View.OnTouchListener(){
            private float firstY,delta;
            @Override public boolean onTouch(View view,android.view.MotionEvent event){
                int action=event.getActionMasked();
                if(action==android.view.MotionEvent.ACTION_DOWN){
                    firstY=event.getRawY();delta=0;return true;
                }
                if(action==android.view.MotionEvent.ACTION_MOVE){
                    delta=Math.max(0,event.getRawY()-firstY);
                    moveSheet(popup,delta);
                    return true;
                }
                if(action==android.view.MotionEvent.ACTION_UP){
                    finishSheetDrag(popup,delta);
                    return true;
                }
                if(action==android.view.MotionEvent.ACTION_CANCEL){
                    finishSheetDrag(popup,0);
                    return true;
                }
                return true;
            }
        });
    }

'''
one('''    private void showMenu(){''',sheet+'''    private void showMenu(){''',"shared animated bottom-sheet swipe helper")

# Remove advanced-tools popup: it only duplicated the user dashboard.
# Keep every unique action in the one visible user menu instead.
start=s.index('''    private void showAdvancedToolsMenu(){''')
end=s.index('''    /** Main user profile hub, inspired by Opera navigation organization. */''',start)
old_advanced=s[start:end]
required=[
 'this::showCrashDiagnostics','this::showAutoScanStatus',
 'this::openDemoPlayer','this::castCurrentPlayback',
 'this::showExportOptions','this::showCrawlExports',
 'this::toggleDesktop','store.clearHistory()',
 'Informações do bloqueio de página','Adicionar aos favoritos'
]
for required_action in required:
    if required_action not in old_advanced:raise RuntimeError("Advanced function lost: "+required_action)
s=s[:start]+s[end:]

# Replace broken immediate-dismiss ScrollView with reusable gesture sheet.
start=s.index('''    private void showMenu(){''')
end=s.index('''    private void togglePopupProtection(){''',start)
menu=s[start:end]
opening='''        ScrollView scroll=new ScrollView(this){
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
        };'''
if opening not in menu:raise RuntimeError("menu gesture old block missing")
menu=menu.replace(opening,'''        ScrollView scroll=new DismissibleSheetScroll(popup);''',1)
grab_start=menu.index('''        View grab=new View(this);''')
grab_end=menu.index('''        LinearLayout header=Styles.row(this);''',grab_start)
menu=menu[:grab_start]+'''        addSheetGrip(box,popup);

'''+menu[grab_end:]
# Header keeps the only Appearance shortcut; remove duplicate preference row.
menu=menu.replace('''        profileWide(box,"☷","Personalizar aparência",this::showAppearance,popup);
''',"")
# Remove duplicate VistaStream row; retain live detector tile.
menu=menu.replace('''        profileWide(box,"▶","Ligar e abrir VistaStream",this::showVistaStreamConnection,popup);
''',"")
# Take Status from above shortcuts, move to after Explore & Playback, preserving exact callbacks.
begin=menu.index('''        TextView toolsHeading=Styles.text(this,"PROTEÇÃO E REPRODUÇÃO"''')
finish=menu.index('''        menuHeading(box,"Os seus atalhos");''',begin)
status=menu[begin:finish]
menu=menu[:begin]+menu[finish:]
placement='''        box.addView(fifth);
'''
if menu.count(placement)!=1:raise RuntimeError("end of Explore not found")
menu=menu.replace(placement,placement+"\n"+status,1)
# Replace Advanced Tools with actual unique options grouped by purpose.
advLink='''        profileWide(box,"⋮","Ferramentas avançadas",this::showAdvancedToolsMenu,popup);
'''
if advLink not in menu:raise RuntimeError("advanced shortcut not found")
menu=menu.replace(advLink,"",1)
point='''        menuHeading(box,"Preferências");'''
if menu.count(point)!=1:raise RuntimeError("preferences heading not found")
new_features=r'''        menuHeading(box,"Ações da página");
        profileWide(box,"＋","Nova aba",()->newTab(false,HOME),popup);
        profileWide(box,"☆","Guardar página nos favoritos",()->{
            if(selected!=null && !HOME.equals(selected.url)){
                store.bookmark(selected.url,selected.title);
                toast("Página adicionada aos favoritos");
            }else toast("Abra um site para guardar nos favoritos.");
        },popup);
        profileWide(box,"⇧","Partilhar página",this::shareCurrentPage,popup);
        profileWide(box,"▣","Modo desktop / móvel",this::toggleDesktop,popup);
        profileWide(box,"◈","Informações do bloqueio de página",this::explainPageLock,popup);

        menuHeading(box,"Scanner e diagnósticos");
        profileWide(box,"◎","Estado do Auto Player Scanner",this::showAutoScanStatus,popup);
        profileWide(box,"⚠","Diagnóstico de falhas",this::showCrashDiagnostics,popup);
        profileWide(box,"▷","Testar player nativo",this::openDemoPlayer,popup);
        profileWide(box,"▶","Transmitir vídeo atual para TV",this::castCurrentPlayback,popup);

        menuHeading(box,"Exportação e dados");
        profileWide(box,"⇧","Exportar biblioteca",this::showExportOptions,popup);
        profileWide(box,"▣","Exportar rastreio ZIP",this::showCrawlExports,popup);
        profileWide(box,"⌫","Limpar histórico",this::confirmClearHistory,popup);

'''
menu=menu.replace(point,new_features+point,1)
s=s[:start]+menu+s[end:]

# Reuse actual existing workflows for advanced functions without a second popup.
extra=r'''
    private void shareCurrentPage(){
        if(selected==null || HOME.equals(selected.url)){
            toast("Abra uma página para partilhar.");
            return;
        }
        Intent share=new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,selected.url);
        startActivity(Intent.createChooser(share,"Partilhar página"));
    }
    private void explainPageLock(){
        new AlertDialog.Builder(this)
            .setTitle("Bloqueio de saída da página")
            .setMessage("Mantém a aba no endereço atual e impede redirecionamentos e mudanças de rota. Os vídeos e recursos carregados dentro da página continuam a funcionar. Ative ou desative nos três pontos superiores.")
            .setPositiveButton("Entendi",null).show();
    }
    private void confirmClearHistory(){
        new AlertDialog.Builder(this)
            .setTitle("Limpar histórico?")
            .setMessage("Eliminar o histórico local do VistaBrowser?")
            .setPositiveButton("Limpar",(d,w)->{
                store.clearHistory();
                showAddresses("history");
            })
            .setNegativeButton("Cancelar",null).show();
    }
'''
one('''    private void togglePopupProtection(){''',extra+'''    private void togglePopupProtection(){''',"advanced unique actions relocated")

# The top technical toggles also get drag-to-close with a moving surface.
first=s.index('''    private void showQuickActions(){''')
last=s.index('''    /** Main user profile hub, inspired by Opera navigation organization. */''',first)
# shared helper placed after top quick, so this slice includes helper and other methods.
old_top='''        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);'''
if s[first:first+900].count(old_top)!=1:raise RuntimeError("top control sheet scroll not found")
s=s[:first]+s[first:].replace(old_top,'''        ScrollView scroll=new DismissibleSheetScroll(popup);
        scroll.setFillViewport(false);''',1)
old_grab='''        View grab=new View(this);
        grab.setBackground(Styles.bg(this,0xFF707367,8));
        LinearLayout.LayoutParams grip=new LinearLayout.LayoutParams(d(40),d(4));
        grip.gravity=Gravity.CENTER_HORIZONTAL;
        grip.bottomMargin=d(15);
        box.addView(grab,grip);'''
one(old_grab,'''        addSheetGrip(box,popup);''',"top toggles drag handle")

# Invariants: one callback per intent, all original distinct capabilities kept.
assert s.count('private void showMenu(){')==1
assert 'private void showAdvancedToolsMenu(){' not in s
assert 'this::showAdvancedToolsMenu' not in s
s_menu=s[s.index('    private void showMenu(){'):s.index('    private void togglePopupProtection(){')]
for title in ("Favoritos","Transferências","Histórico","Biblioteca",
              "Separadores","Aba privada","Vídeos encontrados","Inspetor web",
              "Crawler","Reprodutor / Cast","Proteção","Scanner","VistaStream",
              "Nova aba","Guardar página nos favoritos","Modo desktop / móvel",
              "Estado do Auto Player Scanner","Diagnóstico de falhas",
              "Testar player nativo","Transmitir vídeo atual para TV",
              "Exportar biblioteca","Exportar rastreio ZIP","Limpar histórico"):
    assert title in s_menu,title
assert s_menu.count('"Personalizar aparência"')==1
assert s_menu.count('this::showVistaStreamConnection')==1
assert s_menu.index('"Explorar e reproduzir"')<s_menu.index('"PROTEÇÃO E REPRODUÇÃO"')
assert 'new DismissibleSheetScroll(popup)' in s_menu
assert 'new DismissibleSheetScroll(popup)' in s[s.index('private void showQuickActions(){'):s.index('private void showMenu(){')]
assert 'if(HOME.equals(tab.url))showHomepage();' in s
assert 'if(HOME.equals(link)){' in s
for method in ("private void showDetected(){","private void showLibrary(){",
               "private void showInspector(){","private void showCrawler(){",
               "private boolean sendToVistaStream(","private void showQuickActions(){",
               "private void openNativePlayer(","private void showTabOverview(){",
               "private void showSearchOverlay(","private void showAppearance(){",
               "VistaBrowserUpdates.check(this,false)"):
    assert method in s,method
main.write_text(s,encoding="utf-8")
g=gradle.read_text(encoding="utf-8")
assert "versionName '3.6.0'" in g and "versionCode 27" in g
gradle.write_text(g.replace("versionName '3.6.0'","versionName '3.7.0'").replace("versionCode 27","versionCode 28"),encoding="utf-8")
print("VistaBrowser 3.7: restored native Home, unique features in one menu, relocated status section, animated sheets")
