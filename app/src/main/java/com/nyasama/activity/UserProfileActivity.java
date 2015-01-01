package com.nyasama.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.HashMap;

public class UserProfileActivity extends Activity {

    public void doLogout(View view) {
        Discuz.logout(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                finish();
            }
        });
    }

    public void doSignin(View view) {
        findViewById(R.id.signin_button).setEnabled(false);
        Discuz.signin(new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                findViewById(R.id.signin_button).setEnabled(true);
                Helper.toast(s == null ? getString(R.string.there_is_something_wrong) : s);
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
        Helper.updateVisibility(findViewById(R.id.signin_button), uid == Discuz.sUid);
        Helper.updateVisibility(findViewById(R.id.show_for_others), uid != Discuz.sUid);
        Helper.updateVisibility(findViewById(R.id.hide_when_loading), false);
        Discuz.execute("profile", new HashMap<String, Object>() {{
            put("uid", uid);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.has("Variables")) {
                    Helper.updateVisibility(findViewById(R.id.hide_when_loading), true);

                    JSONObject var = data.optJSONObject("Variables");
                    JSONObject space = var.optJSONObject("space");
                    if (space != null) {
                        ((TextView) findViewById(R.id.username))
                                .setText(space.optString("username"));
                        JSONObject group = space.optJSONObject("group");
                        ((TextView) findViewById(R.id.groupname))
                                .setText(group.optString("grouptitle"));

                        ((TextView) findViewById(R.id.my_threads))
                                .setText(getString(R.string.mythread_button_text) + " " + space.optString("threads"));

                        String gender = space.optString("gender");
                        ((TextView) findViewById(R.id.user_gender))
                                .setText(getString(R.string.gender) + "  " +
                                        getString("1".equals(gender) ? R.string.gender_male :
                                                ("2".equals(gender) ? R.string.gender_femal : R.string.unknown)));
                        ((TextView) findViewById(R.id.user_reside))
                                .setText(getString(R.string.reside) + "  " +
                                        space.optString("resideprovince") + " " + space.optString("residecity") + " " +
                                        space.optString("residedist"));
                        ((TextView) findViewById(R.id.user_likes))
                                .setText(getString(R.string.like) + "  " + space.optString("company") + " " +
                                        space.optString("occupation") + " " + space.optString("position"));
                        TextView userSite = (TextView) findViewById(R.id.user_site);
                        String siteUrl = space.optString("site");
                        userSite.setText(Html.fromHtml(getString(R.string.site) + "  " +
                                (URLUtil.isValidUrl(siteUrl) ? "<a href=\"" + siteUrl + "\">" + siteUrl + "</a>" : siteUrl)));
                        userSite.setMovementMethod(LinkMovementMethod.getInstance());

                        ((TextView) findViewById(R.id.last_activity))
                                .setText(getString(R.string.last_activity) + "  " + space.optString("lastactivity"));
                        ((TextView) findViewById(R.id.last_post))
                                .setText(getString(R.string.last_post) + "  " + space.optString("lastpost"));
                        ((TextView) findViewById(R.id.last_visit))
                                .setText(getString(R.string.last_visit) + "  " + space.optString("lastvisit"));

                        String value;
                        value = space.optString("newpm");
                        ((TextView) findViewById(R.id.my_messages))
                                .setText(getString(R.string.my_message_button) + " " +
                                        ("0".equals(value) ? "" : value));
                        value = space.optString("newprompt");
                        ((TextView) findViewById(R.id.my_notice))
                                .setText(getString(R.string.my_notice_button) + " " +
                                        ("0".equals(value) ? "" : value));
                    }

                    JSONObject extcredits = var.optJSONObject("extcredits");
                    if (extcredits != null) {
                        JSONObject credit = extcredits.optJSONObject("1");
                        String value = space != null ? space.optString("extcredits1") : "";
                        ((TextView) findViewById(R.id.credit))
                                .setText(credit.optString("title") + ": " +
                                        Helper.toSafeInteger(value, 0) + credit.optString("unit"));

                        int[] creditsView = {
                                2, R.id.user_points2, R.id.user_points2,
                                3, R.id.user_points3, R.id.user_points3,
                                4, R.id.user_points4, R.id.user_points4,
                                6, R.id.user_points5, R.id.user_points5,
                        };
                        for (int i = 0; i < creditsView.length; i += 3) {
                            TextView text = (TextView) findViewById(creditsView[i+1]);
                            credit = extcredits.optJSONObject("" + creditsView[i]);
                            if (text != null && credit != null) {
                                value = space != null ? space.optString("extcredits" + creditsView[i]) : "";
                                text.setText(credit.optString("title") + "\n" +
                                        Helper.toSafeInteger(value, 0) + credit.optString("unit"));
                            }
                        }
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
        return Helper.handleOption(this, item.getItemId()) ||
                super.onOptionsItemSelected(item);
    }
}
