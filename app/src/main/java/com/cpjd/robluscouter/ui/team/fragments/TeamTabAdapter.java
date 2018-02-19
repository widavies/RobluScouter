package com.cpjd.robluscouter.ui.team.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RForm;
import com.cpjd.robluscouter.ui.team.TeamViewer;

/**
 * Handles the tabs for an RTab model
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 */
public class TeamTabAdapter extends FragmentStatePagerAdapter {

    private RCheckout checkout;
    private boolean editable;
    private final RForm form;

    /**
     * Creates a team team adapter for managing RTab models
     * @param fm the fragment manager
     * @param checkout the checkout that's be managed
     * @param form the form model, MAY BE NULL
     * @param editable whether the form is read only or not
     */
    public TeamTabAdapter(FragmentManager fm, RCheckout checkout, RForm form, boolean editable) {
        super(fm);
        this.checkout = checkout;
        this.editable = editable;
        this.form = form;
    }

    public boolean isPageRed(int page) {
        return page > 1 && checkout.getTeam().getTabs().get(page).isRedAlliance();
    }

    @Override
    public Fragment getItem(int i) {
        return loadMatch(i);

    }

    /**
     * Marks the match as won
     * @param position the position of the match to mark as won
     * @return boolean representing match status (won or lost)
     */
    public boolean markWon(int position) {
        TeamViewer.checkout.getTeam().getTabs().get(position).setWon(!TeamViewer.checkout.getTeam().getTabs().get(position).isWon());
        TeamViewer.checkout.getTeam().setLastEdit(System.currentTimeMillis());
        notifyDataSetChanged();
        return TeamViewer.checkout.getTeam().getTabs().get(position).isWon();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if(object instanceof Match) {
            Match m = (Match) object;
            m.load();

            return m.getPosition();
        }
        return 0;
    }

    private Fragment loadMatch(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putBoolean("editable", editable);
        bundle.putSerializable("form", form);
        Match match = new Match();
        match.setArguments(bundle);
        return match;
    }

    private boolean isWon(int position) {
        return TeamViewer.checkout.getTeam().getTabs().get(position).isWon();
    }

    private String getWinSuffix(int position) {
        if(isWon(position)) return " ★";
        else return "";
    }


    @Override
    public int getCount() {
        return TeamViewer.checkout.getTeam().getTabs().size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getWinSuffix(position)+" "+TeamViewer.checkout.getTeam().getTabs().get(position).getTitle();
    }
}
