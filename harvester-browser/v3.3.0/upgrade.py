from pathlib import Path
import re
root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
styles=root/"app/src/main/java/com/harvester/browser/ui/Styles.java"
glyph=root/"app/src/main/java/com/harvester/browser/ui/PremiumGlyph.java"
gradle=root/"app/build.gradle"
s=main.read_text(encoding="utf-8")

def once(a,b,label):
    global s
    n=s.count(a)
    if n!=1: raise RuntimeError(f"{label}: expected one match, found {n}")
    s=s.replace(a,b,1)

once('''    private LinearLayout root, browserControls, browserToolbar;''',
'''    private LinearLayout root, browserControls, browserToolbar, bottomNav;
    private TextView bottomTabsCounter;
    private androidx.activity.result.ActivityResultLauncher<String[]> wallpaperPicker;''',"UI state")

once('''        popupBlocked=settings.getBoolean("block_popups",true);''',
'''        popupBlocked=settings.getBoolean("block_popups",true);
        Styles.ACCENT=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getInt("accent_color",0xFFE1DEC9);
        wallpaperPicker=registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
            uri->{
                if(uri==null)return;
                try{getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);}
                catch(Exception ignored){}
                getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE).edit()
                    .putString("wallpaper_mode","gallery")
                    .putString("wallpaper_uri",uri.toString()).apply();
                if(selected!=null&&HOME.equals(selected.url))showHomepage();
                toast("Fundo personalizado aplicado");
            });''',"wallpaper gallery activity result")

# Context-sensitive Opera-like dock; keep all scanner actions via the quick/main menus.
a=s.index('''        navButton(nav,"‹",()->{hideKeyboard();if(selected!=null&&selected.web!=null&&selected.web.canGoBack())selected.web.goBack();});''')
b=s.index('''        browserControls.addView(nav);root.addView(browserControls);''',a)
s=s[:a]+'''        bottomNav=nav;
        configureBottomNavigation();
'''+s[b:]
once('''    private void focusBrowserSearch(){''',
'''    private void configureBottomNavigation(){
        if(bottomNav==null)return;
        bottomNav.removeAllViews();
        boolean home=selected==null || HOME.equals(selected.url);
        navButton(bottomNav,"‹",()->{
            hideKeyboard();
            if(selected!=null&&selected.web!=null&&selected.web.canGoBack())selected.web.goBack();
        }).setContentDescription("Voltar");
        if(home){
            navButton(bottomNav,"›",()->{
                hideKeyboard();
                if(selected!=null&&selected.web!=null&&selected.web.canGoForward())selected.web.goForward();
            }).setContentDescription("Avançar");
            navButton(bottomNav,"⌕",()->showSearchOverlay("")).setContentDescription("Pesquisar");
        }else{
            navButton(bottomNav,"⟳",()->{
                hideKeyboard();
                if(selected!=null&&selected.web!=null)selected.web.reload();
            }).setContentDescription("Atualizar página");
            navButton(bottomNav,"⌂",()->{
                hideKeyboard();
                if(selected!=null&&selected.web!=null){
                    selected.url=HOME;selected.web.loadUrl(HOME);showHomepage();
                }
            }).setContentDescription("Página inicial");
        }
        bottomTabsCounter=navButton(bottomNav,"▣",()->{
            hideKeyboard();showTabOverview();
        });
        bottomTabsCounter.setText(tabs.size()>99?"99+":String.valueOf(tabs.size()));
        bottomTabsCounter.setTextSize(15);
        bottomTabsCounter.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bottomTabsCounter.setBackground(Styles.stroke(this,0x00272727,8,Styles.TEXT));
        bottomTabsCounter.setContentDescription("Separadores");
        navButton(bottomNav,"♙",()->{
            hideKeyboard();showMenu();
        }).setContentDescription("Menu do VistaBrowser");
    }
    private void focusBrowserSearch(){''',"context dock")

once('''        tabCounter.setText(tabs.size()>99?"99+":String.valueOf(tabs.size()));''',
'''        tabCounter.setText(tabs.size()>99?"99+":String.valueOf(tabs.size()));
        configureBottomNavigation();''',"dock sync")

