package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.harvester.browser.ui.Styles;

/**
 * Integrated, non-exported Simples Scraper engine. All original v3.0.1
 * screens and operations are inherited, with VistaBrowser premium palettes.
 * Native scanner tab data lives in VistaBrowser's own app data.
 */
public class VistaScraperActivity extends MainActivityV25 {
    private int activeAccent;
    private boolean light;

    private void selectPremiumPalette(){
        android.content.SharedPreferences pref=getSharedPreferences("vistabrowser-appearance",MODE_PRIVATE);
        int mode=pref.getInt("theme_mode",0);
        light=mode==1 || (mode==0 &&
            (getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)
                ==Configuration.UI_MODE_NIGHT_NO);
        Styles.ACCENT=pref.getInt("accent_color",0xFFE1DEC9);
        Styles.setTheme(light);
        activeAccent=Styles.ink(Styles.ACCENT);

        // The source classes are included unmodified except for making their
        // palette constants runtime-configurable. No scraper algorithms change.
        MainActivityV17.WINE=activeAccent;
        MainActivityV17.WINE2=activeAccent;
        MainActivityV17.INK=Styles.TEXT;
        MainActivityV17.FAINT=Styles.MUTED;
        MainActivityV17.CANVAS=Styles.BG;
        MainActivityV17.WHITE=Styles.SURFACE;
        MainActivityV17.BLUSH=light?0xFFE6E5DB:0xFF33362E;
        MainActivityV17.BORDER=Styles.BORDER;
        MainActivityV21.DARK=Styles.BG;
        MainActivityV21.PALE=Styles.SURFACE;
        MainActivityV22.SUCCESS_GREEN=activeAccent;
        MainActivityV22.GREEN_LIGHT=light?0xFFE6ECE2:0xFF303B30;
        MainActivityV22.RED_WARNING=light?0xFF9D4052:0xFFEEA5AF;
    }
    @Override public void onCreate(Bundle state){
        selectPremiumPalette();
        super.onCreate(state);
        applyPremiumChrome();
        String destination=getIntent()==null?"":getIntent().getStringExtra("vistabrowser_scraper_page");
        String sourceUrl=getIntent()==null?"":getIntent().getStringExtra("vistabrowser_source_url");
        if(sourceUrl!=null&&sourceUrl.startsWith("https://")&&urlBox!=null)
            urlBox.setText(sourceUrl);
        if("scan".equals(destination))navigate(0);
        else if("iframes".equals(destination))navigate(1);
        else if("discoveries".equals(destination))openIframeDiscoveries();
        else if("videos".equals(destination))navigate(2);
        else if("library".equals(destination))navigate(3);
        else if("settings".equals(destination))navigate(4);
        else if("brcatalog".equals(destination))
            startActivity(new Intent(this,BrCatalogActivity.class));
        applyPremiumChrome();
    }
    @Override protected void onResume(){
        super.onResume();
        // Color consistency after returning from player or BR catalogue.
        applyPremiumChrome();
    }
    @Override void buildUi(){
        super.buildUi();
        if(screenTitle!=null){
            screenTitle.setTextColor(Styles.TEXT);
            screenTitle.setTextSize(21);
        }
        if(toolbar21!=null){
            toolbar21.setBackgroundColor(Styles.BG);
            TextView exit=new TextView(this);
            exit.setText("‹");
            exit.setTextSize(32);
            exit.setTextColor(activeAccent);
            exit.setGravity(Gravity.CENTER);
            exit.setContentDescription("Voltar ao VistaBrowser");
            toolbar21.addView(exit,0,new LinearLayout.LayoutParams(dp(38),dp(49)));
            exit.setOnClickListener(v->finish());
        }
        applyPremiumChrome();
    }
    @Override void navigate(int next){
        super.navigate(next);
        applyPremiumChrome();
    }
    @Override TextView tv(String text,int size,int requested){
        TextView view=super.tv(text,size,requested);
        int value=requested;
        if(value==MainActivityV17.INK || value==MainActivityV17.WHITE) value=Styles.TEXT;
        else if(value==MainActivityV17.FAINT)value=Styles.MUTED;
        else if(value==MainActivityV17.WINE || value==MainActivityV22.SUCCESS_GREEN)
            value=activeAccent;
        view.setTextColor(value);
        return view;
    }
    @Override GradientDrawable rounded(int color,int radius){
        int fill=color;
        if(color==MainActivityV21.DARK || color==MainActivityV17.CANVAS)fill=Styles.BG;
        else if(color==MainActivityV17.WHITE)fill=Styles.SURFACE;
        else if(color==MainActivityV17.BLUSH)fill=light?0xFFF0EEE5:0xFF33362F;
        else if(color==MainActivityV17.WINE)fill=activeAccent;
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(Math.max(15,radius)));
        return shape;
    }
    @Override Button action(String label,boolean selected){
        Button b=super.action(label,selected);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(selected?(light?0xFFFFFFFF:0xFF1B1E18):Styles.TEXT);
        b.setBackground(Styles.stroke(this,
            selected?activeAccent:Styles.SURFACE,15,
            selected?activeAccent:Styles.BORDER));
        b.setElevation(0);
        return b;
    }
    @Override EditText field(String hint,int inputType){
        EditText field=super.field(hint,inputType);
        field.setTextColor(Styles.TEXT);
        field.setHintTextColor(Styles.MUTED);
        field.setBackground(Styles.stroke(this,Styles.SURFACE,15,Styles.BORDER));
        return field;
    }
    private void applyPremiumChrome(){
        if(root21==null)return;
        root21.setBackgroundColor(Styles.BG);
        if(toolbar21!=null)toolbar21.setBackgroundColor(Styles.BG);
        if(pages!=null){
            for(int i=0;i<pages.getChildCount();i++){
                View page=pages.getChildAt(i);
                if(page instanceof ScrollView)page.setBackgroundColor(Styles.BG);
            }
        }
        if(screenTitle!=null)screenTitle.setTextColor(Styles.TEXT);
        if(navButtons!=null){
            for(int i=0;i<navButtons.length;i++){
                Button b=navButtons[i];
                if(b==null)continue;
                boolean active=i==screen;
                b.setTextColor(active?(light?Color.WHITE:0xFF1E211C):Styles.TEXT);
                b.setBackground(Styles.stroke(this,
                    active?activeAccent:Styles.SURFACE,16,
                    active?activeAccent:Styles.BORDER));
                b.setElevation(0);
            }
        }
        getWindow().setStatusBarColor(Styles.BG);
        getWindow().setNavigationBarColor(Styles.BG);
        int flags=getWindow().getDecorView().getSystemUiVisibility();
        if(light)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        else flags&=~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }
}
