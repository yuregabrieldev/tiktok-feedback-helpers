package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import com.harvester.browser.ui.Styles;
import com.harvester.browser.core.BrazilianMediaHints;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Locale;

/**
 * VistaBrowser player inspector.
 *
 * Capturar vídeo -> observe a user-initiated HTML5 source -> metadata preview
 * -> VistaStream's *existing catalog matching editor* -> confirmed save.
 *
 * Inspector never approves a video merely because a source was detected.
 * The exact VistaStream editor chooses the Cinemeta/AniList title and episode.
 * Old DOM/iframe inspector controls and popup guard remain inherited.
 */
public final class VistaPremiumInspectorActivity extends PlayerInspectorActivityV28 {
    private static final int VISTA_IMPORT_REQUEST = 4323;
    private String importUrl;
    private String importTitle;
    private String importPage;
    private String importFormat;
    private String importMime;
    private boolean importTransient;
    private boolean importPending;
    private boolean importCompleted;
    private String importId;
    private final Handler importHandler=new Handler(Looper.getMainLooper());
    private boolean importingBusy;

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

    /** Original capture was an activity result to the crawler, with no catalog
     * confirmation. Override only the capture action; preserve inspector logic. */
    @Override void captureForVideos(){
        if(browser==null||isFinishing()||isDestroyed())return;
        if(importingBusy){
            Toast.makeText(this,"Conclua a importação anterior primeiro.",Toast.LENGTH_SHORT).show();
            return;
        }
        // Read only media already exposed by the current website to Android WebView.
        // No DRM circumvention, hidden iframe traversal or token extraction from requests.
        final String script="(function(){try{"+
            "const list=[...document.querySelectorAll('video')].filter(v=>v.readyState>=2);"+
            "let src='',r=0,dur=0;"+
            "for(const v of list){const u=v.currentSrc||v.src||(v.querySelector('source')||{}).src||'';"+
            "if(/^https?:\\/\\//i.test(u)&&v.readyState>=r){src=u;r=v.readyState;"+
            "dur=(isFinite(v.duration)&&v.duration>0)?Math.floor(v.duration):0;}}"+
            "if(!src)return JSON.stringify({ok:false,reason:'Nenhum URL de vídeo HTTP/HTTPS utilizável foi exposto pelo player. Inicie a reprodução e tente novamente.'});"+
            "let f=/\\.m3u8(?:[?#]|$)/i.test(src)?'HLS':"+
            "/\\.mpd(?:[?#]|$)/i.test(src)?'DASH':"+
            "/\\.webm(?:[?#]|$)/i.test(src)?'WEBM':"+
            "/\\.(?:mp4|m4v|mov)(?:[?#]|$)/i.test(src)?'MP4':'HTML5';"+
            "return JSON.stringify({ok:true,url:src,title:document.title||'',format:f,ready:r,duration:dur,page:location.href});"+
            "}catch(err){return JSON.stringify({ok:false,reason:'Não foi possível ler a fonte: '+String(err)});}})()";
        importingBusy=true;
        browser.evaluateJavascript(script,encoded->{
            importingBusy=false;
            if(isFinishing()||isDestroyed())return;
            try{
                if(encoded==null||"null".equals(encoded))
                    throw new IllegalArgumentException("O player não devolveu dados");
                JSONObject obj=new JSONObject(new JSONArray("["+encoded+"]").getString(0));
                if(!obj.optBoolean("ok")){
                    new AlertDialog.Builder(this)
                        .setTitle("Vídeo ainda não identificado")
                        .setMessage(obj.optString("reason","Comece a reproduzir o vídeo e toque novamente em Capturar."))
                        .setPositiveButton("OK",null).show();
                    return;
                }
                String raw=obj.optString("url","").trim();
                Uri uri=Uri.parse(raw);
                if(raw.length()>8192||raw.length()<9||uri.getHost()==null||
                    !("https".equalsIgnoreCase(uri.getScheme())||
                      "http".equalsIgnoreCase(uri.getScheme()))){
                    Toast.makeText(this,"Esta fonte não é uma URL de vídeo válida.",Toast.LENGTH_LONG).show();
                    return;
                }
                String format=obj.optString("format","HTML5");
                String lower=raw.toLowerCase(Locale.ROOT);
                // Classify signed and ephemeral URLs. The editor will save them as
                // temporary sources only, never promise permanent offline content.
                boolean transientSource=false;
                for(String marker:new String[]{"token=","expires=","signature=","sig=",
                    "ip_bind=","policy=","proxy.mp4","&url=","sv=","auth=","key=",
                    "session=","bearer="}){
                    if(lower.contains(marker)){transientSource=true;break;}
                }
                String suggested=getIntent().getStringExtra("mediaTitle");
                if(suggested==null||suggested.trim().isEmpty()){
                    suggested=getIntent().getStringExtra("title");
                }
                if(suggested==null||suggested.trim().isEmpty())suggested=obj.optString("title","");
                if(suggested.trim().isEmpty())suggested="Vídeo encontrado";
                String page=firstPage(
                    getIntent().getStringExtra("sourcePage"),
                    getIntent().getStringExtra("url"),
                    obj.optString("page",""));
                reviewAndImport(raw,suggested,format,page,transientSource);
            }catch(Exception error){
                Toast.makeText(this,"Não foi possível identificar o vídeo: "+error.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private String firstPage(String... candidates){
        for(String value:candidates){
            if(value==null||value.trim().isEmpty())continue;
            try{
                Uri parsed=Uri.parse(value);
                if(parsed.getHost()!=null && (
                    "https".equalsIgnoreCase(parsed.getScheme()) ||
                    "http".equalsIgnoreCase(parsed.getScheme()))){
                    // Do not include search tokens/cookies in referrer metadata.
                    return parsed.buildUpon().clearQuery().fragment(null).build().toString();
                }
            }catch(Exception ignored){}
        }
        return "";
    }

    private String mime(String format){
        if("HLS".equalsIgnoreCase(format))return "application/vnd.apple.mpegurl";
        if("DASH".equalsIgnoreCase(format))return "application/dash+xml";
        if("WEBM".equalsIgnoreCase(format))return "video/webm";
        if("MP4".equalsIgnoreCase(format))return "video/mp4";
        return "video/*";
    }

    private void reviewAndImport(String url,String rawTitle,String format,String page,boolean ephemeral){
        final BrTitleParser.Parsed parsed=BrTitleParser.parse(rawTitle);
        final BrazilianMediaHints.Result hints=BrazilianMediaHints.infer(
            rawTitle,parsed.title,page,format);
        String host=Uri.parse(url).getHost();
        String details="Título sugerido: "+parsed.title+
            "\n"+(parsed.season>0?"Temporada "+parsed.season+" • Episódio "+parsed.episode:
                "Filme / série: por confirmar")+
            "\nFormato: "+format+
            "\nÁudio: "+(hints.audioMode.isEmpty()?"a confirmar":hints.audioMode)+
            "\nIdioma: "+(hints.locale.isEmpty()?"a confirmar":hints.locale)+
            "\nQualidade: "+(hints.quality.isEmpty()?"a confirmar":hints.quality)+
            "\nOrigem: "+(host==null?"não indicada":host)+
            (ephemeral?"\n\nA fonte parece temporária e poderá expirar.":"")+
            "\n\nO VistaStream pesquisará correspondências no catálogo para "+
            "selecionar o título e o episódio certos. Nada é guardado sem confirmação.";
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22),dp(19),dp(22),dp(18));
        content.setBackgroundColor(Styles.SURFACE);
        TextView heading=new TextView(this);
        heading.setText("Guardar no VistaStream");
        heading.setTextColor(Styles.TEXT);heading.setTextSize(21);
        heading.setTypeface(null,1);
        content.addView(heading);
        TextView description=new TextView(this);
        description.setText(details);
        description.setTextColor(Styles.MUTED);
        description.setTextSize(14);
        description.setLineSpacing(dp(4),1f);
        description.setPadding(0,dp(15),0,dp(13));
        content.addView(description);
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setView(content)
            .setNegativeButton("Voltar ao player",null)
            .setPositiveButton("Analisar metadados e associar",null)
            .create();
        dialog.setOnShowListener(d->{
            int positive=Styles.ink(Styles.ACCENT);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(positive);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Styles.MUTED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                dialog.dismiss();
                openVistaEditor(url,rawTitle,format,page,ephemeral,parsed,hints);
            });
        });
        dialog.show();
    }

    private void openVistaEditor(String url,String title,String format,String page,
                                 boolean ephemeral,BrTitleParser.Parsed parsed,
                                 BrazilianMediaHints.Result hints){
        if(importPending)return;
        if(!"".equals(url)&&Uri.parse(url).getHost()==null)return;
        Intent request=new Intent("com.vistastream.tv.action.IMPORT");
        request.putExtra("com.vistastream.tv.extra.URL",url);
        request.putExtra("com.vistastream.tv.extra.TITLE",parsed.title);
        request.putExtra("com.vistastream.tv.extra.KIND",
            parsed.season>0&&parsed.episode>0?"tv":"video");
        request.putExtra("com.vistastream.tv.extra.MIME_TYPE",mime(format));
        if(!page.isEmpty()){
            request.putExtra("com.vistastream.tv.extra.PAGE_URL",page);
            request.putExtra("com.vistastream.tv.extra.REFERER",page);
        }
        if(parsed.season>0&&parsed.episode>0){
            request.putExtra("com.vistastream.tv.extra.SEASON",parsed.season);
            request.putExtra("com.vistastream.tv.extra.EPISODE",parsed.episode);
        }
        if(hints.season>0&&hints.episode>0){
            request.putExtra("com.vistastream.tv.extra.SEASON",hints.season);
            request.putExtra("com.vistastream.tv.extra.EPISODE",hints.episode);
        }
        if(!hints.audioMode.isEmpty())
            request.putExtra("com.vistastream.tv.extra.AUDIO_MODE",hints.audioMode);
        if(!hints.locale.isEmpty())
            request.putExtra("com.vistastream.tv.extra.AUDIO_LANGUAGE",hints.locale);
        if(!hints.quality.isEmpty())
            request.putExtra("com.vistastream.tv.extra.QUALITY",hints.quality);
        if(!hints.providerLabel.isEmpty())
            request.putExtra("com.vistastream.tv.extra.PROVIDER_LABEL",hints.providerLabel);
        importId=String.valueOf(SystemClock.elapsedRealtimeNanos());
        request.putExtra("com.vistastream.tv.extra.CONFIRMATION_TOKEN",importId);
        final String expected=url;
        ResultReceiver callback=new ResultReceiver(importHandler){
            @Override protected void onReceiveResult(int code,Bundle result){
                if(code!=RESULT_OK||result==null||!importPending)return;
                if(!expected.equals(result.getString("com.vistastream.tv.extra.URL","")))return;
                Intent receipt=new Intent()
                    .putExtra("com.vistastream.tv.extra.URL",expected)
                    .putExtra("com.vistastream.tv.extra.TITLE",
                        result.getString("com.vistastream.tv.extra.TITLE",title))
                    .putExtra("com.vistastream.tv.extra.SAVED_WEB_ONLY",
                        result.getBoolean("com.vistastream.tv.extra.SAVED_WEB_ONLY",false))
                    .putExtra("com.vistastream.tv.extra.CATALOG_LINKED",
                        result.getBoolean("com.vistastream.tv.extra.CATALOG_LINKED",false))
                    .putExtra("com.vistastream.tv.extra.SAVED_TEMPORARY",
                        result.getBoolean("com.vistastream.tv.extra.SAVED_TEMPORARY",false));
                finishConfirmedImport(RESULT_OK,receipt);
            }
        };
        request.putExtra("com.vistastream.tv.extra.CONFIRMATION_RECEIVER",callback);
        for(String pkg:new String[]{"com.vistastream.tv.debug","com.vistastream.tv"}){
            Intent start=new Intent(request);
            start.setClassName(pkg,"com.ivor.openstream.harvester.HarvesterImportActivity");
            try{
                importUrl=url;importTitle=title;importPage=page;
                importFormat=format;importMime=mime(format);
                importTransient=ephemeral;importPending=true;importCompleted=false;
                startActivityForResult(start,VISTA_IMPORT_REQUEST);
                return;
            }catch(android.content.ActivityNotFoundException|SecurityException ignored){
                importPending=false;
            }
        }
        new AlertDialog.Builder(this).setTitle("Atualize o VistaStream")
            .setMessage("Não encontrei o editor de catálogo. Instale o VistaStream 0.15.4 ou mais recente e volte a tocar em Capturar vídeo.")
            .setPositiveButton("OK",null).show();
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=VISTA_IMPORT_REQUEST||importCompleted||!importPending)return;
        if(result==RESULT_OK){
            finishConfirmedImport(result,data);
            return;
        }
        // A confirmed ResultReceiver may be delivered after the Android
        // RESULT_CANCELED. Preserve the inspector and wait before saying canceled.
        importHandler.postDelayed(()->{
            if(!importPending||importCompleted||isFinishing()||isDestroyed())return;
            importPending=false;
            Toast.makeText(this,
                "Importação não confirmada. O vídeo continua disponível no inspetor.",
                Toast.LENGTH_LONG).show();
        },2000);
    }

