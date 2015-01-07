package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.nyasama.R;
import com.nyasama.ThisApp;

/**
 * Created by oxyflour on 2014/11/23.
 *
 */
public class SplashActivity extends Activity {

    public static final String releaseUrl = "http://dev.nyasama.com/release";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        final String versionName = ThisApp.getVersion();
        ThisApp.requestQueue.add(new StringRequest(releaseUrl + "/version_and_feature.txt", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                String version = s == null ? "" : s;
                String message = "";
                if (s != null) {
                    int i = s.replace("\r\n", "\n").indexOf('\n');
                    if (i >= 0) {
                        version = s.substring(0, i);
                        message = s.substring(i + 2);
                    }
                }
                if (!version.isEmpty() && !version.equals(versionName)) {
                    final String versionString = version;
                    String messageString = String.format(getString(R.string.new_version_alert_message), version);
                    if (!message.isEmpty())
                        messageString = messageString + "\n" + getString(R.string.new_feature) + "\n" + message;
                    new AlertDialog.Builder(SplashActivity.this)
                            .setTitle(getString(R.string.new_version_alert_title))
                            .setMessage(messageString)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(releaseUrl + "/NyaSama-" + versionString + ".apk")));
                                }
                            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                        }
                    }).show();
                }
                else {
                    Log.e(SplashActivity.class.toString(), "check update failed: no release");
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(SplashActivity.class.toString(), "check update failed: network error");
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }));
    }
}
