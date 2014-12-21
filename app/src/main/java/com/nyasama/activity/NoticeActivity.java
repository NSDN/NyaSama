package com.nyasama.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.CommonListAdapter;
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
        return new CommonListAdapter<Notice>() {
            @Override
            public void convertView(ViewHolder viewHolder, Notice item) {
                viewHolder.setText(R.id.date, item.dateline);
                Spannable note = (Spannable) Html.fromHtml(item.note);
                URLSpan[] urls = note.getSpans(0, note.length(), URLSpan.class);

                SpannableStringBuilder text = new SpannableStringBuilder(note.toString());
                for (URLSpan url : urls) {
                    String urlString = url.getURL();
                    final Uri uri = Uri.parse(urlString);
                    String mod = uri.getQueryParameter("mod");
                    CharacterStyle newUrl = null;
                    if ("space".equals(mod)) {
                        newUrl = new URLSpan(urlString) {
                            @Override
                            public void onClick(@Nullable View widget) {
                                startActivity(new Intent(NoticeActivity.this, UserProfileActivity.class) {{
                                    putExtra("uid", Helper.toSafeInteger(uri.getQueryParameter("uid"), 0));
                                }});
                            }
                        };
                    }
                    else if ("viewthread".equals(mod)) {
                        newUrl = new URLSpan(urlString) {
                            @Override
                            public void onClick(@Nullable View widget) {
                                startActivity(new Intent(NoticeActivity.this, PostListActivity.class) {{
                                    putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("tid"), 0));
                                }});
                            }
                        };
                    }
                    else if ("redirect".equals(mod)) {
                        String go = uri.getQueryParameter("goto");
                        if (go.equals("findpost")) {
                            newUrl = new URLSpan(urlString) {
                                @Override
                                public void onClick(@Nullable View widget) {
                                    startActivity(new Intent(NoticeActivity.this, PostListActivity.class) {{
                                        putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("ptid"), 0));
                                    }});
                                }
                            };
                        }
                        else {
                            newUrl = new ForegroundColorSpan(android.R.color.transparent);
                        }
                    }
                    if (newUrl != null) text.setSpan(newUrl, note.getSpanStart(url), note.getSpanEnd(url),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                TextView noteText = (TextView) viewHolder.getView(R.id.note);
                noteText.setMovementMethod(LinkMovementMethod.getInstance());
                noteText.setText(text);
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
