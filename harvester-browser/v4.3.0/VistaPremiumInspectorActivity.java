package com.yure.videoharvester;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import com.harvester.browser.ui.Styles;

/**
 * Brings the real v2.8 player inspector screens into the VistaBrowser visual
 * system without touching the WebView player, event listeners, DOM scan or
 * popup/inspector engine.
 */
public final class VistaPremiumInspectorActivity extends PlayerInspectorActivityV28 {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(browser==null)return;
        View root=browser.getRootView();
        if(root instanceof ViewGroup)styleContainer((ViewGroup)root,0);
        getWindow().setStatusBarColor(Styles.BG);
        getWindow().setNavigationBarColor(Styles.BG);
        int current=getWindow().getDecorView().getSystemUiVisibility();
        if(Styles.LIGHT)current|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        else current&=~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setSystemUiVisibility(current);
    }
    private void styleContainer(ViewGroup root,int depth){
        if(depth>10)return;
        if(depth==1 && root instanceof LinearLayout)root.setBackgroundColor(Styles.BG);
        for(int i=0;i<root.getChildCount();i++){
            View v=root.getChildAt(i);
            if(v instanceof WebView)continue; // Website styles and player stay untouched.
            if(v instanceof Button){
                Button button=(Button)v;
                String text=button.getText()==null?"":button.getText().toString();
                boolean primary=text.contains("Capturar")||text.contains("Guardar")||
                    text.contains("Salvar")||text.contains("Reproduzir");
                int c=Styles.ink(Styles.ACCENT);
                int fg=primary?(Styles.LIGHT?0xFFFFFFFF:0xFF191C17):Styles.TEXT;
                button.setTextColor(fg);
                button.setAllCaps(false);
                button.setTextSize(13);
                button.setBackground(Styles.stroke(this,
                    primary?c:Styles.SURFACE,16,primary?c:Styles.BORDER));
                button.setMinHeight(dp(48));
                button.setElevation(0);
            }else if(v instanceof TextView){
                TextView text=(TextView)v;
                String value=text.getText()==null?"":text.getText().toString();
                text.setTextColor(value.startsWith("Página:")||
                    value.startsWith("Download:")||value.startsWith("Pop-ups:")?
                    Styles.MUTED:Styles.TEXT);
                if(text.getTextSize()/getResources().getDisplayMetrics().scaledDensity<11)
                    text.setTextSize(12);
            }else if(v instanceof ProgressBar){
                ((ProgressBar)v).setProgressTintList(ColorStateList.valueOf(Styles.ink(Styles.ACCENT)));
                ((ProgressBar)v).setProgressBackgroundTintList(ColorStateList.valueOf(Styles.BORDER));
            }
            if(v instanceof ViewGroup)styleContainer((ViewGroup)v,depth+1);
        }
    }
}