# Home gallery wallpaper lives only in the home; no WebView page content is touched.
once('''        LinearLayout home=Styles.column(this);
        home.setPadding(d(23),d(17),d(23),d(28));
        home.setBackground(Styles.gradient(this,0xFF3D3930,Styles.BG,0));
        scroll.addView(home);''',
'''        LinearLayout home=Styles.column(this);
        home.setPadding(d(23),d(17),d(23),d(28));
        home.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(home);''',"transparent home")
once('''        settings.setContentDescription("Ações rápidas");
        settings.setOnClickListener(v->showQuickActions());''',
'''        settings.setContentDescription("Personalizar aparência e fundo");
        settings.setOnClickListener(v->showAppearance());''',"home appearance icon")

# The Home input is a trigger, not an active EditText. Entering search opens
# the full-screen address/search surface above the native keyboard.
a=s.index('''        EditText homeSearch=new EditText(this);''')
b=s.index('''        home.addView(searchPill,new LinearLayout.LayoutParams(-1,d(62)));''',a)
s=s[:a]+'''        TextView homeSearch=Styles.text(this,"Pesquise ou introduza um endereço",15,false,0xFFD8D5C9);
        homeSearch.setSingleLine(true);
        homeSearch.setEllipsize(android.text.TextUtils.TruncateAt.END);
        homeSearch.setPadding(d(3),0,0,0);
        searchPill.addView(homeSearch,new LinearLayout.LayoutParams(0,d(52),1));
        TextView searchGo=Styles.text(this,"⌕",24,true,Styles.TEXT);
        Styles.glyph(searchGo,"⌕",Styles.TEXT,23);
        searchGo.setGravity(Gravity.CENTER);
        searchPill.addView(searchGo,new LinearLayout.LayoutParams(d(42),d(48)));
        searchPill.setOnClickListener(v->showSearchOverlay(""));
        homeSearch.setOnClickListener(v->showSearchOverlay(""));
        searchGo.setOnClickListener(v->showSearchOverlay(""));
'''+s[b:]
once('''        browserHost.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        synchronizeUi();''',
'''        FrameLayout wallpaperStage=new FrameLayout(this);
        applyWallpaper(wallpaperStage);
        wallpaperStage.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        browserHost.addView(wallpaperStage,new FrameLayout.LayoutParams(-1,-1));
        synchronizeUi();''',"wallpaper canvas")

# Add menu entry, retaining all existing navigation items and app services.
once('''        menuHeading(box,"Navegação");''',
'''        menuHeading(box,"Navegação");
        menuEntry(box,"☷","Personalizar aparência",this::showAppearance,popup);''',"theme shortcut")

# Tabs get a dedicated Opera-style bottom bar without disturbing tab cards.
once('''        if(!tabOverviewFilter.isEmpty())query.setText(tabOverviewFilter);
        else renderTabCards(page.body,"");
    }
    private void renderTabCards(''',
'''        if(!tabOverviewFilter.isEmpty())query.setText(tabOverviewFilter);
        else renderTabCards(page.body,"");
        LinearLayout footer=Styles.row(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(d(9),d(8),d(9),d(8));
        footer.setBackground(Styles.bg(this,0xF40F110F,28));
        tabFooterButton(footer,"▣",()->renderOperaTabs(),"Separadores");
        tabFooterButton(footer,"◷",()->showAddresses("history"),"Histórico");
        TextView add=tabFooterButton(footer,"+",()->{
            closePage();newTab(privateOverviewMode,HOME);
        },"Nova aba");
        add.setTextColor(0xFF24241E);
        add.setBackground(Styles.bg(this,Styles.ACCENT,30));
        tabFooterButton(footer,"▣",()->renderOperaTabs(),"Visão de separadores");
        tabFooterButton(footer,"⋮",this::showMenu,"Menu");
        if(activePage instanceof LinearLayout)
            ((LinearLayout)activePage).addView(footer,
                new LinearLayout.LayoutParams(-1,d(70)));
    }
    private TextView tabFooterButton(LinearLayout row,String icon,Runnable action,String label){
        TextView view=Styles.text(this,icon,25,false,Styles.TEXT);
        Styles.glyph(view,icon,Styles.TEXT,23);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(label);
        row.addView(view,new LinearLayout.LayoutParams(0,d(52),1));
        view.setOnClickListener(v->action.run());
        return view;
    }
    private void renderTabCards(''',"tabs Opera footer")

