package com.nyasama.activity;

import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;

import com.negusoft.holoaccent.AccentHelper;
import com.negusoft.holoaccent.AccentResources;
import com.nyasama.R;
import com.nyasama.ThisApp;

/**
 * Created by oxyflour on 2015/1/2.
 * REF: https://github.com/negusoft/holoaccent/blob/master/HoloAccentExample/src/com/negusoft/holoaccent/example/activity/AccentFragmentActivity.java
 */
public class BaseThemedActivity extends FragmentActivity {

    private final AccentHelper mAccentHelper = new AccentHelper(getOverrideAccentColor(),
            getOverrideAccentColorDark(), getOverrideAccentColorActionBar(), new MyInitListener());

    @Override
    public Resources getResources() {
        return mAccentHelper.getResources(this, super.getResources());
    }

    /**
     * Override this method to set the accent color programmatically.
     * @return The color to override. If the color is equals to 0, the
     * accent color will be taken from the theme.
     */
    public int getOverrideAccentColor() {
        // Note: we have to use ThisApp context to get string, or it will fail
        return ThisApp.preferences.getInt(ThisApp.context.getString(R.string.pref_key_theme_color), 0x616F8B);
    }

    /**
     * Override this method to set the dark variant of the accent color programmatically.
     * @return The color to override. If the color is equals to 0, the dark version will be
     * taken from the theme. If it is specified in the theme either, it will be calculated
     * based on the accent color.
     */
    public int getOverrideAccentColorDark() {
        return 0;
    }

    /**
     * Override this method to set the action bar variant of the accent color programmatically.
     * @return The color to override. If the color is equals to 0, the action bar version will
     * be taken from the theme. If it is specified in the theme either, it will the same as the
     * accent color.
     */
    public int getOverrideAccentColorActionBar() {
        return 0;
    }

    /** Getter for the AccentHelper instance. */
    public AccentHelper getAccentHelper() {
        return mAccentHelper;
    }

    /**
     * Override this function to modify the AccentResources instance. You can add your own logic
     * to the default HoloAccent behaviour.
     */
    public void onInitAccentResources(AccentResources resources) {
        // To be overriden in child classes.
    }

    private class MyInitListener implements AccentHelper.OnInitListener {
        @Override
        public void onInitResources(AccentResources resources) {
            onInitAccentResources(resources);
        }
    }
}
