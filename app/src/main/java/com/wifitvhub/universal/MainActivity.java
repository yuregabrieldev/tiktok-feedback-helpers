package com.wifitvhub.universal;

import android.app.*;
import android.os.*;
import android.app.UiModeManager;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 LinearLayout panel;TextView info;EditText host,pin;HubServer server;ExecutorService pool=Executors.newSingleThreadExecutor();android.content.SharedPreferences prefs;
 boolean tv=false;
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("hub",0);UiModeManager ui=(UiModeManager)getSystemService(UI_MODE_SERVICE);tv=ui.getCurrentModeType()==Configuration.UI_MODE_TYPE_TELEVISION;show();}
 LinearLayout page(){panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(28,24,28,28);panel.setBackgroundColor(Color.rgb(239,244,251));ScrollView s=new ScrollView(this);s.addView(panel);setContentView(s);return panel;}
 void title(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(24);t.setTextColor(Color.rgb(26,39,68));t.setPadding(2,14,2,12);panel.addView(t);}
 EditText edit(String hint,String initial){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setText(initial);panel.addView(e);return e;}
 void label(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(16);t.setPadding(3,8,3,8);panel.addView(t);}
 void button(String text,Runnable fn){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->fn.run());panel.addView(b);}
 void show(){page();title("WiFi TV Hub");if(tv)tvPage();else phonePage();}
 void tvPage(){
  String p=prefs.getString("pin",null);if(p==null){p=String.valueOf(100000+new java.security.SecureRandom().nextInt(900000));prefs.edit().putString("pin",p).apply();}
  label("MODO TV · Hisense Android TV e outras Android/Google TV");
  label("IP: "+localIP()+":8787");label("PIN: "+p);
  label("Mantenha esta tela aberta durante a transferência. Celular e TV precisam estar na mesma rede Wi-Fi.");
  try{server=new HubServer(getApplicationContext(),p);server.start();label("Servidor Wi-Fi iniciado.");}catch(Exception e){label("Falha no servidor: "+e.getMessage());}
  button("Abrir navegador",()->startActivity(new Intent(this,BrowserActivity.class)));
  button("Ver aplicativos",()->showLocalApps());
  button("Ver arquivos recebidos",()->{
   File d=new File(getFilesDir(),"incoming");StringBuilder sb=new StringBuilder();
   File[] fs=d.listFiles();if(fs!=null)for(File f:fs)if(f.isFile())sb.append(f.getName()).append(" (").append(f.length()).append(" bytes)\n");
   new AlertDialog.Builder(this).setTitle("Arquivos recebidos").setMessage(sb.length()==0?"Nenhum arquivo.":sb.toString()).setPositiveButton("OK",null).show();
  });
 }
 String localIP(){try{Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();while(all.hasMoreElements()){NetworkInterface ni=all.nextElement();Enumeration<InetAddress> ips=ni.getInetAddresses();while(ips.hasMoreElements()){InetAddress i=ips.nextElement();if(i instanceof Inet4Address&&!i.isLoopbackAddress())return i.getHostAddress();}}}catch(Exception ignored){}return "IP desconhecido";}
 void showLocalApps(){android.content.pm.PackageManager pm=getPackageManager();Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);List<android.content.pm.ResolveInfo> apps=pm.queryIntentActivities(q,0);String[] names=new String[apps.size()];for(int i=0;i<apps.size();i++)names[i]=apps.get(i).loadLabel(pm).toString();new AlertDialog.Builder(this).setTitle("Aplicativos").setItems(names,(d,i)->{Intent launch=pm.getLaunchIntentForPackage(apps.get(i).activityInfo.packageName);if(launch!=null)startActivity(launch);}).setNegativeButton("Voltar",null).show();}
 void phonePage(){
  label("MODO CELULAR · Primeiro instale este APK também na Android TV, depois conecte pelo Wi-Fi.");
  host=edit("IP da TV, ex.: 192.168.1.50",prefs.getString("ip",""));pin=edit("PIN mostrado na televisão","");
  info=new TextView(this);info.setText("Informe o IP e o PIN da televisão.");info.setTextSize(16);info.setPadding(4,16,4,16);panel.addView(info);
  button("Parear com a TV",()->{String h=host.getText().toString().trim(),p=pin.getText().toString().trim();run(()->{String res=request(h,"POST","/pair",new JSONObject().put("pin",p).toString().getBytes("UTF-8"));String token=new JSONObject(res).getString("token");prefs.edit().putString("ip",h).putString("token",token).apply();return "Conectado à TV: "+h;});});
  button("Testar conexão",()->command("GET","/status",null));
  title("Controle da TV");
  button("Tela inicial",()->command("POST","/home","{}"));
  LinearLayout volumes=new LinearLayout(this);volumes.setOrientation(0);panel.addView(volumes);
  for(String spec:new String[]{"Volume +|up","Volume -|down","Mudo|mute"}){String[] v=spec.split("\\|");Button x=new Button(this);x.setText(v[0]);x.setAllCaps(false);x.setOnClickListener(z->command("POST","/volume","{\"action\":\""+v[1]+"\"}"));volumes.addView(x,new LinearLayout.LayoutParams(0,-2,1));}
  button("Listar e abrir aplicativos",()->{String h=host.getText().toString().trim();run(()->{JSONArray apps=new JSONObject(request(h,"GET","/apps",null)).getJSONArray("apps");String[] names=new String[apps.length()],pkgs=new String[apps.length()];for(int i=0;i<apps.length();i++){JSONObject o=apps.getJSONObject(i);names[i]=o.getString("name");pkgs[i]=o.getString("package");}runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Aplicativos da TV").setItems(names,(d,i)->command("POST","/launch","{\"package\":\""+pkgs[i]+"\"}")).setNegativeButton("Fechar",null).show());return "Encontrados "+names.length+" aplicativos";});});
  button("Abrir site no navegador da TV",()->prompt("Endereço web","https://",(v)->{try{command("POST","/browser",new JSONObject().put("url",v).toString());}catch(Exception e){message(e.toString());}}));
  title("Transferência de arquivos");
  button("Enviar arquivo / APK",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,42);});
  button("Listar arquivos na TV",()->command("GET","/files",null));
  button("Solicitar instalação de APK na TV",()->prompt("Nome do APK recebido","app.apk",v->{try{command("POST","/install",new JSONObject().put("name",v).toString());}catch(Exception e){message(e.toString());}}));
  label("Instalação de APK requer autorização e confirmação no Android TV. Esta versão depende do aplicativo WiFi TV Hub aberto na televisão.");
 }
 interface Value{void accept(String s);}
 void prompt(String title,String initial,Value fn){EditText e=new EditText(this);e.setSingleLine();e.setText(initial);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Enviar",(d,w)->fn.accept(e.getText().toString())).setNegativeButton("Cancelar",null).show();}
 void message(String s){runOnUiThread(()->{if(info!=null)info.setText(s.length()>4000?s.substring(0,4000):s);else Toast.makeText(this,s,Toast.LENGTH_LONG).show();});}
 interface Work{String call()throws Exception;}
 void run(Work work){pool.execute(()->{try{message(work.call());}catch(Exception e){message("Erro: "+e.getMessage());}});}
 void command(String method,String path,String payload){String h=host.getText().toString().trim();run(()->request(h,method,path,payload==null?null:payload.getBytes("UTF-8")));}
 String request(String h,String method,String path,byte[] data)throws Exception{
  if(!h.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))throw new IOException("Introduza o IP IPv4 da TV");
  HttpURLConnection c=(HttpURLConnection)new URL("http://"+h+":8787"+path).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(30000);c.setRequestMethod(method);
  String token=prefs.getString("token","");if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
  if(data!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setFixedLengthStreamingMode(data.length);try(OutputStream out=c.getOutputStream()){out.write(data);}}
  int code=c.getResponseCode();InputStream in=code<400?c.getInputStream():c.getErrorStream();String response="";if(in!=null){try(InputStream s=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=s.read(buf))>0&&b.size()<200000)b.write(buf,0,n);response=b.toString("UTF-8");}}c.disconnect();
  if(code>=400)throw new IOException("HTTP "+code+": "+response);return response;
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=42||result!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;String h=host.getText().toString().trim();run(()->{
  String name="transfer.bin";long size=-1;try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst()){name=c.getString(0);size=c.getLong(1);}}
  if(size<0||size>500000000)throw new IOException("Arquivo deve ter tamanho conhecido e até 500 MB");name=name.replaceAll("[^A-Za-z0-9._-]","_");if(name.length()>100)name=name.substring(0,100);
  HttpURLConnection c=(HttpURLConnection)new URL("http://"+h+":8787/upload").openConnection();c.setRequestMethod("PUT");c.setConnectTimeout(10000);c.setReadTimeout(90000);c.setDoOutput(true);c.setFixedLengthStreamingMode(size);c.setRequestProperty("Authorization","Bearer "+prefs.getString("token",""));c.setRequestProperty("X-Filename",name);
  try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=c.getOutputStream()){byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}
  int code=c.getResponseCode();c.disconnect();if(code>=400)throw new IOException("Falha no envio: HTTP "+code);return "Arquivo enviado: "+name;
 });}
 @Override protected void onDestroy(){if(server!=null)server.stop();pool.shutdownNow();super.onDestroy();}
}
