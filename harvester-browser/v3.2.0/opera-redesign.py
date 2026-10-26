from pathlib import Path
import re

root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
styles_path=root/"app/src/main/java/com/harvester/browser/ui/Styles.java"
gradle=root/"app/build.gradle"
s=main.read_text(encoding="utf-8")

def once(old,new,label):
    global s
    count=s.count(old)
    if count!=1:raise RuntimeError(f"{label}: expected one match, got {count}")
    s=s.replace(old,new,1)

def section(first,last,replacement,label):
    global s
    a=s.index(first)
    b=s.index(last,a)
    if a<0 or b<=a:raise RuntimeError(f"{label}: missing boundaries")
    s=s[:a]+replacement+s[b:]

# All existing core services, media detection, browsing callbacks and storage
# stay in place. Only chrome, menus, tabs, panels and homepage are refreshed.
once('''    private LinearLayout root, browserControls;''',
'''    private LinearLayout root, browserControls, browserToolbar;
    private boolean privateOverviewMode=false;
    private String tabOverviewFilter="";''',"ui state")

once('''        LinearLayout toolbar=Styles.row(this);
        toolbar.setPadding(d(8),d(8),d(8),d(8));''',
'''        LinearLayout toolbar=Styles.row(this);
        browserToolbar=toolbar;
        toolbar.setPadding(d(10),d(5),d(8),d(5));''',"toolbar ref")
once('''        toolbar.addView(home,new LinearLayout.LayoutParams(d(40),d(46)));''',
'''        toolbar.addView(home,new LinearLayout.LayoutParams(d(40),d(46)));
        home.setVisibility(View.GONE);''',"home icon moved to navigation")
once('''        toolbar.addView(plus,new LinearLayout.LayoutParams(d(42),d(46)));''',
'''        toolbar.addView(plus,new LinearLayout.LayoutParams(d(42),d(46)));
        plus.setVisibility(View.GONE);''',"plus icon moved to tabs/menu")
once('''        toolbar.addView(tabCounter,tabsParams);''',
'''        toolbar.addView(tabCounter,tabsParams);
        tabCounter.setVisibility(View.GONE);''',"tabs moved to bottom")
once('''        root.addView(toolbar,new LinearLayout.LayoutParams(-1,d(62)));''',
'''        root.addView(toolbar,new LinearLayout.LayoutParams(-1,d(58)));''',"address toolbar size")
once('''        TextView siteInfo=Styles.text(this,"☷",24,false,Styles.ACCENT);''',
'''        TextView siteInfo=Styles.text(this,"◈",24,false,Styles.ACCENT);''',"site information")
once('''        Styles.glyph(siteInfo,"☷",Styles.ACCENT,20);''',
'''        Styles.glyph(siteInfo,"◈",Styles.ACCENT,20);''',"site icon vector")

bottom_first='''        navButton(nav,"‹",()->{hideKeyboard();if(selected!=null&&selected.web.canGoBack())selected.web.goBack();});'''
bottom_last='''        browserControls.addView(nav);root.addView(browserControls);'''
assert bottom_first in s and bottom_last in s
b=s.index(bottom_first);e=s.index(bottom_last,b)
s=s[:b]+'''        navButton(nav,"‹",()->{hideKeyboard();if(selected!=null&&selected.web!=null&&selected.web.canGoBack())selected.web.goBack();});
        navButton(nav,"›",()->{hideKeyboard();if(selected!=null&&selected.web!=null&&selected.web.canGoForward())selected.web.goForward();});
        TextView searchButton=navButton(nav,"⌕",this::focusBrowserSearch);
        searchButton.setContentDescription("Pesquisar ou introduzir endereço");
        TextView tabsButton=navButton(nav,"▣",()->{hideKeyboard();showTabOverview();});
        tabsButton.setContentDescription("Abrir separadores");
        TextView menuButton=navButton(nav,"☷",()->{hideKeyboard();showQuickActions();});
        menuButton.setContentDescription("Abrir ações e controlos do VistaBrowser");
'''+s[e:]
once('''    private void hideKeyboard(){''',
'''    private void focusBrowserSearch(){
        if(browserToolbar!=null)browserToolbar.setVisibility(View.VISIBLE);
        if(address==null)return;
        address.requestFocus();
        address.selectAll();
        address.post(()->{
            InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.showSoftInput(address,InputMethodManager.SHOW_IMPLICIT);
        });
    }
    private void hideKeyboard(){''',"search navigation shortcut")