# Add original search and custom appearance surfaces. Their only side effect
# is to call existing navigate(), store prefs, or refresh the home page.
insertion=r'''
    /** Opera-inspired address entry, with its own focus and native IME. */
    private void showSearchOverlay(String initial){
        closePage();
        LinearLayout page=Styles.column(this);
        page.setBackgroundColor(0xFF171915);
        appFrame.addView(page,new FrameLayout.LayoutParams(-1,-1));
        activePage=page;
        LinearLayout chips=Styles.row(this);
        chips.setPadding(d(18),d(20),d(18),d(12));
        TextView site=Styles.text(this,"◉  Site",14,true,0xFF292B25);
        site.setGravity(Gravity.CENTER);
        site.setBackground(Styles.bg(this,Styles.ACCENT,28));
        chips.addView(site,new LinearLayout.LayoutParams(d(95),d(44)));
        TextView addressChip=Styles.text(this,"⌕  Pesquisar na Web",14,true,Styles.TEXT);
        addressChip.setGravity(Gravity.CENTER);
        addressChip.setBackground(Styles.bg(this,0xFF43463B,28));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,d(44),1);
        ap.setMargins(d(11),0,0,0);
        chips.addView(addressChip,ap);
        page.addView(chips);
        LinearLayout field=Styles.row(this);
        field.setPadding(d(14),0,d(8),0);
        field.setBackground(Styles.stroke(this,0xFF090A09,28,0xFF383B34));
        EditText query=new EditText(this);
        query.setTextColor(Styles.TEXT);
        query.setHintTextColor(Styles.MUTED);
        query.setHint("Pesquisar ou introduzir URL");
        query.setSingleLine(true);query.setSelectAllOnFocus(true);
        query.setTextSize(17);
        query.setBackgroundColor(Color.TRANSPARENT);
        query.setImeOptions(EditorInfo.IME_ACTION_GO);
        query.setPadding(d(3),0,d(2),0);
        field.addView(query,new LinearLayout.LayoutParams(0,d(60),1));
        TextView copy=Styles.text(this,"▣",24,false,Styles.TEXT);
        Styles.glyph(copy,"▣",Styles.TEXT,23);
        copy.setGravity(Gravity.CENTER);
        field.addView(copy,new LinearLayout.LayoutParams(d(43),d(54)));
        copy.setContentDescription("Copiar endereço");
        copy.setOnClickListener(v->{
            String text=query.getText().toString().trim();
            if(text.isEmpty()&&selected!=null)text=selected.url;
            copy(text);
        });
        TextView close=Styles.text(this,"×",27,false,Styles.TEXT);
        Styles.glyph(close,"×",Styles.TEXT,23);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Fechar pesquisa");
        field.addView(close,new LinearLayout.LayoutParams(d(42),d(54)));
        close.setOnClickListener(v->closePage());
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,d(66));
        fp.setMargins(d(17),d(10),d(17),d(13));
        page.addView(field,fp);
        TextView context=Styles.text(this,"SITE ATUAL",11,true,Styles.MUTED);
        context.setLetterSpacing(.13f);
        context.setPadding(d(28),d(15),0,d(9));
        page.addView(context);
        String current=selected!=null?selected.url:HOME;
        String title=selected!=null?selected.title:"Página inicial";
        LinearLayout suggestion=Styles.column(this);
        suggestion.setPadding(d(18),d(15),d(18),d(16));
        suggestion.setBackground(Styles.bg(this,0xFF2A2D27,19));
        TextView sTitle=Styles.text(this,title==null?"Website":title,16,true,Styles.TEXT);
        sTitle.setSingleLine(true);
        suggestion.addView(sTitle);
        TextView sUrl=Styles.text(this,HOME.equals(current)?"Página inicial":shortDomain(current),13,false,Styles.MUTED);
        sUrl.setSingleLine(true);
        sUrl.setPadding(0,d(7),0,0);
        suggestion.addView(sUrl);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.setMargins(d(17),0,d(17),d(21));
        page.addView(suggestion,sp);
        suggestion.setOnClickListener(v->{
            if(!HOME.equals(current))query.setText(current);
            query.setSelection(query.length());
        });
        TextView appearance=Styles.text(this,"⚙  Personalizar aparência",15,true,Styles.TEXT);
        appearance.setGravity(Gravity.CENTER);
        appearance.setBackground(Styles.bg(this,0xFF48483A,25));
        LinearLayout.LayoutParams spec=new LinearLayout.LayoutParams(-1,d(53));
        spec.setMargins(d(18),d(1),d(18),0);
        page.addView(appearance,spec);
        appearance.setOnClickListener(v->{
            closePage();showAppearance();
        });
        Runnable submit=()->{
            String text=query.getText().toString().trim();
            if(text.isEmpty())return;
            hideSearchIme(query);
            closePage();
            go(text);
        };
        query.setOnEditorActionListener((v,action,event)->{submit.run();return true;});
        addressChip.setOnClickListener(v->{
            query.requestFocus();
            query.setHint("O que quer pesquisar?");
        });
        String text=initial==null?"":initial;
        if(text.isEmpty()&&!HOME.equals(current))text=current;
        query.setText(text);
        query.requestFocus();
        query.selectAll();
        query.postDelayed(()->{
            if(activePage!=page)return;
            InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.showSoftInput(query,InputMethodManager.SHOW_IMPLICIT);
        },160);
    }
    private void hideSearchIme(EditText input){
        InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);
        input.clearFocus();
    }

    /** Fully offline home wallpaper: bundled flower, gallery or plain background. */
    private void applyWallpaper(FrameLayout stage){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        String mode=pref.getString("wallpaper_mode","flower");
        int plain=pref.getInt("home_color",0xFF302E29);
        stage.setBackgroundColor(plain);
        if("none".equals(mode))return;
        ImageView photo=new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setAdjustViewBounds(false);
        boolean loaded=false;
        if("gallery".equals(mode)){
            String saved=pref.getString("wallpaper_uri","");
            try{
                if(!saved.isEmpty()){
                    photo.setImageURI(Uri.parse(saved));
                    loaded=photo.getDrawable()!=null;
                }
            }catch(Exception ignored){}
        }else{
            photo.setImageResource(com.harvester.browser.R.drawable.vb_flower);
            loaded=true;
        }
        if(!loaded)return;
        stage.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        View dim=new View(this);
        dim.setBackground(Styles.gradient(this,0x722A211D,0xC5111312,0));
        stage.addView(dim,new FrameLayout.LayoutParams(-1,-1));
    }
    private void showAppearance(){
        final String[] options={
            "Fotografia floral (predefinida)","Escolher foto da galeria",
            "Usar apenas cor lisa","Cor de destaque","Cor personalizada (HEX)",
            "Cor de fundo da Home"
        };
        new AlertDialog.Builder(this)
            .setTitle("Personalizar o VistaBrowser")
            .setItems(options,(dialog,which)->{
                android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
                switch(which){
                    case 0:
                        pref.edit().putString("wallpaper_mode","flower").apply();
                        appearanceChanged();break;
                    case 1:
                        if(wallpaperPicker!=null)wallpaperPicker.launch(new String[]{"image/*"});
                        break;
                    case 2:
                        pref.edit().putString("wallpaper_mode","none").apply();
                        appearanceChanged();break;
                    case 3:selectPalette(false);break;
                    case 4:customColor();break;
                    case 5:selectPalette(true);break;
                }
            }).setNegativeButton("Fechar",null).show();
    }
    private void selectPalette(boolean background){
        String[] labels={"Marfim (Opera)","Rosa suave","Azul premium","Verde sálvia","Violeta","Grafite"};
        int[] colors={0xFFE1DEC9,0xFFF2AFC0,0xFF9BBEFF,0xFFA6C6A3,0xFFC7AAEF,0xFFD0D0D0};
        int[] backgrounds={0xFF302E29,0xFF382831,0xFF202B3D,0xFF27352B,0xFF2E263D,0xFF202020};
        new AlertDialog.Builder(this)
            .setTitle(background?"Cor de fundo":"Cor do tema")
            .setSingleChoiceItems(labels,-1,(dialog,index)->{
                android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
                if(background)pref.edit().putInt("home_color",backgrounds[index])
                    .putString("wallpaper_mode","none").apply();
                else pref.edit().putInt("accent_color",colors[index]).apply();
                dialog.dismiss();appearanceChanged();
            }).setNegativeButton("Cancelar",null).show();
    }
    private void customColor(){
        EditText value=new EditText(this);
        value.setSingleLine(true);value.setHint("#E1DEC9");
        value.setInputType(android.text.InputType.TYPE_CLASS_TEXT|
            android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        new AlertDialog.Builder(this).setTitle("Cor de destaque (HEX)")
            .setMessage("Introduza um código hexadecimal como #E1DEC9.")
            .setView(value).setNegativeButton("Cancelar",null)
            .setPositiveButton("Aplicar",(dialog,which)->{
                try{
                    String hex=value.getText().toString().trim();
                    if(!hex.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException();
                    int color=Color.parseColor(hex);
                    if(Color.red(color)+Color.green(color)+Color.blue(color)<310){
                        toast("Escolha uma cor mais clara para manter a leitura.");
                        return;
                    }
                    getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
                        .edit().putInt("accent_color",color).apply();
                    appearanceChanged();
                }catch(Exception ex){toast("Cor inválida. Utilize #RRGGBB.");}
            }).show();
    }
    private void appearanceChanged(){
        Styles.ACCENT=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getInt("accent_color",0xFFE1DEC9);
        configureBottomNavigation();
        if(selected!=null&&HOME.equals(selected.url))showHomepage();
        toast("Aparência atualizada");
    }
'''
once('''    private void homeChip(LinearLayout row,String title,Runnable action){''',
insertion+'''    private void homeChip(LinearLayout row,String title,Runnable action){''',"appearance and search implementation")

