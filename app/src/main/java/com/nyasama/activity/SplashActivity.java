package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.nyasama.R;
import com.nyasama.ThisApp;

import java.io.UnsupportedEncodingException;

/**
 * Created by oxyflour on 2014/11/23.
 *
 */
public class SplashActivity extends Activity {

    public static final String releaseUrl = "http://dev.nyasama.com/beta-21";

    public static class UTF8StringRequest extends StringRequest {

        public UTF8StringRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(url, listener, errorListener);
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            try {
                return Response.success(new String(response.data, "utf8"),
                        HttpHeaderParser.parseCacheHeaders(response));
            }
            catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    int mInitJobs = 0;
    void checkInitJobs() {
        mInitJobs --;
        if (mInitJobs <= 0)
            onInitDone();
    }

    String mVersion = "";
    String mUpdateMessage = "";

    void onInitDone() {
        final String versionName = ThisApp.getVersion();
        if (!mVersion.isEmpty() && !mVersion.equals(versionName)) {
            TextView messageText = new TextView(SplashActivity.this);
            messageText.setPadding(32, 32, 32, 32);
            messageText.setText(Html.fromHtml(
                    "<big>" + String.format(getString(R.string.new_version_alert_message), mVersion) + "</big>" +
                    (mUpdateMessage.isEmpty() ? "" : "<br /><br />"+
                            getString(R.string.new_feature) + "<br />"+
                            mUpdateMessage.replace("\n", "<br />"))
            ));
            new AlertDialog.Builder(SplashActivity.this)
                    .setTitle(getString(R.string.new_version_alert_title))
                    .setView(messageText)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(releaseUrl + "/dlapk.php?v=" + mVersion)));
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                        }
                    })
                    .setNeutralButton(R.string.do_not_remind, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ThisApp.preferences.edit().putBoolean(getString(R.string.pref_key_check_update), false).commit();
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                        }
                    })
                    .show();
        }
        else {
            Log.e(SplashActivity.class.toString(), "check update failed: no new version found");
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mInitJobs ++;
        boolean checkUpdate = ThisApp.preferences.getBoolean(getString(R.string.pref_key_check_update), true);
        if (!checkUpdate) new android.os.Handler().post(new Runnable() {
            @Override
            public void run() {
                Log.e(SplashActivity.class.toString(),
                        "we are not checking for updates.");
                checkInitJobs();
            }
        });
        else ThisApp.requestQueue.add(new UTF8StringRequest(releaseUrl + "/update.apk", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                if (s != null) {
                    s = s.replace("\uFEFF", "").replace("\r\n", "\n");
                    int i = s.indexOf('\n');
                    if (i >= 0) {
                        mVersion = s.substring(0, i);
                        mUpdateMessage = s.substring(i + 1);
                    }
                    else {
                        mVersion = s;
                    }
                }
                checkInitJobs();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(SplashActivity.class.toString(), volleyError.getMessage() != null ?
                        volleyError.getMessage() : "Unknown");
                checkInitJobs();
            }
        }));
    }
}
