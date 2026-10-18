package com.wifitvhub.universal;
import android.content.*;
import android.content.pm.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

public final class HubServer {
 final Context ctx;final String pin;final ExecutorService workers=Executors.newCachedThreadPool();
 volatile boolean active=false;ServerSocket server;final android.content.SharedPreferences prefs;
 HubServer(Context c,String p){ctx=c;pin=p;prefs=c.getSharedPreferences("hub",0);}
 void start()throws IOException{server=new ServerSocket(8787);active=true;workers.execute(()->{while(active){try{Socket s=server.accept();workers.execute(()->serve(s));}catch(Exception e){if(!active)return;}}});}
 void stop(){active=false;try{if(server!=null)server.close();}catch(Exception ignored){}workers.shutdownNow();}
 String line(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();while(out.size()<8192){int b=in.read();if(b<0)return null;if(b==10)return out.toString("UTF-8").replace("\r","");out.write(b);}throw new IOException("Header too long");}
 JSONObject body(InputStream in,long size)throws Exception{if(size<0||size>16384)throw new IOException("Invalid JSON size");byte[] b=new byte[(int)size];new DataInputStream(in).readFully(b);return new JSONObject(new String(b,"UTF-8"));}
 void send(OutputStream out,int code,JSONObject value)throws IOException{byte[] data=value.toString().getBytes("UTF-8");String header="HTTP/1.1 "+code+" OK\r\nContent-Type: application/json\r\nContent-Length: "+data.length+"\r\nConnection: close\r\nX-Content-Type-Options: nosniff\r\n\r\n";out.write(header.getBytes("UTF-8"));out.write(data);out.flush();}
 void serve(Socket sock){try(Socket s=sock){s.setSoTimeout(60000);InputStream in=new BufferedInputStream(s.getInputStream());OutputStream out=s.getOutputStream();
  String first=line(in);if(first==null)return;String[] parts=first.split(" ");if(parts.length<2)return;String method=parts[0],path=parts[1].split("\\?")[0];Map<String,String> hdr=new HashMap<>();String l;
  while((l=line(in))!=null&&!l.isEmpty()){int i=l.indexOf(':');if(i>0)hdr.put(l.substring(0,i).toLowerCase(Locale.ROOT),l.substring(i+1).trim());}
  long size;try{size=Long.parseLong(hdr.getOrDefault("content-length","0"));}catch(Exception e){send(out,400,new JSONObject().put("error","Invalid size"));return;}
  if(size<0||size>500000000){send(out,413,new JSONObject().put("error","Too large"));return;}
  if(method.equals("POST")&&path.equals("/pair")){
   JSONObject req=body(in,size);if(!pin.equals(req.optString("pin"))){send(out,403,new JSONObject().put("error","Incorrect PIN"));return;}
   byte[] random=new byte[32];new SecureRandom().nextBytes(random);StringBuilder sb=new StringBuilder();for(byte b:random)sb.append(String.format(Locale.ROOT,"%02x",b&255));String token=sb.toString();prefs.edit().putString("token",token).apply();send(out,200,new JSONObject().put("token",token));return;
  }
  String token=prefs.getString("token","");if(token.isEmpty()||!hdr.getOrDefault("authorization","").equals("Bearer "+token)){send(out,401,new JSONObject().put("error","Pair first"));return;}
  JSONObject ans=new JSONObject().put("ok",true);int status=200;
  if(method.equals("GET")&&path.equals("/status")){ans.put("device",Build.MODEL).put("platform","Android TV").put("connection","ok");}
  else if(method.equals("GET")&&path.equals("/apps")){JSONArray a=new JSONArray();PackageManager pm=ctx.getPackageManager();Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo> rs=pm.queryIntentActivities(q,0);for(ResolveInfo r:rs)a.put(new JSONObject().put("name",r.loadLabel(pm).toString()).put("package",r.activityInfo.packageName));ans.put("apps",a);}
  else if(method.equals("POST")&&path.equals("/launch")){String pkg=body(in,size).optString("package");Intent i=ctx.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){status=404;ans=new JSONObject().put("error","App not found");}else{ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}}
  else if(method.equals("POST")&&path.equals("/browser")){String url=body(in,size).optString("url");if(!url.startsWith("http://")&&!url.startsWith("https://")){status=400;ans=new JSONObject().put("error","Invalid URL");}else{ctx.startActivity(new Intent(ctx,BrowserActivity.class).putExtra("url",url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}}
  else if(method.equals("POST")&&path.equals("/home")){Intent home=new Intent(Intent.ACTION_MAIN);home.addCategory(Intent.CATEGORY_HOME);home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);ctx.startActivity(home);}
  else if(method.equals("POST")&&path.equals("/volume")){String action=body(in,size).optString("action");AudioManager audio=(AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);if(action.equals("up"))audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);else if(action.equals("down"))audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);else if(action.equals("mute"))audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_TOGGLE_MUTE,AudioManager.FLAG_SHOW_UI);}
  else if(method.equals("GET")&&path.equals("/files")){JSONArray a=new JSONArray();File[] fs=new File(ctx.getFilesDir(),"incoming").listFiles();if(fs!=null)for(File f:fs)if(f.isFile())a.put(new JSONObject().put("name",f.getName()).put("bytes",f.length()));ans.put("files",a);}
  else if(method.equals("PUT")&&path.equals("/upload")){String name=hdr.getOrDefault("x-filename","transfer.bin").replaceAll("[^A-Za-z0-9._-]","_");if(name.isEmpty()||name.equals(".")||name.equals(".."))name="transfer.bin";if(name.length()>100)name=name.substring(0,100);File dir=new File(ctx.getFilesDir(),"incoming");dir.mkdirs();File dest=new File(dir,name),temp=new File(dir,name+".part");
   try(FileOutputStream f=new FileOutputStream(temp)){byte[] buf=new byte[65536];long remain=size;while(remain>0){int n=in.read(buf,0,(int)Math.min(buf.length,remain));if(n<0)throw new EOFException();f.write(buf,0,n);remain-=n;}}if(!temp.renameTo(dest))throw new IOException("Could not save file");status=201;ans.put("name",name).put("bytes",size);}
  else if(method.equals("POST")&&path.equals("/install")){String n=body(in,size).optString("name");if(n.contains("/")||n.contains("\\")||!n.toLowerCase().endsWith(".apk")){status=400;ans=new JSONObject().put("error","Invalid APK filename");}else{File f=new File(new File(ctx.getFilesDir(),"incoming"),n);if(!f.isFile()){status=404;ans=new JSONObject().put("error","APK not found");}else{Uri uri=Uri.parse("content://com.wifitvhub.universal.files/"+Uri.encode(n));Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);ctx.startActivity(i);ans.put("requiresConfirmation",true);}}}
  else{status=404;ans=new JSONObject().put("error","Unknown command");}send(out,status,ans);
 }catch(Exception ignored){}}
}
