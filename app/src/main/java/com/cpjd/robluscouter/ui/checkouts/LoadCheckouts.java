package com.cpjd.robluscouter.ui.checkouts;

import android.util.Log;

import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.HandoffStatus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Load checkouts sorts through CheckoutsView.checkouts and finds checkouts that this particular CheckoutTab
 * is interested in.
 *
 * @version 2
 */
public class LoadCheckouts extends Thread {

    private int mode;
    private WeakReference<IO> ioWeakReference;

    private LoadCheckoutsListener listener;

    public interface LoadCheckoutsListener {
        void checkoutsLoaded(ArrayList<RCheckout> checkouts, boolean hideZeroRelevanceCheckouts);
    }

    LoadCheckouts(IO io, LoadCheckoutsListener listener, int mode) {
        this.ioWeakReference = new WeakReference<>(io);
        this.mode = mode;
        this.listener = listener;
    }

    @Override
    public void run() {
        if(ioWeakReference.get() == null) {
            quit();
            return;
        }

            /*
             * Load from file system if necessary
             */
        ArrayList<RCheckout> checkouts;
        if(mode == Constants.CHECKOUTS) checkouts = ioWeakReference.get().loadCheckouts();
        else checkouts = ioWeakReference.get().loadMyCheckouts();

        // If they're equal to null, return early
        if(checkouts == null || checkouts.size() == 0) {
            Log.d("RSBS", "Unable to load checkouts.");
            quit();
            listener.checkoutsLoaded(null, false);
            return;
        }

            /*
             * Remove items that shouldn't be listed
             */
        RSettings settings = ioWeakReference.get().loadSettings();
        if(mode == Constants.CHECKOUTS) {
            for(RCheckout checkout : checkouts) {
                checkout.setCustomRelevance(0);

                if(!settings.isShowPit() && checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT")) continue;
                if(!settings.isShowCompleted() && checkout.getStatus() == HandoffStatus.COMPLETED) continue;
                if(!settings.isShowCheckedOut() && checkout.getStatus() == HandoffStatus.CHECKED_OUT) continue;

                checkout.setCustomRelevance(1);
            }

            Collections.sort(checkouts);
            listener.checkoutsLoaded(checkouts, true);
            quit();
        } else {
            Collections.sort(checkouts);
            listener.checkoutsLoaded(checkouts, false);
            quit();
        }
    }

    void quit() {
        interrupt();
    }
}
