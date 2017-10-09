package com.sgo.saldomu.activities;

import android.Manifest;
import android.app.Application;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.Beans.ListMyProfile_model;
import com.sgo.saldomu.R;
import com.sgo.saldomu.adapter.ExpandableListProfile;
import com.sgo.saldomu.coreclass.DateTimeFormat;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.GeneralizeImage;
import com.sgo.saldomu.coreclass.MyApiClient;
import com.sgo.saldomu.coreclass.MyPicasso;
import com.sgo.saldomu.coreclass.RoundImageTransformation;
import com.sgo.saldomu.coreclass.WebParams;
import com.sgo.saldomu.dialogs.AlertDialogLogout;
import com.sgo.saldomu.fragments.ListMyProfile;
import com.sgo.saldomu.interfaces.OnDateChooseListener;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MONTH;

public class MyProfileActivityNew extends AppCompatActivity implements ExpandableListProfile.onClick {

    ExpandableListView expandableListview;
    SecurePreferences sp;

    private List<String> mListDataHeader;
    private HashMap<String, List<ListMyProfile_model>> mListDataChild;
    View v;
    String noHP, nama, email, dob_string;
    private Calendar calendar;
    private final int RESULT_GALERY = 100;
    private final int RESULT_CAMERA = 200;
    final int RC_CAMERA_STORAGE = 14;
    private Uri mCapturedImageURI;
    private String userID;
    private String accessKey;
    private int RESULT;
    ImageButton cameraKTP, cameraSelfieKTP,cameraTTD;



    Boolean step1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile_new);

        expandableListview = (ExpandableListView) findViewById(R.id.expandableListProfile);
