package com.cpjd.robluscouter.models;

import java.io.Serializable;

import lombok.Data;

/**
 * RCloudSettings is a subset settings model separate from RSettings.
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
public class RCloudSettings implements Serializable {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * Millisecond timestamp of last successful server form, ui, and team sync
     */
    private long lastTeamSync;
    /**
     * Millisecond timestamp of last successful server checkouts sync
     */
    private long lastCheckoutSync;
    /**
     * The team's FRC number, used for showing a user the matches they are in
     * @see com.cpjd.robluscouter.ui.mymatches.MyMatches
     */
    private int teamNumber;
    /**
     * Name of the event that is currently being synced
     */
    private String eventName;
}
