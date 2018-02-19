package com.cpjd.robluscouter.utils;

import com.cpjd.robluscouter.models.RCheckout;

/**
 * Represents the status of a checkout. This IDs much match the output IDs or Roblu Master.
 *
 * @since 4.0.0
 */
public class HandoffStatus {
    /*
     * STATE TYPES
     */
    /**
     * Handoff hasn't been and is not checked out. It is available for scouting
     */
    @SuppressWarnings("unused")
    public static final int AVAILABLE = 0;
    /**
     * The handoff is currently checked out to somebody
     */
    public static final int CHECKED_OUT = 1;
    /**
     * The handoff has been completed
     */
    public static final int COMPLETED = 2;

    public static String statusToString(RCheckout checkout) {
        switch(checkout.getStatus()) {
            case AVAILABLE:
                return "Available";
            case CHECKED_OUT:
                return "Checked out to "+checkout.getNameTag()+" at "+Utils.convertTime(checkout.getTime());
            case COMPLETED:
                return "Completed by "+checkout.getNameTag()+" at "+Utils.convertTime(checkout.getTime());
            default:
                return "No status available";
        }
    }

}
