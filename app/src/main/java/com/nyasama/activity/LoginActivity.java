package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class LoginActivity extends Activity {

    public void doLogin(View view) {
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();
        if (username.equals("") || password.equals("")) {
            mMessage.setText(getString(R.string.login_empty_message));
            return;
        }

        Discuz.login(username, password, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(LoginActivity.this, R.string.network_error_toast);
                }
                else if (!jsonObject.optJSONObject("Message").optString("messagestr").equals("")) {
                    mMessage.setText(jsonObject.optJSONObject("Message").optString("messagestr"));
                }
                else {
                    try {
                        // return uid to parent activity
                        String uid = jsonObject.optJSONObject("Variables").optString("member_uid");
                        setResult(Integer.parseInt(uid));
                    }
                    catch (NumberFormatException e) {
                        //
                    }
                    finish();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mButton = (Button) findViewById(R.id.login_button);
        mMessage = (TextView) findViewById(R.id.message);
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
}
