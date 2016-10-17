package com.sgo.orimakardaya.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.orimakardaya.R;
import com.sgo.orimakardaya.activities.CreatePIN;
import com.sgo.orimakardaya.activities.LoginActivity;
import com.sgo.orimakardaya.activities.PasswordRegisterActivity;
import com.sgo.orimakardaya.activities.Registration;
import com.sgo.orimakardaya.coreclass.*;
import com.sgo.orimakardaya.dialogs.DefinedDialog;
import com.sgo.orimakardaya.securities.Md5;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/*
 Created by Administrator on 7/4/2014.
 */
public class Regist2 extends Fragment {

    SecurePreferences sp;
    Button btnResend, btnSubmit,btnCancel;
    String noHPValue,namaValue,emailValue,authType,custID, token, pass, confPass, memberID;
    int max_resend_sms, max_resend_email;
    EditText TokenValue;
    TextView mNoHPValue,mNamaValue,mEmail;
    ProgressDialog progdialog;
    View v;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.frag_regist2, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sp = CustomSecurePref.getInstance().getmSecurePrefs();

        Bundle args = getArguments();
        noHPValue = args.getString(DefineValue.CUST_PHONE,"");
        namaValue = args.getString(DefineValue.CUST_NAME,"");
        emailValue = args.getString(DefineValue.CUST_EMAIL, "-");
        max_resend_sms = Integer.parseInt(args.getString(DefineValue.MAX_RESEND, "3"));
        max_resend_email = Integer.parseInt(args.getString(DefineValue.MAX_RESEND, "3"));
        authType = args.getString(DefineValue.AUTHENTICATION_TYPE, DefineValue.AUTH_TYPE_PIN);

        TokenValue = (EditText) v.findViewById(R.id.reg2_token_value);
        mNoHPValue = (TextView) v.findViewById(R.id.reg2_noHP_value); mNoHPValue.setText(noHPValue);
        mNamaValue = (TextView) v.findViewById(R.id.reg2_nama_value); mNamaValue.setText(namaValue);
        mEmail = (TextView) v.findViewById(R.id.reg2_email_value);mEmail.setText(emailValue);
        btnSubmit = (Button) v.findViewById(R.id.btn_reg2_verification);
        btnCancel = (Button) v.findViewById(R.id.btn_reg2_cancel);
        btnResend = (Button) v.findViewById(R.id.btn_reg2_resend_token);

        TokenValue.requestFocus();

        if(max_resend_sms != 0) {
            btnResend.setText(getString(R.string.reg2_btn_text_resend_token_sms) + " (" + max_resend_sms + ")");
        }
        else if(max_resend_email > 0)  {
            btnResend.setText(getString(R.string.reg2_btn_text_resend_token_email) + " (" + max_resend_email + ")");
        }

