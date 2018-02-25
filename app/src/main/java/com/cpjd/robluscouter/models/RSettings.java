package com.cpjd.robluscouter.models;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

/**
 * This is the settings model for Roblu Client. Besides user specified form data, this is the ONLY data that Roblu records
 * without user entry. Even name is dependant on if the user enters their Google account. Roblu is very precise on storing
 * very, very little data. Roblu isn't a super secure platform yet, so we don't want to have anything worth stealing!
 */
@Data
public class RSettings implements Serializable {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /*
     * User and general device settings
     */
    /**
     * Stores the UI colors, look, and feel
     */
    private RUI rui;
    /**
     * Stores the team code required for transmitting data to the server
     */
    private String code;
    /**
     * Name of the user, for status usage. Non-critical
     */
    private String name;
    // End user and general device settings

    /*
     * UI preferences
     */
    /**
     * Whether to include pit checkouts in the main checkouts view
     */
    private boolean showPit;
    /**
     * Whether to include complete checkouts in the main checkouts view
     */
    private boolean showCompleted;
    /**
     * Whether to include checked out checkouts in the main checkouts view
     */
    private boolean showCheckedOut;
    // End UI preferences

    /**
     * The server IP for cloud syncing
     */
    private String serverIP;

    /**
     * Array of all server MAC addresses that can be connected to
     */
    private ArrayList<String> bluetoothServerMACs;

    /**
     * Specifies if checkouts should automatically be checked out upon receivable.
     * 0 = disabled
     * 1-3 = Red Devices
     * 4-6 = Blue Devices
     * 7 = Pit
     */
    private int autoAssignmentMode;

    /**
     * Sets the defaults for settings
     */
    public RSettings() {
        rui = new RUI();
        showPit = true;
        name = "";
        setServerIPToDefault();
    }
    public void setServerIPToDefault() {
        this.setServerIP("ec2-13-59-164-241.us-east-2.compute.amazonaws.com");
    }
}
