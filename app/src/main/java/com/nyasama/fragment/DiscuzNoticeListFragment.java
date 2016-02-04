package com.nyasama.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Response;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.NoticeActivity;
import com.nyasama.activity.PostListActivity;
import com.nyasama.activity.UserProfileActivity;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by oxyflour on 2016/2/4.
 *
 */
public class DiscuzNoticeListFragment extends CommonListFragment<Thread>
    implements CommonListFragment.OnListFragmentInteraction<Thread> {

    public static String TAG = "NoticeList";

    public static DiscuzNoticeListFragment getNewFragment(String noticeView, String noticeType) {
        DiscuzNoticeListFragment fragment = new DiscuzNoticeListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_LIST_LAYOUT, R.layout.fragment_simple_list);
        bundle.putInt(ARG_ITEM_LAYOUT, R.layout.fragment_notice_item);
        bundle.putString("view", noticeView);
        bundle.putString("type", noticeType);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<Discuz.Notice>() {
            @Override
            public void convertView(ViewHolder viewHolder, Discuz.Notice item) {
                viewHolder.setText(R.id.date, item.dateline);
                Spannable note = (Spannable) Html.fromHtml(item.note);
                note = (Spannable) Helper.setSpanClickListener(note, URLSpan.class, new Helper.OnSpanClickListener() {
                    @Override
                    public boolean onClick(View widget, String data) {
                        final Uri uri = Uri.parse(data);
                        String mod = uri.getQueryParameter("mod");
                        // TODO: complete these actions
                        if ("space".equals(mod)) {
                            startActivity(new Intent(ThisApp.context, UserProfileActivity.class) {{
                                putExtra("uid", Helper.toSafeInteger(uri.getQueryParameter("uid"), 0));
                            }});
                            return true;
                        } else if ("viewthread".equals(mod)) {
                            startActivity(new Intent(ThisApp.context, PostListActivity.class) {{
                                putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("tid"), 0));
                            }});
                            return true;
                        } else if ("redirect".equals(mod)) {
                            String go = uri.getQueryParameter("goto");
                            if (go.equals("findpost")) {
                                startActivity(new Intent(ThisApp.context, PostListActivity.class) {{
                                    putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("ptid"), 0));
                                }});
                                return true;
                            }
                        } else if ("spacecp".equals(mod) &&
                                "friend".equals(uri.getQueryParameter("ac")) &&
                                "add".equals(uri.getQueryParameter("op"))) {
                            final AlertDialog dialog = new AccentAlertDialog.Builder(getActivity())
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
                                    } else {
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
    }

    @Override
    public void onLoadingMore(final CommonListFragment fragment, final List listData) {
        final Bundle bundle = getArguments();
        Discuz.execute("profile", new HashMap<String, Object>() {{
            put("do", "notice");
            put("view", bundle.getString("view"));
            put("type", bundle.getString("type"));
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
                                Discuz.Notice notice = new Discuz.Notice(list.getJSONObject(key));
                                listData.add(notice);
                            }
                            // sort by id
                            Collections.sort(listData, new Comparator() {
                                @Override
                                public int compare(Object o, Object o2) {
                                    return ((Discuz.Notice) o).id - ((Discuz.Notice) o2).id;
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
                fragment.loadMoreDone(total);
            }
        });
    }
}