        btnResend.setOnClickListener(resendListener);
        btnSubmit.setOnClickListener(submitListener);
        btnCancel.setOnClickListener(cancelListener);

    }


    Button.OnClickListener submitListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(InetHandler.isNetworkAvailable(getActivity())){
                if(inputValidation()){
                        sentData();
                }
            }
            else DefinedDialog.showErrorDialog(getActivity(),getString(R.string.inethandler_dialog_message));
        }
    };

    Button.OnClickListener resendListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(InetHandler.isNetworkAvailable(getActivity())){
                if(max_resend_sms!=0)requestResendToken("Y","N");
                else if(max_resend_email > 0) requestResendToken("N","Y");
                else Toast.makeText(getActivity(),getString(R.string.reg2_notif_max_resend_token_empty),Toast.LENGTH_LONG).show();
            }
            else DefinedDialog.showErrorDialog(getActivity(),getString(R.string.inethandler_dialog_message));
        }
    };

    Button.OnClickListener cancelListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            DefineValue.NOBACK = false;
            getFragmentManager().popBackStack();
        }
    };

    private void switchActivity(Intent i){
        if (getActivity() == null)
            return;

        LoginActivity fca = (LoginActivity) getActivity();
        fca.switchActivity(i, LoginActivity.ACTIVITY_RESULT);

    }


    private void switchActivityPIN(Intent i){
        startActivityForResult(i, LoginActivity.ACTIVITY_RESULT);
    }

    public void changeTextBtnSub() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(max_resend_sms != 0)
                    btnResend.setText(getString(R.string.reg2_btn_text_resend_token_sms) + " (" + max_resend_sms + ")");
                else if(max_resend_email > 0)
                    btnResend.setText(getString(R.string.reg2_btn_text_resend_token_email) + " (" + max_resend_email + ")");
                else if(max_resend_email == 0)
                    btnResend.setText(getString(R.string.reg2_btn_text_resend_token_email) + " (" + max_resend_email + ")");

            }
        });
    }

    //Resend Token
    public void requestResendToken(final String is_sms, final String is_email){
        try{
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");
            progdialog.show();

            btnCancel.setEnabled(false);
            btnResend.setEnabled(false);
            btnSubmit.setEnabled(false);
            TokenValue.setEnabled(false);

            RequestParams params = new RequestParams();
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.CUST_PHONE, noHPValue);
            params.put(WebParams.CUST_NAME,namaValue);
            params.put(WebParams.CUST_EMAIL, emailValue);
            params.put(WebParams.DATE_TIME, DateTimeFormat.getCurrentDateTime());
            params.put(WebParams.IS_SMS, is_sms);
            params.put(WebParams.IS_EMAIL, is_email);

            Timber.d("isi params reg1:"+params.toString());

            MyApiClient.sentDataRegister(getActivity(),params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    btnCancel.setEnabled(true);
                    btnSubmit.setEnabled(true);
                    TokenValue.setEnabled(true);
                    btnResend.setEnabled(true);

                    progdialog.dismiss();
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        if (code.equals(WebParams.SUCCESS_CODE)) {

                            AlertDialog dialogToken;
                            if(is_sms.equalsIgnoreCase("Y")) {
                                --max_resend_sms;
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(getString(R.string.reg2_dialog_token_message_sms))
                                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                dialogToken = builder.create();
                                dialogToken.show();
                            }
                            else if(is_email.equalsIgnoreCase("Y")) {
                                --max_resend_email;
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(getString(R.string.reg2_dialog_token_message_email))
                                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                dialogToken = builder.create();
                                dialogToken.show();
                            }

//                            Toast.makeText(getActivity(), getString(R.string.reg2_notif_text_resend_token), Toast.LENGTH_SHORT).show();
                            changeTextBtnSub();

                            Timber.d("isi response reg2 resend token:" + response.toString());
                        } else {
                            Timber.d("Error Reg2:" + response.toString());
                            code = response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                        }

                        if (max_resend_email == 0) {
                            btnResend.setEnabled(false);
                            Toast.makeText(getActivity(), getString(R.string.reg2_notif_max_resend_token_empty), Toast.LENGTH_SHORT).show();
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

                private void failure(Throwable throwable) {
                    if (MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();
                    if(progdialog.isShowing())
                        progdialog.dismiss();
                    btnCancel.setEnabled(true);
                    btnSubmit.setEnabled(true);
                    TokenValue.setEnabled(true);
                    btnResend.setEnabled(true);
                    Timber.w("Error Koneksi rresend token reg2:" + throwable.toString());
                }
            });
        }catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }


    }

    //Submit
    public void sentData(){
        try{
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");
            progdialog.show();

            btnSubmit.setEnabled(false);

            RequestParams params = new RequestParams();
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.CUST_PHONE, noHPValue);
            params.put(WebParams.CUST_NAME, namaValue);
            params.put(WebParams.CUST_EMAIL, emailValue);
            params.put(WebParams.DATE_TIME, DateTimeFormat.getCurrentDateTime());
            params.put(WebParams.TOKEN_ID, TokenValue.getText().toString());

            Timber.d("isi params reg 2 submit:"+params.toString());

            MyApiClient.sentValidRegister(getActivity(),params, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    progdialog.dismiss();
                    btnCancel.setEnabled(true);
                    btnSubmit.setEnabled(true);
                    TokenValue.setEnabled(true);
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            custID = response.getString(WebParams.CUST_PHONE);
                            token = TokenValue.getText().toString();
                            Intent i = new Intent(getActivity(), PasswordRegisterActivity.class);
                            i.putExtra(DefineValue.AUTHENTICATION_TYPE, authType);
                            startActivityForResult(i, LoginActivity.ACTIVITY_RESULT);
                        } else {
                            Timber.d("isi response gagal submit:" + response.toString());
                            code = response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
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

                private void failure(Throwable throwable) {
                    if (MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();
                    if (progdialog.isShowing())
                        progdialog.dismiss();
                    btnCancel.setEnabled(true);
                    btnSubmit.setEnabled(true);
                    TokenValue.setEnabled(true);
                    Timber.w("Error Koneksi proses data reg2:" + throwable.toString());
                }
            });
        }catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d(requestCode+"/"+resultCode);
        if (requestCode == LoginActivity.ACTIVITY_RESULT) {
            if (resultCode == LoginActivity.RESULT_PIN) {
                pass = data.getStringExtra(DefineValue.NEW_PASSWORD);
                confPass = data.getStringExtra(DefineValue.CONFIRM_PASSWORD);
                sendCreatePass();
            }
            else if(resultCode == LoginActivity.RESULT_FINISHING){
//                if(authType.equals(DefineValue.AUTH_TYPE_OTP)){
//                    pass = data.getStringExtra(DefineValue.NEW_PASSWORD);
//                    confPass = data.getStringExtra(DefineValue.CONFIRM_PASSWORD);
//                }
                sendCreatePin(data);
            }
        }
    }
    public void sendCreatePass(){
        try{
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            RequestParams params = new RequestParams();
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.PASS, pass);
            params.put(WebParams.CONF_PASS, confPass);
            params.put(WebParams.TOKEN_ID, token);
            params.put(WebParams.CUST_ID, custID);

            Timber.d("params create pass:"+params.toString());

            MyApiClient.sentCreatePass(getActivity(),params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        String message = response.getString(WebParams.ERROR_MESSAGE);

                        progdialog.dismiss();
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            memberID = response.getString(WebParams.MEMBER_ID);

                            Intent i = new Intent(getActivity(), CreatePIN.class);
                            i.putExtra(DefineValue.REGISTRATION, true);
                            startActivityForResult(i, LoginActivity.ACTIVITY_RESULT);
                        } else {
                            Timber.d("isi error create pass:" + response.toString());
                            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                            Intent i = new Intent(getActivity(), PasswordRegisterActivity.class);
                            i.putExtra(DefineValue.AUTHENTICATION_TYPE, authType);
                            switchActivityPIN(i);
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

                private void failure(Throwable throwable) {
                    if (MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();
                    if (progdialog.isShowing())
                        progdialog.dismiss();
                    Intent i = new Intent(getActivity(), PasswordRegisterActivity.class);
                    i.putExtra(DefineValue.AUTHENTICATION_TYPE, authType);
                    switchActivityPIN(i);
                    Timber.w("Error Koneksi create pass reg2:" + throwable.toString());
                }
            });
        }
        catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }

    }
    public void sendCreatePin(Intent data){
        try{
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            RequestParams params = new RequestParams();
            params.put(WebParams.MEMBER_ID, memberID);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.PIN, Md5.hashMd5(data.getStringExtra(DefineValue.PIN_VALUE)));
            params.put(WebParams.CONFIRM_PIN, Md5.hashMd5(data.getStringExtra(DefineValue.CONF_PIN)));

            Timber.d("params create pin:"+params.toString());

            MyApiClient.sentCreatePin(getActivity(),params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        String message = response.getString(WebParams.ERROR_MESSAGE);

                        progdialog.dismiss();
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            showDialog();
                        } else {
                            Timber.d("isi error create pin:" + response.toString());
                            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                            Intent i = new Intent(getActivity(), CreatePIN.class);
                            i.putExtra(DefineValue.REGISTRATION, true);
                            switchActivityPIN(i);
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

                private void failure(Throwable throwable) {
                    if (MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();
                    if (progdialog.isShowing())
                        progdialog.dismiss();
                    Intent i = new Intent(getActivity(), CreatePIN.class);
                    i.putExtra(DefineValue.REGISTRATION, true);
                    switchActivityPIN(i);
                    Timber.w("Error Koneksi create pin reg2:" + throwable.toString());
                }
            });
        }
        catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }

    }

    void showDialog(){

        saveImeiICCIDDevice();

        // Create custom dialog object
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        // Include dialog.xml file
        dialog.setContentView(R.layout.dialog_notification);

        // set values for custom dialog components - text, image and button
        Button btnDialogOTP = (Button)dialog.findViewById(R.id.btn_dialog_notification_ok);
        TextView Title = (TextView)dialog.findViewById(R.id.title_dialog);
        TextView Message = (TextView)dialog.findViewById(R.id.message_dialog);Message.setVisibility(View.VISIBLE);
        TextView Message2 = (TextView)dialog.findViewById(R.id.message_dialog2);Message2.setVisibility(View.VISIBLE);
        TextView Message3 = (TextView)dialog.findViewById(R.id.message_dialog3);Message3.setVisibility(View.VISIBLE);

        Title.setText(getResources().getString(R.string.regist2_notif_title));
        Message.setText(getResources().getString(R.string.regist2_notif_message_1));
        Message2.setText(noHPValue);
        Message2.setTextSize(getResources().getDimension(R.dimen.abc_text_size_small_material));
        Message3.setText(getResources().getString(R.string.regist2_notif_message_3));

        btnDialogOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                Fragment test = new Login();
                switchFragment(test,"Login",false);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void saveImeiICCIDDevice(){
        SecurePreferences sp = CustomSecurePref.getInstance().getmSecurePrefs();
        SecurePreferences.Editor edit = sp.edit();
        SMSclass smSclass = new SMSclass(getActivity());
        edit.putString(DefineValue.DEIMEI, smSclass.getDeviceIMEI());
        edit.putString(DefineValue.DEICCID, smSclass.getDeviceICCID());
        edit.commit();
    }

    private void switchFragment(Fragment i, String name, Boolean isBackstack){
        if (getActivity() == null)
            return;

        LoginActivity fca = (LoginActivity) getActivity();
        fca.switchContent(i,name,isBackstack);
    }

    public boolean inputValidation(){
        if(TokenValue.getText().toString().length()==0){
            TokenValue.requestFocus();
            TokenValue.setError(this.getString(R.string.regist2_validation_otp));
            return false;
        }
        return true;
    }

    private void toggleMyBroadcastReceiver(Boolean _on){
        if (getActivity() == null)
            return;

        LoginActivity fca = (LoginActivity ) getActivity();
        fca.togglerBroadcastReceiver(_on,myReceiver);
    }

    public BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Timber.wtf("masuk Receiver ");
            Bundle mBundle = intent.getExtras();
            SmsMessage[] mSMS;
            String strMessage = "";
            String _kode_otp = "";
            String _member_code = "";
            String[] kode = context.getResources().getStringArray(R.array.broadcast_regist_kode_compare);

            if(mBundle != null){
                Object[] pdus = (Object[]) mBundle.get("pdus");
                mSMS = new SmsMessage[pdus.length];

                for (int i = 0; i < mSMS.length ; i++){
                    mSMS[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                    strMessage += mSMS[i].getMessageBody();
                    strMessage += "\n";
                }

                String[] words = strMessage.split("\\s+");
                for (int i = 0 ; i <words.length;i++)
                {
                    if(_kode_otp.equalsIgnoreCase("")){
                        if(words[i].equalsIgnoreCase(kode[0])){
                            if(words[i+1].equalsIgnoreCase(kode[1])) {
                                _kode_otp = words[i+3];
                                _kode_otp = _kode_otp.replace(".", "").replace(" ", "");
                            }
                        }
                    }
                    Timber.d("isi words:"+words[i]);
                }
                TokenValue.setText(_kode_otp + _member_code);
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleMyBroadcastReceiver(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleMyBroadcastReceiver(false);
    }
}