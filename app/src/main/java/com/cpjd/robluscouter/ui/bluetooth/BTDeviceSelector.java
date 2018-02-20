package com.cpjd.robluscouter.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.sync.bluetooth.Bluetooth;
import com.cpjd.robluscouter.ui.dialogs.FastDialogBuilder;

import java.util.ArrayList;

/**
 * BTDeviceSelector manages first time setup for a Bluetooth connection.
 * The scouter can select as many Bluetooth devices as they want, and when
 * they request a Bluetooth sync, each device will be contacted (in its own thread) if its available.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class BTDeviceSelector extends AppCompatActivity implements Bluetooth.BluetoothListener {

    private Bluetooth bluetooth;

    private RecyclerView recyclerView;

    private DevicesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_device_selector);

        bluetooth = new Bluetooth(BTDeviceSelector.this);
        bluetooth.setListener(this);
        bluetooth.enable();
        bluetooth.startScanning();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nearby Bluetooth Devices");
            getSupportActionBar().setSubtitle("Searching for devices...");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        adapter = new DevicesAdapter(getApplicationContext());
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void deviceDiscovered(BluetoothDevice device) {
        Log.d("RSBS", "Found device: "+device.getName());
        adapter.addDevice(device);
    }

    @Override
    public void messageReceived(String header, String message) {

    }

    @Override
    public void deviceConnected(BluetoothDevice device) {

    }

    @Override
    public void deviceDisconnected(BluetoothDevice device, String reason) {

    }

    @Override
    public void errorOccurred(String message) {

    }

    @Override
    public void stateChanged(int state) {

    }

    @Override
    public void discoveryStopped() {

    }

    private class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
        private final Context context;
        private ArrayList<BluetoothDevice> devices;

        /**
         * User color preferences
         */
        private final RUI rui;

        DevicesAdapter(Context context) {
            this.context = context;
            devices = new ArrayList<>();
            rui = new IO(context).loadSettings().getRui();
        }

        public void addDevice(BluetoothDevice device) {
            devices.add(device);
            notifyItemInserted(devices.size() - 1);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_bt_device, parent, false);
            final ViewHolder holder = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Save the MAC address
                    BluetoothDevice device = devices.get(recyclerView.getChildLayoutPosition(v));

                    // Attempt pairing
                    bluetooth.pair(device);

                    RSettings settings = new IO(getApplicationContext()).loadSettings();
                    ArrayList<String> macs = settings.getBluetoothServerMACs();
                    if(macs == null) macs = new ArrayList<>();
                    // Check to make sure the MAC address hasn't already been added
                    for(String s : macs) {
                        if(s.equals(device.getAddress())) {
                            Toast.makeText(getApplicationContext(), "This device is already on your sync list.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }


                    macs.add(device.getAddress());
                    settings.setBluetoothServerMACs(macs);
                    new IO(getApplicationContext()).saveSettings(settings);

                    new FastDialogBuilder()
                            .setTitle("Info")
                            .setMessage("Device "+device.getName()+" was added to the sync list. Every time you press \"Sync with Bluetooth\", Roblu Scouter will attempt to connect to this device. You may add as many devices as you'd like")
                            .setPositiveButtonText("Yes")
                            .build(BTDeviceSelector.this);
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindDevice(devices.get(position));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        /**
         * Specifies how a RTutorial object should be binded to a UI element
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView title;
            public final TextView subtitle;

            ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.title);
                subtitle = view.findViewById(R.id.subtitle);
            }

            void bindDevice(BluetoothDevice device) {
                this.title.setText(device.getName());
                this.subtitle.setText(device.getAddress());

                if(rui != null) {
                    this.title.setTextColor(rui.getText());
                    this.subtitle.setTextColor(rui.getText());
                    this.title.setBackgroundColor(rui.getCardColor());
                    this.subtitle.setBackgroundColor(rui.getCardColor());
                }

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetooth.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
