package com.nyasama.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

public class PMListActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<PMList> {

    public static int PAGE_SIZE_COUNT = 20;
    public static String TAG = "PMList";

    private CommonListFragment<PMList> mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pm_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        mListFragment = CommonListFragment.getNewFragment(
                PMList.class,
                // TODO: replace borrowed fragment_post_list
                R.layout.fragment_post_list,
                R.layout.fragment_pmlist_item,
                R.id.list);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mListFragment).commit();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pm_list, menu);
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

    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<PMList>() {
            @Override
            public void convertView(ViewHolder viewHolder, PMList item) {
                int uid = item.fromUserId != Discuz.sUid ? item.fromUserId : item.toUserId;
                String username = item.fromUserId != Discuz.sUid ? item.fromUser : item.toUser;

                String avatar_url = Discuz.DISCUZ_URL +
                        "uc_server/avatar.php?uid=" + uid + "&size=small";
                ((NetworkImageView) viewHolder.getView(R.id.avatar))
                        .setImageUrl(avatar_url, ThisApp.imageLoader);

                viewHolder.setText(R.id.author, username + " (" + item.number + ")");
                viewHolder.setText(R.id.message, item.message);
                viewHolder.setText(R.id.date, item.lastdate);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("touid", ((PMList) fragment.getData(position)).toUserId);
        startActivity(intent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        final int page = (int) Math.round(Math.floor(listData.size() / PAGE_SIZE_COUNT));
        final int position = page * PAGE_SIZE_COUNT;
        Discuz.execute("mypm", new HashMap<String, Object>() {{
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else {
                    // remove possible duplicated items
                    if (position < listData.size())
                        listData.subList(position, listData.size()).clear();
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray list = var.getJSONArray("list");
                        for (int i = 0; i < list.length(); i ++) {
                            listData.add(new PMList(list.getJSONObject(i)));
                        }

                        if (list.length() < PAGE_SIZE_COUNT)
                            total = listData.size();
                        else
                            total = Integer.MAX_VALUE;

                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load PM Lists Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }
}
