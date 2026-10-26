from pathlib import Path
p=Path("hb-v18/app/src/main/java/com/harvester/browser/MainActivity.java")
s=p.read_text(encoding="utf-8")
def swap(a,b,n):
 global s
 count=s.count(a)
 if count!=1:raise AssertionError(f"{n}: {count} matches")
 s=s.replace(a,b,1)

# Unlike Android web content, native Home controls follow the selected app theme.
swap('''        if(selected==null||!HOME.equals(selected.url))return;
        browserHost.removeAllViews();''',
'''        if(selected==null||!HOME.equals(selected.url))return;
        final boolean lightHome=Styles.LIGHT;
        browserHost.removeAllViews();''',"home photo/light palette state")
swap('''        TextView identity=Styles.text(this,"◈   VISTABROWSER",12,true,0xFFE9E6D8);''',
'''        TextView identity=Styles.text(this,"◈   VISTABROWSER",12,true,
            lightHome?0xFF282B25:0xFFE9E6D8);''',"light wordmark header")
swap('''        TextView wordmark=Styles.text(this,"VistaBrowser",43,false,0xFFF4F2E8);''',
'''        TextView wordmark=Styles.text(this,"VistaBrowser",43,false,
            lightHome?0xFF242821:0xFFF4F2E8);''',"large light wordmark")
swap('''        searchPill.setBackground(Styles.stroke(this,0xED171917,30,0x364A4A42));''',
'''        searchPill.setBackground(Styles.stroke(this,
            lightHome?0xFAFAF9F4:0xED171917,30,
            lightHome?0xFFD1D0C7:0x364A4A42));''',"light search glass")
swap('''        TextView glass=Styles.text(this,"⌕",24,false,Styles.ACCENT);
        Styles.glyph(glass,"⌕",Styles.ACCENT,22);''',
'''        TextView glass=Styles.text(this,"⌕",24,false,
            lightHome?0xFF34372D:Styles.ACCENT);
        Styles.glyph(glass,"⌕",lightHome?0xFF34372D:Styles.ACCENT,22);''',"search leading icon")
swap('''        TextView homeSearch=Styles.text(this,"Pesquise ou introduza um endereço",15,false,0xFFD8D5C9);''',
'''        TextView homeSearch=Styles.text(this,"Pesquise ou introduza um endereço",15,false,
            lightHome?0xFF484A41:0xFFD8D5C9);''',"light search placeholder")
swap('''        TextView searchGo=Styles.text(this,"⌕",24,true,Styles.TEXT);
        Styles.glyph(searchGo,"⌕",Styles.TEXT,23);''',
'''        TextView searchGo=Styles.text(this,"⌕",24,true,
            lightHome?0xFF34372D:Styles.TEXT);
        Styles.glyph(searchGo,"⌕",lightHome?0xFF34372D:Styles.TEXT,23);''',"search trailing icon")

# Home quick-entry chips become translucent ivory when the OS theme is light.
swap('''        chip.setBackground(Styles.stroke(this,0xA1242622,24,0xFF41443C));''',
'''        chip.setBackground(Styles.stroke(this,
            Styles.LIGHT?0xF4F7F5EC:0xA1242622,24,
            Styles.LIGHT?0xFFD1D0C7:0xFF41443C));''',"Home light shortcuts")
swap('''        TextView circle=Styles.text(this,"+",24,true,0xFFEEEBDD);''',
'''        TextView circle=Styles.text(this,"+",24,true,
            lightHome?0xFF34372D:0xFFEEEBDD);''',"speed dial letter")
swap('''                int[] tones={0xFF514C41,0xFF625847,0xFF4A544B,0xFF4E5155};''',
'''                int[] tones=lightHome?
                    new int[]{0xFFE0DED0,0xFFEBDCC9,0xFFD5DFD3,0xFFDCDDE2}:
                    new int[]{0xFF514C41,0xFF625847,0xFF4A544B,0xFF4E5155};''',"speed dial tinted backgrounds")
swap('''        tools.setBackground(Styles.stroke(this,0xE6282925,25,0xFF41423A));''',
'''        tools.setBackground(Styles.stroke(this,
            lightHome?0xF8F8F6EF:0xE6282925,25,
            lightHome?0xFFD3D1C8:0xFF41423A));''',"tools Home card")
