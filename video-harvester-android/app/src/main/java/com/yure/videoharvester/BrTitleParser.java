package com.yure.videoharvester;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata hints for Brazilian film/series/anime titles. Hints do NOT certify
 * the actual audio language, subtitle tracks, or content availability.
 */
public final class BrTitleParser {
  private BrTitleParser() {}
  private static final Pattern COMPACT = Pattern.compile("(?i)\\bS(\\d{1,2})[. _-]*E(\\d{1,3})\\b");
  private static final Pattern WRITTEN = Pattern.compile("(?iu)\\b(\\d{1,2})\\s*[ªaºo]?\\s*temporada\\b.*?\\b(?:epis[oó]dio|ep\\.?)[ :#-]*(\\d{1,3})\\b");
  private static final Pattern NUMBERED_EPISODE = Pattern.compile("(?iu)\\b(?:epis[oó]dio|ep\\.?)[ :#-]*(\\d{1,3})\\b");
  private static final Pattern TECHNICAL = Pattern.compile("(?iu)\\b(?:2160p|1080p|720p|480p|4k|web[ ._-]?dl|blu[ ._-]?ray|brrip|hdrip|h\\.?264|h\\.?265|x264|x265|hevc|aac|ddp5\\.?1)\\b");
  private static final Pattern AUDIO_HINTS = Pattern.compile("(?iu)\\b(?:dublado|legendado|dual[ ._-]?audio|multi[ ._-]?audio|pt[ ._-]?br|portugu[eê]s)\\b");
  private static final Pattern SPACES = Pattern.compile("[\\s._]+");

  public static final class Parsed {
    public final String title;
    public final int season;
    public final int episode;
    public final String audioHint;
    Parsed(String title, int season, int episode, String audioHint) {
      this.title=title; this.season=season; this.episode=episode; this.audioHint=audioHint;
    }
    public String displayHint() {
      StringBuilder b=new StringBuilder();
      if(season>0)b.append("Temporada ").append(season);
      if(episode>0){if(b.length()>0)b.append(" • ");b.append("Episódio ").append(episode);}
      if(!audioHint.isEmpty()){if(b.length()>0)b.append(" • ");b.append(audioHint);}
      return b.toString();
    }
  }

  public static Parsed parse(String input) {
    String source=input==null?"":input.trim();
    String folded=Normalizer.normalize(source,Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    String audio="";
    if(folded.matches("(?s).*\\b(?:dual[ ._-]?audio|multi[ ._-]?audio)\\b.*"))audio="Áudio dual (indicado)";
    else if(folded.matches("(?s).*\\bdublado\\b.*"))audio="Dublado (indicado)";
    else if(folded.matches("(?s).*\\blegendado\\b.*"))audio="Legendado (indicado)";
    int s=0,e=0;
    Matcher m=COMPACT.matcher(source);
    if(m.find()){s=Integer.parseInt(m.group(1));e=Integer.parseInt(m.group(2));}
    else {
      m=WRITTEN.matcher(source);
      if(m.find()){s=Integer.parseInt(m.group(1));e=Integer.parseInt(m.group(2));}
      else {m=NUMBERED_EPISODE.matcher(source);if(m.find())e=Integer.parseInt(m.group(1));}
    }
    String title=source;
    title=COMPACT.matcher(title).replaceAll(" ");
    title=WRITTEN.matcher(title).replaceAll(" ");
    title=NUMBERED_EPISODE.matcher(title).replaceAll(" ");
    title=TECHNICAL.matcher(title).replaceAll(" ");
    title=AUDIO_HINTS.matcher(title).replaceAll(" ");
    title=title.replaceAll("(?iu)\\b(?:\\d{1,2}\\s*[ªaºo]?\\s*temporada)\\b"," ");
    title=title.replaceAll("(?i)\\b(?:19|20)\\d{2}\\b"," ");
    title=SPACES.matcher(title).replaceAll(" ").replaceAll("[-|]+\\s*$","").trim();
    if(title.isEmpty())title=source.isEmpty()?"Vídeo encontrado":source;
    return new Parsed(title,s,e,audio);
  }
}
