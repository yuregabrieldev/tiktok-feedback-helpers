from pathlib import Path

harvester=Path('hb-v18')
vistacast=Path('vista')
base=harvester/'app/src/main/java/com/harvester/browser'
vista=vistacast/'app/src/main/java/com/vistacast/app'
assert (base/'MainActivity.java').exists(), "Harvester Browser not restored"
assert (vista/'MainActivity.java').exists(), "VistaCast source not decoded"

for name in ('PlaybackService.java','LocalVideoServer.java'):
    code=(vista/name).read_text()
    assert 'package com.vistacast.app;' in code
    code=code.replace('package com.vistacast.app;', 'package com.harvester.browser;',1)
    (base/name).write_text(code)

code=(vista/'MainActivity.java').read_text()
assert 'public final class MainActivity extends AppCompatActivity' in code
code=code.replace('package com.vistacast.app;', 'package com.harvester.browser;',1)
code=code.replace('MainActivity', 'NativePlayerActivity')
code=code.replace('▶  VistaCast','▶  VistaCast Player')
# External URL comes from Harvester detected video/library, and is not a gallery file.
code=code.replace('    private boolean fullscreen;', '''    private boolean fullscreen;
    private boolean externalOpened;
    private LinearLayout galleryPanel;''',1)
old='''        buildUi();
        refreshGallery();'''
assert code.count(old)==1,"onCreate gallery initialization mismatch"
code=code.replace(old,'''        buildUi();
        // Do not request gallery permissions while opening a browser video.
        // Gallery opens only when the user taps the Library button.''',1)
old='''        bar.addView(logo, new LinearLayout.LayoutParams(0, dp(51), 1));
        MediaRouteButton cast'''
assert code.count(old)==1,"VistaCast top bar mismatch"
code=code.replace(old,'''        bar.addView(logo, new LinearLayout.LayoutParams(0, dp(51), 1));
        Button galleryToggle=btn("Biblioteca",v->toggleGallery());
        bar.addView(galleryToggle,new LinearLayout.LayoutParams(dp(106),dp(44)));
        MediaRouteButton cast''',1)
old='''        root.addView(section);
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(); list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));'''
assert code.count(old)==1, "VistaCast gallery structure mismatch"
code=code.replace(old,'''        galleryPanel=new LinearLayout(this);
        galleryPanel.setOrientation(LinearLayout.VERTICAL);
        galleryPanel.addView(section);
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(); list.setAdapter(adapter);
        galleryPanel.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(galleryPanel,new LinearLayout.LayoutParams(-1,0,1));
        galleryPanel.setVisibility(View.GONE);''',1)

# Bridge browser saved URL into real VistaCast Media3 CastPlayer session.
old='''                controller.addListener(listener);
                showStatus();
                if (controller.isPlaying()) playPause.setText("Ⅱ");'''
assert code.count(old)==1, "MediaSession controller initialization mismatch"
code=code.replace(old,'''                controller.addListener(listener);
                openBrowserVideo();
                showStatus();
                if (controller.isPlaying()) playPause.setText("Ⅱ");''',1)

anchor='''    private void seek(long offset) {'''
assert code.count(anchor)==1, "Seek method insertion mismatch"
helpers='''    private void toggleGallery(){
        if(galleryPanel==null)return;
        boolean show=galleryPanel.getVisibility()!=View.VISIBLE;
        galleryPanel.setVisibility(show?View.VISIBLE:View.GONE);
        if(show)refreshGallery();
    }

    /** Native Media3 CastPlayer takes care of transferring playback between local and TV. */
    private void openBrowserVideo(){
        if(externalOpened || controller==null)return;
        Intent request=getIntent();
        if(request==null)return;
        String url=request.getStringExtra("url");
        if(url==null || !url.startsWith("https://"))return;
        String title=request.getStringExtra("title");
        String format=request.getStringExtra("format");
        if(title==null||title.trim().isEmpty())title="Vídeo encontrado";
        if(format==null)format="mp4";
        String mime;
        switch(format.toLowerCase(java.util.Locale.ROOT)){
            case "hls": mime="application/x-mpegURL";break;
            case "dash": mime="application/dash+xml";break;
            case "webm":mime="video/webm";break;
            default:mime="video/mp4";break;
        }
        try{
            // Do not proxy third-party media or forward browser cookies/credentials.
            androidx.media3.common.MediaItem media=
                new androidx.media3.common.MediaItem.Builder()
                    .setMediaId(url).setUri(url).setMimeType(mime)
                    .setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title).build()).build();
            controller.setMediaItem(media);
            controller.prepare();
            long position=request.getLongExtra("position_ms",0);
            if(position>0)controller.seekTo(position);
            controller.play();
            titleView.setText(title);
            externalOpened=true;
        }catch(Exception error){
            android.util.Log.e("VistaCastBridge","Could not play Harvester source",error);
            statusView.setText("A fonte não pôde ser aberta: "+error.getClass().getSimpleName());
        }
    }

'''
code=code.replace(anchor,helpers+anchor,1)
# User pressing ▶ in the Harvester library uses an intentional separate Player screen.
(base/'NativePlayerActivity.java').write_text(code)

manifest=harvester/'app/src/main/AndroidManifest.xml'
m=manifest.read_text()
assert 'android:name=".NativePlayerActivity"' in m
if 'android:name=".PlaybackService"' not in m:
    m=m.replace('    </application>',
'''        <service android:name=".PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
        <receiver android:name="androidx.mediarouter.media.MediaTransferReceiver"
            android:exported="false" />
    </application>''',1)
m=m.replace('android:value="com.harvester.browser.HarvesterCastOptionsProvider"',
            'android:value="androidx.media3.cast.DefaultCastOptionsProvider"')
# Allow tokenized local LAN video only for VistaCast gallery -> TV; external website URLs remain HTTPS.
m=m.replace('android:usesCleartextTraffic="false"','android:usesCleartextTraffic="true"')
for permission in ['android.permission.FOREGROUND_SERVICE',
                   'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK',
                   'android.permission.READ_MEDIA_VIDEO',
                   'android.permission.READ_EXTERNAL_STORAGE']:
    if permission not in m:
        attrib=(' android:maxSdkVersion="32"' if permission.endswith("READ_EXTERNAL_STORAGE") else "")
        m=m.replace('    <application',f'    <uses-permission android:name="{permission}"{attrib} />\n    <application',1)
manifest.write_text(m)

gradle=harvester/'app/build.gradle'
g=gradle.read_text()
if "versionName '1.9.0'" not in g: raise RuntimeError('Harvester v1.9 source not found')
g=g.replace('versionCode 10','versionCode 11').replace("versionName '1.9.0'","versionName '2.0.0'")
g+='''

// Reuse the VistaCast Media3 CastPlayer + MediaSessionService implementation.
dependencies {
    def vistaMedia3='1.11.1'
    implementation "androidx.media3:media3-cast:$vistaMedia3"
    implementation "androidx.media3:media3-session:$vistaMedia3"
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'androidx.recyclerview:recyclerview:1.4.0'
}
'''
gradle.write_text(g)
print("Harvester Browser 2.0.0 now opens the real VistaCast CastPlayer for saved/detected URLs.")
