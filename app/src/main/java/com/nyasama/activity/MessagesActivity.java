package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.PMList;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class MessagesActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<PMList> {

    // DO NOT CHANGE PAGE_SIZE_COUNT
    public static int PAGE_SIZE_COUNT = 10;
    public static String TAG = "PMList";

    private CommonListFragment<PMList> mListFragment;

    private int mPMId;
    private int mPage = Integer.MAX_VALUE;
    private int mCount = Integer.MAX_VALUE;
    private AlertDialog mReplyDialog;

    public void doSendMessage(final String text) {
        Discuz.execute("sendpm", new HashMap<String, Object>() {{
            put("pmid", mPMId);
            put("touid", getIntent().getIntExtra("touid", 0));
        }}, new HashMap<String, Object>() {{
            put("message", text);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    JSONObject message = data.optJSONObject("Message");
                    String messageval = message.optString("messageval");
                    if ("do_success".equals(messageval)) {
                        JSONObject var = data.optJSONObject("Variables");
                        mPMId = Integer.parseInt(var.optString("pmid"));
                        mListFragment.reloadAll();
                    }
                    else
                        Helper.toast(message.optString("messagestr"));
                }
                if (mReplyDialog != null && mReplyDialog.isShowing())
                    mReplyDialog.dismiss();
            }
        });
    }

    public void sendMessage() {
        final EditText input = new EditText(this);
        mReplyDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.diag_quick_reply_title)
                .setMessage(R.string.diag_hint_type_something)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mReplyDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                mReplyDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String text = input.getText().toString();
                        if (!text.isEmpty()) {
                            Helper.disableDialog(mReplyDialog);
                            doSendMessage(text);
                        }
                    }
                });
            }
        });
        mReplyDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent().getIntExtra("touid", 0) == 0)
            throw new RuntimeException("user id is required to view messsages!");

        mListFragment = CommonListFragment.getNewFragment(PMList.class,
                R.layout.fragment_post_list,
                R.layout.fragment_pm_item,
                R.id.list, PAGE_SIZE_COUNT);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mListFragment).commit();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_messages, menu);
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
        else if (id == R.id.action_reply) {
            sendMessage();
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {

    }

    @Override
    public void onConvertView(CommonListFragment fragment, CommonListAdapter.ViewHolder viewHolder, PMList item) {

        int avatar_id = item.authorId == Discuz.sUid ?
                R.id.self_user_avatar : R.id.other_user_avatar;
        String avatar_url = Discuz.DISCUZ_URL +
                "uc_server/avatar.php?uid="+item.authorId+"&size=small";
        ((NetworkImageView) viewHolder.getView(avatar_id))
                .setImageUrl(avatar_url, ThisApp.imageLoader);
        int message_id = item.authorId == Discuz.sUid ?
                R.id.self_message : R.id.other_message;
        viewHolder.setText(message_id, item.message);

        viewHolder.getView(R.id.self_message).setVisibility(item.authorId == Discuz.sUid ? View.VISIBLE : View.INVISIBLE);
        viewHolder.getView(R.id.self_user_avatar).setVisibility(item.authorId == Discuz.sUid ? View.VISIBLE : View.INVISIBLE);
        viewHolder.getView(R.id.other_message).setVisibility(item.authorId != Discuz.sUid ? View.VISIBLE : View.INVISIBLE);
        viewHolder.getView(R.id.other_user_avatar).setVisibility(item.authorId != Discuz.sUid ? View.VISIBLE : View.INVISIBLE);

    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final int position, final int page, final List listData) {
        Discuz.execute("mypm", new HashMap<String, Object>() {{
            // we don't have to set 'page' when viewing the last page
            if (listData.size() > 0)
                put("page", mPage - 1);
            put("touid", getIntent().getIntExtra("touid", Discuz.sUid));
            put("subop", "view");
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else {
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray list = var.getJSONArray("list");
                        for (int i = list.length() - 1; i >= 0; i --)
                            listData.add(new PMList(list.getJSONObject(i)));

                        // we need pmid to reply
                        if (var.has("pmid"))
                            mPMId = Integer.parseInt(var.getString("pmid"));
                        if (var.has("count"))
                            mCount = Integer.parseInt(var.getString("count"));
                        if (var.has("page"))
                            mPage = Integer.parseInt(var.getString("page"));
                        total = mPage == 1 ? listData.size() : mCount;

                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Message List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }
}
