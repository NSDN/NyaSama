package com.nyasama.adapter;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Oxyflour on 2014/11/13.
 * REF: http://blog.csdn.net/lmj623565791/article/details/38902805
 */
public abstract class CommonListAdapter<T> extends BaseAdapter {

    protected List<T> mList;
    protected int mLayout;

    public CommonListAdapter() {
    }

    public CommonListAdapter(List<T> list, int layout) {
        setup(list, layout);
    }

    public void setup(List<T> list, int layout) {
        mList = list;
        mLayout = layout;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public T getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = createView(parent, position);
            convertView.setTag(new ViewHolder(convertView));
        }
        convertView((ViewHolder) convertView.getTag(), getItem(position));
        return convertView;
    }

    @SuppressWarnings("unused")
    public View createView(ViewGroup parent, int position) {
        return LayoutInflater.from(parent.getContext())
                .inflate(mLayout, parent, false);
    }

    public abstract void convertView(ViewHolder viewHolder, T item);

    public class ViewHolder {

        private final SparseArray<View> mViews;
        private final View mConvertView;

        public ViewHolder(View convertView) {
            mViews = new SparseArray<View>();
            mConvertView = convertView;
        }

        public View getConvertView() {
            return mConvertView;
        }

        public View getView(int viewId) {
            View view = mViews.get(viewId);
            if (view == null) {
                view = mConvertView.findViewById(viewId);
                mViews.put(viewId, view);
            }
            return view;
        }

        public void setText(int viewId, CharSequence text) {
            TextView view = (TextView) getView(viewId);
            if (view != null)
                view.setText(text);
        }
    }
}