once('''        tabCounter.setText(tabs.size()>99?"99+":String.valueOf(tabs.size()));
        progressIndicator.setText(selected.progress>=100?"":("A carregar • "+selected.progress+"%"));''',
'''        tabCounter.setText(tabs.size()>99?"99+":String.valueOf(tabs.size()));
        progressIndicator.setText(selected.progress>=100?"":("A carregar • "+selected.progress+"%"));
        if(browserToolbar!=null)browserToolbar.setVisibility(
            HOME.equals(selected.url) && !address.hasFocus()?View.GONE:View.VISIBLE);''',"toolbar home behavior")

home=r'''    /** Opera-inspired start page with original branding and local speed-dial bookmarks. */
    private void showHomepage(){
        if(selected==null||!HOME.equals(selected.url))return;
        browserHost.removeAllViews();
        if(browserToolbar!=null)browserToolbar.setVisibility(View.GONE);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout home=Styles.column(this);
        home.setPadding(d(23),d(17),d(23),d(28));
        home.setBackground(Styles.gradient(this,0xFF3D3930,Styles.BG,0));
        scroll.addView(home);

        LinearLayout top=Styles.row(this);
        TextView identity=Styles.text(this,"◈   VISTABROWSER",12,true,0xFFE9E6D8);
        identity.setLetterSpacing(.13f);
        top.addView(identity,new LinearLayout.LayoutParams(0,d(48),1));
        TextView settings=Styles.text(this,"☷",24,false,Styles.TEXT);
        Styles.glyph(settings,"☷",Styles.TEXT,25);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(Styles.touch(this,0x302F3029,24));
        top.addView(settings,new LinearLayout.LayoutParams(d(48),d(48)));
        settings.setContentDescription("Ações rápidas");
        settings.setOnClickListener(v->showQuickActions());
        home.addView(top);

        TextView wordmark=Styles.text(this,"VistaBrowser",43,false,0xFFF4F2E8);
        wordmark.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        wordmark.setLetterSpacing(-.025f);
        wordmark.setGravity(Gravity.CENTER);
        wordmark.setPadding(0,d(62),0,d(25));
        home.addView(wordmark);

        LinearLayout searchPill=Styles.row(this);
        searchPill.setPadding(d(18),d(5),d(10),d(5));
        searchPill.setBackground(Styles.stroke(this,0xED171917,30,0x364A4A42));
        searchPill.setElevation(d(7));
        TextView glass=Styles.text(this,"⌕",24,false,Styles.ACCENT);
        Styles.glyph(glass,"⌕",Styles.ACCENT,22);
        searchPill.addView(glass,new LinearLayout.LayoutParams(d(38),d(50)));
        EditText homeSearch=new EditText(this);
        homeSearch.setSingleLine(true);
        homeSearch.setTextColor(Styles.TEXT);
        homeSearch.setHintTextColor(0xFFB3B3AA);
        homeSearch.setTextSize(16);
        homeSearch.setHint("Pesquisar ou introduzir endereço");
        homeSearch.setImeOptions(EditorInfo.IME_ACTION_GO);
        homeSearch.setBackgroundColor(Color.TRANSPARENT);
        homeSearch.setPadding(0,0,d(4),0);
        searchPill.addView(homeSearch,new LinearLayout.LayoutParams(0,d(52),1));
        TextView searchGo=Styles.text(this,"›",28,true,Styles.TEXT);
        Styles.glyph(searchGo,"›",Styles.TEXT,24);
        searchGo.setGravity(Gravity.CENTER);
        searchPill.addView(searchGo,new LinearLayout.LayoutParams(d(35),d(48)));
        Runnable submit=()->{
            String typed=homeSearch.getText().toString().trim();
            if(typed.isEmpty()){focusBrowserSearch();return;}
            InputMethodManager input=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(input!=null)input.hideSoftInputFromWindow(homeSearch.getWindowToken(),0);
            homeSearch.clearFocus();
            go(typed);
        };
        searchGo.setOnClickListener(v->submit.run());
        homeSearch.setOnEditorActionListener((v,id,event)->{submit.run();return true;});
        home.addView(searchPill,new LinearLayout.LayoutParams(-1,d(62)));

        LinearLayout modes=Styles.row(this);
        homeChip(modes,"◌  Privado",()->newTab(true,HOME));
        homeChip(modes,"▤  Biblioteca",this::showLibrary);
        LinearLayout.LayoutParams modesParams=new LinearLayout.LayoutParams(-1,-2);
        modesParams.topMargin=d(15);home.addView(modes,modesParams);

        TextView shortcutsTitle=Styles.text(this,"Acesso rápido",19,true,Styles.TEXT);
        shortcutsTitle.setPadding(d(2),d(42),0,d(12));
        home.addView(shortcutsTitle);

        List<String[]> entries=new ArrayList<>();
        for(String[] bookmark:store.addressesDetailed("bookmarks")){
            if(bookmark==null||bookmark.length<2||bookmark[1]==null)continue;
            if(!bookmark[1].startsWith("https://"))continue;
            entries.add(new String[]{bookmark[0],bookmark[1]});
            if(entries.size()>=7)break;
        }
        if(entries.isEmpty()){
            entries.add(new String[]{"Google","https://www.google.com"});
            entries.add(new String[]{"YouTube","https://www.youtube.com"});
            entries.add(new String[]{"Wikipedia","https://pt.wikipedia.org"});
            entries.add(new String[]{"GitHub","https://github.com"});
            entries.add(new String[]{"Reddit","https://www.reddit.com"});
        }
        entries.add(new String[]{"+",""});
        int columns=4;
        for(int start=0;start<entries.size();start+=columns){
            LinearLayout row=Styles.row(this);
            for(int c=0;c<columns;c++){
                int index=start+c;
                if(index>=entries.size()){
                    row.addView(new View(this),new LinearLayout.LayoutParams(0,d(101),1));continue;
                }
                String[] item=entries.get(index);
                LinearLayout tile=Styles.column(this);
                tile.setGravity(Gravity.CENTER_HORIZONTAL);
                tile.setPadding(d(2),d(11),d(2),d(9));
                TextView circle=Styles.text(this,"+",24,true,0xFFEEEBDD);
                String initial=item[0].equals("+")?"+":item[0].substring(0,1).toUpperCase(Locale.ROOT);
                circle.setText(initial);
                circle.setGravity(Gravity.CENTER);
                int[] tones={0xFF514C41,0xFF625847,0xFF4A544B,0xFF4E5155};
                circle.setBackground(Styles.stroke(this,tones[index%tones.length],30,0x535B5D51));
                tile.addView(circle,new LinearLayout.LayoutParams(d(56),d(56)));
                TextView name=Styles.text(this,item[0].equals("+")?"Adicionar":item[0],12,false,Styles.TEXT);
                name.setGravity(Gravity.CENTER);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                name.setPadding(d(1),d(8),d(1),0);
                tile.addView(name);
                row.addView(tile,new LinearLayout.LayoutParams(0,d(103),1));
                tile.setOnClickListener(v->{
                    if(item[1].isEmpty())addSpeedDial();
                    else go(item[1]);
                });
            }
            home.addView(row);
        }

        LinearLayout tools=Styles.column(this);
        tools.setPadding(d(18),d(17),d(18),d(18));
        tools.setBackground(Styles.stroke(this,0xE6282925,25,0xFF41423A));
        TextView toolsTitle=Styles.text(this,"As suas ferramentas",18,true,Styles.TEXT);
        tools.addView(toolsTitle);
        TextView description=Styles.text(this,"Descubra vídeos, inspecione páginas ou continue a partir da sua biblioteca.",13,false,Styles.MUTED);
        description.setPadding(0,d(7),0,d(14));description.setLineSpacing(d(2),1f);
        tools.addView(description);
        LinearLayout toolRow=Styles.row(this);
        homeTile(toolRow,"⌕ Inspecionar",this::showInspector);
        homeTile(toolRow,"▶ Vídeos",this::showDetected);
        tools.addView(toolRow);
        LinearLayout toolRow2=Styles.row(this);
        homeTile(toolRow2,"◎ Crawler",this::showCrawler);
        homeTile(toolRow2,"▤ Biblioteca",this::showLibrary);
        tools.addView(toolRow2);
        LinearLayout.LayoutParams toolParams=new LinearLayout.LayoutParams(-1,-2);
        toolParams.topMargin=d(30);home.addView(tools,toolParams);
        TextView foot=Styles.text(this,"VISTABROWSER  •  EXPERIÊNCIA DE NAVEGAÇÃO",10,false,0xFFA6A79D);
        foot.setGravity(Gravity.CENTER);
        foot.setLetterSpacing(.11f);
        foot.setPadding(0,d(34),0,d(15));
        home.addView(foot);
        browserHost.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        synchronizeUi();
    }
    private void addSpeedDial(){
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://exemplo.com");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this)
            .setTitle("Adicionar acesso rápido")
            .setMessage("Guarda um website nos favoritos para aparecer na página inicial.")
            .setView(input)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Adicionar",(dialog,which)->{
                String url=safeUrl(input.getText().toString());
                if(!url.startsWith("https://")){toast("Introduza um website HTTPS válido.");return;}
                String host=Uri.parse(url).getHost();
                if(host==null){toast("Website inválido");return;}
                store.bookmark(url,host.replaceFirst("^www\\.",""));
                showHomepage();
            }).show();
    }
    private void homeChip(LinearLayout row,String title,Runnable action){
        TextView chip=Styles.text(this,title,14,true,Styles.TEXT);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Styles.stroke(this,0xA1242622,24,0xFF41443C));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(52),1);
        p.setMargins(d(3),0,d(3),0);
        row.addView(chip,p);
        chip.setOnClickListener(v->action.run());
    }
    private void homeTile(LinearLayout row,String title,Runnable action){
        LinearLayout tile=Styles.column(this);
        tile.setPadding(d(13),d(11),d(9),d(12));
        tile.setBackground(Styles.touch(this,0xFF34352F,17));
        String glyph=title.substring(0,1);
        String label=title.substring(2);
        TextView ico=Styles.text(this,glyph,22,false,Styles.ACCENT);
        Styles.glyph(ico,glyph,Styles.ACCENT,23);
        tile.addView(ico,new LinearLayout.LayoutParams(d(30),d(28)));
        TextView name=Styles.text(this,label,13,true,Styles.TEXT);
        name.setPadding(0,d(6),0,0);tile.addView(name);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(88),1);
        p.setMargins(d(3),d(3),d(3),d(3));row.addView(tile,p);
        tile.setOnClickListener(v->action.run());
    }
'''
section("    /** Brand-first home screen; original four navigation actions preserved. */",
        "    private void recordResource(",home,"home redesign")

