package com.nyasama.activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.Response;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Notice;
import com.nyasama.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NoticeActivity extends BaseThemedActivity
    implements CommonListFragment.OnListFragmentInteraction<Notice> {

    public static String TAG = "Notice";

    private CommonListFragment<Notice> mListFragment;
    private boolean mShowRead = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_framelayout);

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
        ArrayAdapter spinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                R.layout.fragment_spinner_item_2, android.R.id.text1, spinnerText) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text2)).setText(getTitle());
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                mShowRead = i != 0;

                mListFragment = CommonListFragment.getNewFragment(
                        Notice.class,
                        R.layout.fragment_simple_list,
                        R.layout.fragment_notice_item,
                        R.id.list);

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
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<Notice>() {
            @Override
            public void convertView(ViewHolder viewHolder, Notice item) {
                viewHolder.setText(R.id.date, item.dateline);
                Spannable note = (Spannable) Html.fromHtml(item.note);
                note = (Spannable) Helper.setSpanClickListener(note, URLSpan.class, new Helper.OnSpanClickListener() {
                    @Override
                    public boolean onClick(View widget, String data) {
                        final Uri uri = Uri.parse(data);
                        String mod = uri.getQueryParameter("mod");
                        // TODO: complete these actions
                        if ("space".equals(mod)) {
                            startActivity(new Intent(NoticeActivity.this, UserProfileActivity.class) {{
                                putExtra("uid", Helper.toSafeInteger(uri.getQueryParameter("uid"), 0));
                            }});
                            return true;
                        }
                        else if ("viewthread".equals(mod)) {
                            startActivity(new Intent(NoticeActivity.this, PostListActivity.class) {{
                                putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("tid"), 0));
                            }});
                            return true;
                        }
                        else if ("redirect".equals(mod)) {
                            String go = uri.getQueryParameter("goto");
                            if (go.equals("findpost")) {
                                startActivity(new Intent(NoticeActivity.this, PostListActivity.class) {{
                                    putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("ptid"), 0));
                                }});
                                return true;
                            }
                        }
                        else if ("spacecp".equals(mod) &&
                                "friend".equals(uri.getQueryParameter("ac")) &&
                                "add".equals(uri.getQueryParameter("op"))) {
                            final AlertDialog dialog = new AccentAlertDialog.Builder(NoticeActivity.this)
                                    .setMessage(R.string.list_loading_text)
                                    .show();
                            final int uid = Helper.toSafeInteger(uri.getQueryParameter("uid"), 0);
                            Discuz.execute("friendcp", new HashMap<String, Object>() {{
                                put("op", "add");
                                put("uid", uid);
                            }}, new HashMap<String, Object>() {{
                                // gid = 1 means "friends known from this forum"
                                // TODO: make this changable
                                put("gid", 1);
                            }}, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject data) {
                                    if (data.has("Message")) {
                                        JSONObject message = data.optJSONObject("Message");
                                        Helper.toast(message.optString("messagestr"));
                                    }
                                    else {
                                        Helper.toast(R.string.there_is_something_wrong);
                                    }
                                    dialog.dismiss();
                                }
                            });
                            return true;
                        }
                        return false;
                    }
                });

                TextView noteText = (TextView) viewHolder.getView(R.id.note);
                noteText.setMovementMethod(LinkMovementMethod.getInstance());
                noteText.setText(note);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        // do nothing
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
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
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        if (var.opt("list") instanceof JSONObject) {
                            JSONObject list = var.getJSONObject("list");
                            for (Iterator<String> iter = list.keys(); iter.hasNext(); ) {
                                String key = iter.next();
                                Notice notice = new Notice(list.getJSONObject(key));
                                listData.add(notice);
                            }
                            // sort by id
                            Collections.sort(listData, new Comparator() {
                                @Override
                                public int compare(Object o, Object o2) {
                                    return ((Notice) o).id - ((Notice) o2).id;
                                }
                            });
                        }

                        // No pager
                        total = listData.size();

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
