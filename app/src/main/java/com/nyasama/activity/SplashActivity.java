package com.nyasama.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.nyasama.R;
import com.nyasama.util.Discuz;

/**
 * Created by oxyflour on 2014/11/23.
 *
 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Discuz.init(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}