# Main browser menu: bottom sheet, curated top dashboard, navigation section.
once('''        LinearLayout box=Styles.column(this);box.setPadding(d(1),d(6),d(1),d(9));''',
'''        LinearLayout box=Styles.column(this);
        box.setPadding(d(9),d(12),d(9),d(18));''',"main menu padding")
once('''        popup.setWidth(Math.min(d(340),getResources().getDisplayMetrics().widthPixels-d(22)));
        popup.setHeight(Math.min(d(670),(int)(getResources().getDisplayMetrics().heightPixels*.79)));''',
'''        popup.setWidth(Math.min(d(480),getResources().getDisplayMetrics().widthPixels-d(14)));
        popup.setHeight(Math.min(d(800),(int)(getResources().getDisplayMetrics().heightPixels*.81)));''',"menu sheet size")
once('''        menuHeading(box,"Navegação");
        menuEntry(box,"⇧","Verificar atualizações",()->
            VistaBrowserUpdates.check(this,true),popup);''',
'''        LinearLayout summary=Styles.column(this);
        summary.setPadding(d(18),d(16),d(18),d(16));
        summary.setBackground(Styles.stroke(this,0xFF31332C,24,0xFF515348));
        TextView appName=Styles.text(this,"VistaBrowser",23,true,Styles.TEXT);
        summary.addView(appName);
        TextView appSub=Styles.text(this,"O seu espaço de navegação",13,false,Styles.MUTED);
        appSub.setPadding(0,d(5),0,0);summary.addView(appSub);
        box.addView(summary);
        menuHeading(box,"Navegação");
        menuEntry(box,"‹","Voltar",()->{
            if(selected!=null&&selected.web!=null&&selected.web.canGoBack())selected.web.goBack();
        },popup);
        menuEntry(box,"›","Avançar",()->{
            if(selected!=null&&selected.web!=null&&selected.web.canGoForward())selected.web.goForward();
        },popup);
        menuEntry(box,"⌂","Página inicial",()->{
            if(selected!=null&&selected.web!=null){selected.url=HOME;selected.web.loadUrl(HOME);showHomepage();}
        },popup);
        menuEntry(box,"⟳","Atualizar página",()->{
            if(selected!=null&&selected.web!=null&&!HOME.equals(selected.url))selected.web.reload();
            else showHomepage();
        },popup);
        menuEntry(box,"⇧","Verificar atualizações",()->
            VistaBrowserUpdates.check(this,true),popup);''',"menu navigation actions")
