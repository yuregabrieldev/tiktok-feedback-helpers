package com.yure.videoharvester;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** User-reviewable local collection of crawler discoveries. */
public final class VistaSourceBrowserActivity extends Activity {
    private static final int EXPORT_REQUEST=4371;
    private TextView summary;
    private LinearLayout panel;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(26,32,26,32);
        panel.setBackgroundColor(0xff151719);
        title("Vista Source",26);
        title("Fontes detetadas automaticamente pelo inspetor, sem cookies ou vídeos descarregados. " +
            "Apenas títulos identificados ou aprovados por ti são exportados.",14);
        summary=title("",16);
        button("Rever correspondências pendentes",()->review());
        button("Exportar manifest.json",()->{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("application/json");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_TITLE,"manifest.json");
            startActivityForResult(i,EXPORT_REQUEST);
        });
        button("Voltar",()->finish());
        setContentView(new ScrollView(this){{addView(panel);}});
        refresh();
    }
    private TextView title(String text,int size){
        TextView v=new TextView(this);
        v.setText(text);v.setTextSize(size);v.setTextColor(Color.WHITE);
        v.setPadding(0,7,0,17);panel.addView(v);return v;
    }
    private void button(String text,Runnable action){
        Button b=new Button(this);b.setText(text);b.setOnClickListener(v->action.run());
        panel.addView(b);
    }
    private void refresh(){
        JSONArray items=VistaSourceCollector.entries(this);
        int accepted=0;
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);
            if(item!=null && item.optBoolean("identified"))accepted++;
        }
        summary.setText("Detetadas: "+items.length()+" · identificadas: "+accepted+
            " · por rever: "+(items.length()-accepted));
    }
    private void review(){
        JSONArray items=VistaSourceCollector.entries(this);
        int next=-1;JSONObject candidate=null;
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);
            if(item!=null&&!item.optBoolean("identified")){
                next=i;candidate=item;break;
            }
        }
        if(next<0){new AlertDialog.Builder(this).setMessage("Sem fontes pendentes.")
            .setPositiveButton("OK",null).show();return;}
        final int index=next;
        final JSONObject item=candidate;
        LinearLayout fields=new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(22,10,22,10);
        EditText title=new EditText(this);title.setHint("Título correto");title.setText(item.optString("title"));
        fields.addView(title);
        EditText season=new EditText(this);season.setHint("Temporada (1 para filme)");
        season.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);season.setText("1");fields.addView(season);
        EditText episode=new EditText(this);episode.setHint("Episódio (1 para filme)");
        episode.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);episode.setText("1");fields.addView(episode);
        new AlertDialog.Builder(this).setTitle("Confirmar título e episódio")
            .setView(fields).setNegativeButton("Mais tarde",null)
            .setPositiveButton("Aprovar", (d,w)->{
                String name=title.getText().toString().trim();
                int s,e;
                try{s=Integer.parseInt(season.getText().toString());
                    e=Integer.parseInt(episode.getText().toString());}
                catch(Exception ex){s=0;e=0;}
                if(name.isEmpty()||name.length()>180||s<1||s>999||e<1||e>9999){
                    android.widget.Toast.makeText(this,"Título ou episódio inválido",0).show();
                    return;
                }
                try{
                    item.put("title",name);item.put("season",s);item.put("episode",e);
                    item.put("kind", s>1||e>1?"tv":"movie");
                    item.put("identified",true);
                    items.put(index,item);
                    getSharedPreferences("vista_smart_crawler_v1",MODE_PRIVATE).edit()
                        .putString("items",items.toString()).apply();
                    refresh();
                }catch(Exception ignored){}
            }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_REQUEST||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
            if(out==null)throw new Exception("Ficheiro indisponível");
            out.write(VistaSourceCollector.export(this).getBytes(StandardCharsets.UTF_8));
            android.widget.Toast.makeText(this,"Vista Source exportado.",1).show();
        }catch(Exception e){
            new AlertDialog.Builder(this).setTitle("Falha na exportação")
                .setMessage(e.getMessage()).setPositiveButton("OK",null).show();
        }
    }
}