# Ensure application UI is not half blue and half cream; no string / core JS gets rewritten.
glyphtext=glyph.read_text(encoding="utf-8")
assert 'case "⋮": k="menu";break;' in glyphtext
glyphtext=glyphtext.replace('case "⋮": k="menu";break;','case "⋮": k="menu";break;\n            case "♙":k="profile";break;',1)
glyphtext=glyphtext.replace('''            case "home":
''','''            case "profile":
                circle(canvas,12,12,9);
                circle(canvas,12,9,3);
                canvas.drawArc(new RectF(6,12,18,20),195,150,false,paint);
                break;
            case "home":
''',1)
glyph.write_text(glyphtext,encoding="utf-8")
styled=styles.read_text(encoding="utf-8")
assert 'ACCENT=0xFFE1DEC9' in styled
styled=styled.replace('ACCENT=0xFFE1DEC9','ACCENT=0xFFE1DEC9') # static binding changed below
styled=styled.replace('''public static final int TEXT=0xFFF4F2EA, ACCENT=0xFFE1DEC9, SURFACE=0xFF2B2D28;''',
'''public static final int TEXT=0xFFF4F2EA, SURFACE=0xFF2B2D28;
    public static int ACCENT=0xFFE1DEC9;''',1)
styles.write_text(styled,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
assert "versionName '3.2.0'" in g and "versionCode 23" in g
g= g.replace("versionName '3.2.0'","versionName '3.3.0'").replace("versionCode 23","versionCode 24")
gradle.write_text(g,encoding="utf-8")

for marker in (
    'private void showDetected(){','private void showLibrary(){',
    'private void showInspector(){','private void showCrawler(){',
    'private boolean sendToVistaStream(','private void openNativePlayer(',
    'private void onNativePlaybackRequested(','private void capturePage(',
    'private void download(','private void setPageLockForCurrentTab(',
    'VistaBrowserUpdates.check(this,false)','private void showQuickActions(){',
    'private void showTabOverview(){','private void renderTabCards(',
    'private void autoToggle(','private void showMenu(){',
    'private void showAddresses(','private void renderOperaTabs(){',
    'showSearchOverlay("")','private void showAppearance(){',
):
    if marker not in s:raise RuntimeError("Lost feature: "+marker)
assert 'menuEntry(box,"⟳","Atualizar página"' in s
assert 'navButton(bottomNav,"⟳"' in s
main.write_text(s,encoding="utf-8")
print("VistaBrowser 3.3: Opera-like contextual bar, full-screen search, gallery wallpaper, palettes and original features retained")
