package com.cpjd.robluscouter.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RCloudSettings;
import com.cpjd.robluscouter.models.RForm;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RUI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * IO manages interactions with the file system, mainly by serializing/de-serializing models
 *
 * Directories managed by IO:
 * -/PREFIX/checkouts: master checkouts list that is synced with the server
 * -/PREFIX/pending: locally edited checkouts waiting to upload
 * -/PREFIX/settings.ser
 * -/PREFIX/cloudSettings.ser
 * -/PREFIX/mycheckouts
 * -/PREFIX/images
 *
 * @version 4
 * @since 1.0.0
 * @author Will Davies
 *
 */
public class IO {

    private Context context;
    private static final String PREFIX = "v6";

    public IO(Context context) {
        this.context = context;
    }

    /**
     * Must be called at application startup, ASAP
     *
     * Does the following:
     * -Makes sure settings file exists, if not, creates default
     * -Ensures /checkouts/ exists
     * -Removes old data (from older prefixes)
     *
     * @return true if this is first launch (based on if the settings file had to be created)
     */
    public static boolean init(Context context) {
        // Create prefix directory
        if(!new File(context.getFilesDir(), PREFIX).exists()) {
            if(new File(context.getFilesDir(), PREFIX).mkdir()) Log.d("RSBS", "Prefix dir successfully created.");
        }

        // Create checkouts directory
        File checkouts = new File(context.getFilesDir(), PREFIX+File.separator+"checkouts");
        if(!checkouts.exists()) {
            if(checkouts.mkdir()) Log.d("RSBS", "/checkouts/ dir successfully created.");
        }

        // Create mycheckouts directory
        File mycheckouts = new File(context.getFilesDir(), PREFIX+File.separator+"mycheckouts");
        if(!mycheckouts.exists()) {
            if(mycheckouts.mkdir()) Log.d("RSBS", "/mycheckouts/ dir successfully created.");
        }

        // Create pending dir
        File pending = new File(context.getFilesDir(), PREFIX+File.separator+"pending");
        if(!pending.exists()) {
            if(pending.mkdir()) Log.d("RSBS", "/pending/ dir successfully created.");
        }

        // Check settings
        RSettings settings = new IO(context).loadSettings();
        if(settings == null) {
            settings = new RSettings();
            settings.setRui(new RUI());
            new IO(context).saveCloudSettings(new RCloudSettings());
            new IO(context).saveSettings(settings);
            return true;
        }
        return false;
    }

    /**
     * Load a cloud settings object from internal storage
     * @return RCloudSettings object instance
     */
    public RCloudSettings loadCloudSettings() {
        RCloudSettings cloudSettings = (RCloudSettings) deserializeObject(PREFIX+File.separator+"cloudSettings.ser");
        if(cloudSettings == null) {
            cloudSettings = new RCloudSettings();
            saveCloudSettings(cloudSettings);
        }
        return cloudSettings;
    }

    /**
     * Save a cloud settings reference to internal storage
     * @param settings RCloudSettings object instance
     */
    public void saveCloudSettings(RCloudSettings settings) {
        serializeObject(settings, PREFIX+File.separator+"cloudSettings.ser");
    }


    /**
     * Load settings object from internal storage
     * @return RSettings object instance
     */
    public RSettings loadSettings() {
        return (RSettings) deserializeObject(PREFIX+File.separator+"settings.ser");
    }

    /**
     * Save a settings reference to internal storage
     * @param settings RSettings object instance
     */
    public void saveSettings(RSettings settings) {
        serializeObject(settings, PREFIX+File.separator+"settings.ser");
    }

    /**
     * Gets a form instance from internal storage
     *
     * -Will return null if form has not been synced yet or it doesn't exist
     *
     * @return form instance, may be null!
     */
    public RForm loadForm() {
        return (RForm) deserializeObject(PREFIX+File.separator+"form.ser");
    }

    /**
     * Saves the form object to internal storage
     * @param form the form to save
     */
    public void saveForm(RForm form) {
        serializeObject(form, PREFIX+File.separator+"form.ser");
    }

    /*
     * /checkouts/ METHODS
     */
    /**
     * Save a checkout object instance to internal storage
     * @param checkout the checkout object instance
     */
    public void saveCheckout(RCheckout checkout) {
        serializeObject(checkout, PREFIX+File.separator+"checkouts"+File.separator+checkout.getID()+".ser");
    }

    /**
     * Load a specific checkout instance
     *
     * -Will return null if id is not found in /checkouts/
     * @param id the long id of the checkout to load.
     */
    public RCheckout loadCheckout(int id) {
        RCheckout checkout = (RCheckout) deserializeObject(PREFIX+File.separator+"checkouts"+File.separator+id+".ser");
        if(checkout != null) checkout.setID(id);
        return checkout;
    }

