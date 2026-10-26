from pathlib import Path
import re

root=Path("hb-v18")
main=root/"app/src/main/java/com/harvester/browser/MainActivity.java"
style=root/"app/src/main/java/com/harvester/browser/ui/Styles.java"
gradle=root/"app/build.gradle"

s=main.read_text(encoding="utf-8")
def one(old,new,name):
    global s
    count=s.count(old)
    if count!=1: raise RuntimeError(f"{name}: expected exactly one occurrence; got {count}")
    s=s.replace(old,new,1)

# Original media extraction, browsing, and external VistaStream functionality is
# intentionally untouched. These changes are UI-only.
one('''        root=Styles.column(this);root.setBackgroundColor(0xFF121415);''',
'''        root=Styles.column(this);root.setBackgroundColor(Styles.BG);''',"root background")
one('''        getWindow().setStatusBarColor(0xFF121415);
        getWindow().setNavigationBarColor(0xFF121415);''',
'''        getWindow().setStatusBarColor(Styles.BG);
        getWindow().setNavigationBarColor(Styles.BG);''',"system bar colors")
one('''        appFrame.setBackgroundColor(0xFF101213);''','''        appFrame.setBackgroundColor(Styles.BG);''',"frame")
one('''        toolbar.setPadding(d(7),d(5),d(7),d(5));
        toolbar.setBackgroundColor(0xF116181B);''',
'''        toolbar.setPadding(d(8),d(8),d(8),d(8));
        toolbar.setBackground(Styles.gradient(this,0xFF141925,0xFF10141C,0));
        toolbar.setElevation(d(4));''',"top bar")
one('''        addressRow.setBackground(Styles.bg(this,0xFF323438,24));''',
'''        addressRow.setBackground(Styles.stroke(this,0xFF242B3B,23,0xFF384458));''',"address pill")
one('''        TextView siteInfo=Styles.text(this,"☷",24,false,0xFFD3D5DA);
        siteInfo.setGravity(Gravity.CENTER);''',
'''        TextView siteInfo=Styles.text(this,"☷",24,false,Styles.ACCENT);
        siteInfo.setGravity(Gravity.CENTER);
        Styles.glyph(siteInfo,"☷",Styles.ACCENT,20);''',"site info icon")
one('''        address.setTextColor(0xFFF1F3F5);address.setTextSize(16);''',
'''        address.setTextColor(Styles.TEXT);address.setTextSize(15);''',"address text")
one('''        address.setHint("Pesquisar ou introduzir URL");address.setHintTextColor(0xFFAEB3BD);''',
'''        address.setHint("Pesquisar ou introduzir URL");address.setHintTextColor(Styles.MUTED);''',"address hint")
one('''        tabCounter.setBackground(Styles.stroke(this,0x00222222,10,0xFFD6D8DD));''',
'''        tabCounter.setBackground(Styles.stroke(this,0xFF252E40,12,0xFF607A9E));
        tabCounter.setTextColor(Styles.TEXT);''',"tab counter")
one('''        root.addView(toolbar,new LinearLayout.LayoutParams(-1,d(58)));''',
'''        root.addView(toolbar,new LinearLayout.LayoutParams(-1,d(62)));''',"top bar height")
one('''        progressIndicator=Styles.text(this,"",10,false,0xFFA5C7FF);''',
'''        progressIndicator=Styles.text(this,"",10,false,Styles.ACCENT);''',"progress")
one('''        browserControls.setPadding(d(8),d(3),d(8),d(7));
        browserControls.setBackgroundColor(0xEF1D2024);''',
'''        browserControls.setPadding(d(9),d(7),d(9),d(8));
        browserControls.setBackground(Styles.gradient(this,0xF51C2432,0xF5151A24,20));
        browserControls.setElevation(d(14));''',"bottom dock")
one('''        TextView b=Styles.text(this,text,size,false,0xFFE8EAED);
        b.setGravity(Gravity.CENTER);b.setContentDescription(description);
        return b;''',
'''        TextView b=Styles.text(this,text,size,false,Styles.TEXT);
        b.setGravity(Gravity.CENTER);b.setContentDescription(description);
        Styles.glyph(b,text,Styles.TEXT,24);
        b.setBackground(Styles.touch(this,0x1C82A7E5,16));
        return b;''',"toolbar icons")
