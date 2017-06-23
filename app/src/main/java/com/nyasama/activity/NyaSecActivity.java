package com.nyasama.activity;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by oxyflour on 2015/10/18.
 *
 */
public class NyaSecActivity extends AppCompatActivity {

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
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.diag_request_key)
                .setMessage(R.string.diag_input_phone_number)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
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
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.diag_update_key)
                .setMessage(R.string.diag_input_downloadcode)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
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
        final AlertDialog dialog = new AlertDialog.Builder(this)
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_action_nya);
        //toolbar.setLogo(R.drawable.ic_action_nya);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                finish();
            }
        });

        mCode = (TextView) findViewById(R.id.code);
        mMenu = (TextView) findViewById(R.id.menu);

        mCode.setText(getCode());
        mCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCode.setText(getCode());
                mGameEnabled = true;
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

        mGameSpan = (TextView) findViewById(R.id.nyasec_game);
        mCountSpan = (TextView) findViewById(R.id.nyasec_count);
        mGameSpan.setOnClickListener(mOnGameClick);

        mGameTick.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing())
                    return;
                if (mGameEnabled)
                    mGameSteps.step(1.0 / 4);
                mGameTick.postDelayed(this, GAME_INTERVAL);
            }
        }, NyaSecActivity.GAME_INTERVAL);

        try {
            // https://www.freesound.org/people/Jaz_the_MAN_2/sounds/316901/
            mSpanClickedSound = sound.load(getAssets().openFd("do.wav"), 0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static int GAME_INTERVAL = 1000 / 16;
    // get more at http://matsucon.net/material/dic/
    private final static String[] GAME_SPAN_TEXTS = {
            "ヽ(✿ﾟ▽ﾟ)ノ",
            "(*‘ v`*)",
            "(*´∀`)~♥",
            "(ﾉ>ω<)ﾉ",
            "d(`･∀･)b",
    };
    private final static String[] GAME_SPAN_CLKED = {
            "(=ﾟДﾟ=)",
            "-`д´-",
            "(´ﾟдﾟ`)",
    };

    private TextView mGameSpan;
    private TextView mCountSpan;
    private boolean mGameEnabled;
    private boolean mSpanClicked;
    private int mSpanClickedCount;
    private int mSpanClickedMaxCount;
    private int mSpanClickedSound;
    private Handler mGameTick = new Handler();

    private static Random rand = new Random();
    private static SoundPool sound = new SoundPool(1, AudioManager.USE_DEFAULT_STREAM_TYPE, 0);

    // pitch = 1 => do
    // ref: http://www.phy.mtu.edu/~suits/NoteFreqCalcs.html
    private static int[] MAJOR_PITCHES = { 1, 3, 5, 6, 8, 10, 12 };
    private static float getPlayRate(int pitch) {
        int delta = 0;
        while (pitch > 7) {
            pitch -= 7;
            delta += 12;
        }
        while (pitch < 1) {
            pitch += 7;
            delta -= 12;
        }
        return (float) Math.pow(1.059463094359, MAJOR_PITCHES[pitch - 1] - 1 + delta);
    }

    private Runnable mOnGameUpdate = new Runnable() {
        @Override
        public void run() {
            if (mSpanClicked) {
                mSpanClickedCount ++;
                mSpanClickedMaxCount = Math.max(mSpanClickedCount, mSpanClickedMaxCount);
            }
            else {
                mSpanClickedCount = 0;
            }

            mCountSpan.setText(mSpanClickedCount > 0 ?
                    mSpanClickedCount + " / " + mSpanClickedMaxCount : "");

            mSpanClicked = false;
            mGameSpan.setText(GAME_SPAN_TEXTS[rand.nextInt(GAME_SPAN_TEXTS.length)]);
            mGameSpan.setVisibility(View.VISIBLE);

            double fx = Math.random();
            double fy = mGameSteps.maxVal <= mGameSteps.minVal ? 0.5 :
                    (mGameSteps.value - mGameSteps.minVal) / (mGameSteps.maxVal - mGameSteps.minVal);

            // keep it away from center
            if (fy > 0.5)
                fy = fy * 0.8 + 0.2;
            else
                fy = fy * 0.8;

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            mGameSpan.setTranslationX(Math.round((0.9 - fx * 0.8) * (metrics.widthPixels - mGameSpan.getMeasuredWidth())));
            mGameSpan.setTranslationY(Math.round((0.9 - fy * 0.8) * (metrics.heightPixels - mGameSpan.getMeasuredHeight())));
        }
    };

    private View.OnClickListener mOnGameClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mSpanClicked = true;
            mGameSpan.setText(GAME_SPAN_CLKED[rand.nextInt(GAME_SPAN_CLKED.length)]);

            sound.play(mSpanClickedSound, 1, 1, 0, 0, getPlayRate((int)mGameSteps.value));

            mGameTick.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mSpanClicked)
                        mGameSpan.setVisibility(View.INVISIBLE);
                }
            }, 100);
        }
    };

    // Kamigami ga koishita gensoukyo
    private GameSteps mGameSteps = new GameSteps(
            "3,3,5,6," + "5,6,5,3,2,5,3," +
            "3,3,5,6," + "5,6,8,7,6,5,6," +
            "5,6,5,3,2," + "5,6,5,2,1," +
            "6,6,7,1," + "2,7,6,6", 3, mOnGameUpdate);

    class GameSteps {
        GameSteps(String valueStr, int defDuration, Runnable onUpdate) {
            for (String val : valueStr.split(",")) {
                String[] vs = val.split(":");
                double v = Helper.toSafeDouble(vs[0], 0);
                minVal = Math.min(minVal, v);
                maxVal = Math.max(maxVal, v);
                values.add(v);

                double d = vs.length > 1 ? Helper.toSafeDouble(vs[1], defDuration) : defDuration;
                total += d;
                durations.add(d);
            }

            update = onUpdate;
        }

        Runnable update;

        ArrayList<Double> values = new ArrayList<Double>();
        ArrayList<Double> durations = new ArrayList<Double>();
        double total = 0;
        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;

        int lastIndex = -1;
        double value = 0;
        void updateValue(double progress) {
            double start = 0;
            for (int i = 0; i < durations.size(); i ++) {
                double next = start + durations.get(i);
                if (start <= progress && progress < next) {
                    value = values.get(i);
                    if (lastIndex != i)
                        update.run();
                    lastIndex = i;
                    return;
                }
                start = next;
            }
        }

        double progress;
        void step(double time) {
            progress += time;
            if (total <= 0)
                progress = 0;
            else while (progress >= total)
                progress -= total;
            updateValue(progress);
        }
    }
}