once('''        popup.showAsDropDown(menuAnchor,0,d(6),Gravity.END);''',
'''        popup.showAtLocation(root,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,d(22));''',"opera bottom menu")

# Quick panel retains every toggle, but moves Refresh into main navigation.
once('''        menuEntry(box,"⟳","Atualizar página",()->{
            if(selected!=null && selected.web!=null)selected.web.reload();
        },popup);''',
'''        menuEntry(box,"⋮","Menu principal",this::showMenu,popup);
        menuEntry(box,"⌕","Inspetor web",this::showInspector,popup);
        menuEntry(box,"▶","Vídeos encontrados",this::showDetected,popup);''',"quick actions refresh moved")
once('''        popup.setWidth(Math.min(d(364),getResources().getDisplayMetrics().widthPixels-d(20)));''',
'''        popup.setWidth(Math.min(d(465),getResources().getDisplayMetrics().widthPixels-d(16)));''',"quick panel width")
once('''        popup.setHeight(Math.min(d(640),(int)(getResources().getDisplayMetrics().heightPixels*.70)));''',
'''        popup.setHeight(Math.min(d(740),(int)(getResources().getDisplayMetrics().heightPixels*.76)));''',"quick panel height")

# Tabs: Opera-style normal/private selector, with existing preview, search,
# closing, adding and selecting all preserved. Private thumbnails stay hidden.
first="    private void showTabOverview(){"
last="    private void closeTab(Tab tab){"
a=s.index(first);b=s.index(last,a)
old=s[a:b]
assert "renderTabCards(" in old and "capturePreview(selected)" in old
old_render=old[old.index("    private void renderTabCards("):]
old_render=old_render.replace(
    '''            if(!((tab.title+" "+tab.url).toLowerCase(Locale.ROOT)).contains(filter.toLowerCase(Locale.ROOT)))continue;''',
    '''            if(tab.privateMode!=privateOverviewMode)continue;
            if(!((tab.title+" "+tab.url).toLowerCase(Locale.ROOT)).contains(filter.toLowerCase(Locale.ROOT)))continue;''',1)
