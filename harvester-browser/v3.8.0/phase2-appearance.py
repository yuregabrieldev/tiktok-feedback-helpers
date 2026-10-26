from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text(encoding="utf-8")
def once(a,b,name):
    global s
    n=s.count(a)
    if n!=1:raise RuntimeError(f"{name}: expected 1, found {n}")
    s=s.replace(a,b,1)

start=s.index('''    private void showAppearance(){''')
stop=s.index('''    private void selectPalette(boolean background){''',start)
ui=r'''    /** Opera-inspired settings. All controls persist in browser appearance prefs. */
    private void showAppearance(){
        BrowserPage page=newPage("Aparência");
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        panelHeading(page.body,"Tema");
        LinearLayout themes=Styles.row(this);
        int choice=pref.getInt("theme_mode",0);
        themeCard(themes,"Seguir sistema",0,choice);
        themeCard(themes,"Claro",1,choice);
        themeCard(themes,"Escuro",2,choice);
        page.body.addView(themes);
        panelHeading(page.body,"Cor de destaque");
        LinearLayout palette=Styles.row(this);
        final int[] shades={0xFFE1DEC9,0xFFB5A1F1,0xFFFFB48F,0xFFF7A3E3,0xFF8BD5E6,0xFF84ACF3};
        for(int color:shades){
            FrameLayout swatch=new FrameLayout(this);
            int edge=Styles.ACCENT==color?0xFFFFFFFF:0xFF393C36;
            swatch.setBackground(Styles.stroke(this,color,16,edge));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,d(47),1);
            cp.setMargins(d(3),d(4),d(3),d(5));palette.addView(swatch,cp);
            swatch.setOnClickListener(v->{
                pref.edit().putInt("accent_color",color).apply();
                appearanceChanged();showAppearance();
            });
        }
        page.body.addView(palette);
        panelLine(page.body,"◉","Cor personalizada","Escolher uma cor hexadecimal",
            this::customColor,null);
        panelHeading(page.body,"Página inicial");
        panelLine(page.body,"▣","Imagem de fundo","Personalizar fotografia e fundo",
            this::showHomeAppearance,null);
        panelLine(page.body,"☷","Personalizar página inicial","Logótipo, atalhos e marcadores",
            this::showHomeAppearance,null);
        panelHeading(page.body,"Disposição");
        toggleSettingRow(page.body,"☷","Mostrar atalho na Home",
            "Botão de personalização no canto superior",
            "home_customization",true,true);
        toggleSettingRow(page.body,"▣","Atalhos rápidos",
            "Apresentar os seus sites na página inicial",
            "home_speed_dials",true,true);
        panelHint(page.body,"As preferências ficam guardadas neste dispositivo.");
    }
    private void themeCard(LinearLayout group,String label,int id,int chosen){
        LinearLayout tile=Styles.column(this);
        tile.setGravity(Gravity.CENTER);
        boolean selectedTheme=id==chosen;
        int color=id==1?0xFFF6F4E9:0xFF171915;
        int text=id==1?0xFF181A17:0xFFF2F0E7;
        tile.setBackground(Styles.stroke(this,color,17,
            selectedTheme?Styles.ACCENT:0xFF484A43));
        TextView symbol=Styles.text(this,selectedTheme?"✓":" ",19,true,text);
        symbol.setGravity(Gravity.CENTER);
        tile.addView(symbol,new LinearLayout.LayoutParams(-1,d(25)));
        TextView txt=Styles.text(this,label,12,true,text);
        txt.setGravity(Gravity.CENTER);txt.setMaxLines(2);
        tile.addView(txt,new LinearLayout.LayoutParams(-1,d(39)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(94),1);
        lp.setMargins(d(3),d(4),d(3),d(3));group.addView(tile,lp);
        tile.setOnClickListener(v->{
            getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE).edit()
                .putInt("theme_mode",id).apply();
            applyBrowserTheme();
            showAppearance();
        });
    }
    private void applyBrowserTheme(){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        int mode=pref.getInt("theme_mode",0);
        boolean light=mode==1||(mode==0&&
            (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                ==android.content.res.Configuration.UI_MODE_NIGHT_NO);
        int background=light?0xFFF2F0E9:0xFF191B17;
        // Backgrounds and system bar follow chosen chrome. Web pages keep
        // their own CSS/theme, and existing media player colors stay unmodified.
        if(root!=null)root.setBackgroundColor(background);
        if(appFrame!=null)appFrame.setBackgroundColor(background);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        int flags=getWindow().getDecorView().getSystemUiVisibility();
        if(light)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        else flags&=~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if(selected!=null&&HOME.equals(selected.url))showHomepage();
    }
    private void toggleSettingRow(LinearLayout parent,String glyph,String title,
                                  String description,String key,boolean defaultValue,
                                  boolean refreshHome){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        LinearLayout line=Styles.row(this);
        line.setPadding(d(10),d(11),d(6),d(11));
        TextView ico=Styles.text(this,glyph,21,false,Styles.ACCENT);
        Styles.glyph(ico,glyph,Styles.ACCENT,21);
        ico.setGravity(Gravity.CENTER);
        line.addView(ico,new LinearLayout.LayoutParams(d(43),d(51)));
        LinearLayout labels=Styles.column(this);labels.setPadding(d(8),0,0,0);
        TextView head=Styles.text(this,title,15,true,Styles.TEXT);
        labels.addView(head);
        TextView sub=Styles.text(this,description,12,false,Styles.MUTED);
        sub.setMaxLines(2);sub.setPadding(0,d(5),0,0);labels.addView(sub);
        line.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch sw=new Switch(this);
        sw.setChecked(pref.getBoolean(key,defaultValue));
        Styles.tintedSwitch(sw);
        line.addView(sw);
        sw.setOnCheckedChangeListener((button,enabled)->{
            pref.edit().putBoolean(key,enabled).apply();
            if(refreshHome && selected!=null && HOME.equals(selected.url))showHomepage();
        });
        line.setOnClickListener(v->sw.setChecked(!sw.isChecked()));
        parent.addView(line,new LinearLayout.LayoutParams(-1,-2));
        panelDivider(parent);
    }
    private void showHomeAppearance(){
        BrowserPage page=newPage("Página inicial");
        panelHeading(page.body,"Imagem de fundo");
        panelHint(page.body,"Escolha uma imagem para a sua página inicial.");
        LinearLayout options=Styles.row(this);
        wallpaperOption(options,"+","Galeria",0);
        wallpaperOption(options,"✿","Flor",1);
        wallpaperOption(options,"◐","Fundo liso",2);
        page.body.addView(options);
        panelLine(page.body,"▣","Escolher imagem de fundo","Selecionar foto do dispositivo",
            ()->{if(wallpaperPicker!=null)wallpaperPicker.launch(new String[]{"image/*"});},null);
        panelHeading(page.body,"Personalização");
        toggleSettingRow(page.body,"◈","Logótipo VistaBrowser",
            "Mostrar o nome na página inicial","home_logo",true,true);
        toggleSettingRow(page.body,"☷","Atalho de personalização",
            "Mostrar o botão no canto superior","home_customization",true,true);
        toggleSettingRow(page.body,"⌕","Pesquisar atalhos",
            "Aba privada e acesso à biblioteca","home_search_shortcuts",true,true);
        panelHeading(page.body,"Funcionalidades");
        toggleSettingRow(page.body,"▤","Marcadores rápidos",
            "Mostrar sites favoritos na Home","home_speed_dials",true,true);
        panelLine(page.body,"◉","Cor de fundo","Escolher um fundo liso",
            ()->selectPalette(true),null);
        panelHint(page.body,"As alterações aplicam-se imediatamente à página inicial.");
    }
    private void wallpaperOption(LinearLayout row,String icon,String label,int mode){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        String current=pref.getString("wallpaper_mode","flower");
        String expected=mode==1?"flower":mode==2?"none":"gallery";
        boolean active=expected.equals(current);
        LinearLayout tile=Styles.column(this);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(d(3),d(9),d(3),d(7));
        tile.setBackground(Styles.stroke(this,mode==1?0xFF665450:0xFF363A31,18,
            active?Styles.ACCENT:0xFF55584D));
        TextView graphic=Styles.text(this,icon,27,true,Styles.TEXT);
        graphic.setGravity(Gravity.CENTER);
        tile.addView(graphic,new LinearLayout.LayoutParams(-1,d(49)));
        TextView title=Styles.text(this,label,12,true,Styles.TEXT);
        title.setGravity(Gravity.CENTER);
        tile.addView(title);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(97),1);
        lp.setMargins(d(3),d(4),d(3),d(4));row.addView(tile,lp);
        tile.setOnClickListener(v->{
            if(mode==0){
                if(wallpaperPicker!=null)wallpaperPicker.launch(new String[]{"image/*"});
            }else{
                pref.edit().putString("wallpaper_mode",expected).apply();
                appearanceChanged();showHomeAppearance();
            }
        });
    }
'''
s=s[:start]+ui+s[stop:]
# The bespoke flower wallpaper is already bundled; default remains unchanged.
once('''        top.addView(settings,new LinearLayout.LayoutParams(d(48),d(48)));
        settings.setContentDescription("Personalizar página inicial");''',
'''        if(getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getBoolean("home_customization",true))
            top.addView(settings,new LinearLayout.LayoutParams(d(48),d(48)));
        settings.setContentDescription("Personalizar página inicial");''',"home customization visibility")
