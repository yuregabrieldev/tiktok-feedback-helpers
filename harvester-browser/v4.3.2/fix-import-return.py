from pathlib import Path

base=Path("hb-v18/app/src/main/java")
main=base/"com/harvester/browser/MainActivity.java"
premium=base/"com/yure/videoharvester/VistaScraperPremiumActivity.java"
def patch_exact(file,old,new,label):
    src=file.read_text()
    if src.count(old)!=1:raise RuntimeError(f"{label}: expected one anchor, found {src.count(old)}")
    file.write_text(src.replace(old,new,1))
    print("PASS",label)

# Cross-application ResultReceiver is asynchronous. RESULT_CANCELED can be
# delivered before the successful receipt from the VistaStream editor. Previously
# we cleared awaitingVistaImport immediately; the legitimate save was then
# discarded and the app reported a false cancellation.
patch_exact(main,
'''        MediaItem chosen=awaitingVistaImport;
        if(chosen==null)return; // Receipt already accepted: ignore subsequent Android cancel.
        awaitingVistaImport=null;''',
'''        MediaItem chosen=awaitingVistaImport;
        if(chosen==null)return; // A matching receipt already completed this operation.
        if(resultCode!=RESULT_OK||data==null){
            final MediaItem previous=chosen;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
                // Wait for the independently delivered confirmed-save receipt.
                if(awaitingVistaImport!=previous)return;
                awaitingVistaImport=null;
                if(!isFinishing()&&!isDestroyed())
                    toast("A importação não foi confirmada. O vídeo permanece na lista de encontrados.");
            },1700);
            return;
        }
        awaitingVistaImport=null;''',"deferred false-cancel check in original browser")

# The existing browser library writer has changed across releases. Wrap
# the entire imported-result handler, preserving all of its original branches
# and DB operations while catching any runtime errors before the Activity dies.
patch_exact(main,
'''    private void handleVistaImportResult(int resultCode,Intent data) {''',
'''    private void handleVistaImportResult(int resultCode,Intent data) {
        try{
            handleVistaImportResultUnsafe(resultCode,data);
        }catch(RuntimeException e){
            android.util.Log.e("VistaBrowserImport","Erro ao atualizar a biblioteca local",e);
            awaitingVistaImport=null;
            if(!isFinishing()&&!isDestroyed())
                toast("O retorno do VistaStream foi recebido, mas o navegador não conseguiu atualizar a biblioteca.");
        }
    }
    private void handleVistaImportResultUnsafe(int resultCode,Intent data) {''',
    "safe browser callback, preserving existing library implementation")

patch_exact(premium,
'''        if(item==null)return; // Matching receipt processed; ignore duplicate Android result.
        pendingImport=null;pendingImportUrl=null;
        if(expected==null||result!=RESULT_OK||data==null){
            toast("Importação cancelada: nada foi guardado no scanner");return;
        }''',
'''        if(item==null)return; // Matching receipt processed; ignore duplicate Android result.
        if(expected==null||result!=RESULT_OK||data==null){
            final Catalog previous=item;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
                if(pendingImport!=previous)return; // Receipt already accepted.
                pendingImport=null;pendingImportUrl=null;
                if(!isFinishing()&&!isDestroyed())
                    toast("O VistaStream não confirmou a importação. Pode voltar a testar a fonte.");
            },1700);
            return;
        }
        pendingImport=null;pendingImportUrl=null;''',"deferred false-cancel check in embedded scraper")

patch_exact(premium,
'''        approve(item);
        saveScan();
        renderVideos();renderLibrary();refresh();
        toast("Fonte e metadados confirmados no VistaStream e no scanner");''',
'''        try{
            approve(item);
            saveScan();
            renderVideos();renderLibrary();refresh();
            if(data.getBooleanExtra("com.vistastream.tv.extra.SAVED_TEMPORARY",false))
                toast("Fonte guardada no VistaStream. Poderá expirar; não é um vídeo permanente.");
            else
                toast("Fonte e metadados confirmados no VistaStream e no scanner");
        }catch(RuntimeException e){
            android.util.Log.e("VistaBrowserScraper","Falha local após importação bem-sucedida",e);
            toast("Importado no VistaStream; erro a atualizar a biblioteca do scanner.");
        }''',"safe scanner library after remote import success")

# Both receipts must contain the same attributes as the onActivityResult path.
for file in [main,premium]:
    s=file.read_text()
    old='''                                data.getBoolean("com.vistastream.tv.extra.SAVED_WEB_ONLY",false));'''
    if old in s:
        s=s.replace(old,'''                                data.getBoolean("com.vistastream.tv.extra.SAVED_WEB_ONLY",false))
                                .putExtra("com.vistastream.tv.extra.SAVED_TEMPORARY",
                                    data.getBoolean("com.vistastream.tv.extra.SAVED_TEMPORARY",false));''')
    else:
        old='''                                data.getBoolean("com.vistastream.tv.extra.SAVED_WEB_ONLY",false));'''
    file.write_text(s)

g=Path("hb-v18/app/build.gradle")
s=g.read_text()
assert "versionName '4.3.1'" in s and "versionCode 36" in s
g.write_text(s.replace("versionName '4.3.1'","versionName '4.3.2'")
             .replace("versionCode 36","versionCode 37"))
print("VistaBrowser v4.3.2: false cancellation race solved, crash-safe local persistence and temporary HLS receipt.")
