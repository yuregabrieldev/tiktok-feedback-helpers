package com.yure.videoharvester;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.util.Locale;

/** Explicit user-initiated handoff to the separate Vista Stream APK. */
public final class HarvesterVistaBridge {
  private HarvesterVistaBridge() {}
  private static final String PLAY="com.vistastream.tv.action.PLAY";
  private static final String EXTRA="com.vistastream.tv.extra.";
  private static final String PLAYER="com.ivor.openstream.harvester.HarvesterPlaybackActivity";

  public static boolean isShareable(String address,String format) {
    if(address==null||address.length()>8192||address.length()<8)return false;
    Uri uri=Uri.parse(address);
    if(uri.getHost()==null||!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme())))return false;
    String fmt=format==null?"":format.toUpperCase(Locale.ROOT);
    if(fmt.equals("REFERENCIA_IFRAME")||fmt.equals("IFRAME"))return false;
    String lower=address.toLowerCase(Locale.ROOT);
    for(String marker:new String[]{"token=","ip_bind=","signature=","sig=","expires=","policy=",
        "proxy.mp4","&url=","sv=","auth=","bearer=","key=","session="})
      if(lower.contains(marker))return false;
    return fmt.equals("MP4")||fmt.equals("HLS")||fmt.equals("DASH")||
      fmt.equals("WEBM")||fmt.equals("VIDEO")||fmt.equals("HTML5");
  }

  private static String mime(String format,String url) {
    String f=(format==null?"":format).toUpperCase(Locale.ROOT);
    String l=url.toLowerCase(Locale.ROOT);
    if(f.equals("HLS")||l.matches(".*[.]m3u8([?#].*)?$"))return "application/vnd.apple.mpegurl";
    if(f.equals("DASH")||l.matches(".*[.]mpd([?#].*)?$"))return "application/dash+xml";
    if(f.equals("WEBM")||l.matches(".*[.]webm([?#].*)?$"))return "video/webm";
    if(f.equals("MP4")||l.matches(".*[.](mp4|m4v|mov)([?#].*)?$"))return "video/mp4";
    return null; // Let Media3 probe an otherwise unknown HTML5 format.
  }

  public static void open(Activity host,String address,String title,String format,String sourcePage) {
    if(!isShareable(address,format)) {
      Toast.makeText(host,"Esta fonte é temporária, uma referência iframe ou não é compatível.",Toast.LENGTH_LONG).show();
      return;
    }
    // A production and a debug APK can coexist. The current Vista build is .debug.
    String[] packages={"com.vistastream.tv.debug","com.vistastream.tv"};
    BrTitleParser.Parsed parsed=BrTitleParser.parse(title);
    for(String appPackage:packages) {
      try {
        Intent intent=new Intent(PLAY);
        intent.setClassName(appPackage,PLAYER);
        intent.putExtra(EXTRA+"URL",address);
        intent.putExtra(EXTRA+"TITLE",parsed.title+(parsed.season>0?" • T"+parsed.season:"")+
          (parsed.episode>0?"E"+parsed.episode:""));
        intent.putExtra(EXTRA+"MIME_TYPE",mime(format,address));
        if(sourcePage!=null) {
          Uri page=Uri.parse(sourcePage);
          if(page.getHost()!=null && ("https".equalsIgnoreCase(page.getScheme())||
              "http".equalsIgnoreCase(page.getScheme()))) {
            // Avoid persisting signed/session query parameters as referral metadata.
            String cleanPage=page.buildUpon().clearQuery().fragment(null).build().toString();
            intent.putExtra(EXTRA+"PAGE_URL",cleanPage);
            intent.putExtra(EXTRA+"REFERER",cleanPage);
          }
        }
        host.startActivity(intent);
        return;
      } catch (android.content.ActivityNotFoundException | SecurityException ignored) {
        // Try another installed package variant.
      }
    }
    Toast.makeText(host,"Instale o APK Vista Stream ou atualize-o para a versão com player Harvester.",Toast.LENGTH_LONG).show();
  }
}
