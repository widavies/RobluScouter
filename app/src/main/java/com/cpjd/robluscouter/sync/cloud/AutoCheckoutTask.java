package com.cpjd.robluscouter.sync.cloud;

import android.util.Log;

import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RTab;
import com.cpjd.robluscouter.models.metrics.RCalculation;
import com.cpjd.robluscouter.models.metrics.RFieldData;
import com.cpjd.robluscouter.models.metrics.RMetric;
import com.cpjd.robluscouter.utils.HandoffStatus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * AutoCheckoutTask will run through a list of checkouts and check them out according
 * to the user's "Auto Checkout" preferences
 * @see com.cpjd.robluscouter.models.RSettings
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class AutoCheckoutTask extends Thread {

    /**
     * Allows the user to preload checkouts if they want to, if this
     * is received as null, AutoCheckoutTask will re-load them from the
     * disk.
     */
    private ArrayList<RCheckout> checkouts;

    private RSettings settings;

    private WeakReference<IO> ioWeakReference;

    private AutoCheckoutTaskListener listener;

    /**
     * Specifies whether checkouts that don't belong to the auto checkout mode should be unchecked out
     */
    private boolean uncheckout;

    public interface AutoCheckoutTaskListener {
        void done();
    }

    public AutoCheckoutTask(AutoCheckoutTaskListener listener, IO io, RSettings settings, ArrayList<RCheckout> checkouts, boolean uncheckout) {
        this.checkouts = checkouts;
        this.settings = settings;
        this.ioWeakReference = new WeakReference<>(io);
        this.listener = listener;
        this.uncheckout = uncheckout;
    }

    @Override
    public void run() {
        Log.d("RSBS", "Executing AutoCheckoutTask...");

        if(ioWeakReference.get() == null) {
            quit();
            return;
        }
        IO io = ioWeakReference.get();

        if(settings.getAutoAssignmentMode() <= 0) {

            for(RCheckout checkout : checkouts) {
            /*
             * First, check to see if it should be UNCHECKED out
             */
                if(uncheckout && checkout.getStatus() == HandoffStatus.CHECKED_OUT && checkout.getTeam().getTabs().get(0).getAlliancePosition() != settings.getAutoAssignmentMode() &&
                        checkout.getNameTag().equals(settings.getName())) {

                    boolean shouldCheckout = true;

                    // Test to see if the metric has got some data in it, if it does, don't uncheck it out
                    loop:
                    for(RTab tab : checkout.getTeam().getTabs()) {
                        for(RMetric metric : tab.getMetrics()) {
                            if(!(metric instanceof RCalculation) && !(metric instanceof RFieldData) && metric.isModified()) {
                                shouldCheckout = false;
                                break loop;
                            }
                        }
                    }

                    if(shouldCheckout) {
                        checkout.setStatus(HandoffStatus.AVAILABLE);
                        io.deleteMyCheckout(checkout.getID());
                        io.saveCheckout(checkout);
                        io.savePendingCheckout(checkout); // For the status
                    }

                }
            }
            Log.d("RSBS", "AutoCheckoutMode is 0, stopping AutoCheckoutTask...");
            return;
        }

        if(checkouts == null) {
            checkouts = io.loadCheckouts();
        }

        if(checkouts == null || checkouts.size() == 0) {
            Log.d("RSBS", "No checkouts found. Stopping AutoCheckoutTask...");
            quit();
            return;
        }

        /*
         * Start auto assigning checkouts
         */
        for(RCheckout checkout : checkouts) {

            /*
             * First, check to see if it should be UNCHECKED out
             */
            if(uncheckout && checkout.getStatus() == HandoffStatus.CHECKED_OUT && checkout.getTeam().getTabs().get(0).getAlliancePosition() != settings.getAutoAssignmentMode() &&
                    checkout.getNameTag().equals(settings.getName())) {

                boolean shouldCheckout = true;

                // Test to see if the metric has got some data in it, if it does, don't uncheck it out
                loop : for(RTab tab : checkout.getTeam().getTabs()) {
                    for(RMetric metric : tab.getMetrics()) {
                        if(!(metric instanceof RCalculation) && !(metric instanceof RFieldData) && metric.isModified()) {
                            shouldCheckout = false;
                            break loop;
                        }
                    }
                }

                if(shouldCheckout) {
                    checkout.setStatus(HandoffStatus.AVAILABLE);
                    io.deleteMyCheckout(checkout.getID());
                    io.saveCheckout(checkout);
                    io.savePendingCheckout(checkout); // For the status
                }

            }

            /*
             * First, if it's pit and AutoAssignmentMode = 7, checkout
             */
            if(checkout.getStatus() == HandoffStatus.AVAILABLE && settings.getAutoAssignmentMode() == 7 && checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT")) {
                checkout(io, checkout);
                continue;
            }

            /*
             * If the status is NOT available, OR, the checkout doesn't contain position info,
             * just skip this to avoid disrupting the database
             */
            if(checkout.getStatus() != HandoffStatus.AVAILABLE || checkout.getTeam().getTabs().get(0).getAlliancePosition() == -1) continue;

            /*
             * If the checkout has an alliance position that is equal to the settings mode, check it out
             */
            if(checkout.getTeam().getTabs().get(0).getAlliancePosition() == settings.getAutoAssignmentMode()) {
                checkout(io, checkout);
            }

        }

        Log.d("RSBS", "Completed AutoCheckoutTask.");

        // Stop the thread
        quit();
    }

    /**
     * Checkouts the specified checkout and saves it to the disk
     * @param checkout the checkout to checkout
     */
    private void checkout(IO io, RCheckout checkout) {
        checkout.setStatus(HandoffStatus.CHECKED_OUT);
        checkout.setTime(System.currentTimeMillis());
        checkout.setNameTag(settings.getName());
        io.saveCheckout(checkout);
        io.saveMyCheckout(checkout);
        io.savePendingCheckout(checkout); // For the status
    }

    /**
     * Stops the AutoCheckoutTask thread
     */
    private void quit() {
        settings = null;

        if(listener != null) listener.done();

        interrupt();
    }

}
