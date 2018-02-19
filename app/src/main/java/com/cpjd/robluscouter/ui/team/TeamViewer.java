package com.cpjd.robluscouter.ui.team;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RForm;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.ui.UIHandler;
import com.cpjd.robluscouter.ui.team.fragments.TeamTabAdapter;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.Utils;


/**
 * TeamViewer is adopted from Roblu Master. It's used for displaying information about a team, including
 * all the RTabs it includes.
 *
 * Bundle parameters for specifying TeamViewer behavior.
 * -"checkout" - int extra containing ID of checkout to load
 * -"editable" - whether the team scouting data should be editable (this also identifies which tab it is)
 *
 * Bundle return
 * -"checkout" - int ID of the checkout that was modified
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 */
public class TeamViewer extends AppCompatActivity {

    /**
     * RCheckout contains the team, plus some generic meta-data pulled from the server.
     * It's kept static so that sub-activities like image loading don't have to re-load the
     * checkout. It's not practical to transfer RCheckout between activities because if it contains only
     * a couple pictures, it will exceed an Intent's payload size.
     */
    public static RCheckout checkout;

    private TeamTabAdapter tabAdapter;
    private TabLayout tabLayout;
    private RUI rui;
    private Toolbar toolbar;

    private boolean editable;

    private ViewPager pager;

    private IntentFilter serviceFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_viewer);

        /*
         Flag that determines if any of this team information should be editable. Team information
         should be read only if it's loaded from the "checkouts" list. Also determines which list
         the checkout should be loaded from.
         */
        editable = getIntent().getBooleanExtra("editable", true);


        // load the checkout that the user requested
        int ID = getIntent().getIntExtra("checkout", 0);
        if(editable) checkout = new IO(getApplicationContext()).loadMyCheckout(ID);
        else checkout = new IO(getApplicationContext()).loadCheckout(ID);

        /*
         * What's the RForm reference for? It's used for verifying that a local checkout's form is matched with the Roblu Master form.
         * However, with update 4.0.0, we're not actually going to force a sync on the client if a form isn't available. Instead, all
         * incoming Checkouts will be re-verified by Roblu Master, so if the form can't be loaded here, no biggy.
         */
        RForm form = new IO(getApplicationContext()).loadForm();
        if(form == null) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), this, "Form could not be synced with server. Local form may contain discrepancies.", true, 0);
        else { // verify the form
            checkout.getTeam().verify(form);
            if(editable) new IO(this).saveMyCheckout(checkout);
        }

        /*
         * Setup UI
         */
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rui = new IO(getApplicationContext()).loadSettings().getRui();
        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        getSupportActionBar().setTitle(checkout.getTeam().getName());
        getSupportActionBar().setSubtitle("#"+String.valueOf(checkout.getTeam().getNumber()));
        tabAdapter = new TeamTabAdapter(getSupportFragmentManager(), checkout, form, editable);
        pager = findViewById(R.id.pager);
        pager.setAdapter(tabAdapter);
        pager.setCurrentItem(checkout.getTeam().getPage());
        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(rui.getPrimaryColor());
        tabLayout.setSelectedTabIndicatorColor(rui.getAccent());
        tabLayout.setTabTextColors(RUI.darker(rui.getText(), 0.95f), rui.getText());
        new UIHandler(this, toolbar).update();

        /*
         * Set toolbar color
         */
        if(checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT") || !checkout.getTeam().getTabs().get(0).isRedAlliance()) setColorScheme(rui.getPrimaryColor(), RUI.darker(rui.getPrimaryColor(), 0.85f));
        else setColorScheme(ContextCompat.getColor(getApplicationContext(), R.color.red), ContextCompat.getColor(getApplicationContext(), R.color.darkRed));

                /*
         * Attach to background service
         */
        serviceFilter = new IntentFilter();
        serviceFilter.addAction(Constants.SERVICE_ID);
    }

    private void showPopup(){
        if(!editable) return;

        View menuItemView = findViewById(R.id.match_settings);
        final PopupMenu popup = new PopupMenu(TeamViewer.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.match_options, popup.getMenu());

        final PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.won) {
                    if(checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT")) {
                        Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be marked as won", true, 0);
                    }
                    else {
                        String title = checkout.getTeam().getTabs().get(pager.getCurrentItem()).getTitle();
                        boolean won = tabAdapter.markWon(pager.getCurrentItem());
                        if(won) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" marked as won.", false, rui.getPrimaryColor());
                        else Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" marked as lost.", false, rui.getPrimaryColor());
                        popup.dismiss();
                        new IO(getApplicationContext()).saveMyCheckout(checkout);
                    }
                }
                return true;
            }
        };
        popup.setOnMenuItemClickListener(popupListener);
        if(!checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT")) {
            boolean won = checkout.getTeam().getTabs().get(pager.getCurrentItem()).isWon();
            if(won) popup.getMenu().getItem(0).setTitle("Mark as lost");
        }
        popup.show();
    }

    /**
     * When the user returns home, return the ID of the checkout that was finished being edited, because it will
     * need to be loaded by the CheckoutView UI
     * @param item UI menu element pressed
     * @return true if a menu element selection was processed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            Intent result = new Intent();
            result.putExtra("checkout", checkout.getID());
            if(editable) setResult(Constants.TEAM_EDITED, result);
            finish();
            return true;
        }
        else if(item.getItemId() == R.id.match_settings) {
            showPopup();
            return true;
        }
        return false;
    }

    /**
     * Same story as onOptionsItemSelected(), check the method above.
     */
    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("checkout", checkout.getID());
        if(editable) setResult(Constants.TEAM_EDITED, result);
        finish();
    }

    /**
     * This method can receive global UI refresh requests from other activities or from the background service.
     */
    private BroadcastReceiver uiRefreshRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Return if the update was only for the team viewer
            if(!intent.getBooleanExtra("teamViewerOnly", false)) {
                return;
            }

            launchParent();
        }
    };

    // Force closes the TeamViewer
    private void launchParent() {
        Toast.makeText(getApplicationContext(), "TeamViewer has been force closed because new data has been received.", Toast.LENGTH_LONG).show();
        finish();
    }
    /**
     * Toggles the color of the toolbar
     * @param color the color to switch to
     * @param darkColor a dark complement color to color
     */
    private void setColorScheme(int color, int darkColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(darkColor);
        }
        toolbar.setBackgroundColor(color);
        tabLayout.setBackgroundColor(color);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.GALLERY_EXIT) {
            checkout = new IO(getApplicationContext()).loadCheckout(checkout.getID());
            tabAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Hides the keyboard, sometimes the keyboard doesn't hide itself when we want it to
     * @param activity the activity reference
     */
    private void hideKeyboard(Activity activity) {
        if (activity == null || activity.getCurrentFocus() == null || activity.getCurrentFocus().getWindowToken() == null) return;

        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputManager != null) inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.team_viewer_actionbar, menu);
        if(getIntent().getBooleanExtra("editable", false)) {
            menu.findItem(R.id.match_settings).setVisible(false);
            menu.findItem(R.id.match_settings).setEnabled(false);

        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(uiRefreshRequestReceiver, serviceFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(uiRefreshRequestReceiver);
    }
}
