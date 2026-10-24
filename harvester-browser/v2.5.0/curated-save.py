from pathlib import Path
root=Path("hb-v18")
p=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
s=p.read_text()
def one(a,b,label):
    global s
    n=s.count(a)
    if n!=1:raise RuntimeError(f"{label}: wanted one occurrence, found {n}")
    s=s.replace(a,b,1)
# Never auto-save detected or played videos, even when legacy preferences
# retain the old auto_save=true setting. Explicit selection is mandatory.
one('''        if(added && autoScanEnabled && autoSaveEnabled && !tab.privateMode && !io.isShutdown())
            io.execute(()->{try{store.add(item);}catch(Exception ignored){}});
''',
'''        // Detection is passive. Save requires an explicit user action on +.
''',"remove automatic library writing")
one('''        autoSaveEnabled=settings.getBoolean("auto_save",true);''',
'''        autoSaveEnabled=false; // old installations are migrated to manual saving
        settings.edit().putBoolean("auto_save",false).apply();''',
"force opt-in collection only")
one('''    private boolean autoScanEnabled=true, autoSaveEnabled=true, autoInteractionsEnabled=true;''',
'''    private boolean autoScanEnabled=true, autoSaveEnabled=false, autoInteractionsEnabled=true;
    private static final int VISTA_IMPORT_REQUEST=9821;
    private MediaItem awaitingVistaImport;''',
"add result state")
# Replace bulk save; this action could import ads and HLS variants accidentally.
one('''        TextView save=panelLabel("Guardar todas as "+found.size()+" fontes",16,true,0xFFB4D7EC);
        save.setPadding(d(12),d(19),d(12),d(19));
        save.setOnClickListener(v->{int added=0;for(MediaItem item:found)if(store.add(item))added++;
            toast(added+" fontes novas guardadas");});
        page.body.addView(save);''',
'''        TextView label=panelLabel("Toque em + para escolher, editar e guardar uma fonte",15,true,0xFFB4D7EC);
        label.setPadding(d(12),d(19),d(12),d(19));
        page.body.addView(label);''',"remove unsafe bulk save")
# Add an inline + to each detected row without assuming previous renderer edits.
start=s.index("    private void showDetected(){")
first=s.index("        for(MediaItem item:found){",start)
finish=s.index("            panelDivider(page.body);",first)
s=s[:finish]+'''            LinearLayout resultRow=(LinearLayout)page.body.getChildAt(page.body.getChildCount()-1);
            resultRow.setOnClickListener(v->openNativePlayer(item));
            resultRow.setOnLongClickListener(v->{mediaDetails(item,false);return true;});
            TextView plus=panelIcon("+",28,"Adicionar às duas bibliotecas");
            resultRow.addView(plus,new LinearLayout.LayoutParams(d(49),d(51)));
            plus.setOnClickListener(v->sendToVistaStream(item,true));
'''+s[finish:]

# Present explicit dual-save action in the media details.
one('''            boolean added=store.add(item);toast(added?"Guardado":"Já está guardado");},null);''',
'''            sendToVistaStream(item,true);},null);''',"details imports only by explicit confirm")
one('''case "Guardar na biblioteca":toast(store.add(item)?"Guardado na biblioteca":"Esta URL já está guardada");break;''',
'''case "Guardar na biblioteca":sendToVistaStream(item,true);break;''',"action imports with editor")
# Old autoplay toggle from previous releases no longer writes anything: avoid confusion.
one('''        autoToggle(box,"Guardar fontes automaticamente",autoSaveEnabled,v->{
            autoSaveEnabled=v;setAutoScanFlag("auto_save",v);
        });''',
'''        menuEntry(box,"＋","Guardar vídeos: apenas pelo botão +",
            ()->toast("Apenas os vídeos confirmados são adicionados à biblioteca."),popup);''',
"remove auto save switch")
# Cross-app editor callback. Keep video out of SQLite until Vista confirmed.
one('''    private boolean sendToVistaStream(MediaItem item,boolean editBeforeSaving) {''',
'''    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=VISTA_IMPORT_REQUEST)return;
        MediaItem chosen=awaitingVistaImport;
        awaitingVistaImport=null;
        if(resultCode!=RESULT_OK||chosen==null||data==null){
            toast("Importação cancelada: nada foi guardado.");return;
        }
        String approvedUrl=data.getStringExtra("com.vistastream.tv.extra.URL");
        if(!chosen.url.equals(approvedUrl)){
            toast("Resposta de importação inválida; nada foi guardado.");return;
        }
        String confirmedTitle=data.getStringExtra("com.vistastream.tv.extra.TITLE");
        if(confirmedTitle!=null&&!confirmedTitle.trim().isEmpty())
            chosen.title=confirmedTitle.trim().substring(0,Math.min(180,confirmedTitle.trim().length()));
        // Both actions are committed: Vista saved the curated metadata, and
        // only now Harvester writes this URL into its own library.
        store.deleteMedia(chosen.url);
        boolean saved=store.add(chosen);
        toast(saved?"Guardado no Harvester e no Vista Stream":"Não foi possível guardar no Harvester");
    }

    private boolean sendToVistaStream(MediaItem item,boolean editBeforeSaving) {''',
"receive save confirmation")
one('''                startActivity(intent);
                return true;''',
'''                if(editBeforeSaving){
                    awaitingVistaImport=item;
                    startActivityForResult(intent,VISTA_IMPORT_REQUEST);
                }else startActivity(intent);
                return true;''',
"save only after return")
# Avoid duplicate old import buttons: metadata route now explicit in each row.
one('''        intent.putExtra("position_ms",Math.max(0L,resumeMs));''',
'''        intent.putExtra("position_ms",Math.max(0L,resumeMs));''',"retain position")
p.write_text(s)

g=root/"app/build.gradle"
x=g.read_text()
if "versionName '2.4.0'" not in x:raise RuntimeError("Previous browser patch missing")
g.write_text(x.replace("versionName '2.4.0'","versionName '2.5.0'").replace("versionCode 15","versionCode 16"))
print("Browser v2.5: detected != saved; inline +, Vista metadata editor, save only after RESULT_OK")
