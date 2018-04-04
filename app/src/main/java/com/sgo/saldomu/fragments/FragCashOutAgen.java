package com.sgo.saldomu.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.R;
import com.sgo.saldomu.activities.CashoutActivity;
import com.sgo.saldomu.activities.InsertPIN;
import com.sgo.saldomu.activities.MainPage;
import com.sgo.saldomu.coreclass.CurrencyFormat;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.ErrorDefinition;
import com.sgo.saldomu.coreclass.InetHandler;
import com.sgo.saldomu.coreclass.MyApiClient;
import com.sgo.saldomu.coreclass.WebParams;
import com.sgo.saldomu.dialogs.AlertDialogFrag;
import com.sgo.saldomu.dialogs.AlertDialogLogout;
import com.sgo.saldomu.dialogs.DefinedDialog;
import com.sgo.saldomu.interfaces.OnLoadDataListener;
import com.sgo.saldomu.loader.UtilsLoader;
import com.sgo.saldomu.securities.Md5;
import com.sgo.saldomu.securities.RSA;
import com.sgo.saldomu.widgets.BaseFragment;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;

import timber.log.Timber;

public class FragCashOutAgen extends BaseFragment {

    public final static String TAG = "com.sgo.indonesiakoe.fragments.FragCashOutAgen";

    private View v;
    private String tx_id,nameadmin,amount,fee,total,ccy,authType;
    private ProgressDialog progdialog;
    private Boolean isOTP =  false;
    private EditText et_otp;
    private JSONObject dataInq;
    private View layout_noData;
    private int attempt,failed;
    private Button btnResend, btn_proses, btn_batal;
    private int max_token_resend = 3, count_resend=0;
    private Activity act;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.frag_cash_out_agen, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        act = getActivity();
        if(InetHandler.isNetworkAvailable(getActivity())) {
            sentInquiryWithdraw();
        }
        else DefinedDialog.showErrorDialog(getActivity(), getString(R.string.inethandler_dialog_message));