one('''        TextView b=Styles.text(this,title,23,false,0xFFE0E3E9);b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v->action.run());row.addView(b,new LinearLayout.LayoutParams(0,d(39),1));return b;''',
'''        TextView b=Styles.text(this,title,23,false,Styles.TEXT);
        b.setGravity(Gravity.CENTER);
        Styles.glyph(b,title,Styles.ACCENT,24);
        b.setBackground(Styles.touch(this,0x24445D7C,17));
        b.setContentDescription("Ação "+title);
        b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(46),1);
        p.setMargins(d(2),0,d(2),0);row.addView(b,p);
        return b;''',"bottom actions")

# Rebuild ONLY the about:blank home and its existing four action tiles.
start=s.index("    private void showHomepage(){")
end=s.index("    private void recordResource(",start)
old=s[start:end]
assert 'homeTile(row1,"⌕ Inspecionar",this::showInspector)' in old
assert 'homeTile(row2,"◎ Crawler",this::showCrawler)' in old
replacement='''    /** Brand-first home screen; original four navigation actions preserved. */
    private void showHomepage(){
        if(selected==null||!HOME.equals(selected.url))return;
        browserHost.removeAllViews();
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout home=Styles.column(this);
        home.setPadding(d(22),d(36),d(22),d(28));
        home.setBackground(Styles.gradient(this,0xFF151E30,Styles.BG,32));
        scroll.addView(home);
        LinearLayout hero=Styles.column(this);
        hero.setPadding(d(22),d(28),d(22),d(28));
        hero.setBackground(Styles.gradient(this,0xFF263A60,0xFF171F32,28));
        hero.setElevation(d(8));
        TextView eyebrow=Styles.text(this,"SEU ESPAÇO DE NAVEGAÇÃO",10,true,0xFFB8CCED);
        eyebrow.setLetterSpacing(.17f);hero.addView(eyebrow);
        TextView mark=Styles.text(this,"◈  VistaBrowser",31,true,Styles.TEXT);
        mark.setPadding(0,d(16),0,d(8));hero.addView(mark);
        TextView description=Styles.text(this,"Explore a web com liberdade. Descubra, inspecione e reproduza vídeos numa experiência feita para si.",14,false,0xFFBFCEE5);
        description.setLineSpacing(d(3),1.0f);hero.addView(description);
        TextView hint=Styles.text(this,"⌕  Pesquise ou abra um endereço na barra acima",12,false,Styles.ACCENT);
        hint.setPadding(0,d(22),0,0);hero.addView(hint);
        home.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        TextView heading=Styles.text(this,"O que pretende fazer?",20,true,Styles.TEXT);
        heading.setPadding(d(2),d(35),0,d(16));home.addView(heading);
        LinearLayout row1=Styles.row(this);
        homeTile(row1,"⌕ Inspecionar",this::showInspector);
        homeTile(row1,"▶ Vídeos",this::showDetected);
        home.addView(row1);
        LinearLayout row2=Styles.row(this);
        homeTile(row2,"◎ Crawler",this::showCrawler);
        homeTile(row2,"▤ Biblioteca",this::showLibrary);
        home.addView(row2);
        TextView footer=Styles.text(this,"VISTABROWSER  •  PRIVACIDADE & CONTROLO",10,true,0xFF718198);
        footer.setLetterSpacing(.11f);footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,d(34),0,d(8));home.addView(footer);
        browserHost.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        synchronizeUi();
    }
    private void homeTile(LinearLayout row,String title,Runnable action){
        LinearLayout tile=Styles.column(this);
        tile.setPadding(d(15),d(19),d(13),d(18));
        tile.setBackground(Styles.stroke(this,0xFF1D2738,22,0xFF364760));
        tile.setElevation(d(2));
        String glyph=title.substring(0,1);
        String label=title.substring(2);
        TextView icon=Styles.text(this,glyph,24,false,Styles.ACCENT);
        Styles.glyph(icon,glyph,Styles.ACCENT,25);
        icon.setGravity(Gravity.CENTER_VERTICAL);
        tile.addView(icon,new LinearLayout.LayoutParams(d(35),d(33)));
        TextView name=Styles.text(this,label,15,true,Styles.TEXT);
        name.setPadding(0,d(16),0,0);tile.addView(name);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(125),1);
        p.setMargins(d(4),d(4),d(4),d(6));
        row.addView(tile,p);
        tile.setOnClickListener(v->action.run());
    }
'''
s=s[:start]+replacement+s[end:]

