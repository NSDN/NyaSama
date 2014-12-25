package com.nyasama.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

public class LoginActivity extends Activity
    implements AdapterView.OnItemSelectedListener {

    public static final int REQUEST_CODE_LOGIN = 1;

    public void doLogin(View view) {
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();
        final String answer = mAnswer.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            Helper.toast(getString(R.string.login_empty_message), Gravity.TOP, 0, 0.2);
            return;
        }

        Discuz.login(username, password, mQuestionId, answer, new Response.Listener<JSONObject>() {
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
                        else
                            Helper.toast(message.optString("messagestr"), Gravity.TOP, 0, 0.2);
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return Helper.handleOption(this, item.getItemId()) ||
                super.onOptionsItemSelected(item);
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
