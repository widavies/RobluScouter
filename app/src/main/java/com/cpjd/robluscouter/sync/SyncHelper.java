package com.cpjd.robluscouter.sync;

import android.content.Context;
import android.util.Log;

import com.cpjd.models.CloudCheckout;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RSyncSettings;
import com.cpjd.robluscouter.models.RTab;
import com.cpjd.robluscouter.models.metrics.RGallery;
import com.cpjd.robluscouter.notifications.Notify;
import com.cpjd.robluscouter.sync.cloud.AutoCheckoutTask;
import com.cpjd.robluscouter.utils.HandoffStatus;
import com.cpjd.robluscouter.utils.Utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import lombok.Data;

/**
 * SyncHelper manages the packaging and depackaging of data received over one
 * of the three sync methods.
 *
 * @version 1
 * @since 4.3.5
 * @author Will Davies
 */
public class SyncHelper {

    private ObjectMapper mapper;
    private Context context;
    private IO io;
    private RSettings settings;

    private MODES mode;

    public enum MODES {
        NETWORK,BLUETOOTH,QR
    }

    public SyncHelper(Context context, MODES mode) {
        this.context = context;
        this.mode = mode;
        io = new IO(context);
        this.settings = io.loadSettings();
        mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    /**
     * Packages a list of checkouts and converts them to a string
     * @param checkouts the checkouts to package
     * @throws Exception if no checkouts are received or a error occurred when serializing them
     * @return a string containing all checkout information
     */
    public String packCheckouts(ArrayList<RCheckout> checkouts) throws Exception {
        if(checkouts == null || checkouts.size() == 0) {
            throw new NullPointerException("No checkouts were found to package.");
        }

            // Go through and make sure to add edit history tags to all the RTabs
            for(RCheckout checkout : checkouts) {
                if(checkout.getStatus() == HandoffStatus.COMPLETED && checkout.getTeam().getLastEdit() > 0) {
                    for(RTab t : checkout.getTeam().getTabs()) {
                        LinkedHashMap<String, Long> edits = t.getEdits();
                        if(edits == null) edits = new LinkedHashMap<>();
                        edits.put(settings.getName(), System.currentTimeMillis());
                        t.setEdits(edits);
                    }
                }

                    /*
                     * Pack images
                     */
                for(RTab tab : checkout.getTeam().getTabs()) {
                    for(int i = 0; tab.getMetrics() != null && i < tab.getMetrics().size(); i++) {
                        if(!(tab.getMetrics().get(i) instanceof RGallery)) continue;

                        ((RGallery)tab.getMetrics().get(i)).setImages(new ArrayList<byte[]>());
                        for(int j = 0; ((RGallery)tab.getMetrics().get(i)).getPictureIDs() != null && j < ((RGallery)tab.getMetrics().get(i)).getPictureIDs().size(); j++) {
                            ((RGallery)tab.getMetrics().get(i)).getImages().add(io.loadPicture(((RGallery)tab.getMetrics().get(i)).getPictureIDs().get(j)));
                        }
                    }
                }
            }


        return mapper.writeValueAsString(checkouts);
    }

    /**
     * Deserialize and merges a list of checkouts into the active event
     * @param checkouts the array of checkouts to process
     */
    public void unpackCheckouts(CloudCheckout[] checkouts, RSyncSettings cloudSettings) {
        if(checkouts == null || checkouts.length == 0) {
            throw new NullPointerException("No checkouts to unpack.");
        }

        boolean shouldShowNotification = false;

        ArrayList<RCheckout> refList = new ArrayList<>();

        for(CloudCheckout serial : checkouts) {
            try {
                // Deserialize
                RCheckout checkout = mapper.readValue(serial.getContent(), RCheckout.class);

                refList.add(checkout);

                if(checkout.getNameTag() == null || !checkout.getNameTag().equals(settings.getName())) shouldShowNotification = true;

                // Merge the checkout
                mergeCheckout(checkout);

                if(mode == MODES.NETWORK) {
                    // Update the sync IDs
                    cloudSettings.getCheckoutSyncIDs().put(checkout.getID(), serial.getSyncID());
                }
            } catch(Exception e) {
                Log.d("RBS", "Failed to unpack checkout: "+serial);
            }
        }

        new AutoCheckoutTask(null, io, settings, refList).start();

        // Send a multi-notification instead of spamming the user if they received 6 or more checkouts at once
        if(mode == MODES.NETWORK) {
            if(shouldShowNotification) Notify.notifyNoAction(context, "Successfully pulled "+checkouts.length+" checkouts.", "Roblu Scouter successfully downloaded "+checkouts.length+" checkouts from" +
                    " the server at "+Utils.convertTime(System.currentTimeMillis()));
            // Request general UI refresh
            Utils.requestUIRefresh(context, false, true);
        }

        if(mode == MODES.BLUETOOTH) {
            cloudSettings.setLastBluetoothCheckoutSync(System.currentTimeMillis());

            Notify.notifyNoAction(context, "Successfully pulled "+checkouts.length+" checkouts.", "Roblu Scouter successfully pulled "+checkouts.length+" checkouts from" +
                    " a Bluetooth server at "+Utils.convertTime(System.currentTimeMillis()));
            Utils.requestUIRefresh(context, true, true);
        }
    }

    /**
     * Merges the checkout with the checkouts list
     * @param checkout the checkout to merge
     */
    private void mergeCheckout(RCheckout checkout) {
        io.saveCheckout(checkout);

        Log.d("RBS-Service", "Merged the team: "+checkout.getTeam().getName());
    }

    public String packSyncIDs(LinkedHashMap<Integer, Long> checkoutSyncIDs) throws Exception {

        // This is how the current sync IDs will be packaged to the server
        @Data
        class CheckoutSyncID implements Serializable {
            private int checkoutID;
            private long syncID;

            private CheckoutSyncID(int checkoutID, long syncID) {
                this.checkoutID = checkoutID;
                this.syncID = syncID;
            }
        }

        ArrayList<CheckoutSyncID> checkoutSyncPacks = new ArrayList<>();
        for(Object key : checkoutSyncIDs.keySet()) {
            checkoutSyncPacks.add(new CheckoutSyncID(Integer.parseInt(key.toString()), checkoutSyncIDs.get(key)));
        }

        return mapper.writeValueAsString(checkoutSyncPacks);
    }

    public CloudCheckout[] convertStringSerialToCloudCheckouts(String[] serial) {
        if(serial == null || serial.length == 0) {
            throw new NullPointerException("No checkouts found to process");
        }

        CloudCheckout[] cloudCheckouts = new CloudCheckout[serial.length];
        for(int i = 0; i < serial.length; i++) cloudCheckouts[i] = new CloudCheckout(-1, serial[i]);
        return cloudCheckouts;
    }
}
