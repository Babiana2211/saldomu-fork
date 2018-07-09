package com.sgo.saldomu.services;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.BuildConfig;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.Singleton.MyApiClient;
import com.sgo.saldomu.coreclass.WebParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;


/**
 * Created by Lenovo on 05/04/2017.
 */

public class UpdateLocationService extends JobService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    static public final String TAG = "UpdateLocationService";
    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    Double longitude, latitude;
    SecurePreferences sp;
    JobParameters jobLocation;

    @Override
    public boolean onStartJob(JobParameters job) {
        Timber.d("OnStartJob Location Service");
        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
        }

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

        jobLocation = job;

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Timber.d("OnStopJob Location Service");
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.d("Location Service onConnected Started");
        sp = CustomSecurePref.getInstance().getmSecurePrefs();

        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if ( mLastLocation == null ){
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                Timber.d("Location Service Location Found" + mLastLocation.toString());

                latitude  = mLastLocation.getLatitude();
                longitude = mLastLocation.getLongitude();

                Boolean isAgent = sp.getBoolean(DefineValue.IS_AGENT, false);

                if ( isAgent )
                    updateLocation();

                mGoogleApiClient.disconnect();
                jobFinished(jobLocation, true);

            }
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("LOG_CONNECT_FAILED", "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Log.d("Location LAST LONGITUDE", String.valueOf(mLastLocation.getLongitude()) );
        Log.d("Location LAST LATITUDE", String.valueOf(mLastLocation.getLatitude()) );

        longitude   = mLastLocation.getLongitude();
        latitude    = mLastLocation.getLatitude();

        Boolean isAgent = sp.getBoolean(DefineValue.IS_AGENT, false);

        if ( isAgent )
            updateLocation();

        mGoogleApiClient.disconnect();
        jobFinished(jobLocation, true);
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {


        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                Toast.makeText(this, "GOOGLE API LOCATION CONNECTION FAILED", Toast.LENGTH_SHORT).show();
            }

            return false;
        }

        return true;
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(DefineValue.AGENT_INTERVAL_LOCATION_REQUEST);
        mLocationRequest.setFastestInterval(DefineValue.AGENT_FASTEST_INTERVAL_LOCATION_REQUEST);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DefineValue.AGENT_DISPLACEMENT);
    }

    private void updateLocation() {

        sp = CustomSecurePref.getInstance().getmSecurePrefs();

        SecurePreferences.Editor mEditor = sp.edit();
        try {
            mEditor.putDouble(DefineValue.LAST_CURRENT_LATITUDE, latitude);
            mEditor.putDouble(DefineValue.LAST_CURRENT_LONGITUDE, longitude);
            mEditor.apply();
        } catch( Exception e ) {
            e.printStackTrace();
        }

        String extraSignature   = String.valueOf(latitude) + String.valueOf(longitude);
        RequestParams params    = MyApiClient.getSignatureWithParams(sp.getString(DefineValue.COMMUNITY_ID, ""), MyApiClient.LINK_UPDATE_LOCATION,
                sp.getString(DefineValue.USERID_PHONE, ""), sp.getString(DefineValue.ACCESS_KEY, ""), extraSignature);

        params.put(WebParams.APP_ID, BuildConfig.APP_ID);
        params.put(WebParams.SENDER_ID, DefineValue.BBS_SENDER_ID );
        params.put(WebParams.RECEIVER_ID, DefineValue.BBS_RECEIVER_ID );
        params.put(WebParams.LONGITUDE, longitude );
        params.put(WebParams.LATITUDE, latitude );
        params.put(WebParams.USER_ID, sp.getString(DefineValue.USERID_PHONE, "") );

        MyApiClient.updateLocationService(getApplicationContext(), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {

                    String code = response.getString(WebParams.ERROR_CODE);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                ifFailure(throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                ifFailure(throwable);
            }

            private void ifFailure(Throwable throwable) {

                Timber.w("Error Koneksi Update Location Service:" + throwable.toString());

            }

        });

    }

}
