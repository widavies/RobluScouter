package com.cpjd.robluscouter.ui.setup;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.robluscouter.R;

public class SetupFragmentAdapter extends PagerAdapter {

    private final Activity activity;

    SetupFragmentAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int resId = 0;

        switch (position) {
            case 0:
                resId = R.id.welcome_page;
                break;
            case 1:
                resId = R.id.bluetooth_setup_page;
                break;
            case 2:
                resId = R.id.permissions_setup_page;
                break;
            case 3:
                resId = R.id.finished;
        }

        return activity.findViewById(resId);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, Object object) {
        // Do nothing
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, Object object) {
        return view == object;
    }
}
