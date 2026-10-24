from pathlib import Path
root=Path("hb-v18")
target=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
s=target.read_text()
needle='''            intent.putExtra("com.vistastream.tv.extra.KIND","video");'''
assert s.count(needle)==1
s=s.replace(needle,needle+'''
            // Metadata inferred ONLY from already visible page/video labels.
            // The Vista editor can change every value before saving.
            com.harvester.browser.core.BrazilianMediaHints.Result hints=
                com.harvester.browser.core.BrazilianMediaHints.infer(
                    candidate,item.title,page,item.format);
            if(!hints.locale.isEmpty())
                intent.putExtra("com.vistastream.tv.extra.AUDIO_LANGUAGE",hints.locale);
            if(!hints.audioMode.isEmpty())
                intent.putExtra("com.vistastream.tv.extra.AUDIO_MODE",hints.audioMode);
            if(!hints.quality.isEmpty())
                intent.putExtra("com.vistastream.tv.extra.QUALITY",hints.quality);
            if(!hints.providerLabel.isEmpty())
                intent.putExtra("com.vistastream.tv.extra.PROVIDER_LABEL",hints.providerLabel);
            if(hints.season>0 && hints.episode>0){
                intent.putExtra("com.vistastream.tv.extra.SEASON",hints.season);
                intent.putExtra("com.vistastream.tv.extra.EPISODE",hints.episode);
            }
''',1)
target.write_text(s)
src=Path("harvester-browser/v2.7.0/BrazilianMediaHints.java")
dst=root/"app/src/main/java/com/harvester/browser/core/BrazilianMediaHints.java"
dst.write_text(src.read_text())
core_tests=root/"core-tests"
core_tests.mkdir(parents=True,exist_ok=True)
(core_tests/"BrazilianMediaHintsTests.java").write_text(
    Path("harvester-browser/v2.7.0/BrazilianMediaHintsTests.java").read_text())
build=root/"app/build.gradle"
g=build.read_text()
assert "versionName '2.6.0'" in g, "Missing base version"
build.write_text(g.replace("versionName '2.6.0'","versionName '2.7.0'").replace("versionCode 17","versionCode 18"))
print("Harvester v2.7: independent APK forwards only inferred PT-BR/audio/episode/quality metadata")
