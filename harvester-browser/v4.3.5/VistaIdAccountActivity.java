package com.harvester.browser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Optional, consent-based local connection to the Vista ID Android app.
 * Does not expose saved URLs, scan history, videos, source library or passwords.
 */
public final class VistaIdAccountActivity extends Activity {
    private static final int REQUEST_LINK = 4201;
    private SharedPreferences prefs;
    private int dp(int p) { return (int)(p * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String content, int size) {
        TextView label = new TextView(this);
        label.setText(content);
        label.setTextColor(Color.WHITE);
        label.setTextSize(size);
        label.setPadding(0, dp(10), 0, dp(10));
        return label;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("vista_id_link", MODE_PRIVATE);
        display();
    }
    private void display() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff0b1120);
        root.setPadding(dp(24),dp(26),dp(24),dp(28));
        root.addView(text("Vista ID · opcional", 25));
        String id = prefs.getString("id", null);
        root.addView(text(id == null ? "Continuar sem conta está sempre disponível." :
            "Perfil local: " + prefs.getString("name","") + " · " + id,16));
        root.addView(text("O histórico, favoritos, abas, fontes e vídeos guardados continuam somente no VistaBrowser. Sincronização e partilhas cloud ainda não estão disponíveis.",13));
        Button link = new Button(this);
        link.setText(id == null ? "Ligar Vista ID" : "Alterar Vista ID");
        link.setOnClickListener(v -> {
            try {
                Intent intent = new Intent("com.vista.id.action.LINK_LOCAL_PROFILE");
                intent.setPackage("com.vista.id");
                startActivityForResult(intent, REQUEST_LINK);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Instala primeiro o Vista ID v0.2 ou superior.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Vista ID indisponível: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        root.addView(link);
        if (id != null) {
            Button disconnect = new Button(this);
            disconnect.setText("Desligar sem apagar dados");
            disconnect.setOnClickListener(v -> {
                prefs.edit().remove("id").remove("name").apply();
                display();
            });
            root.addView(disconnect);
        }
        Button close = new Button(this);
        close.setText("Voltar");
        close.setOnClickListener(v -> finish());
        root.addView(close);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_LINK || resultCode != RESULT_OK || data == null) return;
        String id = data.getStringExtra("com.vista.id.profile_id");
        String name = data.getStringExtra("com.vista.id.profile_name");
        String mode = data.getStringExtra("com.vista.id.mode");
        if (id == null || !id.matches("V[A-Z2-9]{12}") || !"local".equals(mode)) {
            Toast.makeText(this, "Resposta Vista ID inválida.", Toast.LENGTH_LONG).show();
            return;
        }
        prefs.edit().putString("id",id).putString("name",name == null ? "" : name.substring(0, Math.min(48,name.length()))).apply();
        display();
    }
}
