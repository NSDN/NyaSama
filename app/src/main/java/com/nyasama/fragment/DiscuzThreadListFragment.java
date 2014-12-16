package com.nyasama.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.activity.PostListActivity;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Thread;
import com.nyasama.util.Discuz.Forum;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by oxyflour on 2014/12/15.
 *
 */
public class DiscuzThreadListFragment extends CommonListFragment<Thread>
    implements CommonListFragment.OnListFragmentInteraction<Thread> {

    public static DiscuzThreadListFragment getNewFragment(int fid, int uid, int pps) {
        DiscuzThreadListFragment fragment = new DiscuzThreadListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_LIST_LAYOUT, R.layout.fragment_simple_list);
        bundle.putInt(ARG_ITEM_LAYOUT, R.layout.fragment_thread_item);
        bundle.putInt(ARG_LIST_VIEW_ID, R.id.list);
        bundle.putInt("fid", fid);
        bundle.putInt("uid", uid);
        if (pps > 0)
            bundle.putInt("pps", pps);
        fragment.setArguments(bundle);
        return fragment;
    }

    public interface OnThreadListInteraction {
        public void onGetThreadData(DiscuzThreadListFragment fragment);
    }

    private OnThreadListInteraction mInteractionInterface;
    private String mTitle;
    public String getTitle() {
        return mTitle;
    }
    private String mMessage;
    public String getMessage() {
        return mMessage;
    }
    private List<Forum> mSubList;
    public List<Forum> getSubList() {
        return mSubList;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThreadListInteraction)
            mInteractionInterface = (OnThreadListInteraction) activity;
        else
            throw new RuntimeException("activity should implement OnThreadInteraction");
    }

    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<Thread>() {
            @Override
            public void convertView(ViewHolder viewHolder, Thread item) {
                viewHolder.setText(R.id.title, Html.fromHtml(item.title));
                viewHolder.setText(R.id.sub,
                        Html.fromHtml(item.author + " " + item.lastpost));
                viewHolder.setText(R.id.inf, item.replies + "/" + item.views);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), PostListActivity.class);
        Thread thread = (Thread) fragment.getData(position);
        intent.putExtra("tid", thread.id);
        intent.putExtra("title", thread.title);
        startActivity(intent);
    }

    @Override
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        Bundle bundle = getArguments();
        final int fid = bundle.getInt("fid", 0);
        final int uid = bundle.getInt("uid", 0);
        final int pps = bundle.getInt("pps", 20);

        final int page = (int) Math.round(Math.floor(listData.size() / pps));
        final int position = page * pps;


        String module = fid > 0 ? "forumdisplay" : (uid > 0 ? "mythread" : "hotthread");
        Discuz.execute(module, new HashMap<String, Object>() {{
            if (fid > 0)
                put("fid", fid);
            else if (uid > 0)
                put("uid", uid);
            put("tpp", pps);
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onResponse(JSONObject data) {
                mMessage = mTitle = null;
                mSubList = null;
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else if (data.has("Message")) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        listData.clear();
                        mMessage = message.getString("messagestr");
                        total = 0;
                    }
                    catch (JSONException e) {
                        Log.e(DiscuzThreadListFragment.class.toString(),
                                "JsonError: Load Thread List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                } else {
                    // remove possible duplicated items
                    if (position < listData.size())
                        listData.subList(position, listData.size()).clear();
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray threads;
                        if (var.opt("forum_threadlist") instanceof JSONArray)
                            threads = var.getJSONArray("forum_threadlist");
                        else if (var.opt("data") instanceof JSONArray)
                            threads = var.getJSONArray("data");
                        else
                            threads = new JSONArray();
                        for (int i = 0; i < threads.length(); i++) {
                            final JSONObject thread = threads.getJSONObject(i);
                            listData.add(new Thread(thread));
                        }

                        if (var.opt("forum") instanceof JSONObject) {
                            JSONObject forum = var.getJSONObject("forum");
                            mTitle = forum.getString("name");
                        }
                        // if we don't know the number of items
                        // just keep loading until there is no more
                        if (threads.length() < pps)
                            total = listData.size();
                        else
                            total = Integer.MAX_VALUE;

                        // save subforums to sublist
                        if (var.opt("sublist") instanceof JSONArray) {
                            JSONArray sublist = var.getJSONArray("sublist");
                            mSubList = new ArrayList<Forum>();
                            if (sublist.length() > 0) {
                                for (int i = 0; i < sublist.length(); i ++) {
                                    final JSONObject subforum = sublist.getJSONObject(i);
                                    mSubList.add(new Forum(subforum));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(DiscuzThreadListFragment.class.toString(),
                                "JsonError: Load Thread List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                loadMoreDone(total);
                mInteractionInterface.onGetThreadData(DiscuzThreadListFragment.this);
            }
        });
    }
}
