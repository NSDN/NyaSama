package com.nyasama.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.nyasama.R;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oxyflour on 2014/11/18.
 *
 */
public class CommonListFragment<T> extends Fragment
     implements AbsListView.OnScrollListener, AbsListView.OnItemClickListener {

    public static final String ARG_LIST_LAYOUT = "list_layout_id";
    public static final String ARG_LIST_VIEW_ID = "list_view_id";
    public static final String ARG_ITEM_LAYOUT = "item_layout_id";
    public static final String ARG_PAGE_SIZE = "page_size";

    private List<T> mListData = new ArrayList<T>();
    private int mPageSize = 10;
    private int mListItemCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;

    private CommonListAdapter<T> mListAdapter;
    private View mListLayoutView;
    private Activity mActivity;
    private int mListLayout;
    private int mItemLayout;
    private int mListViewId;

    public boolean loadMore() {
        final int loadPage = (int) Math.round(Math.floor(mListData.size() / mPageSize));
        final int loadIndex = loadPage * mPageSize;
        final int currentSize = mListData.size();

        if (mActivity != null && mListLayoutView != null &&
                currentSize < mListItemCount && !mIsLoading) {
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.loading),
                    mIsLoading = true);
            ((OnListFragmentInteraction) mActivity)
                    .onLoadingMore(this, loadIndex, loadPage, mListData);
        }
        return mIsLoading;
    }

    public void loadMoreDone(int total) {
        mListItemCount = total;
        mIsLoading = false;
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
        if (mListLayoutView != null) {
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.empty),
                    total <= 0);
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.loading),
                    mListItemCount > mListData.size());
        }
    }

    public boolean reloadAll() {
        mListData.clear();
        mListItemCount = Integer.MAX_VALUE;
        mListAdapter.notifyDataSetChanged();
        return loadMore();
    }

    public boolean reloadLast() {
        mListItemCount = Integer.MAX_VALUE;
        return loadMore();
    }

    public T getData(int position) {
        return mListData.get(position);
    }

    public int getIndex(T data) {
        return mListData.indexOf(data);
    }

    public CommonListAdapter getListAdapter() {
        return mListAdapter;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        loadMore();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mListLayout = bundle.getInt(ARG_LIST_LAYOUT);
            mItemLayout = bundle.getInt(ARG_ITEM_LAYOUT);
            mListViewId = bundle.getInt(ARG_LIST_VIEW_ID);
            mPageSize = bundle.getInt(ARG_PAGE_SIZE);
        }
        if (mListLayout <= 0 || mItemLayout <= 0 || mListViewId <=0 || mPageSize <= 0)
            throw new RuntimeException("Invalid Arguments for ListFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListLayoutView = inflater.inflate(mListLayout, container, false);
        AbsListView listView = (AbsListView) mListLayoutView.findViewById(mListViewId);
        if (listView instanceof ListView) {
            View loading = inflater.inflate(R.layout.fragment_list_loading, listView, false);
            ((ListView) listView).addFooterView(loading, null, false);
        }
        listView.setAdapter(mListAdapter = new CommonListAdapter<T>(mListData, mItemLayout) {
            @Override
            @SuppressWarnings("unchecked")
            public void convert(ViewHolder viewHolder, T item) {
                ((OnListFragmentInteraction) mActivity)
                        .onConvertView(CommonListFragment.this, viewHolder, item);
            }
        });
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
        return mListLayoutView;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ((OnListFragmentInteraction) mActivity).onItemClick(this, view, i, l);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        if (i + i2 >= i3)
            loadMore();
    }

    public interface OnListFragmentInteraction<T> {
        public void onItemClick(CommonListFragment fragment,
                                View view, int position, long id);
        public void onConvertView(CommonListFragment fragment,
                                  CommonListAdapter.ViewHolder viewHolder, T item);
        public void onLoadingMore(CommonListFragment fragment,
                                  int position, int page, List data);
    }
}
