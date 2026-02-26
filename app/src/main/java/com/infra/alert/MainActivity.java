package com.infra.alert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private SmsHttpServer server;
    private static final int SMS_PERM = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERM);
        } else { startServer(); }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == SMS_PERM && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            startServer();
        else Toast.makeText(this, "SMS Permission Denied!", Toast.LENGTH_LONG).show();
    }

    private void startServer() {
        try {
            server = new SmsHttpServer(this, 8080);
            server.start();
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            ((TextView) findViewById(R.id.tv_ip)).setText("http://" + ip + ":8080");
            ((TextView) findViewById(R.id.tv_admin)).setText("Admin: http://" + ip + ":8080/admin");
        } catch (IOException e) {
            Toast.makeText(this, "Server error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }
}
