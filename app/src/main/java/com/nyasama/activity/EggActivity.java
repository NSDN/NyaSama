package com.nyasama.activity;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import cn.ac.nya.nsgdx.NyaAbout;
import cn.ac.nya.nsgdx.NyaEgg;

/**
 * Created by D.zzm on 2014.12.14.
 */

public class EggActivity extends AndroidApplication {
        @Override
        protected void onCreate (Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            config.useImmersiveMode = true;
            initialize(new NyaEgg(), config);
        }
    }

