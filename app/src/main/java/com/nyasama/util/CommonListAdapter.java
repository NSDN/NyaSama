package com.nyasama.util;

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
 * 
 * commonListAdapter 是为各种ListView 准备的适配器，也是一个通用工具
 * 因此在这里更多的考虑的是通用性而非具体实现，用例应该有不少，比如DiscuzForumIndexFragment 中的用例
 * 也可以把这个类做成接口啦
 */
public abstract class CommonListAdapter<T> extends BaseAdapter {

    protected List<T> mList;
    protected int mLayout;
//构造函数区
    public CommonListAdapter() {
    }

    public CommonListAdapter(List<T> list, int layout) {
        setup(list, layout);
    }

    public void setup(List<T> list, int layout) {
        mList = list;
        mLayout = layout;
    }
//构造函数区

//基本（酱油）函数区
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
//基本（酱油）函数区

//重点函数区
/*
Adapter 的文档：http://developer.android.com/reference/android/widget/Adapter.html
adapter 的工作原理是：
自动创建用户能看见的View以及接下来可能会看见的View
当adapter 创建View 时，就会自动调用getView 函数
比较值得说的时第二个参数，convertView 是被用户移到视窗外的View
因此可以拿来，装上新的数据重用
*/
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
//这个有用过，应该用不着加 @SuppressWarnings("unused") 了
    @SuppressWarnings("unused")
    public View createView(ViewGroup parent, int position) {
        return LayoutInflater.from(parent.getContext())
                .inflate(mLayout, parent, false);
    }

//重写这个函数，能改变convertView中的内容
    public abstract void convertView(ViewHolder viewHolder, T item);

//这个类一方面管理convertView中的组件，另一方面也是为了提高效率
    public class ViewHolder {
//SparseArray 是比较推荐存储方式
//文档见：https://developer.android.com/training/articles/memory.html 中的 Use optimized data containers 节
        private final SparseArray<View> mViews;
        private final View mConvertView;

        public ViewHolder(View convertView) {
            mViews = new SparseArray<View>();
            mConvertView = convertView;
        }

        public View getConvertView() {
            return mConvertView;
        }
//findViewById 是比较浪费时间的操作，因此创建的时候放入Array，以后从Array 中找比较快
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
