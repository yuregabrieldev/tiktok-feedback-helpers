from pathlib import Path
root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
gradle=root/"app/build.gradle"
s=main.read_text(encoding="utf-8")

def once(before,after,label):
    global s
    n=s.count(before)
    if n!=1:raise AssertionError(f"{label}: expected one anchor, found {n}")
    s=s.replace(before,after,1)

# Insets on the Activity-owned FrameLayout overlays were missing because
# the main root alone applied system bar padding.
once('''    private FrameLayout appFrame;''',
'''    private FrameLayout appFrame;
    private int safeStatusTop=0, safeNavigationBottom=0;''',"safe area fields")

once('''            root.setPadding(0,topInset,0,bottomInset);
            keyboardVisible=showing;''',
'''            safeStatusTop=topInset;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets nav=insets.getInsets(WindowInsets.Type.navigationBars());
                safeNavigationBottom=nav.bottom;
            }else{
                safeNavigationBottom=insets.getSystemWindowInsetBottom();
            }
            root.setPadding(0,topInset,0,bottomInset);
            if(activePage!=null)applyOverlaySafeArea(activePage);
            keyboardVisible=showing;''',"insets propagation")

once('''    private void setupUi() {''',
'''    private int safeTop(){return Math.max(safeStatusTop,d(24));}
    private void applyOverlaySafeArea(View overlay){
        if(overlay==null)return;
        // appFrame overlays are outside root: apply both system insets here.
        overlay.setPadding(0,safeTop()+d(13),0,Math.max(0,safeNavigationBottom));
    }
    private void setupUi() {''',"overlay helper")

once('''        toolbar.setPadding(d(10),d(5),d(8),d(5));''',
'''        toolbar.setPadding(d(10),d(10),d(8),d(6));''',"toolbar breathing room")
once('''        root.addView(toolbar,new LinearLayout.LayoutParams(-1,d(58)));''',
'''        root.addView(toolbar,new LinearLayout.LayoutParams(-1,d(64)));''',"toolbar height")
once('''        menu.setOnClickListener(v->{hideKeyboard();showMenu();});''',
'''        // Upper three-dots owns scanner settings, page controls and automation.
        menu.setOnClickListener(v->{hideKeyboard();showQuickActions();});''',"top dots route")

# Dedicated internal screens, including TabOverview and Library, now all receive
# the same system insets: header never overlaps status indicators.
once('''        appFrame.addView(page,fp);activePage=page;
        LinearLayout header=Styles.row(this);''',
'''        appFrame.addView(page,fp);activePage=page;
        applyOverlaySafeArea(page);
        LinearLayout header=Styles.row(this);''',"internal screens inset")
once('''        page.setBackgroundColor(0xFF171915);
        appFrame.addView(page,new FrameLayout.LayoutParams(-1,-1));
        activePage=page;''',
'''        page.setBackgroundColor(0xFF171915);
        appFrame.addView(page,new FrameLayout.LayoutParams(-1,-1));
        activePage=page;
        applyOverlaySafeArea(page);''',"search screen inset")
once('''        home.setPadding(d(23),d(17),d(23),d(28));''',
'''        home.setPadding(d(23),d(23),d(23),d(28));''',"home breathing room")

# Navigation and interactive technical switches are still implemented in the
# original activity. Their actions and preferences must not be modified.
once('''        row.setPadding(d(14),d(10),d(13),d(10));
        row.setBackground(Styles.touch(this,0xFF202B3C,16));''',
'''        row.setPadding(d(15),d(7),d(13),d(7));
        row.setBackground(Styles.touch(this,0xFF272923,16));''',"compact original toggle rows") if '''        row.setPadding(d(14),d(10),d(13),d(10));
        row.setBackground(Styles.touch(this,0xFF202B3C,16));''' in s else None
# Blue remnants in the menu theme only, preserving original feature code.
once('''        box.setBackground(Styles.stroke(this,0xFA151C29,24,0xFF56594D));''',
'''        box.setBackground(Styles.stroke(this,0xFF1B1D19,24,0xFF4B4E45));''',"technical surface")
once('''        popup.setBackgroundDrawable(Styles.bg(this,0xFA151C29,24));''',
'''        popup.setBackgroundDrawable(Styles.bg(this,0xFF1B1D19,24));''',"technical popup bg")

