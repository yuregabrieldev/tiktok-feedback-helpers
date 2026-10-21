package com.harvester.browser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.mediarouter.app.MediaRouteButton;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Dedicated playback screen for URLs saved by Harvester Browser.
 * No DRM bypass, proxy, authentication forwarding or access circumvention.
 *
 * Local playback and Cast are independent: Cast failure cannot destroy ExoPlayer.
 */
public final class NativePlayerActivity extends FragmentActivity {
    public static final String TEST_VIDEO =
        "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private static final int DARK=0xFF101315, SURFACE=0xFF25292E;
    private ExoPlayer player;
    private CastContext castContext;
    private SessionManagerListener<CastSession> castListener;
    private RemoteMediaClient observedRemote;
    private final RemoteMediaClient.Callback remoteCallback=new RemoteMediaClient.Callback(){
        @Override public void onStatusUpdated(){runOnUiThread(()->refreshRemoteStatus());}
        @Override public void onMetadataUpdated(){runOnUiThread(()->refreshRemoteStatus());}
    };
    private TextView localState,castState,headline;
    private MediaRouteButton castButton;
    private boolean castInitialized=false,castPending=false,castListenerRegistered=false;
    private String mediaUrl,mediaTitle,mediaFormat;
    private long pendingPosition=0L;
    private boolean attemptSent=false;