    /**
     * Load all checkouts from /checkouts/
     * @return a list of all available checkouts from internal storage, may be null
     */
    public ArrayList<RCheckout> loadCheckouts() {
        File[] files = getChildFiles(PREFIX+File.separator+"checkouts"+File.separator);
        if(files == null || files.length == 0) return null;
        ArrayList<RCheckout> checkouts = new ArrayList<>();
        for(File file : files) checkouts.add(loadCheckout(Integer.parseInt(file.getName().replace(".ser", ""))));
        return checkouts;
    }
    // End of /checkouts/ methods

    /*
     * /mycheckouts/ METHODS
     */
    /**
     * Save a checkout object instance to internal storage
     * @param checkout the checkout object instance
     */
    public void saveMyCheckout(RCheckout checkout) {
        serializeObject(checkout, PREFIX+File.separator+"mycheckouts"+File.separator+checkout.getID()+".ser");
    }

    /**
     * Load a specific checkout instance
     *
     * -Will return null if id is not found in /checkouts/
     * @param id the long id of the checkout to load.
     */
    public RCheckout loadMyCheckout(int id) {
        RCheckout checkout = (RCheckout) deserializeObject(PREFIX+File.separator+"mycheckouts"+File.separator+id+".ser");
        if(checkout != null) checkout.setID(id);
        return checkout;
    }

    /**
     * Deletes a pending checkout from /mycheckouts/.
     * This happens when the checkout has been successfully uploaded
     * @param id the ID of the checkout to delete from /pending/
     */
    public void deleteMyCheckout(int id) {
        delete(new File(context.getFilesDir(), PREFIX+File.separator+"mycheckouts"+File.separator+id+".ser"));
    }

    /**
     * Load all checkouts from /checkouts/
     * @return a list of all available checkouts from internal storage, may be null
     */
    public ArrayList<RCheckout> loadMyCheckouts() {
        File[] files = getChildFiles(PREFIX+File.separator+"mycheckouts"+File.separator);
        if(files == null || files.length == 0) return null;
        ArrayList<RCheckout> checkouts = new ArrayList<>();
        for(File file : files) checkouts.add(loadMyCheckout(Integer.parseInt(file.getName().replace(".ser", ""))));
        return checkouts;
    }
    // End of /mycheckouts/ methods

    /*
     * /pending/ METHODS
     */

    /**
     * Save a checkout object instance to internal storage at /pending/
     *
     * CRITICAL:
     * -DO NOT save anything that is status "completed" but not in "my checkouts:"
     * @param checkout the checkout object instance
     */
    public void savePendingCheckout(RCheckout checkout) {
        serializeObject(checkout, PREFIX+File.separator+"pending"+File.separator+checkout.getID()+".ser");
    }

    /**
     * Load a specific checkout instance
     *
     * -Will return null if id is not found in /pending/
     * @param id the long id of the checkout to load.
     */
    private RCheckout loadPendingCheckout(int id) {
        RCheckout checkout = (RCheckout) deserializeObject(PREFIX+File.separator+"pending"+File.separator+id+".ser");
        if(checkout != null) checkout.setID(id);
        return checkout;
    }

