from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text()
# The original native player/editor remains intact. A ResultReceiver is used
# as a redundant success receipt because some cross-app Android activity
# stacks have returned RESULT_CANCELED after a confirmed VistaStream save.
needle='''                    awaitingVistaImport=item;
                    startActivityForResult(intent,VISTA_IMPORT_REQUEST);'''
assert s.count(needle)==1,"Original VistaStream import launch changed"
replacement='''                    awaitingVistaImport=item;
                    android.os.ResultReceiver confirmation=new android.os.ResultReceiver(
                        new android.os.Handler(android.os.Looper.getMainLooper())){
                        @Override protected void onReceiveResult(int code,android.os.Bundle data){
                            if(code!=RESULT_OK||data==null||awaitingVistaImport==null)return;
                            String saved=data.getString("com.vistastream.tv.extra.URL","");
                            if(!awaitingVistaImport.url.equals(saved))return;
                            Intent receipt=new Intent()
                                .putExtra("com.vistastream.tv.extra.URL",saved)
                                .putExtra("com.vistastream.tv.extra.TITLE",
                                    data.getString("com.vistastream.tv.extra.TITLE",""))
                                .putExtra("com.vistastream.tv.extra.SAVED_WEB_ONLY",
                                    data.getBoolean("com.vistastream.tv.extra.SAVED_WEB_ONLY",false));
                            handleVistaImportResult(RESULT_OK,receipt);
                        }
                    };
                    intent.putExtra("com.vistastream.tv.extra.CONFIRMATION_RECEIVER",
                        confirmation);
                    startActivityForResult(intent,VISTA_IMPORT_REQUEST);'''
s=s.replace(needle,replacement,1)
needle='''        MediaItem chosen=awaitingVistaImport;
        awaitingVistaImport=null;'''
assert s.count(needle)==1,"Confirmation handler changed"
replacement='''        MediaItem chosen=awaitingVistaImport;
        if(chosen==null)return; // Receipt already accepted: ignore subsequent Android cancel.
        awaitingVistaImport=null;
        if(resultCode==RESULT_OK && data!=null &&
            data.getBooleanExtra("com.vistastream.tv.extra.SAVED_WEB_ONLY",false)){
            toast("Página guardada no VistaStream como referência Web. A fonte deverá ser redescoberta; não foi importado um vídeo permanente.");
            return;
        }'''
s=s.replace(needle,replacement,1)
assert "private boolean sendToVistaStream(" in s
assert "private void handleVistaImportResult" in s
p.write_text(s)
print("VistaBrowser source import now accepts explicit matching VistaStream success receipt, ignores duplicate cancel callbacks and distinguishes web-only references.")
