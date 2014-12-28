package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.nyasama.R;
import com.nyasama.ThisApp;

// REF: http://blog.csdn.net/lincyang/article/details/20609673
//设置activity
//
//设置activity 和普通activity区别不大，只是layout文件的格式略不一样
//详细文档见：http://developer.android.com/guide/topics/ui/settings.html
//有两个建议：1, 添加 PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, false);
//              见详细文档 Setting Default Values 节，虽然感觉上不加这句也没问题，但还是加上安心
//            2，明确创建出 OnSharedPreferenceChangeListener 实例
//              见详细文档Reading Preference 节，Listening for preference changes 块的最后部分，同样是加上更安心
public class SettingActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("language")) {
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

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
