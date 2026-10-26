from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text(encoding="utf-8")
def once(a,b,label):
    global s
    n=s.count(a)
    if n!=1:raise RuntimeError(f"{label}: expected 1, found {n}")
    s=s.replace(a,b,1)

once('''    private boolean privateOverviewMode=false;
    private String tabOverviewFilter="";''',
'''    private boolean privateOverviewMode=false;
    private String tabOverviewFilter="";
    private boolean vbSelectTabs=false;
    private boolean vbListTabs=false;
    private final java.util.Set<Tab> vbSelectedTabs=new java.util.HashSet<>();''',
"tabs state")
start=s.index('''    private void showTabOverview(){''')
end=s.index('''    private void renderTabCards(LinearLayout body,String filter){''',start)
replacement=r'''    private void showTabOverview(){
        privateOverviewMode=false;
        tabOverviewFilter="";
        vbSelectTabs=false;
        vbSelectedTabs.clear();
        vbListTabs=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE)
            .getBoolean("tabs_list_mode",false);
        renderOperaTabs();
    }
    private void renderOperaTabs(){
        capturePreview(selected);
        closePage();
        LinearLayout page=Styles.column(this);
        page.setBackground(Styles.gradient(this,0xFF302F28,0xFF1A1D17,0));
        appFrame.addView(page,new FrameLayout.LayoutParams(-1,-1));
        activePage=page;
        applyOverlaySafeArea(page);

        LinearLayout top=Styles.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(d(18),d(10),d(14),d(8));
        TextView normal=Styles.text(this,"Separadores",18,true,
            privateOverviewMode?0xFFBEBBAE:Styles.TEXT);
        TextView priv=Styles.text(this,"Privado",18,true,
            privateOverviewMode?Styles.TEXT:0xFFBEBBAE);
        normal.setGravity(Gravity.CENTER);
        priv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams n=new LinearLayout.LayoutParams(0,d(53),1.15f);
        LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(0,d(53),.95f);
        top.addView(normal,n);top.addView(priv,r);
        TextView search=panelIcon("⌕",26,"Pesquisar separadores");
        search.setBackground(Styles.bg(this,0xFF454639,25));
        top.addView(search,new LinearLayout.LayoutParams(d(48),d(48)));
        page.addView(top);

        LinearLayout underline=Styles.row(this);
        TextView activeLine=Styles.text(this,"",10,false,Styles.ACCENT);
        activeLine.setBackground(Styles.bg(this,Styles.ACCENT,3));
        LinearLayout.LayoutParams lineParams=new LinearLayout.LayoutParams(0,d(3),1);
        lineParams.setMargins(d(18),0,d(18),0);
        View lineA=new View(this),lineB=new View(this);
        lineA.setBackgroundColor(privateOverviewMode?Color.TRANSPARENT:Styles.ACCENT);
        lineB.setBackgroundColor(privateOverviewMode?Styles.ACCENT:Color.TRANSPARENT);
        underline.addView(lineA,new LinearLayout.LayoutParams(0,d(3),1));
        underline.addView(lineB,new LinearLayout.LayoutParams(0,d(3),1));
        underline.setPadding(d(18),0,d(135),d(9));
        page.addView(underline);

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(false);
        scroller.setVerticalScrollBarEnabled(false);
        page.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=Styles.column(this);
        body.setPadding(d(13),d(16),d(13),d(30));
        scroller.addView(body);
        EditText input=panelSearch(body,"Pesquisar separadores",q->{
            tabOverviewFilter=q;
            renderTabCards(body,q);
        });
        input.setVisibility(tabOverviewFilter.isEmpty()?View.GONE:View.VISIBLE);
        if(!tabOverviewFilter.isEmpty()){
            input.setText(tabOverviewFilter);
            input.setSelection(input.length());
        }
        if(vbSelectTabs)panelHint(body,"Toque nos separadores para os selecionar.");
        renderTabCards(body,tabOverviewFilter);

        normal.setOnClickListener(v->{
            privateOverviewMode=false;tabOverviewFilter="";
            vbSelectedTabs.clear();renderOperaTabs();
        });
        priv.setOnClickListener(v->{
            privateOverviewMode=true;tabOverviewFilter="";
            vbSelectedTabs.clear();renderOperaTabs();
        });
        search.setOnClickListener(v->{
            input.setVisibility(View.VISIBLE);
            input.requestFocus();
            InputMethodManager ime=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(ime!=null)ime.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
        });

        LinearLayout footer=Styles.row(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(d(12),d(5),d(12),d(10));
        footer.setBackground(Styles.bg(this,0xFF11130F,31));
        tabFooterButton(footer,"▣",()->{
            vbListTabs=!vbListTabs;
            getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE).edit()
                .putBoolean("tabs_list_mode",vbListTabs).apply();
            renderOperaTabs();
        },"Alternar entre grelha e lista");
        tabFooterButton(footer,"◷",()->showAddresses("history"),"Histórico");
        if(vbSelectTabs){
            TextView endSelection=tabFooterButton(footer,"×",this::confirmCloseSelectedTabs,
                "Fechar separadores selecionados");
            endSelection.setBackground(Styles.bg(this,Styles.ACCENT,28));
            endSelection.setTextColor(0xFF23251F);
        }else{
            TextView plus=tabFooterButton(footer,"+",()->{
                closePage();newTab(privateOverviewMode,HOME);
            },"Novo separador");
            plus.setBackground(Styles.bg(this,Styles.ACCENT,32));
            plus.setTextColor(0xFF23251F);
        }
        int count=0;
        for(Tab tab:tabs)if(tab.privateMode==privateOverviewMode)count++;
        final int visibleCount=count;
        TextView countButton=tabFooterButton(footer,String.valueOf(visibleCount),()->{
            input.setVisibility(View.VISIBLE);input.requestFocus();
            InputMethodManager ime=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(ime!=null)ime.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
        },"Pesquisar "+visibleCount+" separadores");
        countButton.setBackground(Styles.stroke(this,0x00191919,10,Styles.TEXT));
        countButton.setTextSize(16);
        countButton.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        tabFooterButton(footer,"⋮",()->showTabActionMenu(), "Mais ações de separadores");
        page.addView(footer,new LinearLayout.LayoutParams(-1,d(78)));
    }
    private TextView tabFooterButton(LinearLayout row,String symbol,Runnable action,String label){
        TextView view=Styles.text(this,symbol,24,false,Styles.TEXT);
        if(!symbol.matches("[0-9]+"))Styles.glyph(view,symbol,Styles.TEXT,23);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(label);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,d(56),1);
        params.setMargins(d(3),0,d(3),0);
        row.addView(view,params);
        view.setOnClickListener(v->action.run());
        return view;
    }
    private void showTabActionMenu(){
        final PopupWindow popup=new PopupWindow(this);
        LinearLayout menu=Styles.column(this);
        menu.setPadding(d(6),d(8),d(6),d(9));
        menu.setBackground(Styles.bg(this,0xFF22241E,22));
        popup.setContentView(menu);
        popup.setBackgroundDrawable(Styles.bg(this,0xFF22241E,22));
        popup.setWidth(Math.min(d(326),getResources().getDisplayMetrics().widthPixels-d(45)));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setElevation(d(18));popup.setFocusable(true);popup.setOutsideTouchable(true);
        menuEntry(menu,"+","Novo separador",()->newTab(false,HOME),popup);
        menuEntry(menu,"◌","Novo separador privado",()->newTab(true,HOME),popup);
        menuDivider(menu);
        menuEntry(menu,"▣",vbSelectTabs?"Sair da seleção":"Selecionar separadores",()->{
            vbSelectTabs=!vbSelectTabs;vbSelectedTabs.clear();renderOperaTabs();
        },popup);
        menuEntry(menu,"×","Fechar todos os separadores",this::confirmCloseAllTabs,popup);
        menuDivider(menu);
        menuEntry(menu,"☷","Aparência",this::showAppearance,popup);
        popup.showAtLocation(appFrame,Gravity.BOTTOM|Gravity.RIGHT,d(12),d(78));
    }
    private void confirmCloseSelectedTabs(){
        int count=vbSelectedTabs.size();
        if(count==0){toast("Selecione pelo menos um separador.");return;}
        new AlertDialog.Builder(this).setTitle("Fechar "+count+" separadores?")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Fechar",(dialog,which)->{
                java.util.List<Tab> closing=new java.util.ArrayList<>(vbSelectedTabs);
                vbSelectedTabs.clear();vbSelectTabs=false;
                closePage();
                for(Tab tab:closing)if(tabs.contains(tab))closeTab(tab);
                renderOperaTabs();
            }).show();
    }
    private void confirmCloseAllTabs(){
        final java.util.List<Tab> closing=new java.util.ArrayList<>();
        for(Tab tab:tabs)if(tab.privateMode==privateOverviewMode)closing.add(tab);
        if(closing.isEmpty()){toast("Não há separadores a fechar.");return;}
        new AlertDialog.Builder(this).setTitle("Fechar todos os separadores?")
            .setMessage("Serão fechados "+closing.size()+
                (privateOverviewMode?" separadores privados.":" separadores normais."))
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Fechar todos",(dialog,which)->{
                closePage();
                for(Tab tab:closing)if(tabs.contains(tab))closeTab(tab);
                vbSelectedTabs.clear();vbSelectTabs=false;
                renderOperaTabs();
            }).show();
    }
'''
s=s[:start]+replacement+s[end:]
once('''        GridLayout grid=new GridLayout(this);grid.setTag("tab-cards");grid.setColumnCount(2);''',
'''        GridLayout grid=new GridLayout(this);grid.setTag("tab-cards");
        grid.setColumnCount(vbListTabs?1:2);''',"tab layout selector")
