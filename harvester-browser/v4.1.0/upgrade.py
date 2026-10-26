from pathlib import Path
import re
main=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
styles=Path("hb-v18/app/src/main/java/com/harvester/browser/ui/Styles.java")
s=main.read_text()
u=styles.read_text()
def once(a,b,name):
 global s
 c=s.count(a)
 if c!=1:raise RuntimeError(f"{name}: expected one match, got {c}")
 s=s.replace(a,b,1)

# Theme flips while Appearance is an overlay: refresh Home under the overlay
# now; otherwise only the already-rendered navigation dock changes color.
once('''        if(selected!=null && HOME.equals(selected.url) && activePage==null)showHomepage();''',
'''        if(selected!=null && HOME.equals(selected.url))showHomepage();''',
"home repaint after selecting theme")

# Existing wallpaper implementation always placed a DARK gradient on top of
# the flower; use separate, locally generated pastel image when light.
once('''        int plain=pref.getInt("home_color",0xFF302E29);''',
'''        int plain=pref.contains("home_color")?
            pref.getInt("home_color",Styles.BG):
            Styles.LIGHT?0xFFEEE6D7:0xFF302E29;''',"default plain wallpaper follows theme")
once('''            photo.setImageResource(com.harvester.browser.R.drawable.vb_flower);
            loaded=true;''',
'''            photo.setImageResource(Styles.LIGHT?
                com.harvester.browser.R.drawable.vb_flower_light:
                com.harvester.browser.R.drawable.vb_flower);
            loaded=true;''',"theme-dependent floral image")
once('''        dim.setBackground(Styles.gradient(this,0x722A211D,0xC5111312,0));''',
'''        dim.setBackground(Styles.LIGHT?
            Styles.gradient(this,0x18FFF9ED,0x46F2E7D6,0):
            Styles.gradient(this,0x722A211D,0xC5111312,0));''',
"do not darken light flower")
# A lighter Home also works when the user selects a custom image:
# styles the background but preserves their chosen photograph.
once('''            translucentIvory.setBackgroundColor(0x86FAF8ED);''',
'''            translucentIvory.setBackgroundColor(0x32FAF8ED);''',
"keep pastel flower colors visible")
# Accent applied consistently to interactive state and icons (not all body copy).
once('''        searchPill.setBackground(Styles.stroke(this,
            lightHome?0xFAFAF9F4:0xED171917,30,
            lightHome?0xFFD1D0C7:0x364A4A42));''',
'''        searchPill.setBackground(Styles.stroke(this,
            lightHome?0xFAFAF9F4:0xED171917,30,
            Styles.LIGHT?Styles.ink(Styles.ACCENT):Styles.ACCENT));''',
"search field selected accent outline")
once('''        TextView glass=Styles.text(this,"⌕",24,false,
            lightHome?0xFF34372D:Styles.ACCENT);
        Styles.glyph(glass,"⌕",lightHome?0xFF34372D:Styles.ACCENT,22);''',
'''        TextView glass=Styles.text(this,"⌕",24,false,Styles.ACCENT);
        Styles.glyph(glass,"⌕",Styles.ACCENT,22);''',"search accent icon")
once('''        TextView searchGo=Styles.text(this,"⌕",24,true,
            lightHome?0xFF34372D:Styles.TEXT);
        Styles.glyph(searchGo,"⌕",lightHome?0xFF34372D:Styles.TEXT,23);''',
'''        TextView searchGo=Styles.text(this,"⌕",24,true,Styles.ACCENT);
        Styles.glyph(searchGo,"⌕",Styles.ACCENT,23);''',"search action accent")
once('''        bottomTabsCounter.setBackground(Styles.stroke(this,0x00272727,8,Styles.TEXT));''',
'''        bottomTabsCounter.setBackground(Styles.stroke(this,0x00272727,8,Styles.ACCENT));''',
"tab active accent border")
# Bring the selected color into the main profile, active status and tab icons.
once('''        int border=enabled?Styles.ACCENT:0xFF41453B;''',
'''        int border=enabled?Styles.ACCENT:
            Styles.LIGHT?0xFFD6D4C8:0xFF41453B;''',"active controls colored border")

# Hardcoded home monogram circles become actual recognizable favicons.
old='''                circle.setBackground(Styles.stroke(this,tones[index%tones.length],30,0x535B5D51));
                tile.addView(circle,new LinearLayout.LayoutParams(d(56),d(56)));'''
new='''                boolean addIcon=item[1].isEmpty();
                FrameLayout logoBadge=new FrameLayout(this);
                logoBadge.setBackground(Styles.stroke(this,
                    addIcon? (lightHome?0xFFF8F7F1:tones[index%tones.length]):
                    (lightHome?0xFFFDFCF9:0xFFF6F5EF),
                    30,addIcon?Styles.ACCENT:(lightHome?0xFFDFDED4:0xFF484A43)));
                logoBadge.setClipToOutline(true);
                if(addIcon){
                    circle.setBackgroundColor(Color.TRANSPARENT);
                    logoBadge.addView(circle,new FrameLayout.LayoutParams(-1,-1));
                }else{
                    ImageView favicon=new ImageView(this);
                    favicon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    int side=d(41);
                    FrameLayout.LayoutParams imageParams=new FrameLayout.LayoutParams(
                        side,side,Gravity.CENTER);
                    logoBadge.addView(favicon,imageParams);
                    loadSpeedDialFavicon(favicon,item[1],circle);
                }
                tile.addView(logoBadge,new LinearLayout.LayoutParams(d(56),d(56)));'''