# Popups keep all existing entries/actions; only their surfaces and row hierarchy change.
one('''        LinearLayout row=Styles.row(this);row.setPadding(d(15),d(6),d(15),d(6));
        TextView text=Styles.text(this,label,14,false,0xFFF1F2F5);
        row.addView(text,new LinearLayout.LayoutParams(0,d(45),1));
        Switch input=new Switch(this);input.setChecked(current);row.addView(input);
        input.setOnCheckedChangeListener((b,value)->change.accept(value));
        row.setOnClickListener(v->input.setChecked(!input.isChecked()));box.addView(row);''',
'''        LinearLayout row=Styles.row(this);
        row.setPadding(d(14),d(10),d(13),d(10));
        row.setBackground(Styles.touch(this,0xFF202B3C,16));
        TextView text=Styles.text(this,label,14,false,Styles.TEXT);
        text.setMaxLines(2);
        row.addView(text,new LinearLayout.LayoutParams(0,d(46),1));
        Switch input=new Switch(this);input.setChecked(current);
        Styles.tintedSwitch(input);row.addView(input);
        input.setOnCheckedChangeListener((b,value)->change.accept(value));
        row.setOnClickListener(v->input.setChecked(!input.isChecked()));
        LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);
        margin.setMargins(d(8),d(3),d(8),d(3));box.addView(row,margin);''',"premium switches")
one('''        TextView heading=Styles.text(this,title.toUpperCase(Locale.ROOT),11,true,0xFF9DA5B4);
        heading.setPadding(d(17),d(13),d(10),d(4));box.addView(heading);''',
'''        TextView heading=Styles.text(this,title.toUpperCase(Locale.ROOT),11,true,Styles.ACCENT);
        heading.setLetterSpacing(.13f);
        heading.setPadding(d(18),d(20),d(10),d(8));box.addView(heading);''',"menu headings")
one('''        LinearLayout row=Styles.row(this);row.setPadding(d(17),d(4),d(17),d(4));
        TextView icon=Styles.text(this,glyph,21,false,0xFF9FC6FF);icon.setGravity(Gravity.CENTER);
        row.addView(icon,new LinearLayout.LayoutParams(d(38),d(44)));
        TextView label=Styles.text(this,title,15,false,0xFFF1F2F5);
        row.addView(label,new LinearLayout.LayoutParams(-1,d(44)));
        box.addView(row);row.setOnClickListener(v->{popup.dismiss();action.run();});''',
'''        LinearLayout row=Styles.row(this);
        row.setPadding(d(14),d(5),d(14),d(5));
        row.setBackground(Styles.touch(this,0xFF1E2838,14));
        TextView icon=Styles.text(this,glyph,20,false,Styles.ACCENT);
        Styles.glyph(icon,glyph,Styles.ACCENT,21);icon.setGravity(Gravity.CENTER);
        row.addView(icon,new LinearLayout.LayoutParams(d(41),d(45)));
        TextView label=Styles.text(this,title,14,false,Styles.TEXT);
        row.addView(label,new LinearLayout.LayoutParams(0,d(47),1));
        LinearLayout.LayoutParams m=new LinearLayout.LayoutParams(-1,-2);
        m.setMargins(d(8),d(2),d(8),d(2));box.addView(row,m);
        row.setOnClickListener(v->{popup.dismiss();action.run();});''',"premium menu entries")

one('''        box.setBackground(Styles.stroke(this,0xFA25282D,20,0xFF46505E));''',
'''        box.setBackground(Styles.stroke(this,0xFA151C29,24,0xFF405371));''',"quick sheet base")
one('''        popup.setBackgroundDrawable(Styles.bg(this,0xFA25282D,20));''',
'''        popup.setBackgroundDrawable(Styles.bg(this,0xFA151C29,24));''',"quick sheet surface")
one('''        box.setBackground(Styles.stroke(this,0xF925282D,22,0xFF41454B));''',
'''        box.setBackground(Styles.stroke(this,0xFC171D2B,24,0xFF3E4C66));''',"main menu card")
one('''        popup.setBackgroundDrawable(Styles.bg(this,0xF925282D,22));''',
'''        popup.setBackgroundDrawable(Styles.bg(this,0xFC171D2B,24));''',"main menu surface")
one('''        menuEntry(box,"ⓘ","Acerca do VistaBrowser",()->new AlertDialog.Builder(this).setTitle("VistaBrowser 1.5")''',
'''        menuEntry(box,"ⓘ","Acerca do VistaBrowser",()->new AlertDialog.Builder(this).setTitle("VistaBrowser")''',"stale about version")

