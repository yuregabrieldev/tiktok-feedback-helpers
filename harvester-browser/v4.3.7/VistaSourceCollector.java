package com.yure.videoharvester;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Passive, same-origin HTML5 inspection. No synthetic ad clicks, CAPTCHA
 * handling, DRM bypass, private cookies or cross-origin iframe access.
 * Scans are triggered only while the inspector is visible and opted in.
 */
public final class VistaSourceCollector {
    private static final String PREFS = "vista_smart_crawler_v1";
    private static final int MAX_ITEMS = 250;
    private VistaSourceCollector() {}

    public static void scan(Activity activity, WebView browser) {
        if (activity == null || browser == null || activity.isFinishing()) return;
        final String js = "(function(){try{let results=[];let frames=0;" +
            "function scan(doc){if(!doc||frames>24)return;" +
            "doc.querySelectorAll('video,source').forEach(function(el){" +
            "let url=el.currentSrc||el.src||el.getAttribute('src')||'';" +
            "if(url&&/^https?:\\/\\//i.test(url))results.push(url);});" +
            "doc.querySelectorAll('iframe').forEach(function(el){frames++;" +
            "if(frames>24)return;try{scan(el.contentDocument);}catch(e){} });}" +
            "scan(document);return JSON.stringify({title:document.title||''," +
            "page:location.href||'',frames:frames,urls:results.slice(0,100)});" +
            "}catch(e){return '{}';}})()";
        browser.evaluateJavascript(js, value -> {
            try {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (value == null || "null".equals(value)) return;
                JSONObject data = new JSONObject(new JSONArray("[" + value + "]").getString(0));
                String title = data.optString("title", "").trim();
                String page = stripQuery(data.optString("page", ""));
                JSONArray urls = data.optJSONArray("urls");
                if (urls == null) return;
                for (int i=0; i<urls.length(); i++)
                    offer(activity, urls.optString(i), title, page);
            } catch (Exception ignored) { }
        });
    }

    public static boolean eligible(String raw) {
        if (raw == null || raw.length() < 9 || raw.length() > 4096 ||
            raw.contains("@")) return false;
        try {
            Uri u = Uri.parse(raw);
            String scheme = u.getScheme(), host = u.getHost();
            if (host == null || !("https".equalsIgnoreCase(scheme) ||
                                  "http".equalsIgnoreCase(scheme))) return false;
            String q = u.getEncodedQuery();
            if (q != null && q.toLowerCase(Locale.ROOT).matches(
                ".*(?:^|&)(?:token|sig|signature|expires|expiry|auth|key|session|policy|ip_bind)=.*"))
                return false;
            return u.getPath() != null && u.getPath().toLowerCase(Locale.ROOT)
                .matches(".*\\.(?:mp4|m4v|m3u8|mpd|webm|mkv|mov|ts)$");
        } catch (Exception ignored) { return false; }
    }

    private static String stripQuery(String value) {
        try {
            Uri uri = Uri.parse(value);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) ||
                "https".equalsIgnoreCase(uri.getScheme()))) return "";
            return uri.buildUpon().clearQuery().fragment(null).build().toString();
        } catch (Exception ignored) { return ""; }
    }

    private static synchronized void offer(Context context,String url,String title,String page) {
        if (!eligible(url) || title == null || title.trim().isEmpty()) return;
        JSONArray items = entries(context);
        for(int i=0;i<items.length();i++){
            JSONObject old=items.optJSONObject(i);
            if(old!=null && url.equals(old.optString("url")))return;
        }
        try {
            BrTitleParser.Parsed parsed = BrTitleParser.parse(title);
            boolean identified = parsed != null && parsed.title != null &&
                parsed.title.length() >= 3 && parsed.season > 0 && parsed.episode > 0;
            JSONObject item = new JSONObject();
            item.put("url", url);
            item.put("title", identified ? parsed.title : title.substring(0,Math.min(180,title.length())));
            item.put("pageUrl", page);
            item.put("kind", identified ? "tv" : "movie");
            item.put("season", identified ? parsed.season : 1);
            item.put("episode", identified ? parsed.episode : 1);
            item.put("savedAt", System.currentTimeMillis());
            item.put("identified", identified);
            JSONArray next = new JSONArray();
            next.put(item);
            for(int i=0;i<items.length() && next.length()<MAX_ITEMS;i++)next.put(items.get(i));
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putString("items",next.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static synchronized JSONArray entries(Context context) {
        try {
            return new JSONArray(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getString("items","[]"));
        } catch (Exception ignored) { return new JSONArray(); }
    }

    public static String export(Context context) throws Exception {
        JSONArray items = entries(context), approved = new JSONArray();
        for (int i=0; i<items.length() && approved.length()<MAX_ITEMS; i++) {
            JSONObject item = items.optJSONObject(i);
            if(item == null || !item.optBoolean("identified") ||
               !eligible(item.optString("url")))continue;
            JSONObject copy = new JSONObject(item.toString());
            copy.remove("identified");
            approved.put(copy);
        }
        return new JSONObject()
            .put("schemaVersion", 1)
            .put("id", "vista-crawler")
            .put("name", "VistaBrowser · descoberta pessoal")
            .put("type", "vista-source")
            .put("generatedAt", System.currentTimeMillis())
            .put("items", approved)
            .toString(2);
    }
}
