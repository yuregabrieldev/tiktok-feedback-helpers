from pathlib import Path
import re

src=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
sty=Path("hb-v18/app/src/main/java/com/harvester/browser/ui/Styles.java")
s=src.read_text(encoding="utf-8")
u=sty.read_text(encoding="utf-8")
def edit(a,b,label):
 global s
 n=s.count(a)
 if n!=1:raise AssertionError(f"{label}: {n} occurrences")
 s=s.replace(a,b,1)

# All browser views are created with the same mutable theme palette.
old='''    public static final int BG=0xFF181A17, INK=0xFFF2F0E4, BLUE=0xFFE1DEC9,
        MUTED=0xFFB8B8AD, WHITE=Color.WHITE, BORDER=0xFF53564B;
    public static final int TEXT=0xFFF4F2EA, SURFACE=0xFF2B2D28;
    public static int ACCENT=0xFFE1DEC9;'''
new='''    public static int BG=0xFF181A17, INK=0xFFF2F0E4, BLUE=0xFFE1DEC9,
        MUTED=0xFFB8B8AD, WHITE=Color.WHITE, BORDER=0xFF53564B;
    public static int TEXT=0xFFF4F2EA, SURFACE=0xFF2B2D28;
    public static int ACCENT=0xFFE1DEC9;
    public static boolean LIGHT=false;

    /** Shared warm-ivory and charcoal palette for all native UI surfaces. */
    public static void setTheme(boolean light){
        LIGHT=light;
        BG=light?0xFFFAF9F3:0xFF181A17;
        INK=light?0xFF262922:0xFFF2F0E4;
        BLUE=light?0xFF34372F:0xFFE1DEC9;
        MUTED=light?0xFF6A6C61:0xFFB8B8AD;
        WHITE=light?0xFFFAF9F3:Color.WHITE;
        BORDER=light?0xFFD7D7CD:0xFF53564B;
        TEXT=light?0xFF282A25:0xFFF4F2EA;
        SURFACE=light?0xFFF0EEE5:0xFF2B2D28;
    }
    private static boolean mutedNeutral(int color){
        final int r=Color.red(color),g=Color.green(color),b=Color.blue(color);
        return Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<48;
    }
    /** Converts fixed legacy dark UI fills, not content photos or site content. */
    public static int surface(int color){
        if(!LIGHT||color==ACCENT)return color;
        int a=Color.alpha(color);
        if(a<240 || !mutedNeutral(color))return color;
        int avg=(Color.red(color)+Color.green(color)+Color.blue(color))/3;
        if(avg>152)return color;
        int mapped=avg<25?0xFFFAF9F3:
                   avg<40?0xFFF4F2EA:
                   avg<67?0xFFEEEDE4:
                   avg<104?0xFFE6E5DB:0xFFDCDACF;
        return (a<<24)|(mapped&0x00FFFFFF);
    }
    public static int border(int color){
        if(!LIGHT || color==ACCENT)return color;
        int a=Color.alpha(color);
        if(a<32 || !mutedNeutral(color))return color;
        int avg=(Color.red(color)+Color.green(color)+Color.blue(color))/3;
        if(avg<164)return (a<<24)|0x00D3D2C7;
        return color;
    }
    public static int ink(int color){
        if(!LIGHT || color==ACCENT)return color;
        final int a=Color.alpha(color);
        final int r=Color.red(color),g=Color.green(color),b=Color.blue(color);
        int avg=(r+g+b)/3;
        if(mutedNeutral(color) && avg>=185)return (a<<24)|0x00272A25;
        if(mutedNeutral(color) && avg>=115)return (a<<24)|0x006A6D61;
        return color;
    }
'''
if u.count(old)!=1:raise AssertionError("styles palette source anchor missing")
u=u.replace(old,new,1)
u=u.replace("d.setColor(color);d.setCornerRadius", "d.setColor(surface(color));d.setCornerRadius",1)
u=u.replace("d.setStroke(dp(c,1),border);", "d.setStroke(dp(c,1),border(border));",1)
u=u.replace("v.setTextColor(color);", "v.setTextColor(ink(color));",1)
u=u.replace("new int[]{top,bottom}", "new int[]{surface(top),surface(bottom)}",1)
u=u.replace("GradientDrawable normal=bg(c,color,radius);", "GradientDrawable normal=bg(c,color,radius);",1)
u=u.replace('''PremiumGlyph icon=PremiumGlyph.forSymbol(source,color,dp(v.getContext(),size));''',
'''PremiumGlyph icon=PremiumGlyph.forSymbol(source,ink(color),dp(v.getContext(),size));''',1)
u=u.replace('''sw.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));''',
'''sw.setThumbTintList(android.content.res.ColorStateList.valueOf(LIGHT?0xFFFAF9F3:Color.WHITE));''',1)
u=u.replace('''int[] colors=new int[]{0xFF909C7F,0xFF55584D};''',
'''int[] colors=LIGHT?new int[]{0xFF656F58,0xFFC8C9BE}:
            new int[]{0xFF909C7F,0xFF55584D};''',1)
sty.write_text(u,encoding="utf-8")

# Theme semantics at creation, not only system status and footer bars.
edit('''    private static final int PANEL_BG=0xFF181A17, PANEL_CARD=0xFF30332D;
    private static final int PANEL_TEXT=0xFFF5F8FF, PANEL_MUTED=0xFFB8B8AD;''',
'''    private static int PANEL_BG=0xFF181A17, PANEL_CARD=0xFF30332D;
    private static int PANEL_TEXT=0xFFF5F8FF, PANEL_MUTED=0xFFB8B8AD;''',
"dynamic internal page palette")

