package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.nyasama.R;
import com.nyasama.ThisApp;

// REF: http://blog.csdn.net/lincyang/article/details/20609673

public class SettingActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        //findPreference(getString(R.string.pref_key_version)).setSummary(ThisApp.getVersion());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_language))) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.alert_need_reboot))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ThisApp.restart();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }
}