once('''        home.addView(wordmark);

        LinearLayout searchPill=Styles.row(this);''',
'''        if(getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getBoolean("home_logo",true))home.addView(wordmark);

        LinearLayout searchPill=Styles.row(this);''',"home logotype visibility")
once('''        home.addView(modes,modesParams);

        TextView shortcutsTitle''',
'''        if(getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getBoolean("home_search_shortcuts",true))home.addView(modes,modesParams);

        TextView shortcutsTitle''',"home quick search controls visibility")
# Speed dials group is displayed only if selected.
once('''        home.addView(shortcutsTitle);

        List<String[]> entries''',
'''        boolean showDials=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getBoolean("home_speed_dials",true);
        if(showDials)home.addView(shortcutsTitle);

        List<String[]> entries''',"home speed dial heading")
once('''            home.addView(row);
        }

        LinearLayout tools=Styles.column(this);''',
'''            if(showDials)home.addView(row);
        }

        LinearLayout tools=Styles.column(this);''',"home speed dial rows")

# Appearance controls change the global accent without changing existing
# image/scanner/integration functionality.
once('''        configureBottomNavigation();
        if(selected!=null&&HOME.equals(selected.url))showHomepage();
        toast("Aparência atualizada");''',
'''        configureBottomNavigation();
        applyBrowserTheme();
        toast("Aparência atualizada");''',"theme update")
p.write_text(s,encoding="utf-8")
print("Stage2: Opera-like Appearance and Home panels with persisted visual options")
