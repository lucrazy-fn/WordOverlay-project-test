package com.luan.wordoverlay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_OVERLAY = 10;
    private static final int REQ_CAPTURE = 11;
    private static final int REQ_NOTIF = 12;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(48,48,48,48);
        TextView title = new TextView(this); title.setText("Word Overlay"); title.setTextSize(28); title.setTextColor(Color.BLACK);
        TextView info = new TextView(this); info.setText("Bolinha flutuante para resolver fases de palavras.\n\n1. Dê permissão para aparecer sobre outros apps.\n2. Autorize a captura da tela.\n3. Abra o jogo e toque na bolinha."); info.setTextSize(16); info.setTextColor(Color.DKGRAY); info.setPadding(0,24,0,24);
        Button start = new Button(this); start.setText("ATIVAR BOLINHA");
        box.addView(title); box.addView(info); box.addView(start);
        setContentView(box);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);

        start.setOnClickListener(v -> begin());
    }

    private void begin() {
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY); return;
        }
        MediaProjectionManager mpm = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req,result,data);
        if (req == REQ_OVERLAY) { if (Settings.canDrawOverlays(this)) begin(); return; }
        if (req == REQ_CAPTURE && result == RESULT_OK && data != null) {
            Intent s = new Intent(this, OverlayService.class);
            s.setAction(OverlayService.ACTION_START);
            s.putExtra("resultCode", result); s.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(s); else startService(s);
            finish();
        }
    }
}
