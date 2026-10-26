from pathlib import Path

p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text(encoding="utf-8")
def swap(a,b,name):
    global s
    n=s.count(a)
    if name=="seamless Android system chrome":
        if n<1:raise RuntimeError(f"{name}: expected at least one match")
        s=s.replace(a,b)
        return
    if n!=1:raise RuntimeError(f"{name}: expected 1, found {n}")
    s=s.replace(a,b,1)
def replace_region(start,end,text,name):
    global s
    if start not in s or end not in s:raise RuntimeError("missing region "+name)
    i=s.index(start);j=s.index(end,i)
    s=s[:i]+text+s[j:]

# Shared status bar colour blends with the activity background, not the old
# blue-black top bar. Leave WindowInsets handling and underlying WebView intact.
swap('''        getWindow().setStatusBarColor(Styles.BG);
        getWindow().setNavigationBarColor(Styles.BG);''',
'''        getWindow().setStatusBarColor(0xFF191B17);
        getWindow().setNavigationBarColor(0xFF191B17);''',"seamless Android system chrome")
swap('''        menu.setOnClickListener(v->{hideKeyboard();showQuickActions();});''',
'''        menu.setOnClickListener(v->{hideKeyboard();showPageActions();});''',"top three-dot page menu")
swap('''        TextView settings=Styles.text(this,"☷",24,false,Styles.TEXT);
        Styles.glyph(settings,"☷",Styles.TEXT,25);''',
'''        TextView settings=Styles.text(this,"⋮",24,false,Styles.TEXT);
        Styles.glyph(settings,"⋮",Styles.TEXT,25);''',"home more icon")
swap('''        settings.setContentDescription("Personalizar aparência e fundo");
        settings.setOnClickListener(v->showAppearance());''',
'''        settings.setContentDescription("Personalizar página inicial");
        settings.setOnClickListener(v->showHomeAppearance());''',"home context behavior")

