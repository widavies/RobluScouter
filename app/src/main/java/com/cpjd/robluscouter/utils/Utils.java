package com.cpjd.robluscouter.utils;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.models.RTeam;
import com.cpjd.robluscouter.models.metrics.RMetric;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Utils is a general utility class for miscellaneous tasks through Roblu Scouter.
 *
 * @since 1.0.0
 * @author Will Davies
 */
public class Utils {

    /**
     * Width of the screen size in pixels
     */
    public static int WIDTH;

    /**
     * Width of the screen in pixels, this is used to help do some sizing for UI elements later on
     * @see com.cpjd.robluscouter.ui.forms.RMetricToUI
     * @param activity a reference to the activity for screen size fetching
     */
    public static void initWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Utils.WIDTH = size.x;
    }

    /**
     * Rounds a decimal value
     * @param value the decimal to round
     * @param precision the number of digits to the right of the decimal point to keep
     * @return the rounded decimal value
     */
    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     * Converts a millisecond timestamp into a human readable format
     * @param timeMillis unix timestamp to convert
     * @return human readable date string
     */
    public static String convertTime(long timeMillis) {
        if(timeMillis == 0) return "Never";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        Date resultdate = new Date(timeMillis);
        return sdf.format(resultdate);
    }

    public static ArrayList<RMetric> duplicateRMetricArray(ArrayList<RMetric> metrics) {
        if(metrics == null || metrics.size() == 0) return new ArrayList<>();
        ArrayList<RMetric> newMetrics = new ArrayList<>();
        for(RMetric m : metrics) {
            newMetrics.add(m.clone());
        }
        return newMetrics;
    }

    /**
     * Converts a list of RTeam objects into a nicely formatted string
     * @param teams teams to concatenate
     * @return a formatted string representing the RTeam array
     */
    public static String concatenateTeams(ArrayList<RTeam> teams) {
        if(teams == null || teams.size() == 0) return "";

        String temp = "";
        for(int i = 0; i < teams.size(); i++) {
            if(i != teams.size() - 1) temp += "#"+teams.get(i).getNumber()+", ";
            else temp += "#"+teams.get(i).getNumber();
        }
        return temp;
    }

    /**
     * Checks if the Roblu client background service is running
     * @param context  context
     * @return true if the service is running
     */
    public static boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String localService = "com.cpjd.robluscouter.sync.cloud.Service";
            if (localService.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests a UI refresh in checkouts view.
     *
     * Note: This will reload checkouts from the disk.
     * @param context a context reference
     * @param refreshMyCheckouts if the my checkouts tab should be refreshed
     * @param refreshCheckouts if the checkouts tab should be refreshed
     */
    public static void requestUIRefresh(Context context, boolean refreshMyCheckouts, boolean refreshCheckouts) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.SERVICE_ID);
        broadcast.putExtra("mycheckouts", refreshMyCheckouts);
        broadcast.putExtra("checkouts", refreshCheckouts);
        context.sendBroadcast(broadcast);
    }

    public static void requestTeamViewerRefresh(Context context) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.SERVICE_ID);
        broadcast.putExtra("teamViewerOnly", true);
        context.sendBroadcast(broadcast);
    }

    /**
     * Tests the device for an internet connection
     * @param context context
     * @return true if the device has an internet connection (data or wifi)
     */
    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Converts DP (density pixels) to screen pixels
     * @param context context
     * @param dp density pixels to convert
     * @return pixel result
     */
    public static int DPToPX(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * A method from the Android code that is SDK 16 only, but we need it for SDK 19+
     * @return
     */
    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    /**
     * A handy little method for showing small dialogs at the bottom of the screen
     * @param layout the layout to show the dialog on
     * @param context activity context
     * @param text the text to display
     * @param error if it's an error message (if so, color variable is discarded, so just pass it 0)
     * @param color the background color of the dialog, IF it's not an error snackbar
     */
    public static void showSnackbar(View layout, Context context, String text, boolean error, int color) {
        Snackbar s = Snackbar.make(layout, text, Snackbar.LENGTH_LONG);
        if(error) s.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.red));
        else s.getView().setBackgroundColor(color);
        s.show();
    }

    public static void setCursorColor(AppCompatEditText view, @ColorInt int color) {
        try {
            // Get the cursor resource id
            Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            int drawableResId = field.getInt(view);

            // Get the editor
            field = TextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            Object editor = field.get(view);

            // Get the drawable and set a color filter
            Drawable drawable = ContextCompat.getDrawable(view.getContext(), drawableResId);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            Drawable[] drawables = {drawable, drawable};

            // Set the drawables
            field = editor.getClass().getDeclaredField("mCursorDrawable");
            field.setAccessible(true);
            field.set(editor, drawables);
        } catch (Exception ignored) {
        }
    }

    public static boolean isInLandscapeMode(@NonNull Context context) {
        boolean isLandscape = false;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        }
        return isLandscape;
    }

    public static int getScreenWidth(@NonNull Context context) {
        Point size = new Point();
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(size);
        return size.x;
    }
}
