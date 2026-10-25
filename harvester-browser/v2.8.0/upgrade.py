from pathlib import Path

root=Path("hb-v18")
core=root/"app/src/main/java/com/harvester/browser/core/BrazilianMediaHints.java"
src=core.read_text(encoding="utf-8")
needle="    private static final Pattern QUALITY ="
if src.count(needle)!=1: raise RuntimeError("Brazilian hints source is unexpected")
src=src.replace(needle,
'''    private static final Pattern BR_SEASON = Pattern.compile(
        "(?iu)\\\\b(?:temporada|season)\\\\s*0*(\\\\d{1,3})\\\\b|\\\\b0*(\\\\d{1,3})[ªº]\\\\s*temporada\\\\b");
    private static final Pattern BR_EPISODE = Pattern.compile(
        "(?iu)\\\\b(?:epis[oó]dio|episode|ep)[\\\\s#:_\\\\-]*0*(\\\\d{1,4})\\\\b");
''' + needle,1)
needle='''        season=Math.min(999,season);'''
assert src.count(needle)==1
src=src.replace(needle,'''        // Sites brasileiros frequently say "Temporada 1 · Episódio 4"
        // instead of S01E04. Do not invent a season from an episode-only label.
        if(season==0 || episode==0){
            Matcher seasonHint=BR_SEASON.matcher(title);
            Matcher episodeHint=BR_EPISODE.matcher(title);
            if(seasonHint.find() && episodeHint.find()){
                String captured=seasonHint.group(1)!=null?seasonHint.group(1):seasonHint.group(2);
                try {
                    season=Integer.parseInt(captured);
                    episode=Integer.parseInt(episodeHint.group(1));
                } catch(NumberFormatException ignored){season=0;episode=0;}
            }
        }
''' + needle,1)
core.write_text(src,encoding="utf-8")

# Safety: reject expiring media and iframe links when user explicitly asks to
# persist them. Playback through Vista is independent and can still try streams.
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
s=main.read_text(encoding="utf-8")
needle='''        String action=editBeforeSaving?
            "com.vistastream.tv.action.IMPORT":"com.vistastream.tv.action.PLAY";'''
assert s.count(needle)==1
s=s.replace(needle,'''        // Saved sources should not contain signed/short-lived credentials.
        if(editBeforeSaving) {
            String lower=item.url.toLowerCase(java.util.Locale.ROOT);
            if(lower.contains("token=")||lower.contains("signature=")||
               lower.contains("ip_bind=")||lower.contains("expires=")||
               lower.contains("session=")||lower.contains("proxy.mp4")||
               lower.contains("embedblocked")||
               !(lower.matches(".*\\\\.(?:mp4|m4v|mkv|webm|m3u8|mpd|mov|ts)(?:[?#].*)?$"))) {
                toast("Esta fonte parece temporária ou não é um vídeo direto. Guarda a página e procura uma fonte estável.");
                return false;
            }
        }
''' + needle,1)
main.write_text(s,encoding="utf-8")
gradle=root/"app/build.gradle"
g=gradle.read_text()
assert "versionName '2.7.0'" in g
gradle.write_text(g.replace("versionName '2.7.0'","versionName '2.8.0'")
    .replace("versionCode 18","versionCode 19"))
print("Harvester Browser v2.8: richer PT-BR episode hints and safe manual Vista import")
