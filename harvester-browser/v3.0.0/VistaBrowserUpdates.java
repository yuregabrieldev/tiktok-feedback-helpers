package com.harvester.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Official VistaBrowser GitHub OTA updates: explicit user consent is required. */
public final class VistaBrowserUpdates {
    private static final String REPO="yuregabrieldev/tiktok-feedback-helpers";
    private static final String API="https://api.github.com/repos/"+REPO+"/releases?per_page=100";
    private static final String PREFS="vistabrowser-updates";
    private static final long INTERVAL=24L*60L*60L*1000L;
    private static boolean checking=false;

    private VistaBrowserUpdates(){}

    public static void check(Activity activity,boolean manual){
        if(activity==null||activity.isFinishing())return;
        SharedPreferences prefs=activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!manual && System.currentTimeMillis()-prefs.getLong("checked_at",0)<INTERVAL)return;
        synchronized(VistaBrowserUpdates.class){
            if(checking)return;
            checking=true;
        }
        new Thread(()->{
            String error=null;
            JSONObject found=null;
            String current="";
            try{
                current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
                HttpURLConnection con=(HttpURLConnection)new URL(API).openConnection();
                con.setConnectTimeout(10000);con.setReadTimeout(12000);
                con.setRequestProperty("Accept","application/vnd.github+json");
                con.setRequestProperty("User-Agent","VistaBrowser-Android");
                try {
                    if(con.getResponseCode()!=200)throw new Exception("GitHub HTTP "+con.getResponseCode());
                    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
                    try(InputStream input=con.getInputStream()){
                        byte[] buf=new byte[8192];int n;int size=0;
                        while((n=input.read(buf))!=-1){
                            size+=n;if(size>1024*1024)throw new Exception("Resposta demasiado grande");
                            out.write(buf,0,n);
                        }
                    }
                    JSONArray releases=new JSONArray(out.toString("UTF-8"));
                    for(int i=0;i<releases.length();i++){
                        JSONObject r=releases.optJSONObject(i);
                        if(r==null||r.optBoolean("draft")||r.optBoolean("prerelease"))continue;
                        String tag=r.optString("tag_name","");
                        if(!tag.matches("vistabrowser-v[0-9]+\\.[0-9]+\\.[0-9]+"))continue;
                        if(compare(tag.substring("vistabrowser-v".length()),current)<=0)continue;
                        if(found==null || compare(tag,found.optString("tag_name",""))>0)found=r;
                    }
                    prefs.edit().putLong("checked_at",System.currentTimeMillis()).apply();
                }finally{con.disconnect();}
            }catch(Exception e){error=e.getMessage()==null?"Erro de rede":e.getMessage();}
            final JSONObject newest=found;
            final String failure=error;
            final String installed=current;
            synchronized(VistaBrowserUpdates.class){checking=false;}
            activity.runOnUiThread(()->{
                if(activity.isFinishing()||activity.isDestroyed())return;
                if(failure!=null){
                    if(manual)new AlertDialog.Builder(activity)
                        .setTitle("Não foi possível verificar atualizações")
                        .setMessage(failure).setPositiveButton("OK",null).show();
                }else if(newest==null){
                    if(manual)new AlertDialog.Builder(activity)
                        .setTitle("VistaBrowser atualizado")
                        .setMessage("Versão instalada: "+installed+"\\nNão existem atualizações mais recentes.")
                        .setPositiveButton("OK",null).show();
                }else offer(activity,newest);
            });
        },"VistaBrowser-UpdateCheck").start();
    }

    private static int compare(String a,String b){
        String[] x=a.replace("vistabrowser-v","").split("\\.");
        String[] y=b.replace("vistabrowser-v","").split("\\.");
        for(int i=0;i<3;i++){
            int u=component(x,i),v=component(y,i);
            if(u!=v)return u<v?-1:1;
        }
        return 0;
    }
    private static int component(String[] arr,int index){
        if(index>=arr.length)return 0;
        try{return Integer.parseInt(arr[index]);}
        catch(Exception ex){return 0;}
    }

    private static void offer(Activity activity,JSONObject release){
        String tag=release.optString("tag_name","");
        JSONArray assets=release.optJSONArray("assets");
        JSONObject apk=null;
        if(assets!=null)for(int i=0;i<assets.length();i++){
            JSONObject a=assets.optJSONObject(i);
            if(a==null)continue;
            String name=a.optString("name","");
            if(name.startsWith("VistaBrowser-") && name.endsWith(".apk")){
                apk=a;break;
            }
        }
        if(apk==null){
            new AlertDialog.Builder(activity).setTitle("Nova versão disponível")
                .setMessage("A versão "+tag+" foi encontrada, mas o APK ainda não está disponível.")
                .setPositiveButton("OK",null).show();
            return;
        }
        final String url=apk.optString("browser_download_url","");
        final String digest=apk.optString("digest","");
        final String name=apk.optString("name","");
        if(!url.startsWith("https://github.com/"+REPO+"/releases/download/"+tag+"/")
            || !url.endsWith("/"+name) || name.contains("/") || !name.matches("VistaBrowser-[A-Za-z0-9._-]+\\.apk")){
            new AlertDialog.Builder(activity).setTitle("Atualização recusada")
                .setMessage("O endereço do APK não corresponde à release oficial.")
                .setPositiveButton("OK",null).show();
            return;
        }
        String changes=release.optString("body","");
        if(changes.length()>800)changes=changes.substring(0,800)+"…";
        new AlertDialog.Builder(activity)
            .setTitle("Nova versão do VistaBrowser")
            .setMessage("Versão "+tag.substring("vistabrowser-v".length())
                +" disponível.\\n\\n"+changes+"\\n\\nTransferir e instalar agora?")
            .setNegativeButton("Mais tarde",null)
            .setPositiveButton("Atualizar",(dialog,which)->download(activity,url,name,digest))
            .show();
    }

    private static void download(Activity activity,String url,String name,String digest){
        try{
            DownloadManager manager=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
            if(manager==null)throw new Exception("Gestor de transferências indisponível");
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle("VistaBrowser • atualização");
            request.setDescription("APK oficial do VistaBrowser");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setMimeType("application/vnd.android.package-archive");
            request.setDestinationInExternalFilesDir(activity,Environment.DIRECTORY_DOWNLOADS,name);
            long downloadId=manager.enqueue(request);
            activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putLong("pending_id",downloadId).putString("pending_digest",digest).apply();
            BroadcastReceiver receiver=new BroadcastReceiver(){
                @Override public void onReceive(Context context,Intent intent){
                    if(!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))return;
                    if(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)!=downloadId)return;
                    try{activity.getApplicationContext().unregisterReceiver(this);}
                    catch(Exception ignored){}
                    validateAndInstall(activity,manager,downloadId,digest);
                }
            };
            IntentFilter filter=new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if(Build.VERSION.SDK_INT>=33)
                activity.getApplicationContext().registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);
            else activity.getApplicationContext().registerReceiver(receiver,filter);
            android.widget.Toast.makeText(activity,
                "Atualização em transferência. Acompanhe a notificação.",
                android.widget.Toast.LENGTH_LONG).show();
        }catch(Exception ex){
            new AlertDialog.Builder(activity).setTitle("Transferência indisponível")
                .setMessage(ex.getMessage()).setPositiveButton("OK",null).show();
        }
    }

    private static void validateAndInstall(Activity activity,DownloadManager manager,
                                            long id,String expectedDigest){
        new Thread(()->{
            Uri uri=null;String error=null;
            try{
                DownloadManager.Query q=new DownloadManager.Query().setFilterById(id);
                try(Cursor c=manager.query(q)){
                    if(c==null||!c.moveToFirst())throw new Exception("Transferência não encontrada");
                    int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if(status!=DownloadManager.STATUS_SUCCESSFUL)
                        throw new Exception("A transferência não foi concluída");
                }
                uri=manager.getUriForDownloadedFile(id);
                if(uri==null)throw new Exception("APK transferido não encontrado");
                if(expectedDigest.startsWith("sha256:")){
                    MessageDigest sha=MessageDigest.getInstance("SHA-256");
                    try(InputStream in=activity.getContentResolver().openInputStream(uri)){
                        if(in==null)throw new Exception("Não foi possível ler o APK");
                        byte[] buf=new byte[32768];int n;
                        while((n=in.read(buf))!=-1)sha.update(buf,0,n);
                    }
                    StringBuilder hex=new StringBuilder();
                    for(byte b:sha.digest())hex.append(String.format(Locale.ROOT,"%02x",b&0xff));
                    if(!expectedDigest.substring(7).equalsIgnoreCase(hex.toString()))
                        throw new Exception("O APK não passou na verificação SHA-256");
                }
            }catch(Exception e){error=e.getMessage();}
            final Uri verified=uri;final String failure=error;
            activity.runOnUiThread(()->{
                if(activity.isFinishing()||activity.isDestroyed())return;
                if(failure!=null)new AlertDialog.Builder(activity)
                    .setTitle("Não foi possível instalar").setMessage(failure)
                    .setPositiveButton("OK",null).show();
                else showInstaller(activity,verified);
            });
        },"VistaBrowser-VerifyAPK").start();
    }
    private static void showInstaller(Activity activity,Uri uri){
        activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .remove("pending_id").remove("pending_digest").apply();
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(activity)
                .setTitle("Permitir instalação de atualizações")
                .setMessage("Ative 'Permitir desta fonte' nas definições do Android e volte ao VistaBrowser. O ficheiro transferido continua guardado.")
                .setPositiveButton("Abrir definições",(d,w)->{
                    Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+activity.getPackageName()));
                    activity.startActivity(settings);
                }).setNegativeButton("Mais tarde",null).show();
            return;
        }
        try{
            Intent installer=new Intent(Intent.ACTION_VIEW);
            installer.setDataAndType(uri,"application/vnd.android.package-archive");
            installer.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(installer);
        }catch(Exception e){
            new AlertDialog.Builder(activity).setTitle("Instalação indisponível")
                .setMessage("O Android não conseguiu abrir o instalador. Verifique as permissões da aplicação.")
                .setPositiveButton("OK",null).show();
        }
    }
}