    private void finishConfirmedImport(int code,Intent confirmation){
        if(!importPending||importCompleted||code!=RESULT_OK||confirmation==null)return;
        if(!importUrl.equals(confirmation.getStringExtra("com.vistastream.tv.extra.URL")))return;
        if(confirmation.getBooleanExtra("com.vistastream.tv.extra.SAVED_WEB_ONLY",false)){
            importPending=false;
            Toast.makeText(this,
                "Foi guardada apenas uma referência Web. Selecione uma fonte de vídeo reproduzível.",
                Toast.LENGTH_LONG).show();
            return;
        }
        importPending=false;importCompleted=true;
        Intent result=new Intent();
        result.putExtra("page",getIntent().getStringExtra("url"));
        result.putExtra("sourcePage",importPage);
        result.putExtra("captureToVideos",true);
        result.putExtra("mediaDetected",true);
        result.putExtra("videoCount",1);
        result.putExtra("activeVideos",1);
        result.putExtra("mediaCount",1);
        result.putExtra("mediaFormat",importFormat);
        result.putExtra("mediaTitle",confirmation.getStringExtra("com.vistastream.tv.extra.TITLE"));
        result.putExtra("sourceHost",Uri.parse(importUrl).getHost());
        result.putExtra("sessionBound",importTransient||
            confirmation.getBooleanExtra("com.vistastream.tv.extra.SAVED_TEMPORARY",false));
        boolean transientSource=importTransient||
            confirmation.getBooleanExtra("com.vistastream.tv.extra.SAVED_TEMPORARY",false);
        // Do not duplicate session-bound URLs into the scraper catalog.
        result.putExtra("mediaUrl",transientSource?"":importUrl);
        result.putExtra("vistaStreamConfirmed",true);
        result.putExtra("vistaStreamCatalogLinked",
            confirmation.getBooleanExtra("com.vistastream.tv.extra.CATALOG_LINKED",false));
        setResult(RESULT_OK,result);
        Toast.makeText(this,
            confirmation.getBooleanExtra("com.vistastream.tv.extra.CATALOG_LINKED",false)?
                "Fonte associada ao título no VistaStream.":"Guardado no VistaStream.",
            Toast.LENGTH_LONG).show();
        finish();
    }