old_render=old_render.replace(
    '''            GridLayout.LayoutParams cp=new GridLayout.LayoutParams();cp.width=width;cp.height=d(224);''',
    '''            GridLayout.LayoutParams cp=new GridLayout.LayoutParams();cp.width=width;cp.height=d(245);''',1)
old_render=old_render.replace('''            card.setPadding(d(8),d(9),d(8),d(10));''',
    '''            card.setPadding(d(9),d(11),d(9),d(11));''',1)
old_render=old_render.replace('''        if(visible==0){TextView empty=panelLabel("Nenhum separador encontrado",14,false,PANEL_MUTED);''',
    '''        if(visible==0){TextView empty=panelLabel(
            privateOverviewMode?"Não há separadores privados":"Não há separadores nesta pesquisa",
            14,false,PANEL_MUTED);''',1)
tabs=r'''    private void showTabOverview(){
        privateOverviewMode=false;
        tabOverviewFilter="";
        renderOperaTabs();
    }
    private void renderOperaTabs(){
        capturePreview(selected);
        BrowserPage page=newPage("Separadores");
        addPanelAction(page,"+","Nova aba",()->{closePage();newTab(false,HOME);});
        addPanelAction(page,"◌","Nova aba privada",()->{closePage();newTab(true,HOME);});
        LinearLayout selector=Styles.row(this);
        selector.setPadding(d(2),d(10),d(2),d(12));
        TextView normal=Styles.text(this,"Separadores",17,true,
            privateOverviewMode?Styles.MUTED:Styles.TEXT);
        TextView priv=Styles.text(this,"Privado",17,true,
            privateOverviewMode?Styles.TEXT:Styles.MUTED);
        normal.setGravity(Gravity.CENTER);
        priv.setGravity(Gravity.CENTER);
        normal.setBackground(Styles.bg(this,privateOverviewMode?0x002D302B:0xFF48493D,17));
        priv.setBackground(Styles.bg(this,privateOverviewMode?0xFF48493D:0x002D302B,17));
        selector.addView(normal,new LinearLayout.LayoutParams(0,d(48),1));
        selector.addView(priv,new LinearLayout.LayoutParams(0,d(48),1));
        page.body.addView(selector);
        normal.setOnClickListener(v->{
            privateOverviewMode=false;
            renderOperaTabs();
        });
        priv.setOnClickListener(v->{
            privateOverviewMode=true;
            renderOperaTabs();
        });
        EditText query=panelSearch(page.body,"Pesquisar separadores",q->{
            tabOverviewFilter=q;
            renderTabCards(page.body,q);
        });
        if(!tabOverviewFilter.isEmpty())query.setText(tabOverviewFilter);
        else renderTabCards(page.body,"");
    }
'''+old_render
s=s[:a]+tabs+s[b:]

