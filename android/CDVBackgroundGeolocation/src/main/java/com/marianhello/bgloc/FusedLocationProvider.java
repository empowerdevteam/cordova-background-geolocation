package com.marianhello.bgloc.provider;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.provider.AbstractLocationProvider;

public class FusedLocationProvider extends AbstractLocationProvider implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private LocationManager locationManager;
    private boolean isStarted = false;
    private GoogleApiClient googleApiClient;
    private Context context;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Location location;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds

    public FusedLocationProvider(Context context) {
        super(context);
        PROVIDER_ID = Config.FUSED_PROVIDER;
        this.context = context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //  build google api client
        logger.debug("Oncreate*******", "Create");
        googleApiClient = new GoogleApiClient.Builder(context).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
        logger.debug("Oncreate*******", "Create"+googleApiClient);
    }

    @Override
    public void onStart() {
        if (isStarted) {
            return;
        }
        try {
            if (googleApiClient != null) {
                googleApiClient.connect();
            }
            if (!checkPlayServices()) {
                logger.debug("Google Service not available");
            }else{
                logger.debug("Google Service  available");
            }

          //  locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), mConfig.getInterval(), mConfig.getDistanceFilter(), this);
            isStarted = true;
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                logger.debug("Google Service  error");
               // apiAvailability.getErrorDialog(context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {

            }

            return false;
        }

        return true;
    }

    @Override
    public void onStop() {
        if (!isStarted) {
            return;
        }
        try {
           // locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        } finally {
            isStarted = false;
        }
    }



    @Override
    public void onConfigure(Config config) {
        super.onConfigure(config);
       /* if (isStarted) {
            onStop();
            onStart();
        }*/
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Translates a number representing desired accuracy of Geolocation system from set [0, 10, 100, 1000].
     * 0:  most aggressive, most accurate, worst battery drain
     * 1000:  least aggressive, least accurate, best for battery.
     */
    private Integer translateDesiredAccuracy(Integer accuracy) {
        if (accuracy >= 1000) {
            return Criteria.ACCURACY_LOW;
        }
        if (accuracy >= 100) {
            return Criteria.ACCURACY_MEDIUM;
        }
        if (accuracy >= 10) {
            return Criteria.ACCURACY_HIGH;
        }
        if (accuracy >= 0) {
            return Criteria.ACCURACY_HIGH;
        }

        return Criteria.ACCURACY_MEDIUM;
    }

    @Override
    public void onDestroy() {
        logger.debug("Destroying RawLocationProvider");
        this.onStop();
        super.onDestroy();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        logger.debug("OnConnected ..............................");

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        startLocationUpdates();

    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        logger.debug("onfailed ..............................");


    }

    @Override
    public void onLocationChanged(Location location) {
        logger.debug("OnChanged...............................");
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        logger.debug("latitude....",latitude);
        logger.debug("longitude....",longitude);
        showDebugToast("acy:" + location.getAccuracy() + ",v:" + location.getSpeed());
        handleLocation(location);

    }
}
