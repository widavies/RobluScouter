package com.cpjd.robluscouter.sync.cloud;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cpjd.http.Request;
import com.cpjd.models.CloudCheckout;
import com.cpjd.models.CloudTeam;
import com.cpjd.requests.CloudCheckoutRequest;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RForm;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RSyncSettings;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.notifications.Notify;
import com.cpjd.robluscouter.sync.SyncHelper;
import com.cpjd.robluscouter.utils.HandoffStatus;
import com.cpjd.robluscouter.utils.Utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
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

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if(Build.VERSION.SDK_INT >= 26) {
                String CHANNEL_ID = "my_channel_01";
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        "Roblu Scouter Service",
                        NotificationManager.IMPORTANCE_DEFAULT);

                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("")
                        .setContentText("").build();

                startForeground(1, notification);
            }
        } catch(Exception e) {
            Log.d("RSBS", "Failed to start foreground service.");
        }

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

        RSyncSettings cloudSettings = io.loadCloudSettings();
        Request r = new Request(settings.getServerIP());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CloudTeamRequest teamRequest = new CloudTeamRequest(r, settings.getCode());
        CloudCheckoutRequest checkoutRequest = new CloudCheckoutRequest(r, settings.getCode());
        SyncHelper syncHelper = new SyncHelper(getApplicationContext(), SyncHelper.MODES.NETWORK);

        if(settings.isSyncDisabled()) {
            Log.d("Service-RBS", "Syncing is disabled. Terminating loop.");
            return;
        }

        if(!r.ping()) {
            Log.d("Service-RSBS", "Roblu server is down. Unable to connect.");
            return;
        }

        if(!teamRequest.isActive() && settings.getCode() != null && !settings.getCode().equals("")) {
            Log.d("Service-RSBS", "No active event found. Terminating loop early.");
            io.clearCheckouts();
            cloudSettings.setEventName("");
            cloudSettings.setTeamSyncID(0);
            cloudSettings.getCheckoutSyncIDs().clear();
            io.saveCloudSettings(cloudSettings);
            Utils.requestUIRefresh(getApplicationContext(), true, true);
            return;
        }

        /*
         * Fetch form, ui and related data
         */
        try {
            Log.d("Service-RSBS", "Checking for team data...");
            CloudTeam cloudTeam = teamRequest.getTeam(cloudSettings.getTeamSyncID());
            if(cloudTeam != null) {
                RForm form = mapper.readValue(cloudTeam.getForm(), RForm.class);
                io.saveForm(form);
                settings.setRui(mapper.readValue(cloudTeam.getUi(), RUI.class));
                io.saveSettings(settings);

                /*
                 * This helps prevent some errors, if a new event name is received, go ahead and clear out the checkouts
                 */
                if(cloudTeam.getActiveEventName() != null && cloudSettings.getEventName() != null && !cloudTeam.getActiveEventName().equals("")
                        && !cloudSettings.getEventName().equals("") && (cloudTeam.getActiveEventName().toLowerCase().trim().equalsIgnoreCase(
                                cloudSettings.getEventName().toLowerCase().trim()))) {
                    Log.d("Service-RSBS", "No active event found. Terminating loop early.");
                    io.clearCheckouts();
                    cloudSettings.setEventName("");
                    cloudSettings.setTeamSyncID(0);
                    cloudSettings.getCheckoutSyncIDs().clear();
                    io.saveCloudSettings(cloudSettings);
                    Utils.requestUIRefresh(getApplicationContext(), true, true);
                }

                cloudSettings.setEventName(cloudTeam.getActiveEventName());
                cloudSettings.setTeamNumber((int)cloudTeam.getNumber());
                cloudSettings.setTeamSyncID(cloudTeam.getSyncID());
                io.saveCloudSettings(cloudSettings);
                Notify.notifyNoAction(getApplicationContext(), "Form received", "Roblu Scouter successfully received an updated form.");
                Utils.requestTeamViewerRefresh(getApplicationContext());
                Log.d("Service-RSBS", "Form, ui, and team info successfully pulled.");
            }
        } catch(Exception e) {
            Log.d("Service-RSBS", "Failed to check for form, ui, team info: "+e.getMessage());
        }

        // Upload completed checkouts
        uploadCompletedCheckouts(io, settings, checkoutRequest, mapper);

        /*
         * Fetch checkouts
         */
        try {
            Log.d("Service-RSBS", "Checking for checkouts to fetch... ");
            CloudCheckout[] checkouts;

            if(cloudSettings.getCheckoutSyncIDs().size() == 0) checkouts = checkoutRequest.pullCheckouts(null, true);
            else checkouts = checkoutRequest.pullCheckouts(syncHelper.packSyncIDs(cloudSettings.getCheckoutSyncIDs()), false);

            syncHelper.unpackCheckouts(checkouts, cloudSettings);

            Log.d("Service-RSBS", "Successfully fetched checkouts");
        } catch(Exception e) {
            Log.d("Service-RSBS", "Failed to pull checkouts from the server: "+e.getMessage());
        }

        io.saveCloudSettings(cloudSettings);

    }

    private void uploadCompletedCheckouts(IO io, RSettings settings, CloudCheckoutRequest checkoutRequest, ObjectMapper mapper) {
        /*
         * Upload completed checkouts
         */
        try {
            Log.d("Service-RSBS", "Checking for pending checkout uploads...");
            ArrayList<RCheckout> checkouts = io.loadPendingCheckouts();
            boolean success = checkoutRequest.pushCheckouts(new SyncHelper(getApplicationContext(), SyncHelper.MODES.NETWORK).packCheckouts(checkouts));
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
                    /*
                     * Only notify the user if checkouts with status == completed where uploaded
                     */
                Log.d("Service-RSBS", "Successfully uploaded "+checkouts.size()+" checkouts.");
                Utils.requestUIRefresh(getApplicationContext(), true, false);
                if(anyCompleted) Notify.notifyNoAction(getApplication(), "Uploaded checkouts successfully", "Successfully uploaded "+checkouts.size()+" checkouts.");
            } else {
                Log.d("Service-RSBS", "Failed to upload checkouts. Will try again next loop.");
            }
        } catch(Exception e) {
            Log.d("Service-RSBS", "Failed to upload checkouts from /pending/. "+e.getMessage());
        }

    }
}