# Update all shared page surface colors to a restrained Opera-esque charcoal
# and warm-ivory system; no CSS, JavaScript, network or media URLs are edited.
colors={
  '0xFF0F141E':'0xFF181A17',
  '0xFF151E30':'0xFF32332D',
  '0xFF171F32':'0xFF30322B',
  '0xFF263A60':'0xFF44473E',
  '0xFF10151F':'0xFF181A17',
  '0xFF151D2C':'0xFF242721',
  '0xFF1A2638':'0xFF2D302A',
  '0xFF202B3C':'0xFF30332D',
  '0xFF1C2738':'0xFF2C3029',
  '0xFF263246':'0xFF31342F',
  '0xFF293C56':'0xFF46483C',
  '0xFF1C2636':'0xFF2B2E28',
  '0xFF131B28':'0xFF20221E',
  '0xFF2B4166':'0xFF484C3F',
  '0xFF36475F':'0xFF54584B',
  '0xFF1D2738':'0xFF30332D',
  '0xFF364760':'0xFF505347',
  '0xFF1E2838':'0xFF31332D',
  '0xFF1E2938':'0xFF31332D',
  '0xFF1D2B3D':'0xFF31332D',
  '0xFF1D2024':'0xFF292B27',
  '0xFF242B3B':'0xFF292C27',
  '0xFF384458':'0xFF55594D',
  '0xFF3A4D68':'0xFF53584C',
  '0xFF405371':'0xFF56594D',
  '0xFF3E4C66':'0xFF55594D',
  '0xFF2B3B52':'0xFF474A40',
  '0xFF16202E':'0xFF292C26',
  '0xFF151C29':'0xFF242620',
  '0xFF171D2B':'0xFF252721',
  '0xFF3A4E68':'0xFF58594D',
  '0xFF141925':'0xFF22241F',
  '0xFF10141C':'0xFF191B18',
  '0xFF1C2432':'0xFF2A2C27',
  '0xFF151A24':'0xFF1F211D',
  '0xFF607A9E':'0xFF757667',
  '0xFF8DBDFF':'0xFFE1DEC9',
  '0xFF8AB9FF':'0xFFE2DFCB',
  '0xFFB8CCED':'0xFFD8D5C5',
  '0xFFBFCEE5':'0xFFCCCABC',
  '0xFF718198':'0xFF99998C',
  '0xFFA6B5CB':'0xFFB8B8AD',
  '0xFFB4D7EC':'0xFFE2E0CE',
  '0xFF9FC6FF':'0xFFE1DEC9',
  '0xFF9DA5B4':'0xFFB8B8AD',
  '0xFFADCFDB':'0xFFE2E0CE',
}
for old,new in colors.items():s=s.replace(old,new)
# Avoid delivering misleading "1.5" in About screen if still present.
s=s.replace('setTitle("VistaBrowser 1.5")','setTitle("VistaBrowser")')