    private void styleContainer(ViewGroup root,int depth){
        if(depth>10)return;
        if(depth==1 && root instanceof LinearLayout)root.setBackgroundColor(Styles.BG);
        for(int i=0;i<root.getChildCount();i++){
            View v=root.getChildAt(i);
            if(v instanceof WebView)continue;
            if(v instanceof Button){
                Button button=(Button)v;
                String text=button.getText()==null?"":button.getText().toString();
                boolean primary=text.contains("Capturar")||text.contains("Guardar")||
                    text.contains("Salvar")||text.contains("Reproduzir");
                int c=Styles.ink(Styles.ACCENT);
                int fg=primary?(Styles.LIGHT?Color.WHITE:0xFF191C17):Styles.TEXT;
                button.setTextColor(fg);
                button.setAllCaps(false);
                button.setTextSize(13);
                button.setBackground(Styles.stroke(this,
                    primary?c:Styles.SURFACE,16,primary?c:Styles.BORDER));
                button.setMinHeight(dp(48));
                button.setElevation(0);
            }else if(v instanceof TextView){
                TextView t=(TextView)v;
                String value=t.getText()==null?"":t.getText().toString();
                t.setTextColor(value.startsWith("Página:")||
                    value.startsWith("Download:")||value.startsWith("Pop-ups:")?
                    Styles.MUTED:Styles.TEXT);
                if(t.getTextSize()/getResources().getDisplayMetrics().scaledDensity<11)
                    t.setTextSize(12);
            }else if(v instanceof ProgressBar){
                ((ProgressBar)v).setProgressTintList(ColorStateList.valueOf(Styles.ink(Styles.ACCENT)));
                ((ProgressBar)v).setProgressBackgroundTintList(ColorStateList.valueOf(Styles.BORDER));
            }
            if(v instanceof ViewGroup)styleContainer((ViewGroup)v,depth+1);
        }
    }
}
