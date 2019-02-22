package com.cpjd.robluscouter.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.cpjd.robluscouter.R;

/**
 * Allows notifications to be sent to the user for various
 * events.
 *
 * @version 2
 * @since 3.6.3
 * @author Will Davies
 */
public class Notify {

    public static void createNotificationChannel(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("roblu-scouter", "Roblu Scouter", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static void notify(Context activity, String title, String content, Intent action) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "roblu-scouter").setSmallIcon(R.drawable.launcher)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentTitle(title).setPriority(Notification.PRIORITY_HIGH);

        PendingIntent pending = PendingIntent.getActivity(activity, 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pending);

        NotificationManager notifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyMgr != null) notifyMgr.notify((int) ((System.currentTimeMillis() / 1000L) % Integer.MAX_VALUE), builder.build());
    }

    public static void notifyNoAction(Context activity, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "roblu-scouter").setSmallIcon(R.drawable.launcher)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content)).setContentText(content)
                .setContentTitle(title).setPriority(Notification.PRIORITY_HIGH);

        NotificationManager notifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyMgr != null) notifyMgr.notify((int) ((System.currentTimeMillis() / 1000L) % Integer.MAX_VALUE), builder.build());
    }

}
