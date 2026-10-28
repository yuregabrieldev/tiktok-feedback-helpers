package com.harvester.browser;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputType;
import java.net.URI;

/** D-pad-first widescreen TV browser. The original mobile scanner stays intact. */
public final class VistaBrowserTvActivity extends Activity {
    private WebView web;
    private EditText address;
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable box(int color,boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(12));
        d.setStroke(dp(focused?3:1),focused?0xffa4a3ff:0xff4c4a75);
        return d;
    }
    private Button action(String label,Runnable click){
        Button v = new Button(this);
        v.setText(label);v.setAllCaps(false);v.setTextSize(15);v.setTextColor(Color.WHITE);
        v.setBackground(box(0xff313052,false));
        v.setOnClickListener(w->click.run());
        v.setOnFocusChangeListener((w,focused)->w.setBackground(box(0xff313052,focused)));
        return v;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff101226);
        getWindow().setNavigationBarColor(0xff101226);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(16),dp(22),dp(16));
        root.setBackgroundColor(0xff101226);
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("VISTABROWSER  •  TV");
        title.setTextColor(0xffafa8ff);title.setTextSize(19);
        root.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Navega com o comando • scanner e biblioteca no VistaBrowser completo");
        sub.setTextColor(0xffc3c1dd);sub.setTextSize(13);
        root.addView(sub);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams toolbarLp = new LinearLayout.LayoutParams(-1,dp(64));
        toolbarLp.topMargin=dp(14);toolbarLp.bottomMargin=dp(11);
        root.addView(toolbar,toolbarLp);

        address = new EditText(this);
        address.setText("https://www.google.com");
        address.setSingleLine(true);
        address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        address.setTextColor(Color.WHITE);
        address.setTextSize(16);address.setSelectAllOnFocus(true);
        address.setPadding(dp(14),dp(6),dp(14),dp(6));
        address.setBackground(box(0xff272946,false));
        toolbar.addView(address,new LinearLayout.LayoutParams(0,dp(51),1));
        Button go=action("Ir",()->openAddress());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(75),dp(51));
        lp.leftMargin=dp(9);toolbar.addView(go,lp);
        Button back=action("‹",()->{if(web.canGoBack())web.goBack();});
        lp=new LinearLayout.LayoutParams(dp(55),dp(51));lp.leftMargin=dp(8);toolbar.addView(back,lp);
        Button next=action("›",()->{if(web.canGoForward())web.goForward();});
        lp=new LinearLayout.LayoutParams(dp(55),dp(51));lp.leftMargin=dp(8);toolbar.addView(next,lp);
        Button reload=action("Atualizar",()->web.reload());
        lp=new LinearLayout.LayoutParams(dp(115),dp(51));lp.leftMargin=dp(8);toolbar.addView(reload,lp);
        Button full=action("Scanner e biblioteca",()->{
            try{startActivity(new Intent(this,MainActivity.class));}
            catch(Exception ignored){}
        });
        lp=new LinearLayout.LayoutParams(dp(220),dp(51));lp.leftMargin=dp(8);toolbar.addView(full,lp);

        web=new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url) {
                if(!address.hasFocus()) address.setText(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme=request.getUrl().getScheme();
                return !("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme));
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
        if(state!=null)web.restoreState(state);
        else web.loadUrl("https://www.google.com");
    }
    private void openAddress() {
        String value=address.getText().toString().trim();
        if(value.isEmpty())return;
        if(!value.contains("://"))value="https://"+value;
        try {
            URI uri=URI.create(value);
            if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))
                ||uri.getHost()==null) {
                android.widget.Toast.makeText(this,"Insere um endereço HTTP/HTTPS válido.",android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            web.loadUrl(uri.toString());
            web.requestFocus();
        }catch(Exception e){
            android.widget.Toast.makeText(this,"Endereço inválido.",android.widget.Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        if(web!=null)web.saveState(out);
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed() {
        if(web!=null&&web.canGoBack())web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if(web!=null){web.stopLoading();web.destroy();web=null;}
        super.onDestroy();
    }
}
