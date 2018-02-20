package com.cpjd.robluscouter.utils;

public class Constants {

    public static final String SERVICE_ID = "com.cpjd.robluscouter.service";
    public static final String RESTART_BROADCAST = "com.cpjd.robluscouter.RestartService";

    public static final int GENERAL = getNum(); // when the request code doesn't matter
    public static final int CAMERA_REQUEST = getNum(); // request to take a picture
    public static final int GALLERY_EXIT = getNum();
    public static final int TEAM_EDITED = getNum();
    public static final int CLOUD_SIGN_IN = getNum();
    public static final int MY_CHECKOUT_EDIT_REQUEST = getNum();
    public static final int IMAGE_EDITED = getNum();
    public static final int IMAGE_DELETED = getNum();

    public static final int FIELD_DIAGRAM_EDITED = getNum();

    /*
     * RecyclerViewAdapter mode constants
     */
    public static final int MY_CHECKOUTS = 0;
    public static final int CHECKOUTS = 1;
    public static final int MY_MATCHES = 2;

    private static int count = 0;
    private static int getNum() {
        return count++;
    }
}