//
//        VerifiedMember = (Layout) v.findViewById(R.layout.)
        cameraKTP = (ImageButton) findViewById(R.id.camera_ktp_paspor);
        cameraSelfieKTP = (ImageButton) findViewById(R.id.camera_selfie_ktp_paspor);
        cameraTTD = (ImageButton) findViewById(R.id.camera_ttd);

        mListDataHeader= new ArrayList<>();
        mListDataChild = new HashMap<>();
        mListDataHeader.add("Data Member Basic");
        mListDataHeader.add("Data Verified Member");

        for (String header: mListDataHeader) {
            List<ListMyProfile_model> lists = new ArrayList<>();
            if (header.equals("Data Member Basic"))
            {
                lists.add(new ListMyProfile_model("","","", "", true));
                mListDataChild.put(header, lists);
            } else {
                lists.add(new ListMyProfile_model("","","", "", false));
                mListDataChild.put(header, lists);
            }

        }

        String OrifromDate = DateTimeFormat.getCurrentDateMinus(6);
        Calendar date_from = StringToCal(OrifromDate);
        ExpandableListProfile adapter= new ExpandableListProfile(this, mListDataHeader, mListDataChild
                , this, getFragmentManager(), date_from);
        expandableListview.setAdapter(adapter);
        expandableListview.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                Log.d("group", "group post: "+groupPosition);
            }
        });

        expandableListview.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    private Calendar StringToCal(String src){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd",new Locale("id","INDONESIA"));
        Calendar tempCalendar = Calendar.getInstance();

        try {
            tempCalendar.setTime(format.parse(src));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return tempCalendar;
    }

    @Override
    public void onTextChange(String message, int choice) {
        switch (choice)
        {
            case 1 :
                noHP = message;
                break;
            case 2:
                nama = message;
                break;
            case 3 :
                email= message;
                break;
        }
    }

    @Override
    public void DateChooseListener(String date) {
        dob_string = date;
    }

    @Override
    public void NextListener() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("Upgrade Member");
        builder1.setMessage("Apakah Anda ingin upgrade menjadi verified member untuk bisa melakukan transaksi transfer, minta uang dan tarik tunai?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        expandableListview.expandGroup(1, true);
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    @Override
    public void setImageCameraKTP() {
        float density = getResources().getDisplayMetrics().density;
        String _url_profpic;

        if(density <= 1) _url_profpic = sp.getString(DefineValue.IMG_SMALL_URL, null);
        else if(density < 2) _url_profpic = sp.getString(DefineValue.IMG_MEDIUM_URL, null);
        else _url_profpic = sp.getString(DefineValue.IMG_LARGE_URL, null);

        Timber.wtf("url prof pic:"+ _url_profpic);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.user_unknown_menu);
        RoundImageTransformation roundedImage = new RoundImageTransformation(bm);

        Picasso mPic;
        if(MyApiClient.PROD_FLAG_ADDRESS)
            mPic = MyPicasso.getImageLoader(this);
        else
            mPic= Picasso.with(this);

        if(_url_profpic != null && _url_profpic.isEmpty()){
            mPic.load(R.drawable.user_unknown_menu)
                    .error(roundedImage)
                    .fit().centerInside()
                    .placeholder(R.drawable.progress_animation)
                    .transform(new RoundImageTransformation()).into(cameraKTP);
        }
        else {
            mPic.load(_url_profpic)
                    .error(roundedImage)
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.progress_animation)
                    .transform(new RoundImageTransformation())
                    .into(cameraKTP);
        }
    }

    public void camera_ktp_paspor_dialog(View v)
    {
        final String[] items = {"Choose from Gallery" , "Take a Photo"};

        android.app.AlertDialog.Builder a = new android.app.AlertDialog.Builder(MyProfileActivityNew.this);
        a.setCancelable(true);
        a.setTitle("Choose Profile Picture");
        a.setAdapter(new ArrayAdapter<>(MyProfileActivityNew.this, android.R.layout.simple_list_item_1, items),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Timber.wtf("masuk gallery");
                            chooseGallery();
                        } else if (which == 1) {
                            chooseCamera();
                        }

                    }
                }
        );
        a.create();
        a.show();
    }

    private void chooseGallery() {
        Timber.wtf("masuk gallery");
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RESULT_GALERY);
    }

    private void chooseCamera() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this,perms)) {
            runCamera();
        }
        else {
            EasyPermissions.requestPermissions(this,getString(R.string.rationale_camera_and_storage),
                    RC_CAMERA_STORAGE,perms);
        }
    }

    private void runCamera(){
        String fileName = "temp.jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);

        mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
        startActivityForResult(takePictureIntent, RESULT_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case RESULT_GALERY:
                if(resultCode == RESULT_OK){

                    Bitmap photo = null;
                    Uri _urinya = data.getData();
                    if(data.getData() == null) {
                        photo = (Bitmap)data.getExtras().get("data");
                    } else {
                        try {
                            photo = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    GeneralizeImage mGI = new GeneralizeImage(this,photo,_urinya);
                    uploadFileToServer(mGI.Convert());

                }
                break;
            case RESULT_CAMERA:
                if(resultCode == RESULT_OK && mCapturedImageURI!=null){
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(mCapturedImageURI, projection, null, null, null);
                    String filePath;
                    if (cursor != null) {
                        cursor.moveToFirst();
                        int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        filePath = cursor.getString(column_index_data);
                    }
                    else
                        filePath = data.getData().getPath();
//                    File photoFile = new File(filePath);
                    GeneralizeImage mGI = new GeneralizeImage(this,filePath);
                    //getOrientationImage();
                    uploadFileToServer(mGI.Convert());
                    assert cursor != null;
                    cursor.close();
                }
                else{
                    Toast.makeText(this, "Try Again", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    private void uploadFileToServer(File photoFile) {
        Picasso.with(this).load(R.drawable.progress_animation).into(cameraKTP);
//        prgLoading.setVisibility(View.VISIBLE);

        RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID,MyApiClient.LINK_UPLOAD_KTP,
                userID,accessKey);

        try {
            params.put(WebParams.USER_ID,noHP);
            params.put(WebParams.USER_FILE, photoFile);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Timber.d("params upload profile picture: " + params.toString());

        MyApiClient.sentProfilePicture(this, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_code = response.getString("error_code");
                    String error_message = response.getString("error_message");
//                    prgLoading.setVisibility(View.GONE);
//                    Timber.d("response Listbank:" + response.toString());
                    if (error_code.equalsIgnoreCase("0000")) {
                        SecurePreferences.Editor mEditor = sp.edit();

                        mEditor.putString(DefineValue.IMG_URL, response.getString(WebParams.IMG_URL));
                        mEditor.putString(DefineValue.IMG_SMALL_URL, response.getString(WebParams.IMG_SMALL_URL));
                        mEditor.putString(DefineValue.IMG_MEDIUM_URL, response.getString(WebParams.IMG_MEDIUM_URL));
                        mEditor.putString(DefineValue.IMG_LARGE_URL, response.getString(WebParams.IMG_LARGE_URL));

                        mEditor.apply();

                        setImageCameraKTP();

                        RESULT = MainPage.RESULT_REFRESH_NAVDRAW;
                    } else if (error_code.equals(WebParams.LOGOUT_CODE)) {
                        Timber.d("isi response autologout:" + response.toString());
                        String message = response.getString(WebParams.ERROR_MESSAGE);

                        AlertDialogLogout test = AlertDialogLogout.getInstance();
                        test.showDialoginActivity(MyProfileActivityNew.this, message);
                    } else {
                        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(MyProfileActivityNew.this);
                        alert.setTitle("Upload Image");
                        alert.setMessage("Upload Image : " + error_message);
                        alert.setPositiveButton("OK", null);
                        alert.show();

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Unexpected Error occurred! [Most common Error: Device might not be connected to Internet or remote server is not up and running]", Toast.LENGTH_LONG).show();
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

            private void failure(Throwable throwable) {
                if (MyApiClient.PROD_FAILURE_FLAG)
                    Toast.makeText(MyProfileActivityNew.this, getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MyProfileActivityNew.this, throwable.toString(), Toast.LENGTH_SHORT).show();
//                if (prgLoading.getVisibility() == View.VISIBLE)
//                    prgLoading.setVisibility(View.GONE);
                setImageCameraKTP();
                Timber.w("Error Koneksi data update myprofile:" + throwable.toString());
            }

        });
    }
}
