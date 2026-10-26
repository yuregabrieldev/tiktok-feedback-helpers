package com.yure.videoharvester;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.harvester.browser.ui.Styles;
import com.harvester.browser.core.BrazilianMediaHints;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Fully embedded scraper engine with VistaBrowser-native visual/navigation
 * shell. The crawl, iframe, inspector, tests and backup implementations remain
 * inherited from Simples Scraper v3.0.1. */
public final class VistaScraperPremiumActivity extends VistaScraperActivity {
    private boolean shellReady=false;
    private LinearLayout shellDock;
    private TextView shellHint;
    private final ArrayList<LinearLayout> dockCells=new ArrayList<>();
    private final ArrayList<TextView> dockGlyphs=new ArrayList<>();
    private final ArrayList<TextView> dockCaptions=new ArrayList<>();
    private static final int IMPORT_REQUEST=4381;
    private Catalog pendingImport;
    private String pendingImportUrl;
    private final ExecutorService galleryIo=Executors.newSingleThreadExecutor();
    private final String[] stepNames={"Scan","Iframes","Vídeos","Biblioteca","Ajustes"};
    private final String[] stepGlyphs={"⌕","◎","▶","▤","☷"};

    private int accent(){return Styles.ink(Styles.ACCENT);}
    private int onAccent(){
        int c=accent();
        double lum=(0.2126*Color.red(c)+0.7152*Color.green(c)+0.0722*Color.blue(c))/255.;
        return lum>=0.59?0xFF1B1E18:0xFFFFFFFF;
    }
    private GradientDrawable frame(int fill,int radius,int stroke){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);g.setCornerRadius(dp(radius));
        if(stroke!=0)g.setStroke(dp(1),stroke);
        return g;
    }
    private TextView t(String value,int size,boolean bold,int color){
        TextView label=new TextView(this);
        label.setText(value);label.setTextSize(size);label.setTextColor(color);
        label.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)label.setTypeface(null,1);
        return label;
    }
    private void place(LinearLayout parent,View v,int height,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));
        p.topMargin=dp(top);parent.addView(v,p);
    }
    private TextView tap(String icon,String text,Runnable fn,boolean primary){
        TextView button=t(icon+"   "+text,14,true,primary?onAccent():Styles.TEXT);
        button.setGravity(Gravity.CENTER);
        button.setBackground(frame(primary?accent():Styles.BG,17,
            primary?0:Styles.BORDER));
        button.setPadding(dp(12),0,dp(12),0);
        button.setClickable(true);button.setFocusable(true);
        button.setOnClickListener(v->fn.run());
        button.setContentDescription(text);
        return button;
    }
    private void hero(LinearLayout screen,String label,String subtitle){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(19),dp(19),dp(19),dp(19));
        box.setBackground(frame(Styles.LIGHT?0xFFF3F2E9:0xFF242923,25,Styles.BORDER));
        TextView eyebrow=t("VISTABROWSER   /   CRAWLER",10,true,accent());
        box.addView(eyebrow);
        TextView heading=t(label,22,true,Styles.TEXT);
        place(box,heading,-2,11);
        TextView description=t(subtitle,12,false,Styles.MUTED);
        description.setMaxLines(4);
        place(box,description,-2,6);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(17);
        screen.addView(box,0,p);
    }
    @Override void buildUi(){
        super.buildUi();
        // Original WebView, engine fields, loaded project and screen containers
        // stay alive; replace only the navigation shell, not the views containing
        // the crawl state or event handlers.
        buildPremiumToolbar();
        buildPremiumDock();
        shellReady=true;
        updateNavigation();
        if(screenTitle!=null)screenTitle.setTextSize(23);
    }
    private void buildPremiumToolbar(){
        if(toolbar21==null)return;
        toolbar21.removeAllViews();
        toolbar21.setGravity(Gravity.CENTER_VERTICAL);
        // A weighted LayoutParams from the inherited screenTitle was making
        // the vertical toolbar expand to several hundred pixels.
        toolbar21.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(84)));
        toolbar21.setPadding(dp(16),dp(15),dp(16),dp(14));
        toolbar21.setBackgroundColor(Styles.BG);

        TextView back=t("‹",34,false,Styles.TEXT);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Voltar ao VistaBrowser");
        back.setOnClickListener(v->finish());
        toolbar21.addView(back,new LinearLayout.LayoutParams(dp(42),dp(58)));

        LinearLayout titleGroup=new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.setGravity(Gravity.CENTER_VERTICAL);
        titleGroup.setPadding(dp(5),0,0,0);
        shellHint=t("VISTABROWSER  •  SCANNER",10,true,accent());
        titleGroup.addView(shellHint);
        if(screenTitle!=null){
            if(screenTitle.getParent() instanceof android.view.ViewGroup)
                ((android.view.ViewGroup)screenTitle.getParent()).removeView(screenTitle);
            screenTitle.setTextColor(Styles.TEXT);
            screenTitle.setTextSize(23);
            titleGroup.addView(screenTitle,new LinearLayout.LayoutParams(-1,dp(39)));
        }
        toolbar21.addView(titleGroup,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=t("⋮",29,true,Styles.TEXT);
        menu.setGravity(Gravity.CENTER);
        menu.setBackground(frame(Styles.SURFACE,18,Styles.BORDER));
        menu.setContentDescription("Mais ferramentas do crawler");
        menu.setOnClickListener(v->showMenu(menu));
        toolbar21.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(50)));
    }
    private void buildPremiumDock(){
        if(root21==null)return;
        View old=root21.getChildAt(root21.getChildCount()-1);
        if(old!=null)root21.removeView(old);
        shellDock=new LinearLayout(this);
        shellDock.setOrientation(LinearLayout.HORIZONTAL);
        shellDock.setPadding(dp(8),dp(7),dp(8),dp(9));
        shellDock.setBackground(frame(Styles.LIGHT?0xFFFAF9F4:0xFF1F231F,25,Styles.BORDER));
        shellDock.setElevation(dp(7));
        for(int i=0;i<5;i++){
            final int index=i;
            LinearLayout cell=new LinearLayout(this);
            cell.setGravity(Gravity.CENTER);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setPadding(dp(1),dp(6),dp(1),dp(6));
            TextView glyph=t(stepGlyphs[i],23,true,Styles.TEXT);
            glyph.setGravity(Gravity.CENTER);
            cell.addView(glyph,new LinearLayout.LayoutParams(-1,dp(28)));
            TextView caption=t(stepNames[i],10,false,Styles.MUTED);
            caption.setGravity(Gravity.CENTER);
            cell.addView(caption,new LinearLayout.LayoutParams(-1,dp(22)));
            cell.setContentDescription(stepNames[i]);
            cell.setOnClickListener(v->navigate(index));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(66),1);
            lp.leftMargin=dp(2);lp.rightMargin=dp(2);
            shellDock.addView(cell,lp);
            dockCells.add(cell);dockGlyphs.add(glyph);dockCaptions.add(caption);
        }
        root21.addView(shellDock,new LinearLayout.LayoutParams(-1,dp(82)));
    }
    private void updateNavigation(){
        if(!shellReady)return;
        String heading=screen==0?"Explorar páginas":screen==1?
            (detailMode22?"Descobertas e players":"Análise de iframes"):
            screen==2?"Vídeos encontrados":screen==3?"Biblioteca":"Configurações";
        if(screenTitle!=null)screenTitle.setText(heading);
        for(int i=0;i<dockCells.size();i++){
            boolean selected=i==screen;
            LinearLayout cell=dockCells.get(i);
            cell.setBackground(frame(selected?(Styles.LIGHT?0xFFE8E7D9:0xFF373C32):
                (Styles.LIGHT?0xFFFAF9F4:0xFF1F231F),18,selected?accent():0));
            dockGlyphs.get(i).setTextColor(selected?accent():Styles.MUTED);
            dockCaptions.get(i).setTextColor(selected?Styles.TEXT:Styles.MUTED);
            dockCaptions.get(i).setTypeface(null,selected?1:0);
        }
    }
    @Override void navigate(int next){
        super.navigate(next);
        updateNavigation();
    }
    @Override void openIframeDiscoveries(){
        super.openIframeDiscoveries();
        updateNavigation();
    }
    @Override LinearLayout sectionCard(LinearLayout parent,String heading,String description){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17),dp(18),dp(17),dp(19));
        card.setBackground(frame(Styles.SURFACE,24,Styles.LIGHT?Styles.BORDER:0xFF373C34));
        TextView header=t(heading,18,true,Styles.TEXT);
        card.addView(header); // V24 finds "Ações da fila" from the first child
        if(description!=null&&!description.trim().isEmpty()){
            TextView desc=t(description,12,false,Styles.MUTED);
            desc.setLineSpacing(dp(3),1f);
            place(card,desc,-2,7);
        }
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(13);
        parent.addView(card,p);
        return card;
    }
    @Override void btnFull(LinearLayout parent,String label,boolean primary,Runnable fn){
        // Keep every original listener, handler and scanner state.
        String symbol="";
        String cleaned=label.trim();
        if(cleaned.startsWith("▶")||cleaned.startsWith("✓")||cleaned.startsWith("＋")||
           cleaned.startsWith("■")||cleaned.startsWith("↓")||cleaned.startsWith("↑")||
           cleaned.startsWith("▣")){
            symbol=cleaned.substring(0,1);cleaned=cleaned.substring(1).trim();
        }
        TextView control=t((symbol.isEmpty()?"":symbol+"   ")+cleaned,14,true,
            primary?onAccent():Styles.TEXT);
        control.setGravity(Gravity.CENTER);
        control.setBackground(frame(primary?accent():Styles.BG,17,
            primary?0:Styles.BORDER));
        control.setOnClickListener(v->fn.run());
        control.setContentDescription(cleaned);
        place(parent,control,53,10);
    }
    @Override TextView miniStat(LinearLayout parent,String title,int requested){
        LinearLayout cell=new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(12),dp(14),dp(11),dp(12));
        cell.setBackground(frame(Styles.LIGHT?0xFFF5F4EB:0xFF222720,18,Styles.BORDER));
        TextView value=t("0",25,true,accent());
        cell.addView(value);
        TextView label=t(title,11,false,Styles.MUTED);
        label.setMaxLines(2);
        place(cell,label,-2,3);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(91),1);
        if(parent.getChildCount()>0)p.leftMargin=dp(10);
        parent.addView(cell,p);
        return value;
    }
    @Override void createScanPage(){
        super.createScanPage();
        hero(scanBody,"Scan completo",
            "Descubra páginas, players e fontes num projeto com progresso guardado. Defina limites antes de iniciar.");
    }
    @Override void createIframePage(){
        super.createIframePage();
        hero(iframeBody,"Inspeção de players",
            "Analise os iframes encontrados, filtre a fila e confirme quais fontes realmente funcionam.");
        if(iframeProgress22!=null){
            iframeProgress22.setProgressTintList(ColorStateList.valueOf(accent()));
            iframeProgress22.setProgressBackgroundTintList(ColorStateList.valueOf(Styles.BORDER));
        }
    }
    @Override void createVideosPage(){
        LinearLayout card=sectionCard(videosBody,"Rever descobertas",
            "Encontrar uma URL não é guardar um vídeo. Teste a fonte, confirme os metadados e escolha o destino.");
        sourceStat=t("0 fontes candidatas",14,true,accent());
        place(card,sourceStat,-2,12);
        videosCount21=t("",12,false,Styles.MUTED);
        place(card,videosCount21,-2,4);
        TextView refresh=t("⟳   Atualizar descobertas",13,true,Styles.TEXT);
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(frame(Styles.BG,16,Styles.BORDER));
        refresh.setOnClickListener(v->renderVideos());
        place(card,refresh,47,12);
        videosCards=new LinearLayout(this);
        videosCards.setOrientation(LinearLayout.VERTICAL);
        videosBody.addView(videosCards);
        hero(videosBody,"Os seus vídeos",
            "Fonte detetada  →  Reprodução  →  Metadados  →  Galeria / VistaStream");
    }
    @Override void createLibraryPage(){
        LinearLayout header=sectionCard(libraryBody,"Biblioteca do scanner",
            "Títulos aprovados e projetos de vídeo. Os ficheiros físicos ficam na Galeria, não nesta lista.");
        libraryCount21=t("0 títulos",14,true,accent());
        place(header,libraryCount21,-2,10);
        TextView refresh=t("⟳   Atualizar biblioteca",13,true,Styles.TEXT);
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(frame(Styles.BG,16,Styles.BORDER));
        refresh.setOnClickListener(v->renderLibrary());
        place(header,refresh,46,12);
        libraryCards=new LinearLayout(this);
        libraryCards.setOrientation(LinearLayout.VERTICAL);
        libraryBody.addView(libraryCards);
        hero(libraryBody,"Biblioteca",
            "Revise os títulos confirmados, reproduza, exporte ou continue a edição no VistaStream.");
    }
    @Override void createSettingsPage(){
        super.createSettingsPage();
        hero(settingsBody,"Ajustes de rastreio",
            "Limites de análise, execução, backups JSON e fontes brasileiras opcionais.");
    }
    private String resultState(Catalog item){
        boolean iframeOnly=true,hasDirect=false,hasPlayed=false;
        for(Source source:item.sources){
            boolean direct=HarvesterVistaBridge.isShareable(source.url,source.format);
            if(direct){iframeOnly=false;hasDirect=true;}
            if(direct && "PLAYED".equals(getSharedPreferences(PLAY_PREFS,0)
                .getString("source_"+source.url.hashCode(),"")))hasPlayed=true;
        }
        return hasPlayed?"Fonte reproduzida e confirmada":
               hasDirect?"Fonte direta por confirmar":
               iframeOnly?"Apenas referências de player":"Aguarda inspeção";
    }
    private LinearLayout itemCard(LinearLayout parent,Catalog item,boolean saved){
        LinearLayout card=sectionCard(parent,item.title==null?"Vídeo sem título":item.title,
            resultState(item)+"  •  "+item.sources.size()+" referências");
        BrTitleParser.Parsed meta=BrTitleParser.parse(item.title);
        String note=meta.displayHint();
        if(!note.isEmpty())place(card,t(note+" · pistas obtidas do nome",11,false,Styles.MUTED),-2,5);
        if(item.sources.size()>0) {
            Source source=item.sources.get(0);
            String origin;
            try{origin=Uri.parse(source.page).getHost();}catch(Exception e){origin=""; }
            place(card,t("Origem: "+(origin==null||origin.isEmpty()?"não identificada":origin),
                11,false,Styles.MUTED),-2,8);
        }
        return card;
    }
    @Override void renderVideos(){
        if(videosCards==null)return;
        videosCards.removeAllViews();
        int count=0;
        for(Catalog item:new ArrayList<>(catalog.values())){
            if(count++>=120)break;
            LinearLayout card=itemCard(videosCards,item,approved(item));
            btnFull(card,"▷  Rever e guardar",true,()->showMediaWorkflow(item));
            btnFull(card,"▶  Testar reprodução",false,()->testCatalog(item));
        }
        if(catalog.isEmpty())sectionCard(videosCards,"Ainda sem candidatos",
            "Abra Scan ou Iframes, inicie a análise e reveja as fontes quando surgirem.");
    }
    @Override void renderLibrary(){
        if(libraryCards==null)return;
        libraryCards.removeAllViews();
        for(Catalog item:new ArrayList<>(library)){
            LinearLayout card=itemCard(libraryCards,item,true);
            btnFull(card,"▷  Rever e enviar ao VistaStream",true,()->showMediaWorkflow(item));
            btnFull(card,"▶  Testar reprodução",false,()->testCatalog(item));
            btnFull(card,"Remover da biblioteca",false,()->new AlertDialog.Builder(this)
                .setTitle("Remover este título?")
                .setMessage("Remove o registo do scanner, não o vídeo da Galeria ou do VistaStream.")
                .setPositiveButton("Remover",(d,w)->{
                    library.remove(item);saveLibrary();saveScan();renderLibrary();renderVideos();refresh();
                }).setNegativeButton("Cancelar",null).show());
        }
        if(library.isEmpty())sectionCard(libraryCards,"A biblioteca está vazia",
            "Confirme uma fonte no VistaStream para adicionar o título sem importar links temporários.");
    }
    private Source confirmedSource(Catalog item){
        for(Source s:item.sources){
            if(HarvesterVistaBridge.isShareable(s.url,s.format) &&
               "PLAYED".equals(getSharedPreferences(PLAY_PREFS,0)
                    .getString("source_"+s.url.hashCode(),"")))return s;
        }
        return null;
    }
    private Source downloadableSource(Catalog item){
        Source preferred=confirmedSource(item);
        if(preferred!=null&&isDownloadable(preferred))return preferred;
        for(Source s:item.sources)if(isDownloadable(s))return s;
        return null;
    }
    private boolean isDownloadable(Source s){
        if(s==null||!HarvesterVistaBridge.isShareable(s.url,s.format))return false;
        String fmt=s.format==null?"":s.format.toUpperCase(Locale.ROOT);
        String path=Uri.parse(s.url).getPath();
        if(path==null)return false;
        String lower=path.toLowerCase(Locale.ROOT);
        return (fmt.equals("MP4")||fmt.equals("WEBM")||fmt.equals("VIDEO"))&&
            (lower.endsWith(".mp4")||lower.endsWith(".m4v")||lower.endsWith(".webm"));
    }
    private void showMediaWorkflow(Catalog item){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);
        body.setPadding(dp(18),dp(16),dp(18),dp(23));
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(Styles.BG);
        scroll.addView(body);
        TextView heading=t("Guardar vídeo",24,true,Styles.TEXT);
        body.addView(heading);
        place(body,t(item.title,16,true,Styles.TEXT),-2,8);
        Source confirmed=confirmedSource(item);
        Source downloadable=downloadableSource(item);
        BrTitleParser.Parsed meta=BrTitleParser.parse(item.title);
        BrazilianMediaHints.Result hints=BrazilianMediaHints.infer(
            item.title,item.title,
            item.sources.isEmpty()?"":item.sources.get(0).page,
            item.sources.isEmpty()?"":item.sources.get(0).format);
        LinearLayout classification=sectionCard(body,"Metadados identificados",
            "Sugestões baseadas no nome, origem e formato. O VistaStream valida e permite corrigir o catálogo.");
        place(classification,t("Título: "+meta.title,14,true,Styles.TEXT),-2,3);
        String details="Qualidade: "+(hints.quality.isEmpty()?"por confirmar":hints.quality)+
            "\nIdioma/áudio: "+(hints.audioMode.isEmpty()?"por confirmar":hints.audioMode)+
            "\nEpisódio: "+(meta.season>0?meta.season+" / "+meta.episode:"não identificado");
        place(classification,t(details,12,false,Styles.MUTED),-2,6);

        LinearLayout steps=sectionCard(body,"Fluxo de captura",
            "Uma referência iframe não é um ficheiro. A aprovação na biblioteca acontece apenas após confirmação.");
        place(steps,t("01   Testar a reprodução no dispositivo",13,true,Styles.TEXT),-2,2);
        btnFull(steps,"▶  Testar fonte",false,()->testCatalog(item));
        place(steps,t("02   Reproduzir no VistaStream e Chromecast",13,true,Styles.TEXT),-2,17);
        btnFull(steps,"▶  Reproduzir no VistaStream / TV",false,()->playSourceInVista(item));
        place(steps,t("03   Rever título e metadados no VistaStream",13,true,Styles.TEXT),-2,17);
        btnFull(steps,"✓  Rever e importar no VistaStream",true,()->{
            Source chosen=confirmedSource(item);
            if(chosen==null){
                new AlertDialog.Builder(this).setTitle("Confirme uma fonte primeiro")
                    .setMessage("O scanner encontrou referências, mas não há uma fonte direta com reprodução confirmada. Teste a fonte no dispositivo; players iframe não são importados como ficheiros independentes.")
                    .setPositiveButton("Testar", (d,w)->testCatalog(item))
                    .setNegativeButton("Cancelar",null).show();
                return;
            }
            startVistaImport(item,chosen);
        });
        place(steps,t("04   Guardar no dispositivo (opcional)",13,true,Styles.TEXT),-2,17);
        if(downloadable!=null){
            btnFull(steps,"↓  Guardar vídeo na Galeria",false,()->confirmGallery(item,downloadable));
            place(steps,t("Disponível apenas para ficheiros MP4/M4V/WebM HTTP diretos. O ficheiro será guardado em Filmes/VistaBrowser.",11,false,Styles.MUTED),-2,5);
        }else{
            place(steps,t("Sem ficheiro direto compatível. Fontes HLS/DASH e referências de player podem ser reproduzidas, mas não são guardadas como se fossem MP4.",12,false,Styles.MUTED),-2,8);
        }
        final AlertDialog dlg=new AlertDialog.Builder(this).setView(scroll)
            .setNegativeButton("Fechar",null).create();
        dlg.setOnShowListener(d->{
            if(dlg.getWindow()!=null){
                dlg.getWindow().setBackgroundDrawable(frame(Styles.BG,26,Styles.BORDER));
                dlg.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.96f),-2);
            }
        });
        dlg.show();
    }
    private void playSourceInVista(Catalog item){
        Source source=null;
        for(Source candidate:item.sources){
            if(candidate.url==null)continue;
            Uri uri=Uri.parse(candidate.url);
            String scheme=uri.getScheme()==null?"":uri.getScheme();
            String fmt=candidate.format==null?"":candidate.format.toUpperCase(java.util.Locale.ROOT);
            if(!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))
                ||uri.getHost()==null||fmt.equals("REFERENCIA_IFRAME")||fmt.equals("IFRAME"))
                continue;
            if(source==null)source=candidate;
            if(confirmedSource(item)==candidate){source=candidate;break;}
        }
        if(source==null){
            new AlertDialog.Builder(this).setTitle("Apenas referências de player")
                .setMessage("O scanner ainda não detetou uma fonte de vídeo direta. Abra o player original ou teste uma fonte no inspetor.")
                .setPositiveButton("Testar",(d,w)->testCatalog(item))
                .setNegativeButton("Cancelar",null).show();
            return;
        }
        // Playback is transient, independent of approved library ingestion.
        // Signed links may expire; we do not store their tokens persistently.
        BrTitleParser.Parsed title=BrTitleParser.parse(item.title);
        for(String pkg:new String[]{"com.vistastream.tv.debug","com.vistastream.tv"}){
            Intent intent=new Intent("com.vistastream.tv.action.PLAY");
            intent.setClassName(pkg,
                "com.ivor.openstream.harvester.HarvesterPlaybackActivity");
            intent.putExtra("com.vistastream.tv.extra.URL",source.url);
            intent.putExtra("com.vistastream.tv.extra.TITLE",title.title);
            intent.putExtra("com.vistastream.tv.extra.MIME_TYPE",mediaMime(source));
            if(source.page!=null && (source.page.startsWith("https://")||
                    source.page.startsWith("http://"))){
                String clean=Uri.parse(source.page).buildUpon()
                    .clearQuery().fragment(null).build().toString();
                intent.putExtra("com.vistastream.tv.extra.REFERER",clean);
                intent.putExtra("com.vistastream.tv.extra.PAGE_URL",clean);
            }
            try{startActivity(intent);return;}
            catch(android.content.ActivityNotFoundException|SecurityException ignored){}
        }
        toast("Instale o VistaStream atualizado para reproduzir e transmitir à TV");
    }
    private void startVistaImport(Catalog item,Source source){
        if(pendingImport!=null){toast("Conclua primeiro a importação anterior");return;}
        String page=source.page==null?"":source.page;
        BrTitleParser.Parsed title=BrTitleParser.parse(item.title);
        BrazilianMediaHints.Result hints=BrazilianMediaHints.infer(item.title,item.title,page,source.format);
        for(String pkg:new String[]{"com.vistastream.tv.debug","com.vistastream.tv"}){
            Intent intent=new Intent("com.vistastream.tv.action.IMPORT");
            intent.setClassName(pkg,"com.ivor.openstream.harvester.HarvesterImportActivity");
            intent.putExtra("com.vistastream.tv.extra.URL",source.url);
            intent.putExtra("com.vistastream.tv.extra.TITLE",title.title);
            intent.putExtra("com.vistastream.tv.extra.KIND","video");
            intent.putExtra("com.vistastream.tv.extra.MIME_TYPE",mediaMime(source));
            if(hints.season>0&&hints.episode>0){
                intent.putExtra("com.vistastream.tv.extra.SEASON",hints.season);
                intent.putExtra("com.vistastream.tv.extra.EPISODE",hints.episode);
            }
            if(!hints.audioMode.isEmpty())intent.putExtra("com.vistastream.tv.extra.AUDIO_MODE",hints.audioMode);
            if(!hints.locale.isEmpty())intent.putExtra("com.vistastream.tv.extra.AUDIO_LANGUAGE",hints.locale);
            if(!hints.quality.isEmpty())intent.putExtra("com.vistastream.tv.extra.QUALITY",hints.quality);
            if(!hints.providerLabel.isEmpty())intent.putExtra("com.vistastream.tv.extra.PROVIDER_LABEL",hints.providerLabel);
            if(page.startsWith("https://")||page.startsWith("http://")){
                String clean=Uri.parse(page).buildUpon().clearQuery().fragment(null).build().toString();
                intent.putExtra("com.vistastream.tv.extra.PAGE_URL",clean);
                intent.putExtra("com.vistastream.tv.extra.REFERER",clean);
            }
            try{
                pendingImport=item;
                pendingImportUrl=source.url;
                final String expected=source.url;
                android.os.ResultReceiver confirmation=new android.os.ResultReceiver(
                    new android.os.Handler(android.os.Looper.getMainLooper())){
                    @Override protected void onReceiveResult(int result,android.os.Bundle data){
                        if(result!=RESULT_OK||data==null||pendingImport==null)return;
                        if(!expected.equals(data.getString("com.vistastream.tv.extra.URL","")))return;
                        Intent receipt=new Intent()
                            .putExtra("com.vistastream.tv.extra.URL",expected)
                            .putExtra("com.vistastream.tv.extra.TITLE",
                                data.getString("com.vistastream.tv.extra.TITLE",""))
                            .putExtra("com.vistastream.tv.extra.SAVED_WEB_ONLY",
                                data.getBoolean("com.vistastream.tv.extra.SAVED_WEB_ONLY",false));
                        handlePremiumImportResult(RESULT_OK,receipt);
                    }
                };
                intent.putExtra("com.vistastream.tv.extra.CONFIRMATION_RECEIVER",confirmation);
                startActivityForResult(intent,IMPORT_REQUEST);
                return;
            }catch(android.content.ActivityNotFoundException|SecurityException ignored){
                pendingImport=null;pendingImportUrl=null;
            }
        }
        new AlertDialog.Builder(this).setTitle("VistaStream não disponível")
            .setMessage("Instale uma versão do VistaStream que suporte a importação e revisão de metadados.")
            .setPositiveButton("OK",null).show();
    }
    private String mediaMime(Source s){
        String f=s.format==null?"":s.format.toUpperCase(Locale.ROOT);
        if(f.equals("MP4"))return "video/mp4";
        if(f.equals("WEBM"))return "video/webm";
        if(f.equals("HLS"))return "application/vnd.apple.mpegurl";
        if(f.equals("DASH"))return "application/dash+xml";
        return "video/*";
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==IMPORT_REQUEST)handlePremiumImportResult(result,data);
    }
    private void handlePremiumImportResult(int result,Intent data){
        Catalog item=pendingImport;
        String expected=pendingImportUrl;
        if(item==null)return; // Matching receipt processed; ignore duplicate Android result.
        pendingImport=null;pendingImportUrl=null;
        if(expected==null||result!=RESULT_OK||data==null){
            toast("Importação cancelada: nada foi guardado no scanner");return;
        }
        String response=data.getStringExtra("com.vistastream.tv.extra.URL");
        if(!expected.equals(response)){
            toast("O VistaStream não confirmou esta fonte; nada foi adicionado ao scanner");return;
        }
        if(data.getBooleanExtra("com.vistastream.tv.extra.SAVED_WEB_ONLY",false)){
            toast("Página guardada no VistaStream como referência Web. A fonte de vídeo não foi aprovada.");
            return;
        }
        String confirmedTitle=data.getStringExtra("com.vistastream.tv.extra.TITLE");
        if(confirmedTitle!=null&&!confirmedTitle.trim().isEmpty())
            item.title=confirmedTitle.trim().substring(0,Math.min(180,confirmedTitle.trim().length()));
        approve(item);
        saveScan();
        renderVideos();renderLibrary();refresh();
        toast("Fonte e metadados confirmados no VistaStream e no scanner");
    }
    private void confirmGallery(Catalog item,Source source){
        new AlertDialog.Builder(this).setTitle("Guardar vídeo na Galeria?")
            .setMessage("Guarde apenas vídeos que tem autorização para descarregar. Esta opção guarda um ficheiro MP4/WebM direto; não extrai vídeos protegidos nem ficheiros de playlists HLS/DASH.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Guardar",(d,w)->saveToGallery(item,source)).show();
    }
    private void saveToGallery(Catalog item,Source source){
        if(!isDownloadable(source)){toast("Fonte não compatível com transferência direta");return;}
        if(Build.VERSION.SDK_INT<29){
            // DownloadManager owns the public storage destination on Android 8/9.
            try{
                String name=galleryName(item.title,source);
                android.app.DownloadManager.Request request=new android.app.DownloadManager.Request(Uri.parse(source.url));
                request.setTitle(name);
                request.setMimeType(mediaMime(source));
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,"VistaBrowser/"+name);
                ((android.app.DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                toast("Transferência iniciada; acompanhe-a nas notificações");
            }catch(Exception ex){toast("Falha na transferência: "+ex.getMessage());}
            return;
        }
        Toast.makeText(this,"A guardar na Galeria…",Toast.LENGTH_LONG).show();
        galleryIo.execute(()->{
            Uri record=null;
            HttpURLConnection conn=null;
            try{
                URL url=new URL(source.url);
                conn=(HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(13000);conn.setReadTimeout(24000);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("Accept","video/mp4,video/webm;q=0.9,*/*;q=0.1");
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new IOException("HTTP "+code+" (a origem não disponibilizou o ficheiro direto)");
                String mime=conn.getContentType();
                if(mime==null||(!mime.toLowerCase(Locale.ROOT).startsWith("video/") &&
                    !mime.toLowerCase(Locale.ROOT).startsWith("application/octet-stream")))
                    throw new IOException("O servidor não devolveu um ficheiro de vídeo");
                long size=conn.getContentLengthLong();
                if(size>0 && size>2_000_000_000L)throw new IOException("Ficheiro excede 2 GB");
                ContentValues data=new ContentValues();
                data.put(MediaStore.Video.Media.DISPLAY_NAME,galleryName(item.title,source));
                data.put(MediaStore.Video.Media.MIME_TYPE,mediaMime(source));
                data.put(MediaStore.Video.Media.RELATIVE_PATH,Environment.DIRECTORY_MOVIES+"/VistaBrowser");
                data.put(MediaStore.Video.Media.IS_PENDING,1);
                record=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,data);
                if(record==null)throw new IOException("Não foi possível criar o ficheiro na Galeria");
                long total=0;
                try(InputStream in=conn.getInputStream();
                    OutputStream out=getContentResolver().openOutputStream(record)){
                    if(out==null)throw new IOException("Falha na abertura do armazenamento");
                    byte[] buf=new byte[32768];int n;
                    while((n=in.read(buf))!=-1){
                        if(Thread.currentThread().isInterrupted())throw new IOException("Transferência interrompida");
                        total+=n;
                        if(total>2_000_000_000L)throw new IOException("Ficheiro excede 2 GB");
                        out.write(buf,0,n);
                    }
                }
                if(total<1024)throw new IOException("Ficheiro devolvido demasiado pequeno");
                data.clear();data.put(MediaStore.Video.Media.IS_PENDING,0);
                getContentResolver().update(record,data,null,null);
                runOnUiThread(()->toast("Vídeo guardado em Galeria / Filmes / VistaBrowser"));
            }catch(Exception e){
                if(record!=null)try{getContentResolver().delete(record,null,null);}catch(Exception ignored){}
                final String msg=e.getMessage()==null?"Falha desconhecida":e.getMessage();
                runOnUiThread(()->toast("Não foi possível guardar o vídeo: "+msg));
            }finally{if(conn!=null)conn.disconnect();}
        });
    }
    private String galleryName(String title,Source source){
        String clean=(title==null?"Video":title).replaceAll("[^\\p{L}\\p{N} ._-]"," ")
            .replaceAll("\\s+"," ").trim();
        if(clean.isEmpty())clean="Video";
        if(clean.length()>78)clean=clean.substring(0,78);
        String path=Uri.parse(source.url).getPath();
        return clean+"-"+System.currentTimeMillis()+
             ((path!=null&&path.toLowerCase(Locale.ROOT).endsWith(".webm"))?".webm":".mp4");
    }
    @Override protected void onDestroy(){
        galleryIo.shutdownNow();
        super.onDestroy();
    }
}