    /**
     * Deletes a pending checkout from /pending/.
     * This happens when the checkout has been successfully uploaded
     * @param id the ID of the checkout to delete from /pending/
     */
    public void deletePendingCheckout(int id) {
        delete(new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator+id+".ser"));
    }



    /**
     * Load all checkouts from /pending/
     * @return a list of all available checkouts from internal storage, may be null
     */
    public ArrayList<RCheckout> loadPendingCheckouts() {
        File[] files = getChildFiles(PREFIX+File.separator+"pending"+File.separator);
        if(files == null || files.length == 0) return null;
        ArrayList<RCheckout> checkouts = new ArrayList<>();
        for(File file : files) checkouts.add(loadPendingCheckout(Integer.parseInt(file.getName().replace(".ser", ""))));
        return checkouts;
    }
    // End of /pending/methods

    /**
     * Deletes all checkouts and pending checkouts, presumably because the event has been flagged as in-active by the user
     */
    public void clearCheckouts() {
        File checkoutsDir = new File(context.getFilesDir(), PREFIX +File.separator+"checkouts"+File.separator);
        if(checkoutsDir.listFiles() != null) {
            for(File f : checkoutsDir.listFiles()) {
                delete(f);
            }
        }

        File mycheckoutsDir = new File(context.getFilesDir(), PREFIX+File.separator+"mycheckouts"+File.separator);
        if(mycheckoutsDir.listFiles() != null) {
            for(File f : mycheckoutsDir.listFiles()) {
                delete(f);
            }
        }

        File pendingDir = new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator);
        if(pendingDir.listFiles() != null) {
            for(File f : pendingDir.listFiles()) {
                delete(f);
            }
        }

        delete(new File(PREFIX + File.separator+"settings.ser"));
        delete(new File(PREFIX + File.separator+"cloudSettings.ser"));

    }


    // ********************UTILITY METHODS**************************
    /*
     * Pictures - This will allow pictures to be saved and loaded with much less of an effect on memory
     */

    /**
     * Saves a pre-existing image to the file system with a new ID
     * @param image the image to write
     * @return the ID of the new image, -1 if an error occurred
     */
    public int savePicture(byte[] image) {
        try {
            int ID = getNewPictureID();
            File file = new File(context.getFilesDir(), PREFIX+File.separator+"images"+File.separator+ID+".jpg");
            if(!file.getParentFile().exists()) if(file.getParentFile().mkdirs()) Log.d("RSBS", "Successfully created /images directory.");
            FileOutputStream fos = new FileOutputStream(file.getPath());
            fos.write(image);
            fos.close();
            return ID;
        } catch(Exception e) {
            Log.d("RSBS", "Failed to save existing picture.");
            return -1;
        }
    }

    /**
     * Deletes a picture from the local disk. Keep in mind, this picture's ID should also be removed from the associated RGallery
     * metric.
     * @param pictureID the ID of the picture to be deleted
     */
    public void deletePicture(int pictureID) {
        delete(new File(context.getFilesDir(), PREFIX+ File.separator+"images"+File.separator+pictureID+".jpg"));
    }

    /**
     * Loads a picture from the local disk into memory. This byte[] array should be inserted into an RGallery before launching the image gallery viewer.
     * @param pictureID the pictureID that contains the picture
     * @return a byte[] representing the picture
     */
    public byte[] loadPicture(int pictureID) {
        File image = new File(context.getFilesDir(), PREFIX+File.separator+"images"+File.separator+pictureID+".jpg");
        if(!image.getParentFile().exists()) if(image.getParentFile().mkdirs()) Log.d("RSBS", "Successfully created /images directory.");
        Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * Gets a new available picture ID
     * @return the ID of a new picture.
     */
    private int getNewPictureID() {
        File f = new File(context.getFilesDir(), PREFIX+File.separator+"images"+File.separator);
        if(!f.exists()) if(f.mkdirs()) Log.d("RSBS", "Successfully created /images directory.");
        int maxID = 0;
        File[] children = f.listFiles();
        if(children == null || children.length == 0) return maxID;
        for(File file : children) {
            int newID = Integer.parseInt(file.getName().replaceAll(".jpg", ""));
            if(newID > maxID) maxID = newID;
        }
        return maxID + 1;
    }

    /**
     * Gets a temporary picture file for usage with the camera
     * @return returns file where the picture can be stored temporarily
     */
    public File getTempPictureFile() {
        File path = new File(context.getCacheDir(), PREFIX + File.separator +"temp.jpg");
        path.mkdirs();
        if(path.exists()) {
            if(!path.delete()) Log.d("RSBS", "Failed to delete old cached picture file.");
        }
        try {
            if(!path.createNewFile()) Log.d("RSBS", "Failed to create a picture cache file.");
        } catch(Exception e) { return null; }
        return path;
    }

    /**
     * Returns a list of the files within a folder
     * @param location the location of the folder to get children contents from
     * @return a list of the files in the target folder
     */
    private File[] getChildFiles(String location) {
        File file = new File(context.getFilesDir(), location);
        return file.listFiles();
    }

    /**
     * Convert an object instance into a file
     * @param object object instance to write
     * @param location location to write the file to
     */
    private void serializeObject(Object object, String location) {
        try {
            File file = new File(context.getFilesDir(), location);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(object);
            out.close();
            fos.close();
        } catch(Exception e) {
            Log.d("RSBS", "Failed to serialize object at location "+location+" err msg: "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convert a file into an object instance
     * @param location location of the file
     * @return object instance with the corresponding type in /models/
     */
    private Object deserializeObject(String location) {
        try {
            FileInputStream fis = new FileInputStream(new File(context.getFilesDir(), location));
            ObjectInputStream in = new ObjectInputStream(fis);
            Object o = in.readObject();
            in.close();
            fis.close();
            return o;
        } catch(Exception e) {
            Log.d("RSBS", "Failed to deserialize object at location "+location+", err msg: "+e.getMessage());
            return null;
        }
    }

    /**
     * Recursively delete a folder and all of its contents
     * @param folder the folder to delete
     */
    private static void delete(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    delete(f);
                } else {
                    if(f.delete()) Log.d("RSBS", f.getAbsolutePath()+" was deleted successfully.");
                }
            }
        }
        if(folder.delete()) Log.d("RSBS", folder.getAbsolutePath() +" was deleted successfully.");
    }
}