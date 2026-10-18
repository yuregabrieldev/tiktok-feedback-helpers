package com.wifitvhub.universal;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
public class ApkProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 @Override public String getType(Uri uri){return "application/vnd.android.package-archive";}
 @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
 @Override public Uri insert(Uri u,ContentValues v){return null;}
 @Override public int delete(Uri u,String s,String[] a){return 0;}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  if(!"r".equals(mode))throw new FileNotFoundException();
  String n=uri.getLastPathSegment();if(n==null||n.contains("/")||n.contains("\\")||n.equals("..")||!n.toLowerCase().endsWith(".apk"))throw new FileNotFoundException();
  File dir=new File(getContext().getFilesDir(),"incoming"),f=new File(dir,n);
  try{if(!f.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile()))throw new FileNotFoundException();}catch(IOException e){throw new FileNotFoundException();}
  return ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
 }
}
