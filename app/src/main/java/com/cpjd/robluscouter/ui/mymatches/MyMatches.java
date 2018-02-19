package com.cpjd.robluscouter.ui.mymatches;


import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RTeam;
import com.cpjd.robluscouter.ui.UIHandler;
import com.cpjd.robluscouter.ui.checkouts.RecyclerViewAdapter;
import com.cpjd.robluscouter.utils.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Displays the matches that the team is currently in.
 *
 * For this class to work, we need access to:
 * -FRC team number
 * -Checkouts list (we have to reload it since we're starting a new activity)
 *
 * @version 2
 * @since 1.0.1
 * @author Will Davies
 */
public class MyMatches extends AppCompatActivity {

    private LoadMyMatches loader;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private RecyclerViewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mymatches);


        // Bind UI to objects
        progressBar = findViewById(R.id.progress_bar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My matches");
        }

        // Setup recycler view
        recyclerView = findViewById(R.id.recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        // Setup adapter
        adapter = new RecyclerViewAdapter(getApplicationContext(), new IO(getApplicationContext()).loadSettings(), Constants.MY_MATCHES);
        recyclerView.setAdapter(adapter);

        // Setup touch helper
        //ItemTouchHelper.Callback callback = new CheckoutsTouchHelper(adapter, CheckoutTab.MY_MATCHES);
        //ItemTouchHelper helper = new ItemTouchHelper(callback);
       // helper.attachToRecyclerView(recyclerView);

        loader = new LoadMyMatches(new IO(getApplicationContext()), recyclerView, progressBar, adapter);
        loader.execute();

        new UIHandler(this, toolbar).update();
    }

    /**
     * Load matches finds any matches that the team is in and displays them.
     *
     * WeakReferences are used for all the variables to prevent any memory leaks.
     */
    private static class LoadMyMatches extends AsyncTask<Void, Void, ArrayList<RCheckout>> {

        private IO io;
        private final WeakReference<RecyclerView> recyclerViewWeakReference;
        private final WeakReference<ProgressBar> progressBarWeakReference;
        private final WeakReference<RecyclerViewAdapter> recyclerViewAdapterWeakReference;

        LoadMyMatches(IO io, RecyclerView recyclerView, ProgressBar progressBar, RecyclerViewAdapter adapter) {
            this.io = io;
            this.recyclerViewWeakReference = new WeakReference<>(recyclerView);
            this.progressBarWeakReference = new WeakReference<>(progressBar);
            this.recyclerViewAdapterWeakReference = new WeakReference<>(adapter);
        }

        @Override
        protected void onPreExecute() {
            try {
                recyclerViewWeakReference.get().setVisibility(View.GONE);
                progressBarWeakReference.get().setVisibility(View.VISIBLE);
                progressBarWeakReference.get().getIndeterminateDrawable().setColorFilter(io.loadSettings().getRui().getAccent(), PorterDuff.Mode.MULTIPLY);
            } catch(NullPointerException e) {
                Log.d("RSBS", "AsyncTask variables are unavailable due to garbage collecting.");
            }
        }

        @Override
        public ArrayList<RCheckout> doInBackground(Void... params) {
            ArrayList<RCheckout> handoffs = io.loadCheckouts();
            int number = io.loadCloudSettings().getTeamNumber();

            // Make sure we have enough information to continue
            if(number == 0 || handoffs == null || handoffs.size() == 0) return null;

            ArrayList<RCheckout> result = new ArrayList<>();

            for(RCheckout checkout : handoffs) {
                if(checkout.getTeam().getNumber() == number) {
                    if(checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT")) continue;

                    ArrayList<RTeam> teammates = new ArrayList<>();
                    ArrayList<RTeam> opponents = new ArrayList<>();

                    // Find teammates
                    for(RCheckout check : handoffs) {
                        if(check.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase(checkout.getTeam().getTabs().get(0).getTitle())) {
                            if(check.getTeam().getTabs().get(0).isRedAlliance() == checkout.getTeam().getTabs().get(0).isRedAlliance()) teammates.add(check.getTeam());
                            else opponents.add(check.getTeam());
                        }
                    }

                    checkout.getTeam().getTabs().get(0).setTeammates(teammates);
                    checkout.getTeam().getTabs().get(0).setOpponents(opponents);

                    result.add(checkout);
                }
            }

            Collections.sort(result);

            return result;
        }

        @Override
        public void onPostExecute(ArrayList<RCheckout> handoffs) {
            if(handoffs == null) return;

            try {
                if(recyclerViewAdapterWeakReference.get() != null) {
                    recyclerViewAdapterWeakReference.get().removeAll();
                    recyclerViewAdapterWeakReference.get().setCheckouts(handoffs);
                }
                recyclerViewWeakReference.get().setVisibility(View.VISIBLE);
                progressBarWeakReference.get().setVisibility(View.GONE);
            } catch(NullPointerException e) {
                Log.d("RSBS", "AsyncTask variables are unavailable due to garbage collecting.");
            }
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        loader.cancel(true);
    }
}
