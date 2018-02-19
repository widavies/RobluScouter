package com.cpjd.robluscouter.ui.checkouts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.cpjd.http.Request;
import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.sync.bluetooth.BTConnect;
import com.cpjd.robluscouter.sync.bluetooth.Bluetooth;
import com.cpjd.robluscouter.sync.cloud.Service;
import com.cpjd.robluscouter.ui.UIHandler;
import com.cpjd.robluscouter.ui.mymatches.MyMatches;
import com.cpjd.robluscouter.ui.settings.AdvSettings;
import com.cpjd.robluscouter.ui.setup.SetupActivity;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.Utils;

import java.util.ArrayList;


/**
 * Checkouts view is the launcher utility. It contains 2 CheckoutTabs. Each CheckoutTabs
 * contain a list of checkouts. It also manages the control tabs at the top and a mini settings
 * drop down widget.
 *
 * Note: CheckoutsView will actually load the checkouts and sort them into their proper checkouts tabs.
 * CheckoutsView will also receive updates from the Background Service if a UI update is required.
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 */
public class CheckoutsView extends AppCompatActivity {

    private CheckoutTabAdapter tabAdapter;
    private RSettings settings;
    private IntentFilter serviceFilter;

    /**
     * This is the official checkouts array, all of them (my checkouts, checkouts, etc.)
     * The important thing here is that many other classes (CheckoutTab) will sort through
     * this to glean data that they want. This is the array that needs to be updated when
     * a checkout is modified. You'll also have to call forceUpdate() for the checkout
     * tab to re-look through this array.
     *
     * NOTE: It's static!
     */
    public static ArrayList<RCheckout> checkouts;

    /**
     * Although this class doesn't manage Bluetooth syncing, it manages the Bluetooth
     * interface library. For Bluetooth syncing code
     * @see BTConnect
     */
    private Bluetooth bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkouts_view);

        // Initialize startup requirements
        Utils.initWidth(this); // sets width for UI, needed by RMetricToUI
        if(IO.init(getApplicationContext())) { // checks if we need to show startup dialog
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        settings = new IO(getApplicationContext()).loadSettings();

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(new IO(getApplicationContext()).loadCloudSettings().getEventName());
            updateActionBar();
        }

        // Tabs
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabAdapter = new CheckoutTabAdapter(getSupportFragmentManager());

        // Checkouts containers
        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(tabAdapter);

        // Finish tab setup
        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(settings.getRui().getPrimaryColor());
        tabLayout.setSelectedTabIndicatorColor(settings.getRui().getAccent());
        tabLayout.setTabTextColors(RUI.darker(settings.getRui().getText(), 0.95f), settings.getRui().getText());

        // Generic UI config
        new UIHandler(this, toolbar).update();
        // End UI Setup

        // Check to see if the background service is running, if it isn't, start it
        serviceFilter = new IntentFilter();
        serviceFilter.addAction(Constants.SERVICE_ID);
        if(!Utils.isMyServiceRunning(getApplicationContext())) {
            Intent serviceIntent = new Intent(this, Service.class);
            startService(serviceIntent);
        }
    }

    /**
     * This method can receive global UI refresh requests from other activities or from the background service.
     */
    private BroadcastReceiver uiRefreshRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getBooleanExtra("mycheckouts", false) && tabAdapter.getMyCheckouts() != null) {
                tabAdapter.getMyCheckouts().forceUpdate();
            }
            if(intent.getBooleanExtra("checkouts", false) && tabAdapter.getCheckouts() != null) {
                tabAdapter.getCheckouts().forceUpdate();
            }

            // Make sure data is persistent
            settings = new IO(getApplicationContext()).loadSettings();

            if(getSupportActionBar() != null) {
                String subtitle = new IO(getApplicationContext()).loadCloudSettings().getEventName();
                getSupportActionBar().setSubtitle(subtitle);
            }
        }
    };

    /**
     * Manages the mini drop down settings menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.show_pit:
                item.setChecked(!item.isChecked());
                settings.setShowPit(item.isChecked());
                break;
            case R.id.show_checked_out:
                item.setChecked(!item.isChecked());
                settings.setShowCheckedOut(item.isChecked());
                break;
            case R.id.show_completed:
                item.setChecked(!item.isChecked());
                settings.setShowCompleted(item.isChecked());
                break;
            case R.id.mymatches:
                startActivityForResult(new Intent(this, MyMatches.class), Constants.GENERAL);
                break;
            case R.id.bluetooth:
                new BTConnect(settings, bluetooth).start();
                break;
            case R.id.ping:
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
                StrictMode.setThreadPolicy(policy);
                boolean result = new Request(new IO(getApplicationContext()).loadSettings().getServerIP()).ping();
                if(result) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Server is online", false, settings.getRui().getPrimaryColor());
                else Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Server is offline", true, 0);
                break;
            case R.id.settings:
                startActivityForResult(new Intent(this, AdvSettings.class), Constants.GENERAL);
                break;
        }
        invalidateOptionsMenu();
        new IO(getApplicationContext()).saveSettings(settings);
        tabAdapter.getCheckouts().forceUpdate();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_dropdown, menu);
        menu.findItem(R.id.show_completed).setChecked(settings.isShowCompleted());
        menu.findItem(R.id.show_checked_out).setChecked(settings.isShowCheckedOut());
        menu.findItem(R.id.show_pit).setChecked(settings.isShowPit());
        return true;
    }
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(uiRefreshRequestReceiver, serviceFilter);
        tabAdapter.getMyCheckouts().forceUpdate();
        tabAdapter.getCheckouts().forceUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(uiRefreshRequestReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // CheckoutsView will received activity results from all children fragments, so handle the "TEAM_EDITED" event here
        if(resultCode == Constants.TEAM_EDITED) {
            int ID = data.getIntExtra("checkout", 0);
            tabAdapter.getMyCheckouts().getAdapter().reAdd(new IO(getApplicationContext()).loadMyCheckout(ID));
        }

        settings = new IO(getApplicationContext()).loadSettings();
        updateActionBar();
    }

    private void updateActionBar() {
        if(settings.getAutoAssignmentMode() != 0) {
            String device = "";
            switch(settings.getAutoAssignmentMode()) {
                case 1:
                    device += " - Red Device 1";
                    break;
                case 2:
                    device += " - Red Device 2";
                    break;
                case 3:
                    device += " - Red Device 3";
                    break;
                case 4:
                    device += " - Blue Device 1";
                    break;
                case 5:
                    device += " - Blue Device 2";
                    break;
                case 6:
                    device += " - Blue Device 3";
                    break;
                case 7:
                    device += " - Pit";
                    break;
            }
            if(getSupportActionBar() != null) getSupportActionBar().setTitle("Roblu Scouter" + device);
        }
    }
}
