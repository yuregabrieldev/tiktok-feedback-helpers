package com.yure.videoharvester;
import android.app.*;
import android.content.*;
import android.os.*;
public class ScanNotificationService extends Service{
  private static final String CHANNEL="scan";
  @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Video Harvester — Scan",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
  @Override public int onStartCommand(Intent i,int flags,int startId){
    Intent open=new Intent(this,MainActivityV20.class);open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
    Notification n=b.setContentTitle("Video Harvester: scan em andamento").setContentText("Toque para voltar ao crawler. A tela bloqueada pode suspender o WebView.").setSmallIcon(android.R.drawable.ic_menu_search).setOngoing(true).setContentIntent(pi).build();
    if(Build.VERSION.SDK_INT>=29)startForeground(91,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);else startForeground(91,n);
    return START_NOT_STICKY;
  }
  @Override public android.os.IBinder onBind(Intent i){return null;}
}