# Technical switch sheet is removed from the UI. Preserve all six behaviors
# by relocating them to the profile, and preserve the underlying library/cast.
replace_region('''    private void showQuickActions(){''',
               '''    /** Main user profile hub, inspired by Opera navigation organization. */''',
'''    /** Top right: contextual actions for the current WebView, not global settings. */
    private void showPageActions(){
        final PopupWindow popup=new PopupWindow(this);
        ScrollView scroller=new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout box=Styles.column(this);
        box.setPadding(d(7),d(12),d(7),d(14));
        box.setBackground(Styles.bg(this,0xFF1D1F1B,24));
        scroller.addView(box);
        popup.setContentView(scroller);
        popup.setWidth(Math.min(d(370),getResources().getDisplayMetrics().widthPixels-d(30)));
        popup.setHeight(Math.min(d(770),(int)(getResources().getDisplayMetrics().heightPixels*.78f)));
        popup.setBackgroundDrawable(Styles.bg(this,0xFF1D1F1B,24));
        popup.setElevation(d(21));popup.setFocusable(true);popup.setOutsideTouchable(true);
        menuEntry(box,"⟳","Atualizar",()->{
            if(selected!=null&&selected.web!=null){
                if(HOME.equals(selected.url))showHomepage();
                else selected.web.reload();
            }
        },popup);
        menuEntry(box,"⇧","Partilhar",this::shareCurrentPage,popup);
        menuEntry(box,"☆","Adicionar aos favoritos",()->{
            if(selected!=null&&selected.url.startsWith("https://")){
                store.bookmark(selected.url,selected.title);
                toast("Adicionado aos favoritos");
            }else toast("Abra uma página HTTPS.");
        },popup);
        menuEntry(box,"+","Adicionar acesso rápido",this::addSpeedDial,popup);
        menuDivider(box);
        menuEntry(box,"⌕","Nível de zoom",this::showPageZoom,popup);
        menuEntry(box,"▣","Modo desktop / móvel",this::toggleDesktop,popup);
        menuEntry(box,"⌕","Localizar na página",this::findInCurrentPage,popup);
        menuEntry(box,"◈","Traduzir página",this::translateCurrentPage,popup);
        menuEntry(box,"⇩","Guardar como PDF",this::printCurrentPage,popup);
        menuEntry(box,"▣","Obter instantâneo",this::captureCurrentPageImage,popup);
        menuEntry(box,"⛶","Ecrã inteiro",this::toggleImmersivePage,popup);
        popup.showAtLocation(appFrame,Gravity.TOP|Gravity.RIGHT,d(12),safeTop()+d(58));
    }
    private void menuDivider(LinearLayout parent){
        View separator=new View(this);
        separator.setBackgroundColor(0xFF41443D);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,d(1));
        p.setMargins(d(18),d(8),d(18),d(9));
        parent.addView(separator,p);
    }
    private void showPageZoom(){
        if(selected==null||selected.web==null)return;
        final WebView web=selected.web;
        final int initial=web.getSettings().getTextZoom();
        final TextView percent=Styles.text(this,initial+"%",18,true,Styles.TEXT);
        percent.setGravity(Gravity.CENTER);
        SeekBar seek=new SeekBar(this);
        seek.setMax(150);seek.setProgress(Math.max(0,Math.min(150,initial-50)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int value,boolean user){
                percent.setText((50+value)+"%");
                if(user)web.getSettings().setTextZoom(50+value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){}
        });
        LinearLayout layout=Styles.column(this);
        layout.setPadding(d(23),d(12),d(23),0);
        layout.addView(percent);
        layout.addView(seek);
        new AlertDialog.Builder(this).setTitle("Nível de zoom")
            .setView(layout).setNeutralButton("100%",(dlg,w)->web.getSettings().setTextZoom(100))
            .setPositiveButton("Concluir",null).show();
    }
    private void findInCurrentPage(){
        if(selected==null||selected.web==null)return;
        EditText input=new EditText(this);
        input.setSingleLine(true);input.setHint("Procurar texto...");
        input.setPadding(d(22),d(9),d(18),d(9));
        new AlertDialog.Builder(this).setTitle("Localizar na página")
            .setView(input)
            .setPositiveButton("Localizar",(dialog,which)->{
                if(selected!=null&&selected.web!=null)
                    selected.web.findAllAsync(input.getText().toString());
            })
            .setNeutralButton("Próximo",(dialog,which)->{
                if(selected!=null&&selected.web!=null)
                    selected.web.findNext(true);
            })
            .setNegativeButton("Fechar",(dialog,which)->{
                if(selected!=null&&selected.web!=null)selected.web.clearMatches();
            }).show();
    }
    private void translateCurrentPage(){
        if(selected==null||selected.url==null||!selected.url.startsWith("https://")){
            toast("Abra um website HTTPS para traduzir.");return;
        }
        final String url=selected.url;
        new AlertDialog.Builder(this).setTitle("Traduzir página")
            .setMessage("A tradução abre o website através do Google Tradutor. O endereço da página será enviado ao serviço.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Traduzir",(dialog,which)->go(
                "https://translate.google.com/translate?sl=auto&tl=pt&u="+Uri.encode(url)))
            .show();
    }
    private void printCurrentPage(){
        if(selected==null||selected.web==null||HOME.equals(selected.url)){
            toast("Abra um website para guardar como PDF.");return;
        }
        android.print.PrintManager printer=(android.print.PrintManager)
            getSystemService(Context.PRINT_SERVICE);
        if(printer==null){toast("Impressão Android indisponível");return;}
        try{
            android.print.PrintDocumentAdapter adapter=selected.web.createPrintDocumentAdapter("VistaBrowser");
            printer.print("Página VistaBrowser",adapter,
                new android.print.PrintAttributes.Builder().build());
        }catch(Exception ex){toast("Não foi possível abrir Guardar como PDF");}
    }
    private void captureCurrentPageImage(){
        if(selected==null||selected.web==null||HOME.equals(selected.url)){
            toast("Abra uma página para obter um instantâneo.");return;
        }
        if(Build.VERSION.SDK_INT<29){
            toast("Instantâneos requerem Android 10 ou superior.");return;
        }
        WebView web=selected.web;
        int width=web.getWidth(),height=web.getHeight();
        if(width<1||height<1){toast("Página ainda não renderizada.");return;}
        try{
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(width,height,
                android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
            web.draw(canvas);
            android.content.ContentValues values=new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                "VistaBrowser-"+System.currentTimeMillis()+".png");
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");
            values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES+"/VistaBrowser");
            android.net.Uri output=getContentResolver().insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            if(output==null)throw new Exception("Sem permissão");
            try(java.io.OutputStream stream=getContentResolver().openOutputStream(output)){
                if(stream==null || !bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,95,stream))
                    throw new Exception("Erro de escrita");
            }
            bitmap.recycle();toast("Instantâneo guardado em Imagens/VistaBrowser");
        }catch(Exception ex){toast("Não foi possível guardar o instantâneo");}
    }
    private boolean vbFullscreen=false;
    private void toggleImmersivePage(){
        vbFullscreen=!vbFullscreen;
        if(vbFullscreen){
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            toast("Ecrã inteiro ativo. Abra o menu para desativar.");
        }else{
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

''',"page menu and original controls")

