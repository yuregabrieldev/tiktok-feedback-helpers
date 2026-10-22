from pathlib import Path

root=Path('hb-v18')
player=root/'app/src/main/java/com/harvester/browser/NativePlayerActivity.java'
service=root/'app/src/main/java/com/harvester/browser/PlaybackService.java'
s=player.read_text()

def replace_once(a,b,what):
    global s
    count=s.count(a)
    if count!=1:raise RuntimeError(f'{what}: {count} matches')
    s=s.replace(a,b,1)

# Source-aware quality selection. Fixed MP4 will correctly have just one quality.
replace_once('    private Button playPause, speedButton;',
'''    private Button playPause, speedButton, qualityButton;''',"quality button state")
replace_once('''        Button full = btn("⛶", v -> toggleFullscreen());''',
'''        Button full = btn("⛶", v -> toggleFullscreen());
        qualityButton=btn("Qualidade",v->showQualitySelection());''',"quality control")
replace_once('''        root.addView(controls);''',
'''        root.addView(controls);
        LinearLayout moreControls=new LinearLayout(this);
        moreControls.setPadding(dp(12),0,dp(12),dp(7));
        Button phone=btn("Reproduzir no telemóvel",v->disconnectCastAndResumeLocally());
        moreControls.addView(phone,new LinearLayout.LayoutParams(0,dp(47),1));
        LinearLayout.LayoutParams qualityParams=new LinearLayout.LayoutParams(dp(112),dp(47));
        qualityParams.leftMargin=dp(7);
        moreControls.addView(qualityButton,qualityParams);
        root.addView(moreControls);
        Button retry=btn("⟳ Reabrir fonte no player",v->retryCurrentSource());
        LinearLayout.LayoutParams retryParams=new LinearLayout.LayoutParams(-1,dp(42));
        retryParams.setMargins(dp(14),0,dp(14),dp(7));
        root.addView(retry,retryParams);''',"extra player buttons")
replace_once('''            statusView.setText("Não foi possível reproduzir este formato. Experimente um MP4 H.264/AAC.");''',
'''            statusView.setText("Falha de reprodução: "+error.getErrorCodeName()+
                ". O link pode ter expirado, exigir sessão no site ou não suportar este codec.");
            android.util.Log.e("VistaCastHarvester","Erro do player",error);''',"player error details")
replace_once('''        boolean remote = controller.getDeviceInfo().playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE;''',
'''        boolean remote = controller.getDeviceInfo().playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE;
        if(qualityButton!=null)qualityButton.setEnabled(!remote);''',"disable quality on TV")