    private int dp(int px){return Math.round(px*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){
        TextView t=new TextView(this);
        t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setPadding(dp(14),dp(10),dp(14),dp(10));
        return t;
    }
    private Button button(String label){
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);return b;
    }
    private void local(String value){if(localState!=null)localState.setText(value);}
    private void cast(String value){if(castState!=null)castState.setText(value);}
    private void castOnUi(String value){runOnUiThread(()->cast(value));}
    private String errorName(Throwable err){
        String msg=err.getMessage();
        return err.getClass().getSimpleName()+(msg==null?"":": "+msg.substring(0,Math.min(110,msg.length())));
    }
    private void saveError(String where,Throwable error){
        try{
            StringBuilder message=new StringBuilder(where+": "+errorName(error)+"\n");
            for(StackTraceElement element:error.getStackTrace()){
                if(message.length()>5000)break;
                message.append(element.toString()).append("\n");
            }
            getSharedPreferences("harvester-cast-errors",MODE_PRIVATE).edit()
                .putString("last",message.toString()).apply();
        }catch(Exception ignored){}
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        Intent intent=getIntent();
        mediaUrl=intent.getStringExtra("url");
        mediaTitle=intent.getStringExtra("title");
        mediaFormat=intent.getStringExtra("format");
        pendingPosition=Math.max(0L,intent.getLongExtra("position_ms",0L));
        if(mediaTitle==null||mediaTitle.trim().isEmpty())mediaTitle="Reprodutor";
        if(mediaFormat==null||mediaFormat.isEmpty())mediaFormat="mp4";
        mediaFormat=mediaFormat.toLowerCase(java.util.Locale.ROOT);

        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(DARK);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.addView(layout);
        setContentView(scroll);

        TextView header=text("‹     Harvester Player",21,Color.WHITE);
        header.setOnClickListener(v->finish());layout.addView(header);
        headline=text(mediaTitle,17,0xFFF3F5F8);
        layout.addView(headline);
        final PlayerView surface=new PlayerView(this);
        surface.setUseController(true);
        surface.setControllerAutoShow(true);
        surface.setBackgroundColor(Color.BLACK);
        layout.addView(surface,new LinearLayout.LayoutParams(-1,dp(295)));
        localState=text("A preparar o player…",14,0xFFAADFF7);
        layout.addView(localState);

        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10),dp(6),dp(12),dp(6));
        row.setBackgroundColor(SURFACE);
        Button connect=button("Transmitir à TV");
        row.addView(connect,new LinearLayout.LayoutParams(0,dp(59),1));
        castButton=new MediaRouteButton(this);
        castButton.setContentDescription("Escolher dispositivo Google Cast");
        row.addView(castButton,new LinearLayout.LayoutParams(dp(58),dp(58)));
        layout.addView(row);
        castState=text("Google Cast não ligado",14,0xFFC9D4DE);layout.addView(castState);
        LinearLayout commands=new LinearLayout(this);
        commands.setOrientation(LinearLayout.HORIZONTAL);
        Button pause=button("Pausar");commands.addView(pause,new LinearLayout.LayoutParams(0,dp(57),1));
        Button play=button("Reproduzir");commands.addView(play,new LinearLayout.LayoutParams(0,dp(57),1));
        Button stop=button("Parar TV");commands.addView(stop,new LinearLayout.LayoutParams(0,dp(57),1));
        layout.addView(commands);
        Button demo=button("Testar o player com vídeo público");
        layout.addView(demo,new LinearLayout.LayoutParams(-1,dp(62)));
        TextView about=text("A biblioteca guarda a ligação ao ficheiro, não o ficheiro. "
            +"Para enviar à TV, ambos os dispositivos devem conseguir aceder ao vídeo. "
            +"Google Cast não exige introduzir um PIN num formulário próprio do Harvester. "
            +"Este leitor não contorna DRM, cookies, autenticação nem ligações expiradas.",12,0xFFA0ADB6);
        layout.addView(about);
        connect.setOnClickListener(v->chooseDevice());
        pause.setOnClickListener(v->control(false));
        play.setOnClickListener(v->control(true));
        stop.setOnClickListener(v->stopCast());
        demo.setOnClickListener(v->startDemo());

        if(mediaUrl==null||!mediaUrl.startsWith("https://")){
            local("Fonte inválida. Usa o botão de teste para verificar o player.");
        }else{
            startLocal(surface);
        }
        // Cast is deliberately initialized AFTER rendering the local player.
        // If Google Play services are broken, local playback remains usable.
        layout.post(this::initializeCast);
    }

    private void startLocal(PlayerView target){
        try{
            player=new ExoPlayer.Builder(this).build();
            target.setPlayer(player);
            player.addListener(new Player.Listener(){
                @Override public void onPlaybackStateChanged(int state){
                    if(state==Player.STATE_IDLE)local("Player preparado");
                    else if(state==Player.STATE_BUFFERING)local("A carregar o vídeo…");
                    else if(state==Player.STATE_READY)local("Vídeo pronto para reproduzir");
                    else if(state==Player.STATE_ENDED)local("Reprodução concluída");
                }
                @Override public void onPlayerError(PlaybackException e){
                    local("Falha ao carregar o vídeo: "+e.getErrorCodeName()
                      +". A fonte pode ter expirado ou exigir a sessão do site.");
                    saveError("Media3",e);
                }
            });
            loadLocal(mediaUrl,mediaFormat,pendingPosition);
        }catch(RuntimeException | LinkageError ex){
            saveError("Native player init",ex);
            local("O player não conseguiu iniciar: "+errorName(ex));
        }
    }
    private void loadLocal(String url,String format,long positionMs){
        if(player==null)return;
        try{
            androidx.media3.common.MediaItem.Builder b=new androidx.media3.common.MediaItem.Builder().setUri(url);
            if("hls".equals(format)||url.matches("(?i).*\\.m3u8(?:\\?.*)?$"))
                b.setMimeType("application/x-mpegURL");
            if("dash".equals(format)||url.matches("(?i).*\\.mpd(?:\\?.*)?$"))
                b.setMimeType("application/dash+xml");
            player.setMediaItem(b.build());
            player.prepare();
            if(positionMs>0L)player.seekTo(positionMs);
            player.play();
        }catch(RuntimeException ex){
            saveError("Load local video",ex);local("Falha ao iniciar vídeo: "+errorName(ex));
        }
    }
    private void startDemo(){
        mediaUrl=TEST_VIDEO;mediaTitle="Big Buck Bunny — vídeo de demonstração";
        mediaFormat="mp4";pendingPosition=0L;
        headline.setText(mediaTitle);
        if(player==null){local("Leitor local não disponível");return;}
        loadLocal(mediaUrl,mediaFormat,0L);
        local("A testar uma fonte MP4 pública…");
    }
    private void initializeCast(){
        if(castInitialized||isFinishing()||isDestroyed())return;
        castInitialized=true;
        try{
            int availability=GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
            if(availability!=ConnectionResult.SUCCESS){
                cast("Google Play Services indisponível ("+availability+"). "
                    +"O player local continua disponível.");
                castButton.setVisibility(View.GONE);
                return;
            }
            CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),castButton);
            castContext=CastContext.getSharedInstance(this);
            castButton.setOnTouchListener((v,event)->{
                if(event.getAction()==MotionEvent.ACTION_DOWN){castPending=true;attemptSent=false;}
                return false;
            });
            castListener=new SessionManagerListener<CastSession>(){
                @Override public void onSessionStarting(CastSession session){castOnUi("A ligar à TV…");}
                @Override public void onSessionStarted(CastSession session,String id){
                    castOnUi("Ligação à TV estabelecida");sendVideo();
                }
                @Override public void onSessionStartFailed(CastSession session,int code){
                    castOnUi("Não foi possível ligar à TV (código "+code+")");}
                @Override public void onSessionEnding(CastSession session){}
                @Override public void onSessionEnded(CastSession session,int code){
                    castOnUi("TV desligada");detachRemote();attemptSent=false;}
                @Override public void onSessionResuming(CastSession session,String id){}
                @Override public void onSessionResumed(CastSession session,boolean suspended){
                    castOnUi("Ligação Cast retomada");if(castPending)sendVideo();}
                @Override public void onSessionResumeFailed(CastSession session,int code){
                    castOnUi("A ligação à TV não pôde ser retomada");}
                @Override public void onSessionSuspended(CastSession session,int code){
                    castOnUi("Sessão Cast suspensa");}
            };
            registerCastListener();
            cast("Cast disponível. Toca em Transmitir à TV para procurar dispositivos.");
        }catch(RuntimeException | LinkageError ex){
            saveError("Cast setup",ex);
            cast("Google Cast indisponível: "+errorName(ex));
            castButton.setVisibility(View.GONE);
            castContext=null;
        }
    }
    private void chooseDevice(){
        try{
            initializeCast();
            if(castContext==null){cast("O SDK Google Cast não está disponível neste telemóvel.");return;}
            if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},92);
                cast("Autoriza dispositivos próximos e tenta novamente.");return;
            }
            CastSession session=castContext.getSessionManager().getCurrentCastSession();
            castPending=true;attemptSent=false;
            if(session!=null && session.isConnected()){
                cast("TV já ligada. A enviar o vídeo…");sendVideo();return;
            }
            cast("Procura a tua TV na lista do Google Cast. Ela tem de ter Cast integrado ou Chromecast.");
            castButton.performClick();
        }catch(RuntimeException | LinkageError ex){
            saveError("Cast chooser",ex);
            cast("Erro ao abrir o seletor da TV: "+errorName(ex));
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==92){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)
                cast("Permissão concedida. Toca novamente em Transmitir à TV.");
            else cast("Não foi concedida permissão para descobrir dispositivos próximos.");
        }
    }
    private void attachRemote(RemoteMediaClient remote){
        if(observedRemote==remote)return;
        detachRemote();
        observedRemote=remote;
        if(remote!=null)remote.registerCallback(remoteCallback);
    }
    private void detachRemote(){
        if(observedRemote!=null){
            try{observedRemote.unregisterCallback(remoteCallback);}catch(RuntimeException ignored){}
            observedRemote=null;
        }
    }
    private void sendVideo(){
        if(!castPending||attemptSent||castContext==null)return;
        try{
            CastSession session=castContext.getSessionManager().getCurrentCastSession();
            if(session==null||!session.isConnected()){cast("Aguardando ligação à TV…");return;}
            RemoteMediaClient remote=session.getRemoteMediaClient();
            if(remote==null){cast("TV ligada, mas sem receptor Cast multimédia padrão.");return;}
            if(mediaUrl==null||!mediaUrl.startsWith("https://")){
                cast("Fonte HTTPS inválida para transmissão.");return;
            }
            attachRemote(remote);
            MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            metadata.putString(MediaMetadata.KEY_TITLE,mediaTitle);
            String mime="video/mp4";
            if("webm".equals(mediaFormat))mime="video/webm";
            if("hls".equals(mediaFormat))mime="application/x-mpegURL";
            if("dash".equals(mediaFormat))mime="application/dash+xml";
            long at=player!=null?Math.max(0L,player.getCurrentPosition()):pendingPosition;
            MediaInfo info=new MediaInfo.Builder(mediaUrl)
                .setContentType(mime)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata).build();
            attemptSent=true;
            cast("TV ligada. A enviar o pedido de vídeo…");
            remote.load(new MediaLoadRequestData.Builder().setMediaInfo(info)
                .setCurrentTime(at).setAutoplay(true).build())
                .setResultCallback(result->runOnUiThread(()->{
                    if(result.getStatus().isSuccess()){
                        cast("A TV aceitou o pedido. A verificar se inicia a reprodução…");
                    }else{
                        attemptSent=false;
                        cast("TV ligada, mas rejeitou a fonte (código "
                            +result.getStatus().getStatusCode()+").");
                    }
                }));
        }catch(RuntimeException | LinkageError ex){
            attemptSent=false;saveError("Cast media load",ex);
            castOnUi("Falha ao enviar vídeo: "+errorName(ex));
        }
    }
    private void refreshRemoteStatus(){
        if(observedRemote==null)return;
        try{
            MediaStatus status=observedRemote.getMediaStatus();
            if(status==null)return;
            if(status.getPlayerState()==MediaStatus.PLAYER_STATE_PLAYING){
                cast("A reproduzir na TV");
                // Pause local audio after verified remote playback, not merely after LOAD accepted.
                if(player!=null&&player.isPlaying())player.pause();
            }else if(status.getPlayerState()==MediaStatus.PLAYER_STATE_BUFFERING){
                cast("A TV está a carregar o vídeo…");
            }else if(status.getPlayerState()==MediaStatus.PLAYER_STATE_PAUSED){
                cast("Vídeo em pausa na TV");
            }else if(status.getPlayerState()==MediaStatus.PLAYER_STATE_IDLE &&
                      status.getIdleReason()==MediaStatus.IDLE_REASON_ERROR){
                cast("A televisão não conseguiu abrir esta fonte. "
                    +"O URL pode exigir autenticação/IP/cookies ou estar expirado.");
            }
        }catch(RuntimeException ex){saveError("Cast remote status",ex);}
    }
    private void control(boolean play){
        try{
            if(castContext!=null){
                CastSession session=castContext.getSessionManager().getCurrentCastSession();
                RemoteMediaClient r=session==null?null:session.getRemoteMediaClient();
                if(session!=null&&session.isConnected()&&r!=null){
                    if(play)r.play();else r.pause();
                    return;
                }
            }
            if(player!=null){if(play)player.play();else player.pause();}
        }catch(RuntimeException ex){saveError("Player control",ex);cast("Erro no controlo: "+errorName(ex));}
    }
    private void stopCast(){
        try{
            if(castContext==null){cast("Nenhuma TV ligada.");return;}
            CastSession session=castContext.getSessionManager().getCurrentCastSession();
            if(session==null||!session.isConnected()){cast("Nenhuma TV ligada.");return;}
            RemoteMediaClient remote=session.getRemoteMediaClient();
            if(remote!=null)remote.stop();
            cast("Reprodução na TV parada");
        }catch(RuntimeException ex){saveError("Stop Cast",ex);cast("Erro: "+errorName(ex));}
    }
    private void registerCastListener(){
        if(castContext==null||castListener==null||castListenerRegistered)return;
        try{
            castContext.getSessionManager().addSessionManagerListener(castListener,CastSession.class);
            castListenerRegistered=true;
            if(castPending)sendVideo();
        }catch(RuntimeException ex){saveError("Cast session listener",ex);}
    }
    @Override protected void onStart(){
        super.onStart();
        registerCastListener();
    }
    @Override protected void onStop(){
        if(castContext!=null&&castListener!=null&&castListenerRegistered)
            try{castContext.getSessionManager().removeSessionManagerListener(castListener,CastSession.class);}
            catch(RuntimeException ignored){}
        castListenerRegistered=false;
        super.onStop();
    }
    @Override protected void onDestroy(){
        detachRemote();
        if(player!=null){try{player.release();}catch(RuntimeException ignored){}player=null;}
        super.onDestroy();
    }
}