# Share the current webpage using native Android share without touching scanning.
one('''        menuEntry(box,"⟳","Atualizar página",()->{
            if(selected!=null && selected.web!=null)selected.web.reload();
        },popup);''',
'''        menuEntry(box,"⟳","Atualizar página",()->{
            if(selected!=null && selected.web!=null)selected.web.reload();
        },popup);
        menuEntry(box,"⇧","Partilhar página",()->{
            if(selected==null || HOME.equals(selected.url)){
                toast("Abra um site para partilhar.");
                return;
            }
            Intent share=new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,selected.url);
            startActivity(Intent.createChooser(share,"Partilhar página"));
        },popup);''',"share current page")

# Fullscreen panels stay functional; all list data, searches, and callbacks untouched.
one('''    private static final int PANEL_BG=0xFF101213, PANEL_CARD=0xFF272B30;
    private static final int PANEL_TEXT=0xFFF3F5F7, PANEL_MUTED=0xFFA9B2C0;''',
'''    private static final int PANEL_BG=0xFF10151F, PANEL_CARD=0xFF202B3C;
    private static final int PANEL_TEXT=0xFFF5F8FF, PANEL_MUTED=0xFFA6B5CB;''',"panel palette")
one('''        LinearLayout page=Styles.column(this);page.setBackgroundColor(PANEL_BG);''',
'''        LinearLayout page=Styles.column(this);
        page.setBackground(Styles.gradient(this,0xFF151D2C,PANEL_BG,0));''',"panel background")
one('''        LinearLayout header=Styles.row(this);header.setPadding(d(14),d(13),d(13),d(10));''',
'''        LinearLayout header=Styles.row(this);
        header.setPadding(d(14),d(11),d(13),d(8));
        header.setBackground(Styles.gradient(this,0xFF1A2638,0xFF151D2C,0));''',"panel header")
one('''        TextView name=Styles.text(this,title,24,false,PANEL_TEXT);''',
'''        TextView name=Styles.text(this,title,22,true,PANEL_TEXT);''',"panel title")
one('''        TextView view=Styles.text(this,symbol,size,false,PANEL_TEXT);
        view.setGravity(Gravity.CENTER);view.setContentDescription(description);return view;''',
'''        TextView view=Styles.text(this,symbol,size,false,PANEL_TEXT);
        view.setGravity(Gravity.CENTER);view.setContentDescription(description);
        Styles.glyph(view,symbol,PANEL_TEXT,22);
        view.setBackground(Styles.touch(this,0x192A3750,16));
        return view;''',"panel icon")
one('''        search.setPadding(d(18),d(5),d(12),d(5));search.setBackground(Styles.bg(this,0xFF303338,28));''',
'''        search.setPadding(d(18),d(5),d(12),d(5));
        search.setBackground(Styles.stroke(this,0xFF263246,19,0xFF3A4D68));''',"panel search")
one('''        TextView head=panelLabel(label,16,true,PANEL_MUTED);head.setPadding(d(2),d(22),0,d(10));''',
'''        TextView head=panelLabel(label,15,true,Styles.ACCENT);
        head.setPadding(d(2),d(25),0,d(12));''',"panel section headers")
one('''        View divider=new View(this);divider.setBackgroundColor(0xFF303438);
        parent.addView(divider,new LinearLayout.LayoutParams(-1,d(1)));''',
'''        View divider=new View(this);divider.setBackgroundColor(0x00232D3B);
        parent.addView(divider,new LinearLayout.LayoutParams(-1,d(7)));''',"panel separators")
one('''        LinearLayout row=Styles.row(this);row.setPadding(d(5),d(12),d(2),d(12));
        TextView mark=panelLabel(symbol,22,true,0xFFB0DAEA);mark.setGravity(Gravity.CENTER);
        mark.setBackground(Styles.bg(this,0xFF20272B,22));''',
'''        LinearLayout row=Styles.row(this);
        row.setPadding(d(12),d(14),d(8),d(14));
        row.setBackground(Styles.stroke(this,0xFF1C2738,18,0xFF2B3B52));
        TextView mark=panelLabel(symbol,22,true,Styles.ACCENT);
        Styles.glyph(mark,symbol,Styles.ACCENT,23);mark.setGravity(Gravity.CENTER);
        mark.setBackground(Styles.bg(this,0xFF293C56,16));''',"panel rows")
