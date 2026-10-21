package com.harvester.browser;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.mediarouter.app.MediaRouteButton;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

/** Standalone native media player. Saved library entries store links, never video files. */
public final class NativePlayerActivity extends FragmentActivity {
    private ExoPlayer player;
    private CastContext cast;
    private MediaRouteButton routeButton;
    private SessionManagerListener<CastSession> listener;
    private TextView status;
    private String url;
    private String title;
    private String format;
    private boolean pendingCast = false;
    private boolean sent = false;
    private int d(int size){return Math.round(size*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(0xFFE9EDF1);
        t.setTextSize(size);t.setPadding(d(14),d(12),d(14),d(12));return t;
    }
    private void report(String message){if(status!=null)status.setText(message);}
    private void reportSafe(String message){runOnUiThread(()->report(message));}

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        Intent intent=getIntent();
        url=intent.getStringExtra("url");
        title=intent.getStringExtra("title");
        format=intent.getStringExtra("format");
        if(title==null||title.isEmpty())title="Vídeo";
        if(format==null)format="mp4";
        getWindow().setStatusBarColor(0xFF101213);
        getWindow().setNavigationBarColor(0xFF101213);
        LinearLayout base=new LinearLayout(this);base.setOrientation(LinearLayout.VERTICAL);
        base.setBackgroundColor(0xFF101213);
        TextView back=text("‹    Reproduzir vídeo",22);
        back.setOnClickListener(v->finish());base.addView(back);
        TextView headline=text(title,17);base.addView(headline);
        status=text("A preparar a reprodução…",14);status.setTextColor(0xFFA9C0D0);
        if(url==null||!url.startsWith("https://")){
            base.addView(status);
            report("URL ausente ou não HTTPS. Nada foi descarregado.");
            setContentView(base);return;
        }
        PlayerView view=new PlayerView(this);view.setBackgroundColor(Color.BLACK);
        view.setUseController(true);
        base.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        base.addView(status);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(d(10),d(7),d(10),d(9));
        TextView castLabel=text("Transmitir à TV  ",16);castLabel.setTextColor(0xFFA9DDF4);
        row.addView(castLabel,new LinearLayout.LayoutParams(0,d(58),1));
        routeButton=new MediaRouteButton(this);
        row.addView(routeButton,new LinearLayout.LayoutParams(d(55),d(55)));
        castLabel.setOnClickListener(v->chooseTv());
        TextView close=text("Fechar",15);close.setOnClickListener(v->finish());
        row.addView(close);
        base.addView(row);
        TextView hint=text("A biblioteca guarda o URL, não o vídeo. A reprodução local e a transmissão dependem de a fonte estar acessível. Fontes protegidas, expiradas ou ligadas à sessão podem não funcionar.",12);
        hint.setTextColor(0xFF88969E);base.addView(hint);
        setContentView(base);
        try{
            player=new ExoPlayer.Builder(this).build();
            view.setPlayer(player);
            player.addListener(new Player.Listener(){
                @Override public void onPlaybackStateChanged(int state){
                    if(state==Player.STATE_BUFFERING)report("A carregar vídeo…");
                    else if(state==Player.STATE_READY)report("Reprodução local disponível");
                    else if(state==Player.STATE_ENDED)report("Reprodução terminada");
                }
                @Override public void onPlayerError(PlaybackException error){
                    report("Não foi possível reproduzir ("+error.getErrorCodeName()
                        +"). O link pode ter expirado ou exigir acesso pelo site original.");
                }
            });
            androidx.media3.common.MediaItem.Builder media=
                new androidx.media3.common.MediaItem.Builder().setUri(url);
            if("hls".equals(format))media.setMimeType("application/x-mpegURL");
            if("dash".equals(format))media.setMimeType("application/dash+xml");
            player.setMediaItem(media.build());player.prepare();player.play();
        }catch(RuntimeException|LinkageError ex){
            report("Leitor indisponível: "+ex.getClass().getSimpleName());
        }
        initializeCast();
    }
    private void initializeCast(){
        try{
            cast=CastContext.getSharedInstance(this);
            CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),routeButton);
            // Tapping the Cast icon (not only the text) must also select the active video.
            // Do not consume the event: the official button still opens device selection.
            routeButton.setOnTouchListener((view,event)->{
                if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){
                    pendingCast=true;sent=false;
                }
                return false;
            });
            if(android.os.Build.VERSION.SDK_INT>=33 &&
               checkSelfPermission("android.permission.NEARBY_WIFI_DEVICES")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{"android.permission.NEARBY_WIFI_DEVICES"},890);
            }
            listener=new SessionManagerListener<CastSession>(){
                @Override public void onSessionStarting(CastSession s){}
                @Override public void onSessionStarted(CastSession s,String id){sendCast();}
                @Override public void onSessionStartFailed(CastSession s,int code){reportSafe("A ligação à TV falhou (código "+code+")");}
                @Override public void onSessionEnding(CastSession s){}
                @Override public void onSessionEnded(CastSession s,int reason){reportSafe("TV desligada da sessão Cast");sent=false;}
                @Override public void onSessionResuming(CastSession s,String id){}
                @Override public void onSessionResumed(CastSession s,boolean suspended){sendCast();}
                @Override public void onSessionResumeFailed(CastSession s,int reason){reportSafe("Não foi possível retomar o Cast");}
                @Override public void onSessionSuspended(CastSession s,int reason){reportSafe("Ligação à TV interrompida");}
            };
            cast.getSessionManager().addSessionManagerListener(listener,CastSession.class);
        }catch(RuntimeException|LinkageError ex){
            report("Reprodução local disponível se a fonte carregar; Google Cast indisponível: "+ex.getClass().getSimpleName());
            cast=null;
            routeButton.setVisibility(View.GONE);
        }
    }
    private void chooseTv(){
        if(cast==null){report("Google Cast não disponível neste dispositivo");return;}
        pendingCast=true;sent=false;
        CastSession session=cast.getSessionManager().getCurrentCastSession();
        if(session!=null&&session.isConnected()){sendCast();return;}
        report("Escolhe uma TV Google Cast na mesma rede Wi-Fi");
        try{routeButton.performClick();}
        catch(RuntimeException|LinkageError ex){report("Não foi possível mostrar a lista de TVs: "+ex.getClass().getSimpleName());}
    }
    private void sendCast(){
        if(!pendingCast||sent||cast==null)return;
        CastSession session=cast.getSessionManager().getCurrentCastSession();
        if(session==null||!session.isConnected())return;
        RemoteMediaClient remote=session.getRemoteMediaClient();
        if(remote==null){reportSafe("TV ligada, mas sem recetor multimédia compatível");return;}
        try{
            MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            metadata.putString(MediaMetadata.KEY_TITLE,title);
            String mime="video/mp4";
            if("webm".equals(format))mime="video/webm";
            else if("hls".equals(format))mime="application/x-mpegURL";
            else if("dash".equals(format))mime="application/dash+xml";
            MediaInfo info=new MediaInfo.Builder(url).setContentType(mime)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata).build();
            long position=player==null?0L:Math.max(0L,player.getCurrentPosition());
            sent=true;pendingCast=false;
            reportSafe("TV ligada. A enviar vídeo…");
            remote.load(new MediaLoadRequestData.Builder().setMediaInfo(info)
                .setCurrentTime(position).setAutoplay(true).build())
                .setResultCallback(result->{
                    if(result.getStatus().isSuccess()){
                        reportSafe("Pedido aceite pela TV; a verificar se o vídeo reproduz…");
                    }else{
                        sent=false;
                        reportSafe("TV ligada, mas o pedido de vídeo falhou: "+result.getStatus().getStatusCode());
                    }
                });
        }catch(RuntimeException|LinkageError ex){
            sent=false;
            reportSafe("Não foi possível enviar a fonte: "+ex.getClass().getSimpleName());
        }
    }
    @Override protected void onDestroy(){
        if(cast!=null&&listener!=null){
            try{cast.getSessionManager().removeSessionManagerListener(listener,CastSession.class);}catch(Exception ignored){}
        }
        if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}
        super.onDestroy();
    }
}
