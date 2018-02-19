package com.cpjd.robluscouter.ui.checkouts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import lombok.Getter;

/**
 * Backend for CheckoutsView. manages the multiple CheckoutTabs we have
 *
 * @since 1.0.1
 * @version 3
 * @author Will Davies
 */
public class CheckoutTabAdapter extends FragmentStatePagerAdapter {

    /**
     * The CheckoutTab managing the Checkouts array
     */
    @Getter
    private CheckoutTab checkouts;
    /**
     * The CheckouTab managing the MyCheckouts array
     */
    @Getter
    private CheckoutTab myCheckouts;

    CheckoutTabAdapter(FragmentManager fm) {
        super(fm);

        myCheckouts = new CheckoutTab();
        checkouts = new CheckoutTab();
    }

    @Override
    public Fragment getItem(int i) {
        Bundle b = new Bundle();
        b.putInt("mode", i);
        if(i == 0) {
            myCheckouts.setArguments(b);
            return myCheckouts;
        } else {
            checkouts.setArguments(b);
            return checkouts;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "My checkouts";
        else return "Checkouts";
    }
}
