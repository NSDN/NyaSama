package com.nyasama.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nyasama.R;

/**
 * Created by oxyflour on 2014/11/18.
 *
 */
public class SimpleLayoutFragment extends Fragment {
    private int mLayoutId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null)
            mLayoutId = bundle.getInt("layout_id");
        if (mLayoutId <= 0)
            mLayoutId = R.layout.fragment_blank;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(mLayoutId, container, false);
    }
}
