package com.nyasama.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.PMList;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class PMListActivity extends AppCompatActivity
    implements CommonListFragment.OnListFragmentInteraction<PMList> {

    public static int PAGE_SIZE_COUNT = 20;
    public static String TAG = "PMList";

    private CommonListFragment<PMList> mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_framelayout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_action_nya);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                finish();
            }
        });

        mListFragment = CommonListFragment.getNewFragment(
                PMList.class,
                R.layout.fragment_simple_list,
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
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
        final int page = listData.size() / PAGE_SIZE_COUNT;
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
                    Helper.setListLength(listData, page * PAGE_SIZE_COUNT);
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray list = var.getJSONArray("list");
                        for (int i = 0; i < list.length(); i ++) {
                            listData.add(new PMList(list.getJSONObject(i)));
                        }

                        total = Helper.toSafeInteger(var.getString("count"), listData.size());

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
