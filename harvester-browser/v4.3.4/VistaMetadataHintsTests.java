package com.yure.videoharvester;

public final class VistaMetadataHintsTests {
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    public static void main(String[] args) {
        VistaMetadataHints.Hint actual=VistaMetadataHints.inspect("codigo vinganca ea99a493a",
            "https://site.example/filmes/codigo-vinganca-dublado-2026-1080p");
        check(actual.title.equals("codigo vinganca"),"Clean title: "+actual.title);
        check(actual.year==2026,"Year from source URL");
        check(actual.urlUsed,"URL useful");
        VistaMetadataHints.Hint fallback=VistaMetadataHints.inspect("A Viagem 2012",
            "https://example.com/player/index.php?token=secreto");
        check(fallback.title.equals("A Viagem"),"Fallback title");
        check(fallback.year==2012,"Fallback year");
        System.out.println("PASS VistaMetadataHints: title, page URL and year");
    }
}
