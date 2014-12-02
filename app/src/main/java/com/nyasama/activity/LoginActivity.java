package com.nyasama.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class LoginActivity extends Activity
    implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "Login";

    public void doLogin(View view) {
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();
        final String answer = mAnswer.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            mMessage.setText(getString(R.string.login_empty_message));
            return;
        }

        Discuz.login(username, password, mQuestionId, answer, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        String messageval = message.getString("messageval");
                        if ("login_succeed".equals(messageval) ||
                                "location_login_succeed".equals(messageval)) {
                            // return uid to parent activity
                            String uid = data.getJSONObject("Variables").getString("member_uid");
                            setResult(Integer.parseInt(uid));
                            // refresh the form hash
                            Discuz.execute("forumindex", new HashMap<String, Object>(), null,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject jsonObject) {
                                            finish();
                                        }
                                    });
                        }
                        else
                            mMessage.setText(message.optString("messagestr"));
                    }
                    // TODO: remove these
                    catch (JSONException e) {
                        Log.e(TAG, "Parse Json Failed: " + e.getMessage());
                    }
                    catch (NullPointerException e) {
                        Log.e(TAG, "Parse Result Failed: " + e.getMessage());
                    }
                    catch (NumberFormatException e) {
                        Log.e(TAG, "Parse Number Failed: " + e.getMessage());
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
    private TextView mMessage;
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
        mMessage = (TextView) findViewById(R.id.message);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