once('''        menuHeading(box,"Ações rápidas");
        menuEntry(box,"⋮","Menu principal",this::showMenu,popup);''',
'''        menuHeading(box,"Controlos de página");
        menuEntry(box,"⟳","Atualizar página",()->{
            if(selected!=null&&selected.web!=null&&!HOME.equals(selected.url))selected.web.reload();
        },popup);
        menuEntry(box,"▣","Informações do bloqueio de página",()->{
            new AlertDialog.Builder(this)
                .setTitle("Bloquear saída da página")
                .setMessage("Impede redirecionamentos e mudanças de URL na aba atual. A reprodução e os pedidos do vídeo continuam autorizados.")
                .setPositiveButton("OK",null).show();
        },popup);
        menuHeading(box,"Ferramentas");
        menuEntry(box,"⋮","Todas as ferramentas",this::showAdvancedToolsMenu,popup);''',"top technical menu entry")

once('''        autoToggle(box,"Bloquear saída da página atual",''',
'''        menuHeading(box,"Proteção e automação");
        autoToggle(box,"Bloquear saída da página atual",''',"toggle section")
once('''        menuEntry(box,"◈","Autorizar interações neste domínio",
            this::toggleHostPermission,popup);
        // Keep the sheet above system gesture navigation and away from the keyboard.
        popup.showAtLocation(root,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,d(65));''',
'''        menuEntry(box,"◈","Autorizar interações neste domínio",
            this::toggleHostPermission,popup);
        popup.setHeight(Math.min(d(780),(int)(getResources().getDisplayMetrics().heightPixels*.77)));
        // Anchor below the upper three-dot menu and let the settings scroll.
        popup.showAtLocation(appFrame,Gravity.TOP|Gravity.RIGHT,d(10),safeTop()+d(69));''',"upper dots technical popup location")

# Preserve original long list of advanced actions behind an explicit technical
# entry, while a NEW, compact profile sheet becomes the bottom user button.
once('''    private void showMenu(){
        final PopupWindow popup=new PopupWindow(this);''',
'''    private void showAdvancedToolsMenu(){
        final PopupWindow popup=new PopupWindow(this);''',"rename existing menu")

# Rename the advanced heading, remove three redundant navigation actions
# (still present in the bottom bar), and keep all other callbacks reachable.
start=s.index('''    private void showAdvancedToolsMenu(){''')
end=s.index('''    private void toggleDesktop(){''',start)
advanced=s[start:end]
advanced=advanced.replace('''        menuHeading(box,"Navegação");''','''        menuHeading(box,"Ferramentas avançadas");''',1)
for item in [
'''        menuEntry(box,"‹","Voltar",()->{
            if(selected!=null&&selected.web!=null&&selected.web.canGoBack())selected.web.goBack();
        },popup);
''',
'''        menuEntry(box,"›","Avançar",()->{
            if(selected!=null&&selected.web!=null&&selected.web.canGoForward())selected.web.goForward();
        },popup);
''',
'''        menuEntry(box,"⌂","Página inicial",()->{
            if(selected!=null&&selected.web!=null){selected.url=HOME;selected.web.loadUrl(HOME);showHomepage();}
        },popup);
'''
]:
    if item not in advanced:raise AssertionError("advanced menu navigation signature missing")
    advanced=advanced.replace(item,"",1)
advanced=advanced.replace('''        popup.showAtLocation(root,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,d(22));''',
'''        popup.showAtLocation(appFrame,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,0);''',1)
s=s[:start]+advanced+s[end:]

