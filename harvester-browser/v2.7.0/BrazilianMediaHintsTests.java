import com.harvester.browser.core.BrazilianMediaHints;
public final class BrazilianMediaHintsTests {
    static void check(boolean ok, String text) { if(!ok) throw new AssertionError(text); }
    public static void main(String[] args) {
        var episode = BrazilianMediaHints.infer("Anime T02 EP05 - Dublado PT-BR 1080p", "",
            "https://example.org/page", "HLS");
        check(episode.season==2 && episode.episode==5,"Season/episode");
        check("pt-BR".equals(episode.locale),"Locale explicit");
        check("dub".equals(episode.audioMode),"Dubbing");
        check("1080p".equals(episode.quality),"Quality");
        check("example.org".equals(episode.providerLabel),"Provider");
        var guessed = BrazilianMediaHints.infer("Filme Online", "", "https://example.com", "mp4");
        check(guessed.locale.isEmpty() && guessed.audioMode.isEmpty(),
            "Do not invent Brazilian audio");
        var next = BrazilianMediaHints.infer("", "S01E09 Legendado 720p", "", "dash");
        check(next.season==1 && next.episode==9,"SxxExx");
        check("sub".equals(next.audioMode),"Subtitle flag");
        var multi=BrazilianMediaHints.infer("dual áudio pt-br 4k","","","hls");
        check("multi".equals(multi.audioMode) && "2160p".equals(multi.quality),"Multi audio");
        System.out.println("BrazilianMediaHintsTests passed");
    }
}
