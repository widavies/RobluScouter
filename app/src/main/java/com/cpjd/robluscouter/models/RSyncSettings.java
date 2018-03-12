package com.cpjd.robluscouter.models;

import java.io.Serializable;
import java.util.LinkedHashMap;

import lombok.Data;

/**
 * RSyncSettings is a subset settings model separate from RSettings.
 * Essentially, the background service needs to store and save some data.
 * The problem is, if the user makes a change to RSettings, data can get
 * over-written. So anything that needs to be saved from the background service
 * should be saved here.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
@Data
public class RSyncSettings implements Serializable {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * The team's FRC number, used for showing a user the matches they are in
     * @see com.cpjd.robluscouter.ui.mymatches.MyMatches
     */
    private int teamNumber;
    /**
     * Name of the event that is currently being synced
     */
    private String eventName;
    /**
     * Team timestamp, if this time stamp doesn't match the one received from the server,
     * pull the team.
     */
    private long teamSyncID;
    /**
     * Stores the sync ids for each checkout, if the IDs don't match, the checkouts should be synced.
     * The key represents the checkout ID, and the value represents that checkout's corresponding sync
     * ID.
     */
    private LinkedHashMap<Integer, Long> checkoutSyncIDs;

    /**
     * Stores the timestamp of the last successful Bluetooth sync, to minimize data transfers
     */
    private long lastBluetoothCheckoutSync;


    public LinkedHashMap<Integer, Long> getCheckoutSyncIDs() {
        if(checkoutSyncIDs == null) this.checkoutSyncIDs = new LinkedHashMap<>();
        return checkoutSyncIDs;
    }
}