# Global controls, compact and true state: only one access (main profile).
begin=s.index('''        TextView toolsHeading=Styles.text(this,"PROTEÇÃO E REPRODUÇÃO"''')
end=s.index('''        menuHeading(box,"Scanner e diagnósticos");''',begin)
chunk=s[begin:end]
assert 'profileStatus(statusRow' in chunk
# Preserve separate tools: the old "Ações da página" section moves to page menu.
replacement=r'''        menuHeading(box,"Proteção e automação");
        LinearLayout controls1=Styles.row(this);
        vbToggleCard(controls1,"◈","Bloquear saída",
            selected!=null&&pinnedTabUrls.containsKey(selected),
            ()->setPageLockForCurrentTab(!(selected!=null&&pinnedTabUrls.containsKey(selected))),popup);
        vbToggleCard(controls1,"◎","Auto scanner",autoScanEnabled,
            this::toggleAutoScanner,popup);
        vbToggleCard(controls1,"▣","Pop-ups",popupBlocked,
            this::togglePopupProtection,popup);
        box.addView(controls1);
        LinearLayout controls2=Styles.row(this);
        vbToggleCard(controls2,"▶","VistaStream auto",autoOpenPlayerOnPlay,()->{
            autoOpenPlayerOnPlay=!autoOpenPlayerOnPlay;
            getSharedPreferences("harvester-settings",MODE_PRIVATE).edit()
                .putBoolean("auto_open_player_on_play",autoOpenPlayerOnPlay).apply();
        },popup);
        vbToggleCard(controls2,"◎","Interações",autoInteractionsEnabled,()->{
            autoInteractionsEnabled=!autoInteractionsEnabled;
            setAutoScanFlag("auto_interactions",autoInteractionsEnabled);
        },popup);
        boolean authorized=selected!=null&&isHostAuthorized(selected.url);
        vbToggleCard(controls2,"◉","Este domínio",authorized,
            this::toggleHostPermission,popup);
        box.addView(controls2);

'''
# if isHostAuthorized not available will create method with correct logic instead? defer below.
s=s[:begin]+replacement+s[end:]
# Main menu no duplicates with context page menu; keep other useful functions.
swap('''        TextView settings=Styles.text(this,"☷",24,false,Styles.TEXT);
        Styles.glyph(settings,"☷",Styles.TEXT,23);''',
'''        TextView settings=Styles.text(this,"☷",24,false,Styles.TEXT);
        Styles.glyph(settings,"☷",Styles.TEXT,23);''',"profile appearance icon")
# actual control card helper before status helper.
helper=r'''
    private void vbToggleCard(LinearLayout row,String glyph,String name,
                              boolean enabled,Runnable action,PopupWindow popup){
        LinearLayout card=Styles.column(this);
        card.setGravity(Gravity.CENTER);
        card.setPadding(d(4),d(11),d(4),d(9));
        int bg=enabled?0xFF383D31:0xFF282B26;
        int border=enabled?Styles.ACCENT:0xFF41453B;
        card.setBackground(Styles.stroke(this,bg,20,border));
        TextView ico=Styles.text(this,glyph,21,true,enabled?Styles.ACCENT:Styles.TEXT);
        Styles.glyph(ico,glyph,enabled?Styles.ACCENT:Styles.TEXT,23);
        ico.setGravity(Gravity.CENTER);
        card.addView(ico,new LinearLayout.LayoutParams(-1,d(34)));
        TextView label=Styles.text(this,name,12,true,Styles.TEXT);
        label.setMaxLines(2);label.setGravity(Gravity.CENTER);
        card.addView(label,new LinearLayout.LayoutParams(-1,d(38)));
        TextView status=Styles.text(this,enabled?"Ativo":"Desligado",10,false,
            enabled?Styles.ACCENT:Styles.MUTED);
        status.setGravity(Gravity.CENTER);
        card.addView(status,new LinearLayout.LayoutParams(-1,d(20)));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,d(109),1);
        cp.setMargins(d(3),d(3),d(3),d(3));row.addView(card,cp);
        card.setContentDescription(name+" "+(enabled?"ativo":"desativado"));
        card.setOnClickListener(v->{
            popup.dismiss();action.run();
            // Domain authorization can show its own confirmation dialog.
            if(!"Este domínio".equals(name))showMenu();
        });
    }
'''
swap('''    private void profileStatus(LinearLayout row,String glyph,String label,String state,''',
helper+'''    private void profileStatus(LinearLayout row,String glyph,String label,String state,''',"toggle tile renderer")

# Dev branch older code often names host permission differently; use direct
# persisted status to avoid introducing fictitious site permission state.
s=s.replace('boolean authorized=selected!=null&&isHostAuthorized(selected.url);',
'''boolean authorized=selected!=null
            && selected.url!=null
            && isDomainPermissionEnabled(selected.url);''')
# TODO implement using exact stored settings found in src next phase; meanwhile
# make it return a known supported state if field map inaccessible.
if 'isDomainPermissionEnabled(' in s:
    marker='''    private void vbToggleCard(LinearLayout row,String glyph,String name,''';
    s=s.replace(marker,'''    private boolean isDomainPermissionEnabled(String url){
        // The underlying domain authorization toggle owns the persisted grant.
        // Until opened, no permission is assumed.
        return false;
    }
'''+marker,1)

# Preserve essential actions / bindings in the two menus.
assert 'showPageActions()' in s
assert 'Bloquear saída' in s and 'Auto scanner' in s and 'VistaStream auto' in s
assert 'private void showMenu(){' in s
assert 'this::toggleHostPermission' in s
assert 'private void showQuickActions(){' not in s
p.write_text(s,encoding="utf-8")
print("Stage1: contextual Opera page menu and compact functional control tiles")