once('''        int width=(getResources().getDisplayMetrics().widthPixels-d(47))/2;''',
'''        int width=vbListTabs?getResources().getDisplayMetrics().widthPixels-d(38):
            (getResources().getDisplayMetrics().widthPixels-d(47))/2;''',"tabs grid width")
once('''            card.setBackground(Styles.stroke(this,
                    tab==selected?0xFF484C3F:0xFF2B2E28,21,
                    tab==selected?Styles.ACCENT:0xFF54584B));
            GridLayout.LayoutParams cp=new GridLayout.LayoutParams();cp.width=width;cp.height=d(245);''',
'''            boolean marked=vbSelectedTabs.contains(tab);
            card.setBackground(Styles.stroke(this,
                    tab==selected?0xFFE7E5D6:marked?0xFF404C37:0xFF282A24,22,
                    marked?Styles.ACCENT:tab==selected?0xFFFFFFFF:0xFF454940));
            GridLayout.LayoutParams cp=new GridLayout.LayoutParams();
            cp.width=width;cp.height=d(vbListTabs?177:245);''',"premium tab card and selections")
once('''            TextView icon=panelLabel(tab.privateMode?"◌":"◇",19,true,Styles.ACCENT);
            head.addView(icon,new LinearLayout.LayoutParams(0,d(32),1));''',
'''            TextView icon=panelLabel(tab.privateMode?"◌":HOME.equals(tab.url)?"⌂":"◈",
                20,true,tab==selected?0xFF22241F:Styles.ACCENT);
            if(tab.web!=null && tab.web.getFavicon()!=null && !tab.privateMode){
                ImageView favicon=new ImageView(this);
                favicon.setImageBitmap(tab.web.getFavicon());
                head.addView(favicon,new LinearLayout.LayoutParams(d(27),d(27)));
            }else head.addView(icon,new LinearLayout.LayoutParams(d(27),d(32)));
            TextView tabTitle=panelLabel(HOME.equals(tab.url)?"Página inicial":tab.title,
                13,true,tab==selected?0xFF24251F:Styles.TEXT);
            tabTitle.setSingleLine(true);
            tabTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(0,d(30),1);
            t.leftMargin=d(5);head.addView(tabTitle,t);''',"tab favicon and heading")
