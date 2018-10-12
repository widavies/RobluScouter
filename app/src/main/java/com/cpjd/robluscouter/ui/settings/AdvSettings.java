package com.cpjd.robluscouter.ui.settings;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.cpjd.http.Request;
import com.cpjd.models.CloudTeam;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.sync.cloud.AutoCheckoutTask;
import com.cpjd.robluscouter.ui.UIHandler;
import com.cpjd.robluscouter.ui.bluetooth.BTDeviceSelector;
import com.cpjd.robluscouter.ui.dialogs.FastDialogBuilder;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.Utils;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

/**
 * AdvSettings manages application settings, including Google Sign-in
 *
 * This class is pretty messy. Hit me up if you'd like to improve it.
 *
 * @author Will Davies
 */
public class AdvSettings extends AppCompatActivity{

    private static RSettings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        settings = new IO(getApplicationContext()).loadSettings();

        getFragmentManager().beginTransaction().replace(R.id.blankFragment, new SettingsFragment()).commit();

        // UIHandler updates our activity to match what's set in RUI
        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, AutoCheckoutTask.AutoCheckoutTaskListener {

        private RUI rui;

        // text constants that will be accessible in the about libraries view
        private final String PRIVACY = "Roblu Privacy & Terms of Use\n" +
                "\nData that Roblu stores and transfers:\n-Google email\n-Google display name\n-FRC Name and Number\n-Any and all form data, including scouters' data, local data, and more." +
                "\n\nRoblu does NOT manage your Google password, payments, or any other data on your device that is not created by Roblu. Data is transferred over" +
                " an internet connection if syncing is enabled, and all connections are encrypted and secured using your team code. Scouting data is not inherently extremely sensitive information, so appropriate " +
                "cautions have been made to the level of security required. At any time, Roblu many crash or malfunction and all your data could be deleted. By using Roblu, you agree to all responsibility if your " +
                "data is lost, or data is stolen. If you do not agree, do not use Roblu.";

        private final String CONTRIBUTIONS = "Will Davies, Andy Pethan, Alex Harker, James Black & Boneyard Robotics";

        private final String CHANGELOG = "3.5.9\n-Added my matches\n-Improvements to searching and filtering\n-Ads removed, UI customizer available for everyone\n-Reworked cloud controls\n-Event import now searchable\n-Bug fixes" +
                "\n\n3.5.8\n-Bug fixes\n\n3.5.5 - 3.5.7\n-Changed app name to Roblu Master\n-Bug fixes\n\n3.5.4\n-Added custom sorting\n-Mark matches as won, delete, open on TBA\n-Bug fixes\n\n3.5.3\n-Bug fixes\n\n3.5.2\n-Added gallery elements\n-Bug fixes" +
                "\n\n3.5.0 - 3.5.1\n-Bug fixes\n\n3.0.0 - 3.4.9\n-Completed redesigned system\n-Redesigned file system\n-New form editor\n-New form elements\n-TBA-API improvements\n-Less restrictions on naming, editing, etc\n-New interface\n\n" +
                "2.0.0-2.9.9\nRoblu Version 2, we don't talk about that anymore\n\n1.0.0-1.9.9\nRoblu Version 1 is where humans go to die";

        private ProgressDialog pd;
        private Preference p;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if(!(getActivity() instanceof AdvSettings)) {
                getActivity().finish();
                return;
            }
            addPreferencesFromResource(R.xml.preferences);

            EditTextPreference username = (EditTextPreference) findPreference("username");
            username.setDefaultValue(settings.getName());
            username.setText(settings.getName());
            username.setOnPreferenceChangeListener(this);

            findPreference("about").setOnPreferenceClickListener(this);
            findPreference("privacy").setOnPreferenceClickListener(this);
            findPreference("server_ip").setOnPreferenceChangeListener(this);
            findPreference("bt_devices").setOnPreferenceClickListener(this);
            findPreference("reset").setOnPreferenceClickListener(this);
            findPreference("website").setOnPreferenceClickListener(this);
            findPreference("tutorial").setOnPreferenceClickListener(this);
            p = findPreference("sync_list");
            StringBuilder devices = new StringBuilder();
            if(settings.getBluetoothServerMACs() != null) {
                for(String s : settings.getBluetoothServerMACs()) {
                    devices.append(s).append("\n");
                }
            }
            p.setOnPreferenceClickListener(this);
            p.setSummary("This will clear the following devices from your sync list:\n"+devices.toString());

            ListPreference lp = (ListPreference) findPreference("auto_checkouts");
            lp.setValueIndex(settings.getAutoAssignmentMode());
            lp.setOnPreferenceChangeListener(this);

            EditTextPreference preference = (EditTextPreference) findPreference("team");
            preference.setOnPreferenceChangeListener(this);
            preference.getEditText().setHint("Team code");

            android.preference.CheckBoxPreference disableSyncing = (android.preference.CheckBoxPreference) findPreference("disable_syncing");
            disableSyncing.setOnPreferenceChangeListener(this);
            disableSyncing.setChecked(settings.isSyncDisabled());

            rui = settings.getRui();
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getKey().equals("about")) {
                new LibsBuilder().withFields(R.string.class.getFields()).withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR).withAboutIconShown(true).withAboutVersionShown(true).withAboutDescription("Copyright 2017. A scouting app for robotics competitions focused on customization, simplicity, and functionality. Roblu is a" +
                        " project designed to streamline your scouting experience. Thank you to Andy Pethan and Isaac Faulkner for all the help. App written by Will Davies.")
                        .withActivityTitle("About Roblu").withLicenseShown(true).withAboutSpecial1("Privacy").withAboutSpecial1Description(PRIVACY).withAboutSpecial2("Contributors").withAboutSpecial2Description(CONTRIBUTIONS).withAboutSpecial3("Changelog")
                        .withAboutSpecial3Description(CHANGELOG).
                        start(getActivity());
                return true;
            }
            else if(preference.getKey().equals("sync_list")) {
                new FastDialogBuilder()
                        .setTitle("Are you sure")
                        .setMessage("Are you sure you want to remove ALL devices from your sync list?")
                        .setPositiveButtonText("Yes")
                        .setNegativeButtonText("No")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                settings.getBluetoothServerMACs().clear();
                                new IO(getActivity()).saveSettings(settings);
                                p.setSummary("This will clear the following devices from your sync list:\n");
                            }

                            @Override
                            public void denied() {

                            }

                            @Override
                            public void neutral() {

                            }
                        }).build(getActivity());
            }
            else if(preference.getKey().equals("bt_devices")) {
                Intent i = new Intent(getActivity(), BTDeviceSelector.class);
                startActivityForResult(i, Constants.GENERAL);
                return true;
            }
            else if(preference.getKey().equals("tutorial")) {
                String url = "https://docs.google.com/document/d/191km1s6HqZF8Ag6bCwn_XWA54MepjL5gU8-N05ean3w/edit?usp=sharing";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            else if(preference.getKey().equals("privacy")) {
                String url = "https://www.roblu.net/privacy";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            else if(preference.getKey().equals("website")) {
                String url = "https://www.roblu.net";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            else if(preference.getKey().equals("reset")) {
                new FastDialogBuilder()
                        .setTitle("WARNING")
                        .setMessage("This will delete ALL scouting data in this app. Are you sure?")
                        .setPositiveButtonText("Delete")
                        .setNegativeButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                IO io = new IO(getActivity());
                                io.clearCheckouts();
                            }

                            @Override
                            public void denied() {

                            }

                            @Override
                            public void neutral() {

                            }
                        }).build(getActivity());
            }
            return false;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            settings = new IO(getActivity()).loadSettings();

            StringBuilder devices = new StringBuilder();
            if(settings.getBluetoothServerMACs() != null) {
                for(String s : settings.getBluetoothServerMACs()) {
                    devices.append(s).append("\n");
                }
            }
            p.setSummary("This will clear the following devices from your sync list:\n"+devices.toString());
        }

        @Override
        public boolean onPreferenceChange(final Preference preference, Object o) {
           if(preference.getKey().equals("team")) {
               StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
               StrictMode.setThreadPolicy(policy);

               if(!Utils.hasInternetConnection(getActivity())) {
                    Utils.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "You are not connected to the internet.", true, 0);
                    return false;
                }
               Request r = new Request(settings.getServerIP());
               CloudTeam cloudTeam = new CloudTeamRequest(r, o.toString()).getTeam(-1);
               if(r.ping() && cloudTeam != null) {
                   Log.d("RSBS", "Successfully joined team!");
                   Toast.makeText(getActivity(), "Successfully joined team.", Toast.LENGTH_LONG).show();
               }

               settings.setCode(o.toString());
           }
           // disable syncing option
           else if(preference.getKey().equalsIgnoreCase("disable_syncing")) {
               settings.setSyncDisabled((Boolean)o);
           }
           // user selected auto checkouts option
           else if(preference.getKey().equalsIgnoreCase("auto_checkouts")) {
               settings.setAutoAssignmentMode(((ListPreference)preference).findIndexOfValue(o.toString()));
               pd = ProgressDialog.show(getActivity(), "Checking out checkouts...", "Please wait...");
               pd.setCancelable(false);
               new AutoCheckoutTask(this, new IO(getActivity()), settings, null, true).start();
           }
           else if(preference.getKey().equalsIgnoreCase("server_ip")) {
               if(o.toString() == null || o.toString().equals("") || o.toString().replaceAll(" ", "").equals("")) {
                   settings.setServerIPToDefault();
               } else settings.setServerIP(o.toString());
           }
           else if(preference.getKey().equals("username")) {
               settings.setName(o.toString());
           }
            new IO(getActivity()).saveSettings(settings);
            return true;
        }

        @Override
        public void done() {
            pd.dismiss();
        }
    }
}