one('''        if(action!=null){row.setOnClickListener(v->action.run());row.setBackground(Styles.bg(this,0xFF191C1F,12));}''',
'''        if(action!=null){row.setOnClickListener(v->action.run());}''',"list row click")
one('''            card.setBackground(Styles.stroke(this,tab==selected?0xFFADCFDB:0xFF2B3036,20,
                    tab==selected?0xFFADCFDB:0xFF4B4F54));''',
'''            card.setBackground(Styles.stroke(this,
                    tab==selected?0xFF2B4166:0xFF1C2636,21,
                    tab==selected?Styles.ACCENT:0xFF36475F));''',"tab card colors")
one('''            TextView name=panelLabel(tab.title,14,true,tab==selected?0xFF10242B:PANEL_TEXT);''',
'''            TextView name=panelLabel(tab.title,14,true,PANEL_TEXT);''',"tab title color")
one('''            FrameLayout preview=new FrameLayout(this);preview.setBackground(Styles.bg(this,0xFF1D2024,14));''',
'''            FrameLayout preview=new FrameLayout(this);
            preview.setBackground(Styles.bg(this,0xFF131B28,16));
            preview.setClipToOutline(true);''',"tab preview")
one('''            TextView icon=panelLabel(tab.privateMode?"◌":"◇",19,true,0xFFBADEEA);''',
'''            TextView icon=panelLabel(tab.privateMode?"◌":"◇",19,true,Styles.ACCENT);''',"tab icon tint")
one('''        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=d(18);''',
'''        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=d(20);''',"tab grid spacing")

# Assertions explicitly guard against accidental removal of critical entry points.
for marker in (
    'private void showDetected(){',
    'private void showLibrary(){',
    'private void showInspector(){',
    'private void showCrawler(){',
    'private boolean sendToVistaStream(',
    'private void openNativePlayer(',
    'private void onNativePlaybackRequested(',
    'private void capturePage(',
    'private void download(',
    'private void setPageLockForCurrentTab(',
    'VistaBrowserUpdates.check(this,false)',
    'private void showQuickActions(){',
    'private void showTabOverview(){',
):
    if marker not in s:raise RuntimeError("Required functionality not found: "+marker)

main.write_text(s,encoding="utf-8")

styles=style.read_text(encoding="utf-8")
assert "public static final int BG=0xFFF6F7FA" in styles
styles=styles.replace(
  "public static final int BG=0xFFF6F7FA, INK=0xFF182334, BLUE=0xFF168BE0,\n        MUTED=0xFF738094, WHITE=Color.WHITE, BORDER=0xFFE7EAF0;",
  "public static final int BG=0xFF0F141E, INK=0xFFEFF4FE, BLUE=0xFF8AB9FF,\n        MUTED=0xFFA6B5CB, WHITE=Color.WHITE, BORDER=0xFF39485E;\n    public static final int TEXT=0xFFF5F8FF, ACCENT=0xFF8DBDFF, SURFACE=0xFF1C2636;"
)
inject='''    public static GradientDrawable gradient(Context c,int top,int bottom,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
        d.setCornerRadius(dp(c,radius));return d;
    }
    public static android.graphics.drawable.Drawable touch(Context c,int color,int radius){
        GradientDrawable normal=bg(c,color,radius);
        return new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x608DBDFF),normal,null);
    }
    public static void tintedSwitch(Switch sw){
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        int[][] states=new int[][]{new int[]{android.R.attr.state_checked},new int[]{}};
        int[] colors=new int[]{0xFF518FEB,0xFF35445A};
        sw.setTrackTintList(new android.content.res.ColorStateList(states,colors));
    }
    public static void glyph(TextView v,String source,int color,int size){
        PremiumGlyph icon=PremiumGlyph.forSymbol(source,color,dp(v.getContext(),size));
        if(icon==null)return;
        android.text.SpannableString span=new android.text.SpannableString(" ");
        span.setSpan(new android.text.style.ImageSpan(icon,
            android.text.style.DynamicDrawableSpan.ALIGN_BOTTOM),0,1,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        v.setText(span);
        v.setGravity(Gravity.CENTER);
    }
'''
styles=styles.replace("    public static void pad(View v,int n){",inject+"    public static void pad(View v,int n){",1)
style.write_text(styles,encoding="utf-8")
g=gradle.read_text(encoding="utf-8")
assert "versionName '3.0.0'" in g and "versionCode 21" in g
g=g.replace("versionName '3.0.0'","versionName '3.1.0'").replace("versionCode 21","versionCode 22")
gradle.write_text(g,encoding="utf-8")
print("VistaBrowser 3.1 premium UI applied without changing navigation, media, scanner, or storage behavior")
