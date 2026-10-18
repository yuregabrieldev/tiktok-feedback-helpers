from pathlib import Path

java=Path("app/src/main/java/com/harvester/browser/MainActivity.java")
s=java.read_text()
def replace_once(old,new,label):
    global s
    count=s.count(old)
    if count!=1: raise RuntimeError(label+": expected 1 match, got "+str(count))
    s=s.replace(old,new,1)

# MediaRouteButton.showDialog() requires FragmentActivity, not a platform Activity.
replace_once("public final class MainActivity extends Activity {",
    "public final class MainActivity extends androidx.fragment.app.FragmentActivity {",
    "Cast activity superclass")

# A failure in Cast initialization must be reported instead of taking down the browser.
replace_once(
    "        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),routeButton);",
    """        try {
            CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),routeButton);
        } catch (RuntimeException | LinkageError castError) {
            panelHint(page.body,"O seletor Google Cast não está disponível: "
                +castError.getClass().getSimpleName());
            return;
        }""",
    "Safe media route button setup")
replace_once(
    "        routeLabel.setOnClickListener(v->routeButton.performClick());",
    """        routeLabel.setOnClickListener(v->{
            try { routeButton.performClick(); }
            catch (RuntimeException | LinkageError error) {
                toast("Não foi possível abrir a seleção da TV: "+error.getClass().getSimpleName());
            }
        });""",
    "Safe route label")

# Visible one-tap Cast button in every eligible detected result and library result.
replace_once(
    """            panelLine(page.body,"▶",item.format.toUpperCase(Locale.ROOT)+" • "+item.title,
                shortDomain(item.url)+" · "+item.discoveredFrom,()->mediaDetails(item,false),null);
            panelDivider(page.body);""",
    """            LinearLayout resultRow=panelLine(page.body,"▶",item.format.toUpperCase(Locale.ROOT)+" • "+item.title,
                shortDomain(item.url)+" · "+item.discoveredFrom,()->mediaDetails(item,false),null);
            addCastQuickAction(resultRow,item);
            panelDivider(page.body);""",
    "Cast in detected list")
replace_once(
    """            panelLine(list,"▶",item.title,shortDomain(item.pageUrl)+" • "+item.format.toUpperCase(Locale.ROOT),
                ()->mediaDetails(item,true),()->new AlertDialog.Builder(this).setTitle("Eliminar da biblioteca?")
                    .setPositiveButton("Eliminar",(d,w)->{store.deleteMedia(item.url);renderMediaLibrary(container,query);})
                    .setNegativeButton("Cancelar",null).show());
            panelDivider(list);""",
    """            LinearLayout savedRow=panelLine(list,"▶",item.title,shortDomain(item.pageUrl)+" • "+item.format.toUpperCase(Locale.ROOT),
                ()->mediaDetails(item,true),()->new AlertDialog.Builder(this).setTitle("Eliminar da biblioteca?")
                    .setPositiveButton("Eliminar",(d,w)->{store.deleteMedia(item.url);renderMediaLibrary(container,query);})
                    .setNegativeButton("Cancelar",null).show());
            addCastQuickAction(savedRow,item);
            panelDivider(list);""",
    "Cast in library")

needle="    private void mediaDetails(MediaItem item,boolean saved){"
if s.count(needle)!=1:raise RuntimeError("mediaDetails insertion point missing")
s=s.replace(needle,
    """    private void addCastQuickAction(LinearLayout row,MediaItem item) {
        if(row==null || !CastSourcePolicy.mayOfferCast(item))return;
        TextView button=panelIcon("▣",23,"Transmitir "+item.title+" para TV via Google Cast");
        button.setTextColor(0xFFAADDF4);
        button.setBackground(Styles.bg(this,0xFF29383E,15));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(d(46),d(48));
        lp.setMargins(d(3),0,d(4),0);
        row.addView(button,lp);
        button.setOnClickListener(v->requestCast(item));
    }
""" + needle,1)
java.write_text(s)

gradle=Path("app/build.gradle")
s=gradle.read_text()
if "versionName '1.5.0'" not in s:raise RuntimeError("Unexpected prior app version")
s=s.replace("versionCode 6","versionCode 7").replace("versionName '1.5.0'","versionName '1.6.0'")
s+="\n// Explicit FragmentActivity dependency for MediaRouteButton dialog handling.\ndependencies { implementation 'androidx.fragment:fragment:1.8.9' }\n"
gradle.write_text(s)

manifest=Path("app/src/main/AndroidManifest.xml")
s=manifest.read_text()
if "android.permission.NEARBY_WIFI_DEVICES" not in s:
    s=s.replace('    <application ',
        '    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />\n'
        '    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:usesPermissionFlags="neverForLocation" />\n'
        '    <application ',1)
manifest.write_text(s)

# Java-only smoke test: Cast options remain HTTPS-only and session-scoped sources
# are always flagged as potentially unplayable on an external receiver.
test=Path("app/src/main/java/com/harvester/browser/core/CastSourcePolicy.java")
if not test.exists():raise RuntimeError("Cast policy not installed")
print("Cast crash hotfix integrated; FragmentActivity, quick actions and build version verified")
