package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.volley.Response;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.negusoft.holoaccent.dialog.DividerPainter;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by oxyflour on 2015/10/18.
 *
 */
public class NyaSecActivity extends BaseThemedActivity {

    private final static String PREF_KEY_PREFIX = "nyasec-key-";

    private TextView mCode;
    private TextView mMenu;

    private String translateMessage(String msg) {
        if ("invalid phone number".equals(msg))
            return getString(R.string.nyasec_invalid_phone_number);
        else if ("retry later".equals(msg))
            return getString(R.string.nyasec_retry_later);
        else if ("send sms failed".equals(msg))
            return getString(R.string.nyasec_send_sms_failed);
        else if ("invalid download link".equals(msg))
            return getString(R.string.nyasec_invalid_download_link);
        return msg;
    }

    private String getCode() {
        String key = ThisApp.preferences.getString(PREF_KEY_PREFIX + Discuz.sUid, null);
        return key != null ? makeCode(key) : "XXXXXX";
    }

    private String makeCode(String key) {
        long tick = System.currentTimeMillis() / 1000L / 60;
        return makeCode(key, tick);
    }

    private String makeCode(String key, long tick) {
        String hash = Helper.toSafeMD5(key + tick);
        int len = hash.length();
        String hex = hash.substring(len - 8, len);
        long num = 0;
        try {
            num = Long.parseLong(hex, 16);
        }
        catch (NumberFormatException e) {
            num = 0;
        }
        return (num % 1000000 + 1000000 + "").substring(1);
    }

    private void doRequestKey(final AlertDialog dialog, final String phonenum) {
        Helper.enableDialog(dialog, false);
        Discuz.pluginapi(new HashMap<String, Object>() {{
            put("id", "nyasec:key");
            put("ac", "request");
            put("phonenum", phonenum);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject instanceof Discuz.JSONVolleyError) {
                    Helper.enableDialog(dialog, true);
                    Helper.toast(R.string.network_error_toast);
                }
                else if ("ok".equals(jsonObject.optString("result"))) {
                    dialog.dismiss();
                    ThisApp.preferences.edit().putString(PREF_KEY_PREFIX + Discuz.sUid, null).commit();
                    updateKey();
                }
                else {
                    Helper.enableDialog(dialog, true);
                    Helper.toast(translateMessage(jsonObject.optString("message")));
                }
            }
        });
    }

    private void doUpdateKey(final AlertDialog dialog, final String code) {
        Helper.enableDialog(dialog, false);
        Discuz.pluginapi(new HashMap<String, Object>() {{
            put("id", "nyasec:key");
            put("ac", "download");
            put("code", code);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject instanceof Discuz.JSONVolleyError) {
                    Helper.enableDialog(dialog, true);
                    Helper.toast(R.string.network_error_toast);
                }
                else if ("ok".equals(jsonObject.optString("result"))) {
                    dialog.dismiss();

                    String key = jsonObject.optString("message");
                    ThisApp.preferences.edit().putString(PREF_KEY_PREFIX + Discuz.sUid, key).commit();

                    mCode.setText(makeCode(key));
                    Helper.toast(R.string.update_naysec_key_success);
                }
                else {
                    Helper.enableDialog(dialog, true);
                    Helper.toast(translateMessage(jsonObject.optString("message")));
                }
            }
        });
    }

    private void requestKey() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        final AlertDialog dialog = new AccentAlertDialog.Builder(this)
                .setTitle(R.string.diag_request_key)
                .setMessage(R.string.diag_input_phone_number)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                new DividerPainter(NyaSecActivity.this).paint(dialog.getWindow());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                doRequestKey(dialog, input.getText().toString());
                            }
                        });
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void updateKey() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        final AlertDialog dialog = new AccentAlertDialog.Builder(this)
                .setTitle(R.string.diag_update_key)
                .setMessage(R.string.diag_input_downloadcode)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                new DividerPainter(NyaSecActivity.this).paint(dialog.getWindow());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                doUpdateKey(dialog, input.getText().toString());
                            }
                        });
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void clearKey() {
        final AlertDialog dialog = new AccentAlertDialog.Builder(this)
                .setTitle(R.string.action_clear_key)
                .setMessage(R.string.clear_key_confirm)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ThisApp.preferences.edit().putString(PREF_KEY_PREFIX + Discuz.sUid, null).commit();
                        mCode.setText(getCode());
                        Helper.toast(R.string.toast_key_cleared);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nyasec);
        mCode = (TextView) findViewById(R.id.code);
        mMenu = (TextView) findViewById(R.id.menu);

        mCode.setText(getCode());
        mCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCode.setText(getCode());
            }
        });

        mMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(NyaSecActivity.this, view);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.menu_nyasec, menu);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int action = menuItem.getItemId();
                        if (action == R.id.action_reset)
                            requestKey();
                        else if (action == R.id.action_update)
                            updateKey();
                        else if (action == R.id.action_clear)
                            clearKey();
                        return false;
                    }
                });

                popup.show();
            }
        });
    }
}
