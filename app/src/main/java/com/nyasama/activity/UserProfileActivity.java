package com.nyasama.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class UserProfileActivity extends Activity {

    final static String TAG = "UserProfile";

    public void doLogout(View view) {
        Discuz.logout(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                finish();
            }
        });
    }

    public void doShowThreads(View view) {
        Intent intent = new Intent(this, ThreadListActivity.class);
        intent.putExtra("uid", getIntent().getIntExtra("uid", Discuz.sUid));
        startActivity(intent);
    }

    public void doShowFavs(View view) {
        Intent intent = new Intent(this, FavListActivity.class);
        startActivity(intent);
    }

    public void doShowNotice(View view) {
        Intent intent = new Intent(this, NoticeActivity.class);
        startActivity(intent);
    }

    public void doShowMessages(View view) {
        startActivity(new Intent(this, PMListActivity.class));
    }

    public void doSendMessage(View view) {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("touid", getIntent().getIntExtra("uid", Discuz.sUid));
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        final int uid = getIntent().getIntExtra("uid", Discuz.sUid);
        String avatar_url = Discuz.DISCUZ_URL +
                "uc_server/avatar.php?uid="+uid+"&size=medium";
        ((NetworkImageView) findViewById(R.id.avatar))
                .setImageUrl(avatar_url, ThisApp.imageLoader);
        Helper.updateVisibility(findViewById(R.id.hide_for_others), uid == Discuz.sUid);
        Helper.updateVisibility(findViewById(R.id.show_for_others), uid != Discuz.sUid);
        Discuz.execute("profile", new HashMap<String, Object>() {{
            put("uid", uid);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        ((TextView) findViewById(R.id.username))
                                .setText(var.optJSONObject("space").optString("username"));
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Load User profile Failed: " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