once(old,new,"website favicons instead of initials")
once('''        int columns=4;''',
'''        int columns=5;''',"five Opera-like quick accesses per row")
# Name and icon spacing fit 5 columns without clipping labels.
once('''                tile.setPadding(d(2),d(11),d(2),d(9));''',
'''                tile.setPadding(d(1),d(11),d(1),d(9));''',"speed dial spacing")

# Use standard domain favicons only; never run network requests on UI thread.
# Simple bounded per-process cache; label fallback is shown on lookup failure.
favicon_methods=r'''
    private final android.util.LruCache<String,android.graphics.Bitmap> vbFaviconCache=
        new android.util.LruCache<String,android.graphics.Bitmap>(96);
    private final java.util.concurrent.ExecutorService vbFaviconExecutor=
        java.util.concurrent.Executors.newFixedThreadPool(3);
    private void loadSpeedDialFavicon(ImageView icon,String address,TextView fallback){
        String host=null;
        try{host=Uri.parse(address).getHost();}catch(Exception ignored){}
        if(host==null||host.trim().isEmpty()){
            icon.setVisibility(View.GONE);
            icon.addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->{});
            return;
        }
        final String domain=host.toLowerCase(java.util.Locale.ROOT);
        final android.graphics.Bitmap previous=vbFaviconCache.get(domain);
        if(previous!=null && !previous.isRecycled()){
            icon.setImageBitmap(previous);return;
        }
        final TextView letter=fallback;
        // Render initials immediately as a deliberate fallback until the icon is loaded.
        letter.setGravity(Gravity.CENTER);
        icon.setImageDrawable(null);
        // Put the letter under the transparent ImageView in the same badge.
        if(icon.getParent() instanceof FrameLayout){
            FrameLayout badge=(FrameLayout)icon.getParent();
            if(letter.getParent()==null){
                letter.setBackgroundColor(Color.TRANSPARENT);
                badge.addView(letter,0,new FrameLayout.LayoutParams(-1,-1));
            }
        }
        vbFaviconExecutor.execute(()->{
            android.graphics.Bitmap bitmap=null;
            java.net.HttpURLConnection connection=null;
            try{
                String url="https://www.google.com/s2/favicons?domain="+
                    Uri.encode(domain)+"&sz=128";
                connection=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
                connection.setConnectTimeout(4500);
                connection.setReadTimeout(4500);
                connection.setRequestProperty("User-Agent","VistaBrowser/4.1");
                connection.connect();
                if(connection.getResponseCode()==200 &&
                    connection.getContentType()!=null &&
                    connection.getContentType().startsWith("image/")){
                    try(java.io.InputStream stream=connection.getInputStream()){
                        bitmap=android.graphics.BitmapFactory.decodeStream(stream);
                    }
                }
            }catch(Exception ignored){}
            finally{if(connection!=null)connection.disconnect();}
            final android.graphics.Bitmap result=bitmap;
            if(result!=null && result.getWidth()>=16 && result.getHeight()>=16){
                synchronized(vbFaviconCache){vbFaviconCache.put(domain,result);}
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    icon.setImageBitmap(result);
                    letter.setVisibility(View.GONE);
                });
            }
        });
    }
'''
insert="    private void addSpeedDial(){"
once(insert,favicon_methods+insert,"quick access favicon downloader")
# Colors should affect settings and other internal pages too: no stale overlay
# frames after theme toggled. The selected theme already reopens Appearance,
# and other pages reconstruct on navigation.
# Accent dark/light contrast for checked switches.
if 'int[] colors=LIGHT?new int[]{0xFF656F58,0xFFC8C9BE}' not in u:
    raise AssertionError("theme switch palette unexpectedly changed")
u=u.replace('''int[] colors=LIGHT?new int[]{0xFF656F58,0xFFC8C9BE}:
            new int[]{0xFF909C7F,0xFF55584D};''',
'''int accent=accentInk();
        int[] colors=LIGHT?new int[]{accent,0xFFC8C9BE}:
            new int[]{ACCENT,0xFF55584D};''',1)
# Our previously dark surfaces with translucent alpha were never mapped in light mode.
u=u.replace('''        if(a<240 || !mutedNeutral(color))return color;''',
'''        if(a<130 || !mutedNeutral(color))return color;''',1)
u=u.replace('''        return (a<<24)|(mapped&0x00FFFFFF);''',
'''        return ((Math.max(a,225))<<24)|(mapped&0x00FFFFFF);''',1)
u=u.replace('''android.content.res.ColorStateList.valueOf(0x80BEBCA4)''',
'''android.content.res.ColorStateList.valueOf(LIGHT?0x454B5547:ACCENT)''',1)

main.write_text(s)
styles.write_text(u)
gradle=Path("hb-v18/app/build.gradle")
g=gradle.read_text()
assert "versionName '4.0.0'" in g and "versionCode 31" in g
gradle.write_text(g.replace("versionName '4.0.0'","versionName '4.1.0'")
                   .replace("versionCode 31","versionCode 32"))
assert 'vb_flower_light' in s
assert 'String mode=pref.getString("wallpaper_mode","flower");' in s
assert 'private void loadSpeedDialFavicon(' in s
assert 'int columns=5;' in s
assert 'private void showAdvancedBrowserSettings()' in s
assert 'private boolean sendToVistaStream(' in s
print("VistaBrowser v4.1: theme redraw, pastel wallpapers, global accents, real website favicons and five-column quick access")
