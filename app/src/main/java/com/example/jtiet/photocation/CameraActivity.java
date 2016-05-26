package com.example.jtiet.photocation;

import android.Manifest;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.*;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private static final String APP_NAME = "Photo-cation";


    private Camera mCamera;
    private CameraPreview mPreview;

    private String mAddressString;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private double mLongitude;
    private double mLatitude;

    public TextView mAddressLocationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

        mCamera = getCameraInstance();

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mAddressLocationTextView = (TextView) findViewById(R.id.location_textview);

        Button captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {

        }
        return c;
    }

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(APP_NAME, "Error creating media file.");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(APP_NAME, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(APP_NAME, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private static File getFile(int type) {
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_NAME);

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d(APP_NAME, "Failed to create directory!!!");
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(storageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(storageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        }
        else {
            return null;
        }
        return mediaFile;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (checkLocationPermission() == 0) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLongitude = mLastLocation.getLongitude();
            mLatitude = mLastLocation.getLatitude();

            mAddressString = getAddressFromCoords(mLatitude, mLongitude, 1);
            mAddressLocationTextView.setText(mAddressString);
        }
    }

    public String getAddressFromCoords(double latitude, double longitude, int maxResults) {
        String address = null;
        List<Address> addresses;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, maxResults);
            address = addresses.get(0).getAddressLine(1);
            Log.d("CameraActivity", address);
        } catch (IOException e) {
            Log.d("CameraActivity", "getAddressFromCoords: Cannot reverse geocode");
            e.printStackTrace();
        }
        return address;
    }

    private int checkLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionCheck;
    }

}
