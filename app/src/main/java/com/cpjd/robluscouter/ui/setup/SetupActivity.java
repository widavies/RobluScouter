package com.cpjd.robluscouter.ui.setup;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.ui.checkouts.CheckoutsView;

public class SetupActivity extends FragmentActivity implements View.OnClickListener {

    private DisableSwipeViewPager pager;
    private RSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        settings = new IO(getApplicationContext()).loadSettings();

        if(getActionBar() != null) getActionBar().hide();

        pager = findViewById(R.id.view_pager);
        View welcomeNextButton = findViewById(R.id.welcome_next_page);
        View bluetoothNextButton = findViewById(R.id.bluetooth_next_page);
        View signInButton = findViewById(R.id.signin_button);
        View permsNext = findViewById(R.id.permissions_next_page);
        View finished = findViewById(R.id.finished_next);
        pager.setOffscreenPageLimit(5);
        pager.setAdapter(new SetupFragmentAdapter(this));

        welcomeNextButton.setOnClickListener(this);
        bluetoothNextButton.setOnClickListener(this);
        permsNext.setOnClickListener(this);
        signInButton.setOnClickListener(this);
        finished.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        pager.goToNextPage();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.welcome_next_page:
                pager.goToNextPage();
                break;
            case R.id.bluetooth_next_page:
                pager.goToNextPage();
                break;
            case R.id.permissions_next_page:
                String[] perms = {
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
                ActivityCompat.requestPermissions(this, perms, 0);
                break;
            case R.id.signin_button:
                pager.goToNextPage();
                break;
            case R.id.finished_next:
                setupFinished();
                break;
        }
    }

    public void setupFinished() {
        startActivity(new Intent(this, CheckoutsView.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        pager.goToPreviousPage();
    }

}