once('''            }else{
                TextView placeholder=panelLabel(tab.privateMode?"Navegação privada":shortDomain(tab.url),13,false,PANEL_MUTED);
                placeholder.setGravity(Gravity.CENTER);placeholder.setPadding(d(7),d(7),d(7),d(7));
                preview.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));
            }''',
'''            }else if(HOME.equals(tab.url) && !tab.privateMode){
                ImageView image=new ImageView(this);
                image.setImageResource(R.drawable.vb_flower);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                preview.addView(image,new FrameLayout.LayoutParams(-1,-1));
                TextView homeLabel=panelLabel("VistaBrowser",19,true,0xFFFFFFFF);
                homeLabel.setGravity(Gravity.CENTER);
                homeLabel.setBackgroundColor(0x55302420);
                preview.addView(homeLabel,new FrameLayout.LayoutParams(-1,-1));
            }else{
                TextView placeholder=panelLabel(tab.privateMode?"Navegação privada":shortDomain(tab.url),
                    13,false,PANEL_MUTED);
                placeholder.setGravity(Gravity.CENTER);
                placeholder.setPadding(d(7),d(7),d(7),d(7));
                preview.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));
            }''',"Home tabs screenshot")
once('''            TextView name=panelLabel(tab.title,14,true,PANEL_TEXT);
            name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setPadding(d(4),d(9),0,0);card.addView(name);
            close.setOnClickListener(v->{closeTab(tab);showTabOverview();});
            card.setOnClickListener(v->{closePage();selectTab(tab);});''',
'''            close.setOnClickListener(v->{
                vbSelectedTabs.remove(tab);
                closeTab(tab);
                renderOperaTabs();
            });
            card.setOnClickListener(v->{
                if(vbSelectTabs){
                    if(vbSelectedTabs.contains(tab))vbSelectedTabs.remove(tab);
                    else vbSelectedTabs.add(tab);
                    renderTabCards(body,filter);
                }else{
                    closePage();selectTab(tab);
                }
            });''',"tabs selection and click")
# Avoid re-rendering by closing last tab triggering temporary empty page; still safe.
p.write_text(s,encoding="utf-8")
print("Stage3: dedicated Opera-inspired tabs, previews, normal/private and real multi-select")
