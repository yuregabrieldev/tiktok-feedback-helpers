import com.harvester.browser.core.BrazilianMediaHints;

/** Separate regression cases for natural Brazilian season / episode labels. */
public final class BrazilianMediaHintsV28Tests {
    private static void expect(boolean condition,String message) {
        if(!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var br = BrazilianMediaHints.infer("Anime Exemplo 1ª Temporada — Episódio 04 Dublado PT-BR","",
            "https://exemplo.org/animes/exemplo","hls");
        expect(br.season==1 && br.episode==4,"Portuguese ordinal season/episode");
        expect("dub".equals(br.audioMode),"Dublado detection");
        var newer = BrazilianMediaHints.infer("Filme","Temporada 2 - Episódio 12",
            "https://exemplo.org","mp4");
        expect(newer.season==2 && newer.episode==12,"Temporada explicit");
        var unknown = BrazilianMediaHints.infer("Episódio 08","","https://exemplo.org","mp4");
        expect(unknown.season==0 && unknown.episode==0,"Do not infer a season without evidence");
        System.out.println("BrazilianMediaHintsV28Tests passed");
    }
}