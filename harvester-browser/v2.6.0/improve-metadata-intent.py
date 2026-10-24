from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text()
original='''            intent.putExtra("com.vistastream.tv.extra.TITLE",item.title);'''
assert s.count(original)==1
s=s.replace(original,'''            // The stream metadata can be just "Vídeo" while the current
            // page has the actual movie title. Prefer the page title for
            // suggested catalog matching, without guessing a movie ID.
            String candidate=item.title==null?"":item.title.trim();
            if(candidate.isEmpty() || candidate.equalsIgnoreCase("vídeo") ||
                candidate.equalsIgnoreCase("video") || candidate.startsWith("HLS") ||
                candidate.startsWith("DASH")){
                if(selected!=null && selected.title!=null &&
                    !selected.title.trim().isEmpty())
                    candidate=selected.title.trim();
            }
            if(candidate.isEmpty())candidate="Vídeo encontrado";
            intent.putExtra("com.vistastream.tv.extra.TITLE",
                candidate.substring(0,Math.min(candidate.length(),180)));''',1)
# If the website exposes a year in the full title, send it as a suggestion.
needle='''            intent.putExtra("com.vistastream.tv.extra.KIND","video");'''
assert s.count(needle)==1
s=s.replace(needle,'''            java.util.regex.Matcher yearFinder=java.util.regex.Pattern
                .compile("\\\\b(?:19|20)\\\\d{2}\\\\b").matcher(candidate);
            if(yearFinder.find()){
                try{intent.putExtra("com.vistastream.tv.extra.YEAR",
                    Integer.parseInt(yearFinder.group()));}
                catch(NumberFormatException ignored){}
            }
            intent.putExtra("com.vistastream.tv.extra.KIND","video");''',1)
p.write_text(s)
g=Path("hb-v18/app/build.gradle")
s=g.read_text()
assert "versionName '2.5.0'" in s
g.write_text(s.replace("versionName '2.5.0'","versionName '2.6.0'").replace("versionCode 16","versionCode 17"))
print("Harvester 2.6: forward cleaned original page title and release year as catalog suggestions.")
