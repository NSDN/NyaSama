package com.nyasama.activity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.ThisApp;

// REF: http://blog.csdn.net/lincyang/article/details/20609673
// REF: https://github.com/negusoft/holoaccent/wiki/Preferences-Activity

public class SettingActivity extends BaseThemedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new ThisPreferenceFragment())
                    .commit();
        }
    }

    public static class ThisPreferenceFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference);

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            // update version string
            findPreference(getString(R.string.pref_key_version)).setSummary(ThisApp.getVersion());
            // update setting
            onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(),
                    getString(R.string.pref_key_show_image));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (getActivity() == null) return;

            ThisApp.onSharedPreferenceChanged(sharedPreferences, key);
            if (key.equals(getString(R.string.pref_key_language))) {
                if (!getActivity().isFinishing()) new AccentAlertDialog.Builder(getActivity())
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
            else if (key.equals(getString(R.string.pref_key_show_image))) {
                findPreference(getString(R.string.pref_key_thumb_size))
                        .setEnabled(sharedPreferences.getBoolean(key, false));
            }
        }
    }
}
