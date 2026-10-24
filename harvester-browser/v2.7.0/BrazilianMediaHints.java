package com.harvester.browser.core;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata normalization for videos the WebView has already observed.
 *
 * Inspired by the *structure* of PT-BR CloudStream/Aniyomi extensions. This
 * does not scrape sites, bypass access controls, download binaries, or make
 * HTTP requests. Every prediction is editable in Vista Stream before save.
 */
public final class BrazilianMediaHints {
    private BrazilianMediaHints() {}

    private static final Pattern EPISODE = Pattern.compile(
        "(?i)\\b(?:s(?:eason)?\\s*0*(\\d{1,3})[ ._-]*e(?:p(?:is[oó]dio)?)?\\s*0*(\\d{1,4})|t(?:emporada)?\\s*0*(\\d{1,3})\\s*(?:e|ep|epis[oó]dio)\\s*0*(\\d{1,4})|(\\d{1,3})x(\\d{1,4}))\\b");
    private static final Pattern QUALITY = Pattern.compile("(?i)\\b(2160|1440|1080|720|480|360)p\\b");

    public static final class Result {
        public final String locale, audioMode, quality, format, providerLabel;
        public final int season, episode;
        public Result(String locale, String audioMode, String quality, String format,
                      String providerLabel, int season, int episode) {
            this.locale=locale; this.audioMode=audioMode; this.quality=quality;
            this.format=format; this.providerLabel=providerLabel;
            this.season=season; this.episode=episode;
        }
    }

    public static Result infer(String detectedTitle, String pageTitle,
                               String pageUrl, String mediaFormat) {
        String title=(safe(detectedTitle)+" "+safe(pageTitle)).toLowerCase(Locale.ROOT);
        String folded=fold(title);
        boolean brazilian=folded.matches(".*\\b(pt[- _]?br|portugues(?: brasileiro)?|brazilian portuguese)\\b.*");
        String locale=brazilian?"pt-BR":"";
        boolean multi=contains(folded,"dual audio") || contains(folded,"multi audio") ||
                      contains(folded,"audio dual") || contains(folded,"audio multiplo");
        boolean dubbed=contains(folded,"dublado") || contains(folded,"dubbed");
        boolean subbed=contains(folded,"legendado") || contains(folded,"subbed");
        String audio=multi?"multi":dubbed?"dub":subbed?"sub":"";
        Matcher q=QUALITY.matcher(folded);
        String quality=q.find()?q.group(1)+"p":"";
        if(quality.isEmpty() && contains(folded,"4k")) quality="2160p";
        Matcher ep=EPISODE.matcher(title);
        int season=0, episode=0;
        if(ep.find()){
            try {
                if(ep.group(1)!=null){season=Integer.parseInt(ep.group(1));episode=Integer.parseInt(ep.group(2));}
                else if(ep.group(3)!=null){season=Integer.parseInt(ep.group(3));episode=Integer.parseInt(ep.group(4));}
                else {season=Integer.parseInt(ep.group(5));episode=Integer.parseInt(ep.group(6));}
            } catch(NumberFormatException ignored){season=0;episode=0;}
        }
        season=Math.min(999,season);
        episode=Math.min(9999,episode);
        String provider=host(pageUrl);
        String format=safe(mediaFormat).toLowerCase(Locale.ROOT);
        return new Result(locale,audio,quality,format,provider,season,episode);
    }

    private static boolean contains(String title,String term) {
        return (" "+title+" ").contains(" "+term+" ");
    }
    private static String safe(String value){return value==null?"":value.trim();}
    private static String fold(String s) {
        String t=java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","");
        return t.replace('_',' ').replace('-',' ').replaceAll("\\s+"," ");
    }
    private static String host(String url){
        try {
            URI u=new URI(safe(url));
            String host=u.getHost();
            if(host==null || !("https".equalsIgnoreCase(u.getScheme()) ||
                                "http".equalsIgnoreCase(u.getScheme())))return "";
            return host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.","").substring(
                0,Math.min(90,host.replaceFirst("^www\\.","").length()));
        }catch(Exception ignored){return "";}
    }
}
