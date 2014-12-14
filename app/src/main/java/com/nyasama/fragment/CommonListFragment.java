package com.nyasama.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
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
     implements AbsListView.OnScrollListener {

    public static final String ARG_LIST_LAYOUT = "list_layout_id";
    public static final String ARG_LIST_VIEW_ID = "list_view_id";
    public static final String ARG_ITEM_LAYOUT = "item_layout_id";

    private List<T> mListData = new ArrayList<T>();
    private int mListItemCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;
    private boolean mHasError = false;

    private CommonListAdapter<T> mListAdapter;
    private View mListLayoutView;
    private OnListFragmentInteraction<T> mInteractionInterface;

    protected int mListLayout;
    protected int mItemLayout;
    protected int mListViewId = R.id.list;

    @SuppressWarnings("unchecked unused")
    public static <T> CommonListFragment<T> getNewFragment(Class<T> c, int listLayout, int itemLayout, int listViewId) {
        Bundle bundle = new Bundle();
        bundle.putInt(CommonListFragment.ARG_LIST_LAYOUT, listLayout);
        bundle.putInt(CommonListFragment.ARG_ITEM_LAYOUT, itemLayout);
        bundle.putInt(CommonListFragment.ARG_LIST_VIEW_ID, listViewId);
        CommonListFragment<T> fragment = new CommonListFragment<T>();
        fragment.setArguments(bundle);
        // init mListLayout etc
        fragment.loadArguments(bundle);
        return fragment;
    }

    public boolean loadMore() {
        final int currentSize = mListData.size();

        if (mListLayoutView != null &&
                currentSize < mListItemCount && !mIsLoading) {
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.loading),
                    mIsLoading = true);
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.error),
                    mHasError = false);
            if (mInteractionInterface != null)
                mInteractionInterface.onLoadingMore(this, mListData);
        }
        return mIsLoading;
    }

    public void loadMoreDone(int total) {
        mListItemCount = total;
        if (mHasError = total < 0) {
            mListItemCount = 0;
            mListData.clear();
            Log.e("ListFragment", "load failed");
        }

        mIsLoading = false;
        mListAdapter.notifyDataSetChanged();
        if (mListLayoutView != null) {
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.empty),
                    mListItemCount == 0 && !mHasError);
            Helper.updateVisibility(mListLayoutView.findViewById(R.id.error),
                    mHasError);
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

    public void loadArguments(Bundle bundle) {
        mListLayout = bundle.getInt(ARG_LIST_LAYOUT);
        mItemLayout = bundle.getInt(ARG_ITEM_LAYOUT);
        mListViewId = bundle.getInt(ARG_LIST_VIEW_ID);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (this instanceof OnListFragmentInteraction)
            mInteractionInterface = (OnListFragmentInteraction) this;
        else if (activity instanceof OnListFragmentInteraction)
            mInteractionInterface = (OnListFragmentInteraction) activity;
        else
            throw new RuntimeException("you should implement OnListFragmentInteraction on activity or subclass");
        loadMore();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null)
            loadArguments(bundle);
    }

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListLayoutView = inflater.inflate(mListLayout, container, false);

        AbsListView listView = (AbsListView) mListLayoutView.findViewById(mListViewId);
        if (listView instanceof ListView) {
            View loading = inflater.inflate(R.layout.fragment_list_loading, listView, false);
            ((ListView) listView).addFooterView(loading, null, false);
        }
        // the loading view should initial hidden
        Helper.updateVisibility(mListLayoutView.findViewById(R.id.loading), false);

        mListAdapter = mInteractionInterface.getListViewAdaptor(this);
        mListAdapter.setup(mListData, mItemLayout);
        listView.setAdapter(mListAdapter);
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mInteractionInterface.onItemClick(CommonListFragment.this, view, i, l);
            }
        });

        Button reloadButton = (Button) mListLayoutView.findViewById(R.id.reload);
        if (reloadButton != null) reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadAll();
            }
        });

        return mListLayoutView;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        if (i + i2 >= i3)
            loadMore();
    }

    @SuppressWarnings("unused")
    public interface OnListFragmentInteraction<T> {
        public CommonListAdapter getListViewAdaptor(CommonListFragment fragment);
        public void onItemClick(CommonListFragment fragment,
                                View view, int position, long id);
        public void onLoadingMore(CommonListFragment fragment, List data);
    }
}
