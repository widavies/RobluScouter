package com.cpjd.robluscouter.sync.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.cpjd.robluscouter.models.RSettings;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;

/**
 * Manages a Bluetooth connection with the server and sending data to it.
 *
 * Brief description of processes:
 * -Load the sync-list from RSettings, these are all the phones / tablets the user would like to connect
 * to and sync with.
 * -After a successful sync is achieved, the following will be transmitted, in this order:
 *      -Scouting data sent to Roblu Master
 *      -Form and UI received from Roblu Master
 *      -Scouting status and data receive from Roblu Master
 * -If this is a FIRST sync (the local device contains NO DATA), the scouter can request a full event list
 * from the client. Use the initial boolean flag in the constructor for this.
 *
 * Let's talk about physically how data is transferred.
 *
 * Roblu apps will use a identical communication protocol for transferring data. Prior to the content of a message,
 * an identification string will be sent, followed by the actual serialized content of the message.
 *
 * TAGS
 * -"form" - Received by Scouter, the following string should be processed as an RForm object
 * -"ui" - Received by Scouter, the following string should be processed as an RUI object
 * -"scoutingData" - Received by Master, the following string should be deserialized as a RCheckouts array and merged
 * -"checkouts" - Received by Scouter, the following string should be deserialized as a RCheckouts array and merged
 * -"success-<original tag>" - signifies that the device we are connected to successfully processed our request and data
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class BTConnect extends Thread implements Bluetooth.BluetoothListener {

    /**
     * Roblu uses a Bluetooth wrapper library to simplify connections and lessen the amount of bugs.
     * The library is available here: https://github.com/OmarAflak/Bluetooth-Library
     * This library provides easy access to searching, connecting, and sending data between
     * Bluetooth capable devices.
     */
    private Bluetooth bluetooth;

    /**
     * Contains a String array of all Bluetooth MAC addresses of devices we've acknowledged before, each one
     * will be contacted, and if available, a Bluetooth sync will occur
     */
    private ArrayList<String> bluetoothServerMACs;

    /**
     * Stores the index of the current Bluetooth device in {@link #bluetoothServerMACs} that
     * we are connected to
     */
    private int index = 0;

    /**
     * This listener will just receive some generic updates when certain events happen within BTConnect.
     * It is just some generic code for the UI to process.
     */
    private BTConnectListener listener;

    @Override
    public void deviceDiscovered(BluetoothDevice device) {

    }


    public interface BTConnectListener {
        void success();
        void errorOccurred(String message);
    }

    /**
     * Used for deserializing and serializing objects to and from strings
     */
    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private RSettings settings;

    /**
     * Creates a BTConnect object for syncing to a Bluetooth device
     * @param bluetooth {@link #bluetooth}
     */
    public BTConnect(RSettings settings, Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
        this.settings = settings;
    }

    /**
     * Starts the sync task
     */
    @Override
    public void run() {
        /*
         * First, load dependencies
         */
        bluetoothServerMACs = settings.getBluetoothServerMACs();

        if(bluetoothServerMACs == null) {
            Log.d("RSBS", "No Bluetooth servers found in the sync list. Aborting BTConnect.");
            return;
        }

        if(!bluetooth.isEnabled()) {
            bluetooth.enable();
        }

        //bluetooth.setCommunicationCallback(this); // tell the @Override methods in this class to listen to this Bluetooth object

        if(!connectToNextDevice()) listener.errorOccurred("No Bluetooth servers were found to connect to.");
    }

    /**
     * Attempts to connect to the next device in {@link #bluetoothServerMACs}
     * @return false if there isn't a next device to connect to, true if a device is being connected to
     */
    private boolean connectToNextDevice() {
        if(index >= bluetoothServerMACs.size()) return false;
        Log.d("RSBS", "Attempting to connect to device: "+bluetoothServerMACs.get(index));
       // bluetooth.connectToAddress(bluetoothServerMACs.get(index));
        index++;
        return true;
    }

    /**
     * This method should be called after a successful connection, it will perform the actual syncing of data
     *
     * This code is loosely mirrored from
     * @see com.cpjd.robluscouter.sync.cloud.Service
     */
    private void transfer() {
        bluetooth.send("Hello from Roblu Scouter!\n", "dskl");
    }

    @Override
    public void messageReceived(String header, String message) {

    }

    @Override
    public void deviceConnected() {

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

}
