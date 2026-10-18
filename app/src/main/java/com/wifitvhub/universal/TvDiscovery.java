package com.wifitvhub.universal;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Queue;

public final class TvDiscovery {
 public interface Callback { void onFound(String name,String ip,boolean hub); void onStatus(String message); }
 static final String HUB_TYPE="_wifitvhub._tcp.";
 static final String CAST_TYPE="_googlecast._tcp.";
 private final Context context;
 private final NsdManager nsd;
 private final Callback callback;
 private final Queue<DiscoveryItem> pending=new ArrayDeque<>();
 private boolean resolving=false;
 private boolean active=false;
 private NsdManager.DiscoveryListener hubListener,castListener;
 private WifiManager.MulticastLock multicastLock;

 static final class DiscoveryItem {
  final NsdServiceInfo service;final boolean hub;
  DiscoveryItem(NsdServiceInfo s,boolean h){service=s;hub=h;}
 }
 public TvDiscovery(Context c,Callback cb){
  context=c.getApplicationContext();callback=cb;
  nsd=(NsdManager)context.getSystemService(Context.NSD_SERVICE);
 }
 public void start(){
  if(active)return;
  active=true;
  try{
   WifiManager w=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
   if(w!=null){multicastLock=w.createMulticastLock("WiFiTVHub discovery");multicastLock.setReferenceCounted(false);multicastLock.acquire();}
  }catch(Exception ignored){}
  hubListener=listen(true);
  castListener=listen(false);
  try{nsd.discoverServices(HUB_TYPE,NsdManager.PROTOCOL_DNS_SD,hubListener);}catch(Exception e){callback.onStatus("Erro ao buscar WiFi TV Hub: "+e.getMessage());}
  try{nsd.discoverServices(CAST_TYPE,NsdManager.PROTOCOL_DNS_SD,castListener);}catch(Exception e){callback.onStatus("Erro ao buscar Google Cast: "+e.getMessage());}
 }
 private NsdManager.DiscoveryListener listen(boolean hub){
  return new NsdManager.DiscoveryListener(){
   @Override public void onStartDiscoveryFailed(String type,int code){callback.onStatus("Falha ao buscar "+type+" ("+code+").");}
   @Override public void onStopDiscoveryFailed(String type,int code){}
   @Override public void onDiscoveryStarted(String type){}
   @Override public void onDiscoveryStopped(String type){}
   @Override public void onServiceFound(NsdServiceInfo info){if(active)enqueue(new DiscoveryItem(info,hub));}
   @Override public void onServiceLost(NsdServiceInfo info){}
  };
 }
 private synchronized void enqueue(DiscoveryItem item){
  if(!active)return;
  pending.add(item);
  resolveNext();
 }
 private synchronized void resolveNext(){
  if(!active||resolving||pending.isEmpty())return;
  DiscoveryItem item=pending.poll();resolving=true;
  try{
   nsd.resolveService(item.service,new NsdManager.ResolveListener(){
    @Override public void onResolveFailed(NsdServiceInfo service,int errorCode){done();}
    @Override public void onServiceResolved(NsdServiceInfo service){
     InetAddress host=service.getHost();
     if(active&&host instanceof Inet4Address)callback.onFound(service.getServiceName(),host.getHostAddress(),item.hub);
     done();
    }
    private void done(){synchronized(TvDiscovery.this){resolving=false;resolveNext();}}
   });
  }catch(Exception e){resolving=false;resolveNext();}
 }
 public synchronized void stop(){
  if(!active)return;
  active=false;
  pending.clear();
  if(hubListener!=null)try{nsd.stopServiceDiscovery(hubListener);}catch(Exception ignored){}
  if(castListener!=null)try{nsd.stopServiceDiscovery(castListener);}catch(Exception ignored){}
  if(multicastLock!=null)try{if(multicastLock.isHeld())multicastLock.release();}catch(Exception ignored){}
 }
 public static NsdManager.RegistrationListener register(Context ctx,String deviceName,int port){
  final NsdManager nsd=(NsdManager)ctx.getSystemService(Context.NSD_SERVICE);
  NsdServiceInfo info=new NsdServiceInfo();
  info.setServiceName(deviceName);
  info.setServiceType(HUB_TYPE);
  info.setPort(port);
  NsdManager.RegistrationListener listener=new NsdManager.RegistrationListener(){
   @Override public void onRegistrationFailed(NsdServiceInfo serviceInfo,int errorCode){}
   @Override public void onUnregistrationFailed(NsdServiceInfo serviceInfo,int errorCode){}
   @Override public void onServiceRegistered(NsdServiceInfo serviceInfo){}
   @Override public void onServiceUnregistered(NsdServiceInfo serviceInfo){}
  };
  nsd.registerService(info,NsdManager.PROTOCOL_DNS_SD,listener);
  return listener;
 }
 public static void unregister(Context ctx,NsdManager.RegistrationListener listener){
  if(listener==null)return;
  try{((NsdManager)ctx.getSystemService(Context.NSD_SERVICE)).unregisterService(listener);}catch(Exception ignored){}
 }
}
