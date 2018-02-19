package com.cpjd.robluscouter.ui.setup;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class DisableSwipeViewPager extends ViewPager {

    private boolean swipeEnabled = false;

    public DisableSwipeViewPager(Context context) {
        super(context);
    }

    public DisableSwipeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return swipeEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = swipeEnabled && super.onTouchEvent(event);
        if(result) performClick();
        return result;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void goToNextPage() {
        if(getAdapter() == null) {
            Log.d("RSBS", "Setup adapter is not available");
            return;
        }
        if (getCurrentItem() < getAdapter().getCount() - 1) {
            setCurrentItem(getCurrentItem() + 1);
        }
    }

    public void goToPreviousPage() {
        if (getCurrentItem() > 0) {
            setCurrentItem(getCurrentItem() - 1);
        }
    }
}