        CashoutActivity fca = (CashoutActivity) getActivity();
        fca.setTitleToolbar(getString(R.string.title_cashout_tunai));
    }

    private void initializeNoData(){
        String contactCenter = sp.getString(DefineValue.LIST_CONTACT_CENTER, "");
        String listContactPhone = "";

        try {
            JSONArray arrayContact = new JSONArray(contactCenter);
            JSONObject mObject;
//            String id;
            for(int i=0; i < arrayContact.length() ; i++ ) {
                mObject = arrayContact.getJSONObject(i);
//                id = mObject.optString(WebParams.ID, "0");
                if(i==0) {
                    listContactPhone = mObject.optString(WebParams.NAME,"")+"\n"+
                            mObject.optString(WebParams.CONTACT_PHONE,"")+" "+
                            getString(R.string.or)+" "+
                            mObject.optString(WebParams.CONTACT_EMAIL,"");
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        View layout_no_trans = v.findViewById(R.id.layout_no_transaction);
        layout_no_trans.setVisibility(View.VISIBLE);
        layout_noData = v.findViewById(R.id.layout_no_data);
        TextView tv_textNoData = layout_noData.findViewById(R.id.txt_alert);
        tv_textNoData.setText(getString(R.string.no_transaction));
        TextView tv_message_noData = v.findViewById(R.id.cashouttunai_message_notransaction);
        tv_message_noData.setText(getString(R.string.cashoutagen_nodata_message) + "\n" + listContactPhone);
    }

    private void InitializeData(JSONObject mJson){

        View layout_main = v.findViewById(R.id.layout_cashoutagen);
        layout_main.setVisibility(View.VISIBLE);

        dataInq = mJson;

        TextView tvUserID = v.findViewById(R.id.cashoutagen_userId_value);
        TextView tvTxID = v.findViewById(R.id.cashoutagen_trxid_value);
        TextView tvNameAdmin = v.findViewById(R.id.cashout_admin_name_value);
        TextView tvAmount = v.findViewById(R.id.cashoutagen_amount_value);
        TextView tvFee = v.findViewById(R.id.cashoutagen_fee_value);
        TextView tvTotal = v.findViewById(R.id.cashoutagen_total_amount_value);
        btn_proses = v.findViewById(R.id.btn_verification);
        btn_batal = v.findViewById(R.id.btn_cancel);

        max_token_resend = mJson.optInt(WebParams.MAX_RESEND,3);
        count_resend = mJson.optInt(WebParams.COUNT_RESEND,0);
        count_resend = max_token_resend - count_resend;

        tx_id = mJson.optString(WebParams.TX_ID, "");
        nameadmin = mJson.optString(WebParams.NAME_ADMIN,"");
        amount = mJson.optString(WebParams.AMOUNT,"");
        fee = mJson.optString(WebParams.FEE,"");
        total = mJson.optString(WebParams.TOTAL,"");
        ccy = mJson.optString(WebParams.CCY_ID,"");

        tvUserID.setText(userPhoneID);
        tvTxID.setText(tx_id);
        tvNameAdmin.setText(nameadmin);
        tvAmount.setText(ccy+" "+CurrencyFormat.format(amount));
        tvFee.setText(ccy+" "+CurrencyFormat.format(fee));
        tvTotal.setText(ccy+" "+CurrencyFormat.format(total));

        authType = sp.getString(DefineValue.AUTHENTICATION_TYPE,"");

        if(authType.equalsIgnoreCase("OTP")) {
            isOTP = true;
            View layoutOTP = v.findViewById(R.id.layout_token);
            et_otp = layoutOTP.findViewById(R.id.cashout_token_value);
            layoutOTP.setVisibility(View.VISIBLE);

            View layout_resendbtn = v.findViewById(R.id.layout_btn_resend);
            btnResend= v.findViewById(R.id.btn_resend);
            layout_resendbtn.setVisibility(View.VISIBLE);

            btnResend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(InetHandler.isNetworkAvailable(getActivity())) {
                        btn_proses.setEnabled(false);
                        btnResend.setEnabled(false);
                        btn_batal.setEnabled(false);

                        if (count_resend != 0)
                            sentResendToken();
                    }
                    else DefinedDialog.showErrorDialog(getActivity(), getString(R.string.inethandler_dialog_message));
                }
            });

            changeTextBtnSub();
        }
        else {
            new UtilsLoader(getActivity(),sp).getFailedPIN(userPhoneID,new OnLoadDataListener() { //get pin attempt
                @Override
                public void onSuccess(Object deData) {
                    attempt = (int)deData;
                }

                @Override
                public void onFail(Bundle message) {

                }

                @Override
                public void onFailure(String message) {

                }
            });
        }

        btn_proses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(InetHandler.isNetworkAvailable(getActivity())) {
                    if (inputValidation()) {
                        btn_proses.setEnabled(false);
                        btn_batal.setEnabled(false);
                        if (isOTP) {
                            btnResend.setEnabled(false);
                            sentReqCodeWithdraw(RSA.opensslEncrypt(et_otp.getText().toString()));
                        } else
                            CallPINinput(-1);
                    }
                }else DefinedDialog.showErrorDialog(getActivity(), getString(R.string.inethandler_dialog_message));

            }
        });

        btn_batal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(InetHandler.isNetworkAvailable(getActivity())) {
                    btn_proses.setEnabled(false);
                    btn_batal.setEnabled(false);
                    if (isOTP)
                        btnResend.setEnabled(false);
                    showDialogDel();
                }
                else DefinedDialog.showErrorDialog(getActivity(), getString(R.string.inethandler_dialog_message));
            }
        });

    }

    public boolean inputValidation(){
        if(isOTP && et_otp.getText().toString().length()==0){
            et_otp.requestFocus();
            et_otp.setError(this.getString(R.string.forgetpass_edittext_validation));
            return false;
        }
        return true;
    }

    private void CallPINinput(int _attempt){
        Intent i = new Intent(getActivity(), InsertPIN.class);
        i.putExtra(DefineValue.IS_FORGOT_PASSWORD, true);
        if(_attempt == 1)
            i.putExtra(DefineValue.ATTEMPT,_attempt);
        startActivityForResult(i, MainPage.REQUEST_FINISH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(!btn_proses.isEnabled()){
            btn_proses.setEnabled(true);
            btn_batal.setEnabled(true);
            if(isOTP)
                btnResend.setEnabled(true);
        }
        //Timber.d("onActivity result", "Biller Fragment"+" / "+requestCode+" / "+resultCode);
        if(requestCode == MainPage.REQUEST_FINISH){
            //  Log.d("onActivity result", "Biller Fragment masuk request exit");
            if(resultCode == InsertPIN.RESULT_PIN_VALUE){
                String value_pin = data.getStringExtra(DefineValue.PIN_VALUE);
                sentReqCodeWithdraw(value_pin);
            }
        }
    }

    public void changeTextBtnSub() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (count_resend == 0)
                    btnResend.setEnabled(false);
                else
                    btnResend.setText(getString(R.string.reg2_btn_text_resend_token_sms) + " (" + count_resend + ")");

            }
        });
    }


    public void sentInquiryWithdraw(){
        try{

            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            extraSignature = memberIDLogin+"COC";

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID, MyApiClient.LINK_INQUIRY_WITHDRAW,
                    userPhoneID , accessKey, extraSignature);
            params.put(WebParams.MEMBER_ID, memberIDLogin);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.USER_ID, userPhoneID);
            params.put(WebParams.CASHOUT_TYPE, "COC");

            Timber.d("isi params sent inquiry withdraw:" + params.toString());

            JsonHttpResponseHandler mHandler = new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {

                        String code = response.getString(WebParams.ERROR_CODE);
                        Timber.d("isi response inquiry withdraw:"+response.toString());
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            InitializeData(response);
                        }
                        else if(code.equals(WebParams.LOGOUT_CODE)){
                            Timber.d("isi response autologout:" + response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(),message);
                        }
                        else if(code.equals(ErrorDefinition.NO_TRANSACTION)){
//                            initializeNoData();

                            if(progdialog.isShowing())
                                progdialog.dismiss();

                            String contactCenter = sp.getString(DefineValue.LIST_CONTACT_CENTER, "");
                            if(contactCenter.equals("")) {
                                getHelpList();
                            }
                            else {
                                initializeNoData();
                            }
                        }
                        else {
                            code = response.getString(WebParams.ERROR_CODE) + ":" + response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                            getFragmentManager().popBackStack();
                        }
                        if(progdialog.isShowing())
                            progdialog.dismiss();

                    } catch (JSONException e) {
                        progdialog.dismiss();
                        Toast.makeText(getActivity(), getString(R.string.internal_error), Toast.LENGTH_LONG).show();
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
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    if(progdialog.isShowing())
                        progdialog.dismiss();
                    Timber.w("Error Koneksi update inq withdraw:"+throwable.toString());
                }
                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    if(!isAdded())
                        MyApiClient.CancelRequestWS(getActivity(), true);
                }

                @Override
                public void onCancel() {
                    super.onCancel();
                    if(progdialog.isShowing())
                        progdialog.dismiss();
                }
            };

            MyApiClient.sentInqWithdraw(getActivity(),params, mHandler);

        }catch (Exception e){
            Timber.d("httpclient:"+ e.getMessage());
        }
    }

    public void sentReqCodeWithdraw(String tokenid){
        try{

            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID, MyApiClient.LINK_REQCODE_WITHDRAW,
                    userPhoneID , accessKey);
            params.put(WebParams.MEMBER_ID, memberIDLogin);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.USER_ID, userPhoneID);
            params.put(WebParams.MEMBER_ID,memberIDLogin);
            params.put(WebParams.TX_ID, tx_id);
            params.put(WebParams.TOKEN_ID, tokenid );


            Timber.d("isi params sent req code Withdraw:" + params.toString());

            JsonHttpResponseHandler mHandler = new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        btn_proses.setEnabled(true);
                        btn_batal.setEnabled(true);
                        if(isOTP && count_resend>0)
                            btnResend.setEnabled(true);

                        String code = response.getString(WebParams.ERROR_CODE);
                        Timber.d("isi response req Code withdraw:"+response.toString());
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            Fragment newFrag = FragCashOutAgenCode.
                                    newInstance(response.optString(WebParams.OTP_MEMBER,""),
                                            dataInq);
                            switchContent(newFrag, FragCashOutAgenCode.TAG);
                        }
                        else if(code.equals(WebParams.LOGOUT_CODE)){
                            Timber.d("isi response autologout:" + response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(),message);
                        }
                        else if(code.equals(ErrorDefinition.WRONG_PIN_CASHOUT)){
                            attempt = response.optInt(WebParams.FAILED_ATTEMPT, -1);
                            failed = response.optInt(WebParams.MAX_FAILED,3);
                            Toast.makeText(getActivity(), response.getString(WebParams.ERROR_MESSAGE), Toast.LENGTH_LONG).show();
                            CallPINinput(failed - attempt);
                        }
                        else {

                            code = response.getString(WebParams.ERROR_CODE) + ":" + response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                            getFragmentManager().popBackStack();
                        }
                        if(progdialog.isShowing())
                            progdialog.dismiss();

                    } catch (JSONException e) {
                        progdialog.dismiss();
                        Toast.makeText(getActivity(), getString(R.string.internal_error), Toast.LENGTH_LONG).show();
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

                private void failure (Throwable throwable){

                    btn_proses.setEnabled(true);
                    btn_batal.setEnabled(true);
                    if(isOTP && count_resend>0)
                        btnResend.setEnabled(true);
                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    if(progdialog.isShowing())
                        progdialog.dismiss();
                    Timber.w("Error Koneksi reqcode withdraw:"+throwable.toString());
                }

                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    if(!isAdded())
                        MyApiClient.CancelRequestWS(getActivity(), true);
                }

                @Override
                public void onCancel() {
                    super.onCancel();
                    if(progdialog.isShowing())
                        progdialog.dismiss();
                }
            };

            MyApiClient.sentReqCodeWithdraw(getActivity(), params, mHandler);

        }catch (Exception e){
            Timber.d("httpclient:"+ e.getMessage());
        }
    }


    public void sentResendToken(){
        try{

            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID, MyApiClient.LINK_RESENT_TOKEN_BILLER,
                    userPhoneID , accessKey);
            params.put(WebParams.TX_ID, tx_id);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.USER_ID, userPhoneID);

            Timber.d("isi params sent resend token:" + params.toString());

            JsonHttpResponseHandler mHandler = new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {

                        btn_proses.setEnabled(true);
                        btn_batal.setEnabled(true);
                        if(isOTP && count_resend>0)
                            btnResend.setEnabled(true);

                        String code = response.getString(WebParams.ERROR_CODE);
                        Timber.d("isi response resend token:"+response.toString());
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            count_resend = count_resend - 1;

                            changeTextBtnSub();
                            Toast.makeText(getActivity(), getString(R.string.reg2_notif_text_resend_token), Toast.LENGTH_SHORT).show();
                        }
                        else if(code.equals(WebParams.LOGOUT_CODE)){
                            Timber.d("isi response autologout:" + response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(),message);
                        }
                        else if(code.equals(ErrorDefinition.NO_TRANSACTION)){
                            layout_noData.setVisibility(View.VISIBLE);
                        }
                        else {
                            code = response.getString(WebParams.ERROR_CODE) + ":" + response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                            getFragmentManager().popBackStack();
                        }
                        if(progdialog.isShowing())
                            progdialog.dismiss();

                    } catch (JSONException e) {
                        progdialog.dismiss();
                        Toast.makeText(getActivity(), getString(R.string.internal_error), Toast.LENGTH_LONG).show();
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
                    btn_proses.setEnabled(true);
                    btn_batal.setEnabled(true);
                    if(isOTP && count_resend>0)
                        btnResend.setEnabled(true);

                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    if(progdialog.isShowing())
                        progdialog.dismiss();
                    Timber.w("Error Koneksi resend token:"+throwable.toString());
                }

                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    if(!isAdded())
                        MyApiClient.CancelRequestWS(getActivity(), true);
                }

                @Override
                public void onCancel() {
                    super.onCancel();
                    if(progdialog.isShowing())
                        progdialog.dismiss();
                }
            };

            MyApiClient.sentResendToken(getActivity(), params, mHandler);

        }catch (Exception e){
            Timber.d("httpclient:"+ e.getMessage());
        }
    }


    public void sentDelWithdraw(){
        try{

            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID, MyApiClient.LINK_DELTRX_WITHDRAW,
                    userPhoneID , accessKey);
            params.put(WebParams.MEMBER_ID, memberIDLogin);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.USER_ID, userPhoneID);
            params.put(WebParams.TX_ID, tx_id);


            Timber.d("isi params sent del Withdraw:" + params.toString());

            JsonHttpResponseHandler mHandler = new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {

                        btn_proses.setEnabled(true);
                        btn_batal.setEnabled(true);
                        if(isOTP && count_resend>0)
                            btnResend.setEnabled(true);

                        String code = response.getString(WebParams.ERROR_CODE);
                        Timber.d("isi response del withdraw:"+response.toString());
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            getActivity().finish();
                            Toast.makeText(getActivity(), getString(R.string.cashoutagen_del_withdraw_text),Toast.LENGTH_LONG).show();
                        }
                        else if(code.equals(WebParams.LOGOUT_CODE)){
                            Timber.d("isi response autologout:" + response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(),message);
                        }
                        else {

                            code = response.getString(WebParams.ERROR_CODE) + ":" + response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                            getFragmentManager().popBackStack();
                        }
                        if(progdialog.isShowing())
                            progdialog.dismiss();

                    } catch (JSONException e) {
                        progdialog.dismiss();
                        Toast.makeText(getActivity(), getString(R.string.internal_error), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    failed(throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    failed(throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    failed(throwable);
                }

                private void failed(Throwable throwable){

                    btn_proses.setEnabled(true);
                    btn_batal.setEnabled(true);
                    if(isOTP && count_resend>0)
                        btnResend.setEnabled(true);
                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    if(progdialog.isShowing())
                        progdialog.dismiss();
                    Timber.w("Error Koneksi del withdraw:"+throwable.toString());
                }

                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    if(!isAdded())
                        MyApiClient.CancelRequestWS(getActivity(), true);
                }

                @Override
                public void onCancel() {
                    super.onCancel();
                    if(progdialog.isShowing())
                        progdialog.dismiss();
                }
            };

            MyApiClient.sentDelTrxWithdraw(getActivity(), params, mHandler);

        }catch (Exception e){
            Timber.d("httpclient:"+ e.getMessage());
        }
    }

    public void getHelpList() {
        try {
            progdialog = DefinedDialog.CreateProgressDialog(act, "");
//            progdialog.show();

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID,MyApiClient.LINK_USER_CONTACT_INSERT,
                    userPhoneID,accessKey);
            params.put(WebParams.USER_ID, userPhoneID);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            Timber.d("isi params help list:" + params.toString());

            MyApiClient.getHelpList(getActivity(), params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        String message = response.getString(WebParams.ERROR_MESSAGE);

                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            Timber.d("isi params help list:" + response.toString());

                            SecurePreferences.Editor mEditor = sp.edit();
                            mEditor.putString(DefineValue.LIST_CONTACT_CENTER, response.getString(WebParams.CONTACT_DATA));
                            mEditor.apply();

                            initializeNoData();
//                            try {
//                                JSONArray arrayContact = new JSONArray(contactCenter);
//                                for (int i = 0; i < arrayContact.length(); i++) {
//                                    if (i == 0) {
//                                        listContactPhone = arrayContact.getJSONObject(i).getString(WebParams.CONTACT_PHONE);
//                                        listAddress = arrayContact.getJSONObject(i).getString(WebParams.ADDRESS);
//                                    }
//                                }
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }

                        } else if (code.equals(WebParams.LOGOUT_CODE)) {
                            Timber.d("isi response autologout:" + response.toString());
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(act, message);
                        } else {
                            Timber.d("isi error help list:" + response.toString());
                            Toast.makeText(act, message, Toast.LENGTH_LONG).show();
                        }

                        if(progdialog.isShowing())
                            progdialog.dismiss();

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

                    Timber.w("Error Koneksi help list help:" + throwable.toString());
                }
            });
        }
        catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }
    }

    private void showDialogDel(){
        final AlertDialogFrag dialog_frag = AlertDialogFrag.newInstance(getString(R.string.cashoutagen_del_dialog_title),
                getString(R.string.cashoutagen_del_withdraw_dialog_text),getString(R.string.ok),getString(R.string.cancel),false);
        dialog_frag.setOkListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sentDelWithdraw();
            }
        });
        dialog_frag.setCancelListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                btn_proses.setEnabled(true);
                btn_batal.setEnabled(true);
                if(isOTP)
                    btnResend.setEnabled(true);
                dialog_frag.dismiss();
            }
        });
        dialog_frag.setTargetFragment(FragCashOutAgen.this, 0);
//        dialog_frag.show(getActivity().getSupportFragmentManager(), AlertDialogFrag.TAG);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.add(dialog_frag, null);
        ft.commitAllowingStateLoss();
    }

    private void switchContent(Fragment mFrag, String tag){
        if (getActivity() == null)
            return;

        CashoutActivity fca = (CashoutActivity) getActivity();
        fca.switchContent(mFrag, getString(R.string.menu_item_title_cash_out), true, tag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}