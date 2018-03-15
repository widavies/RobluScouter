package com.cpjd.robluscouter.ui.checkouts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.ui.team.TeamViewer;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.HandoffStatus;

import java.util.ArrayList;

import lombok.Getter;

/**
 * Abstract class that acts as a checkouts container. Manages a list of checkouts in a nice recycler view setup.
 *
 * @since 1.0.0
 * @version 2
 * @author Will Davies
 */
public class CheckoutTab extends Fragment implements CheckoutClickListener, LoadCheckouts.LoadCheckoutsListener {
    /**
     * General context reference
     */
    protected View view;
    /**
     * Represents a checkouts array as a UI array
     */
    @Getter
    protected RecyclerView recyclerView;
    /**
     * Think of the RecyclerViewAdapter as the backend to the RecyclerView
     */
    @Getter
    protected RecyclerViewAdapter adapter;
    /**
     * UI loading bar to let the user know we're working on loading something into the recycler view
     */
    @Getter
    private ProgressBar progressBar;

    /**
     * Whether this is the "Checkouts" tab or the "MyCheckouts" tab
     * @see Constants for modes list
     */
    private int mode;

    private LoadCheckouts loadCheckouts;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.checkout_tab, container, false);

        // Load startup things
        mode = getArguments().getInt("mode");

        RSettings settings = new IO(view.getContext()).loadSettings();

        /*
         * Setup UI
         */
        // Setup the progress bar
        progressBar = view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        // Setup the recycler view
        recyclerView = view.findViewById(R.id.recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        // Setup the recycler view backend
        adapter = new RecyclerViewAdapter(view.getContext(), settings, mode);
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);

        // Setup up the kinematic motion helper for the recycler view
        ItemTouchHelper.Callback callback = new RecyclerViewTouchHelper(adapter, settings, mode);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);
        // End setup UI

        forceUpdate();

        return view;
    }

    /**
     * If a checkout is clicked, we want to pass it off to the TeamViewer class for UI handling
     * @param v the view object that was clicked
     */
    @Override
    public void checkoutClicked(View v) {
        // Disable editing while upload pending
        if(adapter.getCheckouts().get(recyclerView.getChildAdapterPosition(v)).getStatus() == HandoffStatus.COMPLETED && mode == Constants.MY_CHECKOUTS) {
            Intent intent = new Intent(getActivity(), TeamViewer.class);
            intent.putExtra("checkout", adapter.getCheckouts().get(recyclerView.getChildAdapterPosition(v)).getID());
            intent.putExtra("editable", false);
            startActivityForResult(intent, Constants.GENERAL);
            return;
        }

        Intent intent = new Intent(getActivity(), TeamViewer.class);
        intent.putExtra("checkout", adapter.getCheckouts().get(recyclerView.getChildAdapterPosition(v)).getID());
        if(mode == Constants.CHECKOUTS) {
            intent.putExtra("editable", false);
            startActivityForResult(intent, Constants.GENERAL);
        } else startActivityForResult(intent, Constants.GENERAL);
    }

    /**
     * Forces this tab to re-sort it checkouts from CheckoutView.checkouts
     */
    public void forceUpdate() {
        if(view == null) return;

        if(loadCheckouts != null) {
            loadCheckouts.quit();
        }

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        loadCheckouts = new LoadCheckouts(new IO(view.getContext()), CheckoutTab.this, mode);
        loadCheckouts.start();
    }

    @Override
    public void checkoutsLoaded(final ArrayList<RCheckout> checkouts, final boolean hideZeroRelevanceCheckouts) {
        Log.d("RSBS", "Checkouts loaded.");

        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setCheckouts(checkouts, hideZeroRelevanceCheckouts);
                }
            });
        }
    }
}


