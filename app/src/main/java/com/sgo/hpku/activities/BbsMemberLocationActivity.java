package com.sgo.hpku.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.MapView;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.hpku.BuildConfig;
import com.sgo.hpku.R;
import com.sgo.hpku.adapter.GooglePlacesAutoCompleteArrayAdapter;
import com.sgo.hpku.coreclass.BaseActivity;
import com.sgo.hpku.coreclass.CustomAutoCompleteTextView;
import com.sgo.hpku.coreclass.DateTimeFormat;
import com.sgo.hpku.coreclass.DefineValue;
import com.sgo.hpku.coreclass.GlobalSetting;
import com.sgo.hpku.coreclass.HashMessage;
import com.sgo.hpku.coreclass.InetHandler;
import com.sgo.hpku.coreclass.MyApiClient;
import com.sgo.hpku.coreclass.WebParams;
import com.sgo.hpku.dialogs.AlertDialogLogout;
import com.sgo.hpku.dialogs.DefinedDialog;
import com.sgo.hpku.entityRealm.MerchantCommunityList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sgo.hpku.models.OpenHourDays;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

public class BbsMemberLocationActivity extends BaseActivity implements OnMapReadyCallback,
        AdapterView.OnItemClickListener, TextView.OnEditorActionListener, EasyPermissions.PermissionCallbacks {

    String memberId, memberDefaultAddress, countryName, provinceName, districtName, shopId, shopName, address, memberType, agentName, commName, postalCode;
    Realm myRealm;
    TextView tvDetailMemberName, tvCommName, tvAddress, tvDistrict, tvProvince;
    private GoogleMap mMap;
    MapView mapView;
    Double selectedLat, selectedLong;
    Double defaultLat, defaultLong;
    Button btnSubmit;
    ProgressDialog progdialog;
    SecurePreferences sp;
    MerchantCommunityList memberDetail;
    GooglePlacesAutoCompleteArrayAdapter googlePlacesAutoCompleteBbsArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        memberId            = getIntent().getStringExtra("memberId");
        shopId              = getIntent().getStringExtra("shopId");
        shopName            = getIntent().getStringExtra("shopName");
        memberType          = getIntent().getStringExtra("memberType");
        agentName           = getIntent().getStringExtra("memberName");
        commName            = getIntent().getStringExtra("commName");
        provinceName        = getIntent().getStringExtra("province");
        districtName        = getIntent().getStringExtra("district");
        address             = getIntent().getStringExtra("address");

        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_location),
                    GlobalSetting.RC_LOCATION_PERM, Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if ( !GlobalSetting.isLocationEnabled(this) )
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.alertbox_gps_warning))
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                            Intent intent = new Intent(getApplicationContext(), MainPage.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        } else {


        }

        myRealm = Realm.getDefaultInstance();

        setActionBarIcon(R.drawable.ic_arrow_left);




        //memberDetail        = myRealm.where(MerchantCommunityList.class).equalTo("memberId", memberId).equalTo("shopId", shopId).findFirst();

        googlePlacesAutoCompleteBbsArrayAdapter = new GooglePlacesAutoCompleteArrayAdapter(getApplicationContext(), R.layout.google_places_auto_complete_listview);

        setActionBarTitle(getString(R.string.update_merchant_location) + " - " + shopName);

        tvDetailMemberName  = (TextView) findViewById(R.id.tvDetailMemberName);
        tvCommName          = (TextView) findViewById(R.id.tvCommName);
        tvAddress           = (TextView) findViewById(R.id.tvAddress);
        tvProvince          = (TextView) findViewById(R.id.tvProvince);
        tvDistrict          = (TextView) findViewById(R.id.tvDistrict);

        tvDetailMemberName.setText(agentName);
        tvCommName.setText(commName);
        tvProvince.setText(provinceName);
        tvDistrict.setText(districtName);
        tvAddress.setText(address);

        memberDefaultAddress    = districtName + ", "+ provinceName;

        defaultLat      = -6.121435;
        defaultLong     = 106.774124;

        btnSubmit       = (Button) findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(btnSubmitListener);

    }

    Button.OnClickListener btnSubmitListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (InetHandler.isNetworkAvailable(getApplicationContext())) {
                if ( selectedLat == null || countryName == null )
                {
                    DefinedDialog.showErrorDialog(getApplicationContext(), getString(R.string.err_empty_coordinate_message));
                }
                else {
                    progdialog              = DefinedDialog.CreateProgressDialog(getApplicationContext(), "");

                    RequestParams params    = new RequestParams();
                    UUID rcUUID             = UUID.randomUUID();
                    String  dtime           = DateTimeFormat.getCurrentDateTime();

                    params.put(WebParams.RC_UUID, rcUUID);
                    params.put(WebParams.RC_DATETIME, dtime);
                    params.put(WebParams.APP_ID, BuildConfig.AppID);
                    params.put(WebParams.SENDER_ID, DefineValue.BBS_SENDER_ID);
                    params.put(WebParams.RECEIVER_ID, DefineValue.BBS_RECEIVER_ID);
                    params.put(WebParams.SHOP_ID, shopId);
                    params.put(WebParams.MEMBER_ID, memberId);
                    params.put(WebParams.DISTRICT, districtName);
                    params.put(WebParams.PROVINCE, provinceName);
                    params.put(WebParams.COUNTRY, countryName.toUpperCase());
                    params.put(WebParams.LATITUDE, selectedLat);
                    params.put(WebParams.LONGITUDE, selectedLong);
                    params.put(WebParams.ZIP_CODE, postalCode);

                    String signature = HashMessage.SHA1(HashMessage.MD5(rcUUID + dtime + DefineValue.BBS_SENDER_ID + DefineValue.BBS_RECEIVER_ID + memberId.toUpperCase() + shopId.toUpperCase()
                            + BuildConfig.AppID + selectedLat + selectedLong));

                    params.put(WebParams.SIGNATURE, signature);

                    MyApiClient.updateMemberLocation(getApplication(), params, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            //progdialog.dismiss();

                            try {

                                String code = response.getString(
                                        WebParams.ERROR_CODE);
                                if (code.equals(WebParams.SUCCESS_CODE)) {
                                    //myRealm.beginTransaction();


                                    RequestParams params2 = new RequestParams();

                                    UUID rcUUID2             = UUID.randomUUID();
                                    String  dtime2           = DateTimeFormat.getCurrentDateTime();

                                    params2.put(WebParams.RC_UUID, rcUUID2);
                                    params2.put(WebParams.RC_DATETIME, dtime2);
                                    params2.put(WebParams.APP_ID, BuildConfig.AppID);
                                    params2.put(WebParams.SENDER_ID, DefineValue.BBS_SENDER_ID);
                                    params2.put(WebParams.RECEIVER_ID, DefineValue.BBS_RECEIVER_ID);
                                    params2.put(WebParams.SHOP_ID, shopId);
                                    params2.put(WebParams.MEMBER_ID, memberId);
                                    params2.put(WebParams.FLAG_ALL_DAY, DefineValue.STRING_YES);
                                    params2.put(WebParams.FLAG_CLOSED_TYPE, DefineValue.CLOSED_TYPE_NONE);


                                    String signature = HashMessage.SHA1(HashMessage.MD5(rcUUID2 + dtime2 + DefineValue.BBS_SENDER_ID + DefineValue.BBS_RECEIVER_ID + memberId.toUpperCase() + shopId.toUpperCase() + BuildConfig.AppID));

                                    params2.put(WebParams.SIGNATURE, signature);


                                    MyApiClient.setupOpeningHour(BbsMemberLocationActivity.this, params2, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            progdialog.dismiss();
                                            Timber.d("isi response sent request cash in:" + response.toString());

                                            try {
                                                String code = response.getString(WebParams.ERROR_CODE);
                                                if (code.equals(WebParams.SUCCESS_CODE)) {
                                                    Bundle bundle = new Bundle();
                                                    bundle.putInt(DefineValue.INDEX, BBSActivity.BBSKELOLA);

                                                    Intent intent = new Intent(getApplicationContext(), BBSActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    intent.putExtras(bundle);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                                else if(code.equals(WebParams.LOGOUT_CODE)){
                                                    String message = response.getString(WebParams.ERROR_MESSAGE);
                                                    AlertDialogLogout test = AlertDialogLogout.getInstance();
                                                    //test.showDialoginActivity(getApplication(),message);
                                                }
                                                else {
                                                    code = response.getString(WebParams.ERROR_MESSAGE);
                                                    Toast.makeText(getApplicationContext(), code, Toast.LENGTH_LONG).show();
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                            super.onFailure(statusCode, headers, responseString, throwable);
                                            failure(throwable);
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                            super.onFailure(statusCode, headers, throwable, errorResponse);
                                            failure(throwable);
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                                            super.onFailure(statusCode, headers, throwable, errorResponse);
                                            failure(throwable);
                                        }

                                        private void failure(Throwable throwable){
                                            if(MyApiClient.PROD_FAILURE_FLAG)
                                                Toast.makeText(getApplicationContext(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                                            else
                                                Toast.makeText(getApplicationContext(), throwable.toString(), Toast.LENGTH_SHORT).show();
                                            if (progdialog.isShowing())
                                                progdialog.dismiss();

                                            Timber.w("Error Koneksi sent request setup open hour:"+throwable.toString());
                                        }
                                    });

                                    //String tempSetupOpenHour = response.getString("setup_open_hour");
                                    //if (tempSetupOpenHour.equals("")) {
                                        //tempSetupOpenHour = DefineValue.STRING_NO;
                                    //}
                                    //memberDetail.setSetupOpenHour(response.getString("setup_open_hour"));
                                    //myRealm.copyToRealmOrUpdate(memberDetail);
                                    //myRealm.commitTransaction();


                                    /*if (memberType.equals(DefineValue.SHOP_MERCHANT)) {
                                        Intent intent = new Intent(getApplicationContext(), BbsMerchantCategoryActivity.class);
                                        intent.putExtra("memberId", memberId);
                                        intent.putExtra("shopId", shopId);
                                        intent.putExtra("setupOpenHour", tempSetupOpenHour);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        getApplicationContext().startActivity(intent);
                                        finish();
                                    } else {*/
                                        /*Intent intent = new Intent(getApplicationContext(), BbsListSettingKelolaActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();*/


                                    //}

                                } else if ( code.equals(WebParams.LOGOUT_CODE) ) {
                                    String message = response.getString(WebParams.ERROR_MESSAGE);
                                    AlertDialogLogout test = AlertDialogLogout.getInstance();
                                    //test.showDialoginActivity(getActi,message);
                                } else {



                                    code = response.getString(WebParams.ERROR_MESSAGE);
                                    Toast.makeText(getApplicationContext(), code, Toast.LENGTH_LONG).show();
                                }
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
                            if (MyApiClient.PROD_FAILURE_FLAG)
                                Toast.makeText(getApplication(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getApplication(), throwable.toString(), Toast.LENGTH_SHORT).show();

                            progdialog.dismiss();
                            Timber.w("Error Koneksi login:" + throwable.toString());

                        }

                    });
                }
            }
            else DefinedDialog.showErrorDialog(getApplicationContext(), getString(R.string.inethandler_dialog_message));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (mMap == null) {

            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(this);
        }
    }

    public void setAdministrativeName() {
        Geocoder geocoder = new Geocoder(this, new Locale("id"));

        List<Address> addressList = null;

        try {
            addressList = geocoder.getFromLocation(
                    selectedLat,
                    selectedLong,
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.d("IOEXCEPTION", "IO EXCEPTION", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e("ILLEGAL", "ILLEGAL EXP" + ". " +
                    "Latitude = " + selectedLat +
                    ", Longitude = " +
                    selectedLong, illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addressList == null || addressList.size()  == 0) {

        } else {
            Address addressDetail = addressList.get(0);

            ArrayList<String> addressFragments = new ArrayList<String>();
            districtName    = addressDetail.getSubAdminArea();
            provinceName    = addressDetail.getAdminArea();
            countryName     = addressDetail.getCountryName();
            postalCode      = addressDetail.getPostalCode();

        }
    }

    public void onMapSearch(View view) {

        CustomAutoCompleteTextView locationSearch = (CustomAutoCompleteTextView) findViewById(R.id.editText);
        locationSearch.setAdapter(googlePlacesAutoCompleteBbsArrayAdapter);
        locationSearch.setOnItemClickListener(this);
        locationSearch.setOnEditorActionListener(this);

        String location = locationSearch.getText().toString();
        List<Address> addressList = null;
        Double latitude        = defaultLat;
        Double longitude       = defaultLong;
        mMap.clear();

        //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ((InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(locationSearch.getWindowToken(), 0);

        if (!location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

                if ( addressList.size() > 0 ) {
                    Address address = addressList.get(0);
                    latitude        = address.getLatitude();
                    longitude       = address.getLongitude();
                    selectedLat     = latitude;
                    selectedLong    = longitude;

                    setAdministrativeName();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            LatLng latLng = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(location));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.err_empty_merchant_address), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        List<Address> addressList = null;
        Double latitude = defaultLat, longitude = defaultLong;

        if (memberDefaultAddress != null || !memberDefaultAddress.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(memberDefaultAddress, 1);
                Address address = addressList.get(0);
                latitude        = address.getLatitude();
                longitude       = address.getLongitude();
                selectedLat     = latitude;
                selectedLong    = longitude;

                setAdministrativeName();
            } catch (IOException e) {
                e.printStackTrace();
            }

            LatLng latLng = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(memberDefaultAddress));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }

        // Setting onclick event listener for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                // clearing map and generating new marker points if user clicks on map more than two times
                mMap.clear();
                selectedLat     = point.latitude;
                selectedLong    = point.longitude;
                mMap.addMarker(new MarkerOptions().position(point).title(memberDefaultAddress));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));



            }
        });
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_bbs_member_location;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //listener ketika button back di action bar diklik
        if (id == android.R.id.home) {
            //kembali ke activity sebelumnya
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeToolbar() {
        setActionBarIcon(R.drawable.ic_arrow_left);
        setActionBarTitle(getString(R.string.shop_member_detail));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return false;
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

        Intent i = new Intent(this, BbsMemberLocationActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("memberId", memberId);
        i.putExtra("shopId", shopId);
        startActivity(i);
        finish();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {


        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.alertbox_title_warning));
            alertDialog.setMessage(getString(R.string.alertbox_message_warning));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(getApplicationContext(), MainPage.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    });
            alertDialog.show();
        }
    }
}