# Profile shortcut grid follows an Opera-style 2-column layout. Actions are
# delegated to existing methods, without introducing dummy toggles or menus.
profile=r'''
    /** Main user profile hub, inspired by Opera navigation organization. */
    private void showMenu(){
        final PopupWindow popup=new PopupWindow(this);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box=Styles.column(this);
        box.setPadding(d(17),d(18),d(17),d(30));
        box.setBackgroundColor(0xFF191B17);
        scroll.addView(box);
        popup.setContentView(scroll);
        popup.setWidth(-1);
        popup.setHeight(Math.min(getResources().getDisplayMetrics().heightPixels-d(75),
            (int)(getResources().getDisplayMetrics().heightPixels*.89f)));
        android.graphics.drawable.GradientDrawable backdrop=
            new android.graphics.drawable.GradientDrawable();
        backdrop.setColor(0xFF191B17);
        float r=d(30);
        backdrop.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});
        popup.setBackgroundDrawable(backdrop);
        popup.setElevation(d(19));
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);

        View grab=new View(this);
        grab.setBackground(Styles.bg(this,0xFF6B6C60,8));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(d(42),d(4));
        gp.gravity=Gravity.CENTER_HORIZONTAL;
        gp.bottomMargin=d(21);
        box.addView(grab,gp);

        LinearLayout header=Styles.row(this);
        header.setPadding(d(16),d(18),d(12),d(18));
        header.setBackground(Styles.bg(this,0xFF2A2D27,24));
        TextView avatar=Styles.text(this,"♙",30,true,Styles.TEXT);
        Styles.glyph(avatar,"♙",Styles.TEXT,28);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Styles.bg(this,0xFF41443B,25));
        header.addView(avatar,new LinearLayout.LayoutParams(d(58),d(58)));
        LinearLayout heading=Styles.column(this);
        heading.setPadding(d(14),d(2),0,0);
        TextView title=Styles.text(this,"VistaBrowser",20,true,Styles.TEXT);
        heading.addView(title);
        TextView subtitle=Styles.text(this,"O seu espaço de navegação",12,false,Styles.MUTED);
        subtitle.setPadding(0,d(6),0,0);
        heading.addView(subtitle);
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=Styles.text(this,"☷",24,false,Styles.TEXT);
        Styles.glyph(settings,"☷",Styles.TEXT,23);
        settings.setGravity(Gravity.CENTER);
        settings.setContentDescription("Personalizar aparência");
        header.addView(settings,new LinearLayout.LayoutParams(d(43),d(51)));
        settings.setOnClickListener(v->{popup.dismiss();showAppearance();});
        box.addView(header);

        TextView toolsHeading=Styles.text(this,"PROTEÇÃO E REPRODUÇÃO",11,true,Styles.MUTED);
        toolsHeading.setLetterSpacing(.12f);
        toolsHeading.setPadding(d(5),d(25),0,d(10));
        box.addView(toolsHeading);

        LinearLayout statusRow=Styles.row(this);
        profileStatus(statusRow,"◈","Proteção",
            popupBlocked?"Ativada":"Desativada",popup,
            this::showQuickActions);
        profileStatus(statusRow,"◎","Scanner",
            autoScanEnabled?"Ativo":"Inativo",popup,
            this::showQuickActions);
        profileStatus(statusRow,"▶","VistaStream",
            autoOpenPlayerOnPlay?"Ligado":"Desligado",popup,
            this::showQuickActions);
        box.addView(statusRow);

        menuHeading(box,"Os seus atalhos");
        LinearLayout first=Styles.row(this);
        profileTile(first,"☆","Favoritos",()->showAddresses("bookmarks"),popup);
        profileTile(first,"⇩","Transferências",this::showDownloads,popup);
        box.addView(first);
        LinearLayout second=Styles.row(this);
        profileTile(second,"◷","Histórico",()->showAddresses("history"),popup);
        profileTile(second,"▤","Biblioteca",this::showLibrary,popup);
        box.addView(second);
        LinearLayout third=Styles.row(this);
        profileTile(third,"▣","Separadores",this::showTabOverview,popup);
        profileTile(third,"◌","Aba privada",()->newTab(true,HOME),popup);
        box.addView(third);

        menuHeading(box,"Explorar e reproduzir");
        LinearLayout fourth=Styles.row(this);
        profileTile(fourth,"▶","Vídeos encontrados",this::showDetected,popup);
        profileTile(fourth,"⌕","Inspetor web",this::showInspector,popup);
        box.addView(fourth);
        LinearLayout fifth=Styles.row(this);
        profileTile(fifth,"◎","Crawler",this::showCrawler,popup);
        profileTile(fifth,"▣","Reprodutor / Cast",this::openPlayerCastHub,popup);
        box.addView(fifth);

        menuHeading(box,"Preferências");
        profileWide(box,"☷","Personalizar aparência",this::showAppearance,popup);
        profileWide(box,"⇧","Verificar atualizações",
            ()->VistaBrowserUpdates.check(this,true),popup);
        profileWide(box,"⋮","Ferramentas avançadas",this::showAdvancedToolsMenu,popup);
        profileWide(box,"ⓘ","Acerca do VistaBrowser",()->new AlertDialog.Builder(this)
            .setTitle("VistaBrowser")
            .setMessage("Navegador com inspeção web, deteção de vídeos, biblioteca, Chromecast e integração VistaStream.")
            .setPositiveButton("OK",null).show(),popup);
        popup.showAtLocation(appFrame,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,0);
    }
    private void profileStatus(LinearLayout row,String glyph,String label,String state,
                               PopupWindow sheet,Runnable callback){
        LinearLayout tile=Styles.column(this);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(d(4),d(12),d(4),d(12));
        tile.setBackground(Styles.bg(this,0xFF292C26,22));
        TextView icon=Styles.text(this,glyph,23,false,Styles.ACCENT);
        Styles.glyph(icon,glyph,Styles.ACCENT,24);
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon,new LinearLayout.LayoutParams(-1,d(38)));
        TextView name=Styles.text(this,label,12,true,Styles.TEXT);
        name.setGravity(Gravity.CENTER);
        tile.addView(name);
        TextView status=Styles.text(this,state,11,false,Styles.MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,d(5),0,0);
        tile.addView(status);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,d(117),1f);
        params.setMargins(d(3),0,d(3),0);
        row.addView(tile,params);
        tile.setOnClickListener(v->{sheet.dismiss();callback.run();});
    }
    private void profileTile(LinearLayout row,String symbol,String title,
                             Runnable action,PopupWindow sheet){
        LinearLayout card=Styles.row(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(d(15),d(12),d(8),d(12));
        card.setBackground(Styles.bg(this,0xFF292C26,21));
        TextView icon=Styles.text(this,symbol,23,false,Styles.TEXT);
        Styles.glyph(icon,symbol,Styles.TEXT,23);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon,new LinearLayout.LayoutParams(d(40),d(47)));
        TextView name=Styles.text(this,title,14,true,Styles.TEXT);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(d(5),0,0,0);
        card.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,d(83),1f);
        params.setMargins(d(3),d(5),d(3),d(5));
        row.addView(card,params);
        card.setOnClickListener(v->{sheet.dismiss();action.run();});
    }
    private void profileWide(LinearLayout box,String glyph,String title,
                             Runnable action,PopupWindow popup){
        LinearLayout row=Styles.row(this);
        row.setPadding(d(14),d(8),d(10),d(8));
        row.setBackground(Styles.bg(this,0xFF262923,17));
        TextView symbol=Styles.text(this,glyph,20,false,Styles.ACCENT);
        Styles.glyph(symbol,glyph,Styles.ACCENT,21);
        symbol.setGravity(Gravity.CENTER);
        row.addView(symbol,new LinearLayout.LayoutParams(d(40),d(40)));
        TextView label=Styles.text(this,title,14,true,Styles.TEXT);
        row.addView(label,new LinearLayout.LayoutParams(0,d(47),1f));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(d(3),d(4),d(3),d(4));
        box.addView(row,p);
        row.setOnClickListener(v->{popup.dismiss();action.run();});
    }
'''
once('''    private void toggleDesktop(){''',profile+'''    private void toggleDesktop(){''',"insert profile dashboard")

