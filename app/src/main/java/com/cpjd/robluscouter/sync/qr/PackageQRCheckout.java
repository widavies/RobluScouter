package com.cpjd.robluscouter.sync.qr;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.models.RTab;
import com.cpjd.robluscouter.models.metrics.RFieldDiagram;
import com.cpjd.robluscouter.models.metrics.RGallery;
import com.cpjd.robluscouter.models.metrics.RMetric;
import com.cpjd.robluscouter.utils.HandoffStatus;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.zip.GZIPOutputStream;

/**
 * Packages 1 checkout into a QR code and displays it.
 *
 * @since 4.3.0
 * @author Will Davies
 */
public class PackageQRCheckout extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("QR Exporter");
        }

        int checkoutID = getIntent().getIntExtra("checkoutID", 0);

        RSettings settings = new IO(getApplicationContext()).loadSettings();

        RCheckout checkout = new IO(getApplicationContext()).loadMyCheckout(checkoutID);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(checkout.getTeam().getName());
        }

        /*
         * A few parsing operations need to occur:
         * -All galleries  need to be removed, a QR code quite simply isn't
         * big enough to store them.
         * -All team TBA data should be removed (again, size constraints)
         *
         */
        checkout.getTeam().setFullName("");
        checkout.getTeam().setMotto("");
        checkout.getTeam().setWebsite("");
        checkout.getTeam().setLocation("");
        checkout.getTeam().setRookieYear(0);

        for(RTab tab : checkout.getTeam().getTabs()) {
            for(RMetric metric : tab.getMetrics()) {
                if(metric instanceof RGallery) {
                    ((RGallery) metric).setPictureIDs(null);
                    ((RGallery) metric).setImages(null);
                } else if(metric instanceof RFieldDiagram) {
                    ((RFieldDiagram) metric).setDrawings(null);
                }
            }

            LinkedHashMap<String, Long> edits = tab.getEdits();
            if(edits == null) edits = new LinkedHashMap<>();
            edits.put(settings.getName(), System.currentTimeMillis());
            tab.setEdits(edits);
        }

        /*
         * Flag all the status parameters and prepare the checkout for upload
         */
        checkout.setStatus(HandoffStatus.COMPLETED);
        checkout.setNameTag(settings.getName());
        checkout.setTime(System.currentTimeMillis());

        final ProgressDialog pd = ProgressDialog.show(PackageQRCheckout.this, "Generating...", "Roblu Scouter is generating a QR code. This might take a bit.");
        pd.setCancelable(true);
        pd.show();

        final RCheckout toExport = checkout;

        final ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Compress string
                    String serial = mapper.writeValueAsString(toExport);
                    byte[] data = compress(serial);
                    Log.d("RBS", "Here: "+new String(data, "UTF-8")+"END");
                    QrCode code = QrCode.encodeBinary(data, QrCode.Ecc.HIGH);
                    final Bitmap b = code.toImage(5, 2);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            ((ImageView) findViewById(R.id.qr)).setImageBitmap(b);
                        }
                    });
                } catch(Exception e) {
                    Log.d("RSBS", "Failed to generate QR code. "+e.getMessage());
                }
            }
        }).start();

    }

    private byte[] compress(String data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            try (GZIPOutputStream gzip = new GZIPOutputStream(out))
            {
                gzip.write(data.getBytes(StandardCharsets.UTF_8));
            }
            return out.toByteArray();
            //return out.toString(StandardCharsets.ISO_8859_1);
            // Some single byte encoding
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