# Opera-like soft photo, instead of white bars surrounding a dark background.
swap('''        applyWallpaper(wallpaperStage);
        wallpaperStage.addView(scroll,new FrameLayout.LayoutParams(-1,-1));''',
'''        applyWallpaper(wallpaperStage);
        if(lightHome){
            View translucentIvory=new View(this);
            translucentIvory.setBackgroundColor(0x86FAF8ED);
            wallpaperStage.addView(translucentIvory,
                new FrameLayout.LayoutParams(-1,-1));
        }
        wallpaperStage.addView(scroll,new FrameLayout.LayoutParams(-1,-1));''',
"softened floral backdrop for light home")

# Keep the UI controls consistent while navigating between WebViews.
swap('''        LinearLayout addressRow=Styles.row(this);
        addressRow.setBackground(Styles.stroke(this,0xFF292C27,23,0xFF55594D));''',
'''        LinearLayout addressRow=Styles.row(this);
        addressContainer=addressRow;
        addressRow.setBackground(Styles.stroke(this,0xFF292C27,23,0xFF55594D));''',
"address bar outer view reference")
swap('''    private FrameLayout appFrame;''',
'''    private FrameLayout appFrame;
    private LinearLayout addressContainer;''',"track address rounded surface")
swap('''        if(address!=null){
            address.setTextColor(Styles.TEXT);
            address.setHintTextColor(Styles.MUTED);
            address.setBackground(Styles.stroke(this,light?0xFFF0EEE5:0xFF292C27,28,Styles.BORDER));
        }''',
'''        if(addressContainer!=null)
            addressContainer.setBackground(Styles.stroke(this,
                light?0xFFF0EEE5:0xFF292C27,23,Styles.BORDER));
        if(address!=null){
            address.setTextColor(Styles.TEXT);
            address.setHintTextColor(Styles.MUTED);
            address.setBackgroundColor(Color.TRANSPARENT);
        }''',"properly refresh toolbar input container")
swap('''        if(bottomNav!=null){
            bottomNav.setBackground(Styles.bg(this,light?0xFFFAF9F3:0xFF1C2230,27));
            configureBottomNavigation();
        }''',
'''        if(bottomNav!=null){
            bottomNav.setBackground(Styles.bg(this,
                light?0xFFFAF9F3:0xFF1C2230,27));
            if(browserControls!=null)
                browserControls.setBackground(Styles.bg(this,
                    light?0xFFFAF9F3:0xFF1C2230,28));
            configureBottomNavigation();
        }''',"bottom bar surface and icons")

# Opera-style tab overview uses the same background and spacing palette as
# pages, not a permanent dark olive in light mode.
swap('''        page.setBackground(Styles.gradient(this,0xFF302F28,0xFF1A1D17,0));''',
'''        page.setBackground(Styles.gradient(this,
            Styles.LIGHT?0xFFECEADE:0xFF302F28,
            Styles.LIGHT?0xFFFAF9F4:0xFF1A1D17,0));''',
"light tab manager backdrop")
swap('''            card.setBackground(Styles.stroke(this,
                    tab==selected?0xFFE7E5D6:marked?0xFF404C37:0xFF282A24,22,
                    marked?Styles.ACCENT:tab==selected?0xFFFFFFFF:0xFF454940));''',
'''            card.setBackground(Styles.stroke(this,
                    Styles.LIGHT?
                        (tab==selected?0xFFFFFFFF:marked?0xFFE4ECD9:0xFFF4F2E9):
                        (tab==selected?0xFFE7E5D6:marked?0xFF404C37:0xFF282A24),
                    22, marked?Styles.ACCENT:
                        tab==selected?0xFFE7E6DB:
                        Styles.LIGHT?0xFFC8C9BD:0xFF454940));''',
"Opera-like pale tab cards with selection")

# Theme selector thumbnails stay black/white irrespective of current palette,
# while selected indicator is readable in both modes.
swap('''        preview.setStroke(d(selectedTheme?2:1),
            selectedTheme?Styles.ACCENT:Styles.border(0xFF484A43));''',
'''        preview.setStroke(d(selectedTheme?2:1),
            selectedTheme?Styles.ink(Styles.ACCENT):
                Styles.LIGHT?0xFFBCBCAF:0xFF484A43);''',
"theme selector highlighted border")
# Restore focus ring and system icon contrast immediately on theme choice.
assert s.count("private void applyBrowserTheme(){")==1
assert "Styles.surface(" in s
assert "if(lightHome)" in s
assert "browserControls.setBackground(" in s
p.write_text(s,encoding="utf-8")
print("Stage2: Opera-like complete light Home, readable wallpaper overlay, unified chrome and tab surfaces")
