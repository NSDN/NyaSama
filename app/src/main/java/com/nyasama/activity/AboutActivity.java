package com.nyasama.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import cn.ac.nya.nsgdx.NSGDX;

/**
 * Created by D.zzm on 2014.12.14.
 */

public class AboutActivity extends AndroidApplication {
        @Override
        protected void onCreate (Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            config.useImmersiveMode = true;
            initialize(new NSGDX().setOnExitCallback(new Runnable() {
                @Override
                public void run() { exit(); }
            }), config);
        }
    }

