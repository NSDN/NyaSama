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
import com.nyasama.R;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.FavItem;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class FavListActivity extends AppCompatActivity
    implements CommonListFragment.OnListFragmentInteraction<FavItem> {

    final static int PAGE_SIZE_COUNT = 20;
    public static String TAG = "FavList";

    private CommonListFragment<FavItem> mListFragment;

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
                FavItem.class,
                R.layout.fragment_simple_list,
                R.layout.fragment_fav_item,
                R.id.list);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mListFragment).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fav_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        else if (id == R.id.action_my_profile) {
            startActivity(new Intent(this, UserProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<FavItem>() {
            @Override
            public void convertView(ViewHolder viewHolder, FavItem item) {
                viewHolder.setText(R.id.title, item.title);
                viewHolder.setText(R.id.date, item.dateline);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        FavItem item = mListFragment.getData(position);
        if ("tid".equals(item.type)) {
            Intent intent = new Intent(this, PostListActivity.class);
            intent.putExtra("tid", item.dataId);
            intent.putExtra("title", item.title);
            startActivity(intent);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        final int page = listData.size() / PAGE_SIZE_COUNT;
        Discuz.execute("myfavthread", new HashMap<String, Object>() {{
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else {
                    Helper.setListLength(listData, page * PAGE_SIZE_COUNT);
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray list = var.getJSONArray("list");
                        for (int i = 0; i < list.length(); i++) {
                            listData.add(new FavItem(list.getJSONObject(i)));
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
