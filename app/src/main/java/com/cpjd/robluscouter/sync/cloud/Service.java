package com.cpjd.robluscouter.sync.cloud;

import android.content.Intent;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cpjd.http.Request;
import com.cpjd.models.CloudCheckout;
import com.cpjd.models.CloudTeam;
import com.cpjd.requests.CloudCheckoutRequest;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RCloudSettings;
import com.cpjd.robluscouter.models.RForm;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RTab;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.models.metrics.RGallery;
import com.cpjd.robluscouter.notifications.Notify;
import com.cpjd.robluscouter.utils.HandoffStatus;
import com.cpjd.robluscouter.utils.Utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the background service for the Roblu Cloud API.
 *
 * @version 2
 * @since 3.6.1
 * @author Will Davies
 */
public class Service extends android.app.Service {

    /**
     * To fix concurrency issues, checkouts can only be updated every 3 ticks.
     */
    private int tickCount;
    private boolean checkoutsUploadedLastTick;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("Service-RSBS", "Service initialized.");

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                loop();
            }
        };
        timer.schedule(timerTask, 0, 10000);
        return START_STICKY;
    }

    /**
     * This is the main background service looper, this should perform any necessary
     * Roblu Cloud sync operations
     */
    public void loop() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        if(!Utils.hasInternetConnection(getApplicationContext())) {
            Log.d("Service-RSBS", "No internet connection detected. Ending loop() early.");
            return;
        }

        /*
         * Create all the utilities we need for this loop
         */
        IO io = new IO(getApplicationContext());
        RSettings settings = io.loadSettings();

        RCloudSettings cloudSettings = io.loadCloudSettings();
        Request r = new Request(settings.getServerIP());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CloudTeamRequest teamRequest = new CloudTeamRequest(r, settings.getCode());
        CloudCheckoutRequest checkoutRequest = new CloudCheckoutRequest(r, settings.getCode());

        if(!r.ping()) {
            Log.d("Service-RSBS", "Roblu server is down. Unable to connect.");
            return;
        }

        if(!teamRequest.isActive()) {
            Log.d("Service-RSBS", "No active event found. Terminating loop early.");
            io.clearCheckouts();
            cloudSettings.setEventName("");
            cloudSettings.setLastCheckoutSync(0);
            cloudSettings.setLastTeamSync(0);
            io.saveCloudSettings(cloudSettings);
            Utils.requestUIRefresh(getApplicationContext(), true, true);
            return;
        }

        /*
         * Fetch form, ui and related data
         */
        try {
            Log.d("Service-RSBS", "Checking for team data...");
            CloudTeam cloudTeam = teamRequest.getTeam(cloudSettings.getLastTeamSync());
            if(cloudTeam != null) {
                RForm form = mapper.readValue(cloudTeam.getForm(), RForm.class);
                io.saveForm(form);
                settings.setRui(mapper.readValue(cloudTeam.getUi(), RUI.class));
                io.saveSettings(settings);
                cloudSettings.setEventName(cloudTeam.getActiveEventName());
                cloudSettings.setTeamNumber((int)cloudTeam.getNumber());
                cloudSettings.setLastTeamSync(System.currentTimeMillis());
                io.saveCloudSettings(cloudSettings);
                Notify.notifyNoAction(getApplicationContext(), "Form received", "Roblu Scouter successfully received an updated form.");
                Utils.requestTeamViewerRefresh(getApplicationContext());
                Log.d("Service-RSBS", "Form, ui, and team info successfully pulled.");
            }
        } catch(Exception e) {
            Log.d("Service-RSBS", "Failed to check for form, ui, team info: "+e.getMessage());
        }

        // Upload completed checkouts
        uploadCompletedCheckouts(io, settings, checkoutRequest, mapper, cloudSettings);

        /*
         * Fetch checkouts
         */
        try {
            Log.d("Service-RSBS", "Checking for checkouts to fetch...");
            CloudCheckout[] checkouts = checkoutRequest.pullCheckouts(cloudSettings.getLastCheckoutSync());
            boolean shouldShowNotification = false;

            if(checkouts.length > 0) {
                ArrayList<RCheckout> refList = new ArrayList<>();

                long maxTimestamp = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // server runs on UTC

                for(CloudCheckout s : checkouts) {
                    // Handle the timestamp
                    long time;
                    try {
                        time = sdf.parse(s.getTime().replace("T", " ").replace("Z", " ")).getTime();
                    } catch(Exception e) {
                        time = 0;
                    }
                    if(time > maxTimestamp) {
                        maxTimestamp = time;
                    }

                    // Deserialize the checkout
                    RCheckout checkout = mapper.readValue(s.getContent(), RCheckout.class);
                    if(checkout.getNameTag() == null || !checkout.getNameTag().equals(settings.getName())) shouldShowNotification = true;

                    /*
                     * Unpack images
                     */
                    for(RTab tab : checkout.getTeam().getTabs()) {
                        for(int i = 0; tab.getMetrics() != null && i < tab.getMetrics().size(); i++) {
                            if(!(tab.getMetrics().get(i) instanceof RGallery)) continue;

                            ((RGallery)tab.getMetrics().get(i)).setPictureIDs(new ArrayList<Integer>());
                            for(int j = 0; ((RGallery)tab.getMetrics().get(i)).getImages() != null && j < ((RGallery)tab.getMetrics().get(i)).getImages().size(); j++) {
                                ((RGallery)tab.getMetrics().get(i)).getPictureIDs().add(io.savePicture(((RGallery)tab.getMetrics().get(i)).getImages().get(j)));
                            }
                            // Set metrics to null
                            ((RGallery)tab.getMetrics().get(i)).setImages(null);
                        }
                    }

                    refList.add(checkout);
                    io.saveCheckout(checkout);
                }

                /*
                 * Run the auto-assignment checkout task
                 */
                new AutoCheckoutTask(null, new IO(getApplicationContext()),settings, refList).start();
                cloudSettings.setLastCheckoutSync(maxTimestamp);
                io.saveCloudSettings(cloudSettings);
                io.saveCloudSettings(cloudSettings);
                Log.d("Service-RSBS", "Successfully pulled "+checkouts.length+" checkouts.");

                Utils.requestUIRefresh(getApplicationContext(), false, true);
            } else Log.d("Service-RSBS", "No checkouts fetched.");

            if(checkouts.length > 0) {
                if(shouldShowNotification) Notify.notifyNoAction(getApplication(), "Successfully pulled "+checkouts.length+" checkouts.", "Roblu Scouter successfully downloaded "+checkouts.length+" checkouts from" +
                        " the server at "+Utils.convertTime(System.currentTimeMillis()));
            }
        } catch(Exception e) {
            Log.d("Service-RSBS", "Failed to pull checkouts from the server: "+e.getMessage());
        }

        io.saveCloudSettings(cloudSettings);
    }

    private void uploadCompletedCheckouts(IO io, RSettings settings, CloudCheckoutRequest checkoutRequest, ObjectMapper mapper, RCloudSettings cloudSettings) {
        /*
         * Upload completed checkouts
         */
        try {
            Log.d("Service-RSBS", "Checking for pending checkout uploads...");
            ArrayList<RCheckout> checkouts = io.loadPendingCheckouts();
            if(checkouts != null && checkouts.size() > 0) {
                if(checkoutsUploadedLastTick) {
                    tickCount++;
                    if(tickCount == 2) {
                        tickCount = 0;
                        checkoutsUploadedLastTick = false;
                    }
                    return;
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

                boolean success = checkoutRequest.pushCheckouts(mapper.writeValueAsString(checkouts));
                if(success) {
                    boolean anyCompleted = false;
                    for(RCheckout checkout : checkouts) {
                        io.deletePendingCheckout(checkout.getID());
                        /*
                         * Be careful here:
                         * Anything in /pending/ with status "completed" will get deleted. So make sure that
                         * this doesn't have access to completed checkouts in the 'checkouts' array
                         */
                        if(checkout.getStatus() == HandoffStatus.COMPLETED) {
                            io.deleteMyCheckout(checkout.getID());
                            anyCompleted = true;
                        }
                    }
                    cloudSettings.setLastCheckoutSync(System.currentTimeMillis());
                    io.saveCloudSettings(cloudSettings);
                    /*
                     * Only notify the user if checkouts with status == completed where uploaded
                     */
                    Log.d("Service-RSBS", "Successfully uploaded "+checkouts.size()+" checkouts.");
                    Utils.requestUIRefresh(getApplicationContext(), true, false);
                    if(anyCompleted) Notify.notifyNoAction(getApplication(), "Uploaded checkouts successfully", "Successfully uploaded "+checkouts.size()+" checkouts.");
                    checkoutsUploadedLastTick = true;
                } else {
                    Log.d("Service-RSBS", "Failed to upload checkouts. Will try again next loop.");
                }
            }
        } catch(Exception e) {
            Log.d("Service-RSBS", "Failed to upload checkouts from /pending/. "+e.getMessage());
        }

    }
}