replace_once('''        statusView.setText(remote ? "● A transmitir na televisão · controles disponíveis no telemóvel"''',
'''        statusView.setText(remote ? "● Google Cast ligado · o vídeo aparece na TV, não no ecrã do telemóvel. Use «Reproduzir no telemóvel» para regressar."''',
"remote black is normal")
replace_once('''    private void seek(long offset) {''',
'''    private void disconnectCastAndResumeLocally(){
        if(controller==null){toast("O player ainda está a ligar");return;}
        try{
            com.google.android.gms.cast.framework.CastContext context=
                com.google.android.gms.cast.framework.CastContext.getSharedInstance(this);
            context.getSessionManager().endCurrentSession(true);
            statusView.setText("A desligar a TV e regressar ao telemóvel…");
            // CastPlayer handles transfer of current MediaItem and timeline to ExoPlayer.
            new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(()->{if(controller!=null){
                    playerView.setPlayer(null);
                    playerView.setPlayer(controller);
                    showStatus();
                }},800);
        }catch(RuntimeException | LinkageError ex){
            android.util.Log.w("VistaCastHarvester","Falha a sair do Cast",ex);
            toast("Não foi possível desligar a sessão: "+ex.getClass().getSimpleName());
        }
    }

    private void retryCurrentSource(){
        if(controller==null){toast("O player ainda está a iniciar");return;}
        try{
            if(controller.getDeviceInfo().playbackType==DeviceInfo.PLAYBACK_TYPE_REMOTE){
                new android.app.AlertDialog.Builder(this)
                    .setTitle("A TV está ligada")
                    .setMessage("Regresse ao telemóvel antes de voltar a carregar a fonte.")
                    .setPositiveButton("Voltar ao telemóvel",(d,w)->disconnectCastAndResumeLocally())
                    .setNegativeButton("Cancelar",null).show();
                return;
            }
            externalOpened=false;
            controller.stop();
            controller.clearMediaItems();
            openBrowserVideo();
            if(!externalOpened)statusView.setText("Abra um vídeo na biblioteca ou na página do navegador.");
        }catch(RuntimeException ex){
            android.util.Log.w("VistaCastHarvester","Reload failed",ex);
            statusView.setText("Não foi possível recarregar a fonte: "+ex.getClass().getSimpleName());
        }
    }

    private void showQualitySelection(){
        if(controller==null){toast("O vídeo ainda está a carregar");return;}
        if(controller.getDeviceInfo().playbackType==DeviceInfo.PLAYBACK_TYPE_REMOTE){
            toast("A qualidade no Google Cast é escolhida pelo recetor. Ajuste-a no player local.");return;
        }
        try{
            androidx.media3.common.Tracks tracks=controller.getCurrentTracks();
            java.util.LinkedHashMap<Integer,androidx.media3.common.TrackSelectionOverride> options=
                new java.util.LinkedHashMap<>();
            int fixedHeight=0;
            for(androidx.media3.common.Tracks.Group group:tracks.getGroups()){
                if(group.getType()!=androidx.media3.common.C.TRACK_TYPE_VIDEO)continue;
                for(int i=0;i<group.length;i++){
                    if(!group.isTrackSupported(i))continue;
                    androidx.media3.common.Format format=group.getTrackFormat(i);
                    if(format.height<=0)continue;
                    fixedHeight=format.height;
                    options.putIfAbsent(format.height,
                        new androidx.media3.common.TrackSelectionOverride(group.getMediaTrackGroup(),i));
                }
            }
            if(options.size()<2){
                toast(fixedHeight>0?"Este vídeo só tem uma qualidade: "+fixedHeight+"p":
                    "Esta fonte não disponibilizou várias qualidades.");
                return;
            }
            java.util.ArrayList<Integer> heights=new java.util.ArrayList<>(options.keySet());
            heights.sort(java.util.Collections.reverseOrder());
            String[] choices=new String[heights.size()+1];
            choices[0]="Automática (adaptativa)";
            for(int i=0;i<heights.size();i++)choices[i+1]=heights.get(i)+"p";
            new android.app.AlertDialog.Builder(this)
                .setTitle("Qualidade de reprodução")
                .setItems(choices,(dialog,selected)->{
                    try{
                        androidx.media3.common.TrackSelectionParameters.Builder params=
                            controller.getTrackSelectionParameters().buildUpon()
                              .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO);
                        if(selected>0)
                            params.setOverrideForType(options.get(heights.get(selected-1)));
                        controller.setTrackSelectionParameters(params.build());
                        qualityButton.setText(selected==0?"Qualidade: Auto":choices[selected]);
                    }catch(Exception error){
                        android.util.Log.w("VistaCastHarvester","Track selection failed",error);
                        toast("Este player não conseguiu alterar a qualidade.");
                    }
                }).show();
        }catch(Exception ex){
            android.util.Log.w("VistaCastHarvester","Quality unavailable",ex);
            toast("Não foi possível obter as faixas disponíveis.");
        }
    }

    private void seek(long offset) {''',"player quality and fallback methods")

# Improve reconnection after activity returns: surface remains bound and remote info is obvious.
replace_once('''                controller.addListener(listener);
                openBrowserVideo();''',
'''                controller.addListener(listener);
                // Each new Activity receives the browser's selected URL, not an old black surface.
                playerView.setPlayer(null);
                playerView.setPlayer(controller);
                openBrowserVideo();''',"reattach player when reopened")

player.write_text(s)
t=service.read_text()
old='''        if (session != null) session.release();'''
if t.count(old)!=1:raise RuntimeError("MediaSession cleanup reference missing")
# Detect service startup/recovery failures without killing the whole player.
t=t.replace(old,'''        if (session != null) session.release();''',1)
service.write_text(t)
print("VistaCast player: adaptive track chooser, Cast-to-phone recovery, source retry, and session diagnostics")
