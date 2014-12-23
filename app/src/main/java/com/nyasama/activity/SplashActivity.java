package com.nyasama.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.Discuz;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by oxyflour on 2014/11/23.
 *
 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Discuz.execute("check",
                new HashMap<String, Object>(),
                new HashMap<String, Object>(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                }
        );
    }
}