a='''    private void applyBrowserTheme(){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        int mode=pref.getInt("theme_mode",0);
        boolean light=mode==1||(mode==0&&
            (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                ==android.content.res.Configuration.UI_MODE_NIGHT_NO);
        int background=light?0xFFF2F0E9:0xFF191B17;
        // Backgrounds and system bar follow chosen chrome. Web pages keep
        // their own CSS/theme, and existing media player colors stay unmodified.
        if(root!=null)root.setBackgroundColor(background);
        if(appFrame!=null)appFrame.setBackgroundColor(background);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        int flags=getWindow().getDecorView().getSystemUiVisibility();
        if(light)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        else flags&=~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if(selected!=null&&HOME.equals(selected.url))showHomepage();
    }'''
b='''    private boolean browserLightMode(){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        int mode=pref.getInt("theme_mode",0);
        return mode==1 || (mode==0 &&
            (getResources().getConfiguration().uiMode&
                android.content.res.Configuration.UI_MODE_NIGHT_MASK)==
                android.content.res.Configuration.UI_MODE_NIGHT_NO);
    }
    private void initializeThemePalette(){
        boolean light=browserLightMode();
        Styles.setTheme(light);
        PANEL_BG=Styles.BG;
        PANEL_CARD=Styles.SURFACE;
        PANEL_TEXT=Styles.TEXT;
        PANEL_MUTED=Styles.MUTED;
    }
    private void applyBrowserTheme(){
        initializeThemePalette();
        boolean light=Styles.LIGHT;
        int background=Styles.BG;
        if(root!=null)root.setBackgroundColor(background);
        if(appFrame!=null)appFrame.setBackgroundColor(background);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        if(Build.VERSION.SDK_INT>=26){
            getWindow().getDecorView().setSystemUiVisibility(
                light?(getWindow().getDecorView().getSystemUiVisibility()
                      |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR):
                      (getWindow().getDecorView().getSystemUiVisibility()
                      &~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)));
        }else{
            int flags=getWindow().getDecorView().getSystemUiVisibility();
            getWindow().getDecorView().setSystemUiVisibility(light?
                flags|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:
                flags&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        // Repaint browser chrome without discarding WebViews, tabs or player state.
        if(bottomNav!=null){
            bottomNav.setBackground(Styles.bg(this,light?0xFFFAF9F3:0xFF1C2230,27));
            configureBottomNavigation();
        }
        if(browserToolbar!=null)
            browserToolbar.setBackground(Styles.bg(this,Styles.BG,0));
        if(address!=null){
            address.setTextColor(Styles.TEXT);
            address.setHintTextColor(Styles.MUTED);
            address.setBackground(Styles.stroke(this,light?0xFFF0EEE5:0xFF292C27,28,Styles.BORDER));
        }
        if(selected!=null && HOME.equals(selected.url) && activePage==null)showHomepage();
    }'''
edit(a,b,"full theme initialization and live browser chrome")

# initialize BEFORE the native UI tree gets built, particularly on cold startup.
if s.count('setupUi();')==1:
 s=s.replace('setupUi();','initializeThemePalette();setupUi();',1)
else:
 print("setupUi() anchor count",s.count('setupUi();'),"— relying on onResume")
# Global direct background setters left outside Styles bg/gradient must be mapped,
# including the panel bottom sheet and fullscreen search.
s=re.sub(r'setBackgroundColor\((0xFF[0-9A-Fa-f]{6})\)',
         r'setBackgroundColor(Styles.surface(\1))',s)
s=re.sub(r'backdrop\.setColor\((0xFF[0-9A-Fa-f]{6})\)',
         r'backdrop.setColor(Styles.surface(\1))',s)
# Avoid binary appearance theme selector re-tint: preview tiles represent a literal
# dark vs light preview regardless of which mode is currently selected.
old='''        tile.setBackground(Styles.stroke(this,color,17,
            selectedTheme?Styles.ACCENT:0xFF484A43));
        TextView symbol=Styles.text(this,selectedTheme?"✓":" ",19,true,text);'''
new='''        android.graphics.drawable.GradientDrawable preview=
            new android.graphics.drawable.GradientDrawable();
        preview.setColor(color);preview.setCornerRadius(d(17));
        preview.setStroke(d(selectedTheme?2:1),
            selectedTheme?Styles.ACCENT:Styles.border(0xFF484A43));
        tile.setBackground(preview);
        TextView symbol=Styles.text(this,selectedTheme?"✓":" ",19,true,text);
        symbol.setTextColor(text);'''
edit(old,new,"actual contrasting theme preview cards")
edit('''        TextView txt=Styles.text(this,label,12,true,text);
        txt.setGravity(Gravity.CENTER);''',
'''        TextView txt=Styles.text(this,label,12,true,text);
        txt.setTextColor(text);
        txt.setGravity(Gravity.CENTER);''',"theme card label contrast")
# The local panel search input used explicit old static colors before palette dynamic.
edit('''        search.setTextColor(PANEL_TEXT);search.setHintTextColor(PANEL_MUTED);''',
'''        search.setTextColor(Styles.TEXT);search.setHintTextColor(Styles.MUTED);''',
"internal search field text colors")
# bottom popup background in profile is a raw GradientDrawable.
assert 'backdrop.setColor(Styles.surface(0xFF191B17));' in s
assert 'Styles.setTheme(light);' in s
src.write_text(s,encoding="utf-8")
print("Stage1: shared light/dark palette, theme-aware drawables/text, full browser chrome and native screen colors")
