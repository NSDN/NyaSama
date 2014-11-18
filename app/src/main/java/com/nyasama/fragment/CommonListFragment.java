package com.nyasama.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
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

    private List<T> mListData = new ArrayList<T>();
    private int mPageSize = 10;
    private int mListItemCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;

    private CommonListAdapter<T> mListAdapter;
    private Activity mActivity;
    private int mListLayoutId;
    private int mItemLayoutId;

    public boolean loadMore() {
        final int loadPage = (int) Math.round(Math.floor(mListData.size() / mPageSize));
        final int loadIndex = loadPage * mPageSize;
        final int currentSize = mListData.size();

        if (mActivity != null && currentSize < mListItemCount && !mIsLoading) {
            ((OnListFragmentInteraction) mActivity).onLoadingMore(loadIndex, loadPage, mListData);
            Helper.updateVisibility(mActivity.findViewById(R.id.loading), mIsLoading = true);
        }
        return mIsLoading;
    }

    public void loadMoreDone(int total) {
        mListItemCount = total;
        mListAdapter.notifyDataSetChanged();
        mIsLoading = false;
        Helper.updateVisibility(mActivity.findViewById(R.id.loading), mListItemCount > mListData.size());
    }

    public boolean reloadAll() {
        mListData.clear();
        mListItemCount = Integer.MAX_VALUE;
        mListAdapter.notifyDataSetChanged();
        return loadMore();
    }

    /*
    public boolean reloadLast() {
        mListItemCount = Integer.MAX_VALUE;
        return loadMore();
    }
    */

    public T getData(int position) {
        return mListData.get(position);
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
            mListLayoutId = bundle.getInt("list_layout");
            mItemLayoutId = bundle.getInt("item_layout");
            mPageSize = bundle.getInt("page_size");
        }
        if (mListLayoutId <= 0 || mItemLayoutId <= 0 || mPageSize <= 0)
            throw new RuntimeException("Incorrect Arguments for ListFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(mListLayoutId, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.list);
        listView.addFooterView(inflater.inflate(R.layout.fragment_list_loading, listView, false), null, false);
        listView.setAdapter(mListAdapter = new CommonListAdapter<T>(mListData, mItemLayoutId) {
            @Override
            @SuppressWarnings("unchecked")
            public void convert(ViewHolder viewHolder, T item) {
                ((OnListFragmentInteraction) mActivity).onConvertView(viewHolder, item);
            }
        });
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(this);
        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ((OnListFragmentInteraction) mActivity).onItemClick(view, i, l);
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
        public void onItemClick(View view, int position, long id);
        public void onConvertView(CommonListAdapter.ViewHolder viewHolder, T item);
        public void onLoadingMore(int position, int page, List data);
    }
}
