package com.yure.videoharvester;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight, offline-only PT-BR URL/title hints for reviewed VistaStream imports. */
public final class VistaMetadataHints {
    private static final Pattern YEAR=Pattern.compile("(?<!\\d)(?:19\\d{2}|20\\d{2})(?!\\d)");
    private static final Pattern HASH=Pattern.compile("(?i)\\b(?=[a-f0-9]*[a-f])(?=[a-f0-9]*\\d)[a-f0-9]{8,}\\b");
    private static final Pattern JUNK=Pattern.compile("(?i)\\b(?:dublado|legendado|dual[ _.-]*audio|multi[ _.-]*audio|dub|sub|pt[ _.-]*br|1080p?|720p?|2160p?|480p?|4k|full[ _.-]*hd|hdtv|bluray|webrip|web[ _.-]*dl|assistir|online|gratis|completo|filme|movie)\\b");
    public static final class Hint {
        public final String title;
        public final int year;
        public final boolean urlUsed;
        Hint(String title,int year,boolean urlUsed){this.title=title;this.year=year;this.urlUsed=urlUsed;}
    }
    private VistaMetadataHints(){}
    private static String clean(String input){
        return YEAR.matcher(JUNK.matcher(HASH.matcher(input.replaceAll("[._+\\-]+"," "))
            .replaceAll(" ")).replaceAll(" ")).replaceAll(" ")
            .replaceAll("(?i)\\b(?:x264|x265|h264|hevc|aac|mkv|mp4|m3u8)\\b"," ")
            .replaceAll("\\s+"," ").trim();
    }
    public static Hint inspect(String rawTitle,String pageUrl){
        if(rawTitle==null)rawTitle="";
        if(pageUrl==null)pageUrl="";
        String segment="";
        try{
            URI uri=new URI(pageUrl.trim());
            if(("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))
                &&uri.getHost()!=null){
                String path=uri.getRawPath();
                if(path!=null&&!path.isEmpty()){
                    segment=URLDecoder.decode(path.substring(path.lastIndexOf('/')+1),"UTF-8");
                    segment=segment.replaceFirst("(?i)\\.(?:html?|php)$","");
                }
            }
        }catch(Exception ignored){}
        String fromUrl=clean(segment);
        String fromTitle=clean(rawTitle);
        int year=0;
        Matcher m=YEAR.matcher(segment);
        if(!m.find())m=YEAR.matcher(rawTitle);
        if(m.find(0)){
            try{year=Integer.parseInt(m.group());}catch(NumberFormatException ignored){}
        }
        boolean strongUrl=fromUrl.length()>=5&&fromUrl.contains(" ")
            &&!fromUrl.toLowerCase(Locale.ROOT).matches("(?:player|server|embed|watch|video).*");
        String title=fromTitle;
        if(strongUrl&&(fromTitle.isEmpty() || fromTitle.toLowerCase(Locale.ROOT).contains(fromUrl.toLowerCase(Locale.ROOT)) ||
            fromUrl.toLowerCase(Locale.ROOT).contains(fromTitle.toLowerCase(Locale.ROOT))))title=fromUrl;
        else if(title.isEmpty()&&strongUrl)title=fromUrl;
        if(title.isEmpty())title=rawTitle.trim();
        return new Hint(title,year,strongUrl);
    }
}
