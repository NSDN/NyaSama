package com.nyasama.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Notice;
import com.nyasama.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NoticeActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<Notice> {

    public static int PAGE_SIZE_COUNT = 20;
    public static String TAG = "Notice";

    private CommonListFragment<Notice> mListFragment;
    private boolean mShowRead = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        ActionBar actionBar = getActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        String spinnerText[] = {
                getString(R.string.title_notice_unread),
                getString(R.string.title_notice_read),
        };
        SpinnerAdapter spinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                android.R.layout.simple_spinner_dropdown_item, spinnerText);
        actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                mShowRead = i != 0;

                mListFragment = CommonListFragment.getNewFragment(
                        Notice.class,
                        R.layout.fragment_post_list,
                        R.layout.fragment_notice_item,
                        R.id.list, PAGE_SIZE_COUNT);
                mListFragment.setListAdapter(new CommonListAdapter<Notice>() {
                    @Override
                    public void convertView(ViewHolder viewHolder, Notice item) {
                        viewHolder.setText(R.id.note, Html.fromHtml(item.note));
                    }
                });

                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, mListFragment)
                    .commit();
                return true;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_notice, menu);
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
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        // TODO:
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final int position, int page, final List listData) {
        Discuz.execute("profile", new HashMap<String, Object>() {{
            put("do", "notice");
            if (mShowRead)
                put("isread", 1);
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

                        int len = 0;
                        if (var.opt("list") instanceof JSONObject) {
                            JSONObject list = var.getJSONObject("list");
                            for (Iterator<String> iter = list.keys(); iter.hasNext(); ) {
                                String key = iter.next();
                                listData.add(new Notice(list.getJSONObject(key)));
                                len ++;
                            }
                        }

                        if (len < PAGE_SIZE_COUNT)
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
