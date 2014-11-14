package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.HashMap;

public class NewPostActivity extends Activity
    implements TextWatcher {

    public void doPost(View view) {
        final String title = mTitle.getText().toString();
        final String content = mContent.getText().toString();
        if (title.isEmpty() || content.isEmpty()) {
            Helper.toast(this, R.string.post_content_empty_message);
            return;
        }

        Intent intent = getIntent();
        final String fid = intent.getStringExtra("fid");
        final String tid = intent.getStringExtra("tid");
        if (fid == null && tid == null)
            throw new RuntimeException("fid or tid is required!");

        Discuz.execute(fid != null ? "newthread" : "sendreplay", new HashMap<String, Object>() {{
            if (fid != null) {
                put("fid", fid);
                put("topicsubmit", "yes");
            }
            else {
                put("tid", tid);
                put("replysubmit", "yes");
            }
        }}, new HashMap<String, Object>() {{
            if (fid != null)
                put("subject", title);
            put("message", content);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(NewPostActivity.this, R.string.network_error_toast);
                }
                else {
                    JSONObject message = jsonObject.optJSONObject("Message");
                    String messageval = message.optString("messageval");
                    if ("post_replay_success".equals(messageval) ||
                            "post_newthread_succeed".equals(messageval)) {
                        try {
                            // return tid to parent activity
                            String tid = jsonObject.optJSONObject("Variables").optString("tid");
                            setResult(Integer.parseInt(tid));
                        }
                        catch (NumberFormatException e) {
                            //
                        }
                        finish();
                    }
                    else {
                        new AlertDialog.Builder(NewPostActivity.this)
                                .setTitle("There is sth wrong...")
                                .setMessage(message.optString("messagestr"))
                                .show();
                    }
                }
                mPostButton.setEnabled(true);
            }
        });
        mPostButton.setEnabled(false);
    }

    private EditText mTitle;
    private EditText mContent;
    private MenuItem mPostButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mTitle = (EditText) findViewById(R.id.input_title);
        mContent = (EditText) findViewById(R.id.input_content);
        mTitle.addTextChangedListener(this);
        mContent.addTextChangedListener(this);

        Intent intent = getIntent();
        if (intent.hasExtra("thread_title")) {
            mTitle.setText(intent.getStringExtra("thread_title"));
            mTitle.setEnabled(false);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_post, menu);
        mPostButton = menu.findItem(R.id.action_send);
        mPostButton.setEnabled(false);
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
        else if (id == R.id.action_send) {
            doPost(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        mPostButton.setEnabled(!(mTitle.getText().toString().isEmpty() ||
                mContent.getText().toString().isEmpty()));
    }
}
