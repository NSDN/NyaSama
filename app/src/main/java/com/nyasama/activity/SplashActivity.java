package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
        ThisApp.requestQueue.add(new StringRequest(releaseUrl + "/version.txt", new Response.Listener<String>() {
            @Override
            public void onResponse(final String s) {
                if (s != null && s.length() > 0 && !s.equals(versionName)) {
                    new AlertDialog.Builder(SplashActivity.this)
                            .setTitle(getString(R.string.new_version_alert_title))
                            .setMessage(String.format(getString(R.string.new_version_alert_message), s))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(releaseUrl + "/NyaSama-" + s + ".apk")));
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