# Back/page buttons remain in the bottom navigation, not duplicated in profile.
# Menu invocation on the bottom profile glyph remains showMenu().
assert 'navButton(bottomNav,"♙",()->{' in s
assert 'hideKeyboard();showMenu();' in s
assert 'menu.setOnClickListener(v->{hideKeyboard();showQuickActions();});' in s
# Runtime invariants from previous working versions.
for marker in (
'private void showDetected(){','private void showLibrary(){',
'private void showInspector(){','private void showCrawler(){',
'private boolean sendToVistaStream(','private void openNativePlayer(',
'private void onNativePlaybackRequested(','private void capturePage(',
'private void download(','private void setPageLockForCurrentTab(',
'VistaBrowserUpdates.check(this,false)','private void showQuickActions(){',
'private void showTabOverview(){','private void renderTabCards(',
'private void autoToggle(','private void showMenu(){',
'private void showAddresses(','private void showSearchOverlay(',
'private void showAppearance(){','private void showAdvancedToolsMenu(){',
):
    assert marker in s,marker
for toggle in ('Bloquear saída da página atual','Abrir VistaStream ao iniciar um vídeo',
 'Analisar páginas automaticamente','Interações em domínios autorizados',
 'Bloquear pop-ups','Autorizar interações neste domínio'):
    assert toggle in s,toggle
main.write_text(s,encoding="utf-8")
g=gradle.read_text(encoding="utf-8")
assert "versionName '3.3.0'" in g and "versionCode 24" in g
g=g.replace("versionName '3.3.0'","versionName '3.4.0'").replace("versionCode 24","versionCode 25")
gradle.write_text(g,encoding="utf-8")
print("VistaBrowser 3.4: restored top technical settings, clean Opera-style profile hub, fixed bottom sheet and overlay insets")
