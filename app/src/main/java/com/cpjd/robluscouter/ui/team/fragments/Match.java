package com.cpjd.robluscouter.ui.team.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RForm;
import com.cpjd.robluscouter.models.metrics.RBoolean;
import com.cpjd.robluscouter.models.metrics.RCalculation;
import com.cpjd.robluscouter.models.metrics.RCheckbox;
import com.cpjd.robluscouter.models.metrics.RChooser;
import com.cpjd.robluscouter.models.metrics.RCounter;
import com.cpjd.robluscouter.models.metrics.RDivider;
import com.cpjd.robluscouter.models.metrics.RFieldDiagram;
import com.cpjd.robluscouter.models.metrics.RGallery;
import com.cpjd.robluscouter.models.metrics.RMetric;
import com.cpjd.robluscouter.models.metrics.RSlider;
import com.cpjd.robluscouter.models.metrics.RStopwatch;
import com.cpjd.robluscouter.models.metrics.RTextfield;
import com.cpjd.robluscouter.ui.forms.RMetricToUI;
import com.cpjd.robluscouter.ui.team.TeamViewer;

import java.util.ArrayList;

/**
 * Match manages the loading of one RTab object
 *
 * Bundle incoming parameters:
 * -"position" - the spacial position of this tab, with the leftmost position being '0'
 * -"editable" - whether the team data should be editable
 * -"form" - a reference to the form model, might be NULL
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 *
 */
public class Match extends Fragment implements RMetricToUI.ElementsListener {

    /**
     * Spacial position of this tab
     */
    private int position;
    private RForm form;
    /**
     * Utility for converting RMetric models into UI elements that can be interacted with
     */
    private RMetricToUI els;

    private LinearLayoutCompat layout;

    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.match_tab, container, false);

        layout = view.findViewById(R.id.match_layout);

        Bundle bundle = this.getArguments();
        position = bundle.getInt("position");
        form = (RForm) bundle.getSerializable("form");

        els = new RMetricToUI(getActivity(), new IO(view.getContext()).loadSettings().getRui(), bundle.getBoolean("editable", false));
        els.setListener(this);

        load();

        return view;
    }

    public void load() {
        if(layout != null && layout.getChildCount() > 0) layout.removeAllViews();

        if(form != null) {
            ArrayList<RMetric> elements;
            if(position == 0 && TeamViewer.checkout.getTeam().getTabs().size() > 1) elements = form.getPit();
            else elements = form.getMatch();

            for(RMetric s : elements) {
                for (RMetric e : TeamViewer.checkout.getTeam().getTabs().get(position).getMetrics()) {
                    if(e.getID() == s.getID()) {
                        loadMetric(e);
                    }
                }
            }
        } else {
            for(RMetric s : TeamViewer.checkout.getTeam().getTabs().get(position).getMetrics()) {
                loadMetric(s);
            }
        }
    }

    private void loadMetric(RMetric e) {
        if (e instanceof RBoolean) layout.addView(els.getBoolean((RBoolean)e));
        else if (e instanceof RCheckbox) layout.addView(els.getCheckbox((RCheckbox)e));
        else if (e instanceof RChooser) layout.addView(els.getChooser((RChooser)e));
        else if (e instanceof RCounter) layout.addView(els.getCounter((RCounter) e));
        else if (e instanceof RGallery) layout.addView(els.getGallery(false, position, (RGallery) e));
        else if (e instanceof RSlider) layout.addView(els.getSlider((RSlider) e));
        else if (e instanceof RStopwatch) layout.addView(els.getStopwatch((RStopwatch) e));
        else if (e instanceof RTextfield) layout.addView(els.getTextfield((RTextfield) e));
        else if(e instanceof RDivider) layout.addView(els.getDivider((RDivider)e));
        else if(e instanceof RFieldDiagram) layout.addView(els.getFieldDiagram(position, (RFieldDiagram)e));
        else if(e instanceof RCalculation) layout.addView(els.getCalculationMetric(TeamViewer.checkout.getTeam().getTabs().get(position).getMetrics(), ((RCalculation)e)));
        else System.out.println("Couldn't resolve item!");
    }

    @Override
    public void changeMade(RMetric metric) {
                /*
         * Notify any calculation metrics that a change was made
         */
        for(int i = 0; i < layout.getChildCount(); i++) {
            CardView cv = (CardView) layout.getChildAt(i);
            if(cv.getTag() != null && cv.getTag().toString().split(":")[0].equals("CALC")) {
                // We've discovered a calculation metric, we have access to the ID, so acquire a new copy of the view
                int ID = Integer.parseInt(cv.getTag().toString().split(":")[1]);
                for(RMetric m : TeamViewer.checkout.getTeam().getTabs().get(position).getMetrics()) {
                    if(m.getID() == ID) {
                        // Get the calculation
                        String value = m.getTitle()+"\nValue: "+((RCalculation)m).getValue(TeamViewer.checkout.getTeam().getTabs().get(position).getMetrics());
                        // Set the text
                        RelativeLayout rl = (RelativeLayout) cv.getChildAt(0);
                        TextView tv = (TextView) rl.getChildAt(0);
                        tv.setText(value);
                        break;
                    }
                }
            }
        }

        // set the metric as modified - this is a critical line, otherwise scouting data will get deleted
        metric.setModified(true);

        TeamViewer.checkout.getTeam().setLastEdit(System.currentTimeMillis());
        new IO(view.getContext()).saveMyCheckout(TeamViewer.checkout);
    }

    public int getPosition() {
        return position;
    }
}