# Regression guards: retain all inherited functional entry points.
for marker in [
  'private void showDetected(){','private void showLibrary(){',
  'private void showInspector(){','private void showCrawler(){',
  'private boolean sendToVistaStream(','private void openNativePlayer(',
  'private void onNativePlaybackRequested(','private void capturePage(',
  'private void download(','private void setPageLockForCurrentTab(',
  'VistaBrowserUpdates.check(this,false)','private void showQuickActions(){',
  'private void showTabOverview(){','private void renderTabCards(',
  'private void autoToggle(','private void showMenu(){',
  'private void showAddresses(',
]:
    if marker not in s:raise RuntimeError("Lost functional entry point: "+marker)
if 'menuEntry(box,"⟳","Atualizar página"' not in s:raise RuntimeError("Refresh missing in Navigation")
if s.count('menuEntry(box,"⟳","Atualizar página"')!=1:raise RuntimeError("Refresh must be in main menu only")
main.write_text(s,encoding="utf-8")

styles=styles_path.read_text(encoding="utf-8")
assert "public static final int BG=0xFF0F141E" in styles
styles=styles.replace(
 'public static final int BG=0xFF0F141E, INK=0xFFEFF4FE, BLUE=0xFF8AB9FF,\n        MUTED=0xFFA6B5CB, WHITE=Color.WHITE, BORDER=0xFF39485E;\n    public static final int TEXT=0xFFF5F8FF, ACCENT=0xFF8DBDFF, SURFACE=0xFF1C2636;',
 'public static final int BG=0xFF181A17, INK=0xFFF2F0E4, BLUE=0xFFE1DEC9,\n        MUTED=0xFFB8B8AD, WHITE=Color.WHITE, BORDER=0xFF53564B;\n    public static final int TEXT=0xFFF4F2EA, ACCENT=0xFFE1DEC9, SURFACE=0xFF2B2D28;'
)
styles=styles.replace('0x608DBDFF','0x80BEBCA4').replace('0xFF518FEB','0xFF909C7F').replace('0xFF35445A','0xFF55584D')
styles_path.write_text(styles,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
assert "versionName '3.1.0'" in g and "versionCode 22" in g
gradle.write_text(g.replace("versionName '3.1.0'","versionName '3.2.0'").replace("versionCode 22","versionCode 23"),encoding="utf-8")
print("VistaBrowser 3.2 Opera-inspired UI: home, chrome, native menus, tabs and all panels; original functional entry points retained.")
