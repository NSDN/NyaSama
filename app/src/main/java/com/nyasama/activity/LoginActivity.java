package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.negusoft.holoaccent.dialog.DividerPainter;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class LoginActivity extends BaseThemedActivity
    implements AdapterView.OnItemSelectedListener {

    public static final int REQUEST_CODE_LOGIN = 1;
    public static final int SEC_IMG_PADDING = 2;

    public static class SeccodeImage {
        String src;
        String code;
        public SeccodeImage(String src, String code) {
            this.src = src;
            this.code = code;
        }
    }

    public void updateSeccode(final AlertDialog dialog, final GridView list) {
        mSechash = 'S' + Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 5);
        Discuz.execute("plugin", new HashMap<String, Object>() {{
            put("id", "seobbseccode:update");
            put("idhash", mSechash);
            put("asjson", true);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                    dialog.dismiss();
                }
                else {
                    String target = data.optString("target");
                    if (target != null) {
                        dialog.setMessage(getString(R.string.diag_ask_which) + target + "?");
                    }

                    JSONObject imgsObj = data.optJSONObject("imgs");
                    final List<SeccodeImage> imgs = new ArrayList<SeccodeImage>();
                    if (imgsObj != null) for (Iterator<String> iter = imgsObj.keys(); iter.hasNext(); ) {
                        String code = iter.next();
                        String src = imgsObj.optString(code);
                        imgs.add(new SeccodeImage(src, code));
                    }

                    list.setAdapter(new CommonListAdapter<SeccodeImage>(imgs, R.layout.fragment_seccode_item) {
                        @Override
                        public void convertView(ViewHolder viewHolder, SeccodeImage item) {
                            ((NetworkImageView) viewHolder.getView(R.id.img)).setImageUrl(Discuz.DISCUZ_URL + item.src, ThisApp.imageLoader);
                        }
                    });
                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            mSeccode = imgs.get(i).code;
                            dialog.dismiss();
                            doLogin(null);
                        }
                    });
                }
            }
        });
    }

    public void askSeccode() {
        final GridView gridView = new GridView(this);
        gridView.setNumColumns(3);
        gridView.setLayoutParams(new AbsListView.LayoutParams(
                AbsListView.LayoutParams.WRAP_CONTENT,
                AbsListView.LayoutParams.WRAP_CONTENT));
        final AlertDialog dialog = new AccentAlertDialog.Builder(this)
            .setTitle(R.string.diag_ask_input_seccode)
            .setMessage(R.string.diag_loading_seccodes)
            .setView(gridView)
            .setPositiveButton(R.string.action_reload, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                new DividerPainter(dialog.getContext()).paint(dialog.getWindow());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateSeccode(dialog, gridView);
                    }
                });
            }
        });
        dialog.show();
        updateSeccode(dialog, gridView);
    }

    public void doLogin(View view) {
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();
        final String answer = mAnswer.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            Helper.toast(getString(R.string.login_empty_message), Gravity.TOP, 0, 0.2);
            return;
        }

        Discuz.login(username, password, mQuestionId, answer, mSechash, mSeccode, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else {
                    JSONObject message = data.optJSONObject("Message");
                    JSONObject var = data.optJSONObject("Variables");
                    if (message != null) {
                        String messageval = message.optString("messageval");
                        if ("login_succeed".equals(messageval) ||
                                "location_login_succeed".equals(messageval)) {
                            // return uid to parent activity
                            if (var != null)
                                setResult(Helper.toSafeInteger(var.optString("member_uid"), 0));
                            finish();
                        }
                        else if ("submit_seccode_invalid".equals(messageval)) {
                            Helper.toast(message.optString("messagestr"));
                            askSeccode();
                        }
                        else {
                            Helper.toast(message.optString("messagestr"), Gravity.TOP, 0, 0.2);
                        }
                    }

                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString("username", username);
                    editor.apply();
                }
                mButton.setEnabled(true);
            }
        });
        mButton.setEnabled(false);
    }

    private EditText mUsername;
    private EditText mPassword;
    private Button mButton;
    private Spinner mQuestion;
    private int mQuestionId = 0;
    private EditText mAnswer;

    private String mSeccode;
    private String mSechash;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mButton = (Button) findViewById(R.id.login_button);
        mQuestion = (Spinner) findViewById(R.id.question);
        mAnswer = (EditText) findViewById(R.id.answer);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.login_question, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mQuestion.setAdapter(adapter);
        mQuestion.setOnItemSelectedListener(this);

        mPrefs = getSharedPreferences("login_info", MODE_PRIVATE);
        mUsername.setText(mPrefs.getString("username", ""));

        Discuz.execute("login", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        mQuestionId = i;
        Helper.updateVisibility(mAnswer, mQuestionId > 0);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}
