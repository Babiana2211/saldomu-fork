package com.sgo.saldomu.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.faber.circlestepview.CircleStepView;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.Beans.CashInHistoryModel;
import com.sgo.saldomu.Beans.CashOutHistoryModel;
import com.sgo.saldomu.BuildConfig;
import com.sgo.saldomu.R;
import com.sgo.saldomu.activities.RegisterSMSBankingActivity;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.InetHandler;
import com.sgo.saldomu.coreclass.MyApiClient;
import com.sgo.saldomu.coreclass.RealmManager;
import com.sgo.saldomu.coreclass.SMSclass;
import com.sgo.saldomu.coreclass.ToggleKeyboard;
import com.sgo.saldomu.coreclass.WebParams;
import com.sgo.saldomu.dialogs.AlertDialogLogout;
import com.sgo.saldomu.dialogs.DefinedDialog;
import com.sgo.saldomu.dialogs.SMSDialog;
import com.sgo.saldomu.entityRealm.BBSAccountACTModel;
import com.sgo.saldomu.entityRealm.BBSBankModel;
import com.sgo.saldomu.widgets.CustomAutoCompleteTextView;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import pub.devrel.easypermissions.EasyPermissions;
import io.realm.RealmChangeListener;
import timber.log.Timber;

/**
 * Created by thinkpad on 4/21/2017.
 */

public class BBSTransaksiInformasi extends Fragment implements EasyPermissions.PermissionCallbacks {
    public final static String TAG = "com.sgo.saldomu.fragments.BBSTransaksiInformasi";
    private final String MANDIRISMS = "MANDIRISMS";
    private static final int RC_READ_PHONE_STATE = 122;
    private static final int RC_SEND_SMS = 123;
    private View v;
    private ProgressDialog progdialog;
    private Activity act;
    private TextView tvTitle;
    private CustomAutoCompleteTextView actv_rekening_cta;
    private Spinner sp_rekening_act;
    private List<HashMap<String,String>> aListAgent;
    private SimpleAdapter adapterAgent;
    private List<BBSBankModel> listbankSource;
    private List<BBSAccountACTModel> listbankBenef;
    private String CTA = "CTA";
    private String ATC = "ATC";
    private String SOURCE = "SOURCE";
    private String BENEF = "BENEF";
    private EditText etNoHp, etRemark;
    private Button btnNext, btnBack;
    private SMSclass smSclass;
    private SMSDialog smsDialog;
    private Boolean isSMSBanking = false, isSimExist = false;
    private BBSTransaksiInformasi.ActionListener actionListener;
    private String userID, accessKey, comm_code, member_code, source_product_code="", source_product_type,
            benef_product_code, benef_product_name, benef_product_type, source_product_h2h,
            api_key, callback_url, source_product_name, productValue="", comm_id, city_id, amount,
            transaksi, no_benef, name_benef,city_name,no_source;
    Realm realmBBS;
    CashInHistoryModel cashInHistoryModel;
    CashOutHistoryModel cashOutHistoryModel;
    SecurePreferences sp;

    public interface ActionListener{
        void ChangeActivityFromCashInput(Intent data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = CustomSecurePref.getInstance().getmSecurePrefs();
        userID = sp.getString(DefineValue.USERID_PHONE,"");
        accessKey = sp.getString(DefineValue.ACCESS_KEY,"");
        realmBBS = Realm.getInstance(RealmManager.BBSConfiguration);
        realmBBS.addChangeListener(new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm element) {
                setBankDataBenef();
                if(adapterAgent != null)
                    adapterAgent.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDestroy() {
        RealmManager.closeRealm(realmBBS);
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getTargetFragment() instanceof BBSTransaksiInformasi.ActionListener) {
            actionListener = (BBSTransaksiInformasi.ActionListener) getTargetFragment();
        } else {
            if(context instanceof BBSTransaksiInformasi.ActionListener){
                actionListener = (BBSTransaksiInformasi.ActionListener) context;
            }
            else {
                throw new RuntimeException(context.toString()
                        + " must implement ActionListener");
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        actionListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        v =  inflater.inflate(R.layout.bbs_transaksi_informasi, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        act = getActivity();

        Bundle bundle = getArguments();
        if(bundle!= null) {
            transaksi = bundle.getString(DefineValue.TRANSACTION);
            amount = bundle.getString(DefineValue.AMOUNT);
            comm_id = bundle.getString(DefineValue.COMMUNITY_ID);
            comm_code = bundle.getString(DefineValue.COMMUNITY_CODE);
            member_code = bundle.getString(DefineValue.MEMBER_CODE);
            callback_url = bundle.getString(DefineValue.CALLBACK_URL);
            api_key = bundle.getString(DefineValue.API_KEY);
            if(transaksi.equalsIgnoreCase(getString(R.string.cash_in))) {
                String cashIn = sp.getString(DefineValue.CASH_IN_HISTORY_TEMP, "");
                Gson gson = new Gson();
                cashInHistoryModel = gson.fromJson(cashIn, CashInHistoryModel.class);

                benef_product_code = bundle.getString(DefineValue.BENEF_PRODUCT_CODE);
                benef_product_name = bundle.getString(DefineValue.BENEF_PRODUCT_NAME);
                benef_product_type = bundle.getString(DefineValue.BENEF_PRODUCT_TYPE);
                if (cashInHistoryModel!=null)
                {
                    source_product_code=(cashInHistoryModel.getSource_product_code());
                    source_product_name=(cashInHistoryModel.getSource_product_name());
                    source_product_type=(cashInHistoryModel.getSource_product_type());
                    source_product_h2h=(cashInHistoryModel.getSource_product_h2h());
                }
                no_benef = bundle.getString(DefineValue.NO_BENEF);
                name_benef = bundle.getString(DefineValue.NAME_BENEF);
                setBankDataSourceCTA();
                if(benef_product_type.equalsIgnoreCase(DefineValue.ACCT)) {
                    city_id = bundle.getString(DefineValue.ACCT_CITY_CODE);
                    city_name = bundle.getString(DefineValue.ACCT_CITY_NAME);
                }
            }
            else {
                String cashOut = sp.getString(DefineValue.CASH_OUT_HISTORY_TEMP, "");
                Gson gson1 = new Gson();
                cashOutHistoryModel = gson1.fromJson(cashOut, CashOutHistoryModel.class);

                source_product_code = bundle.getString(DefineValue.SOURCE_PRODUCT_CODE);
                source_product_type = bundle.getString(DefineValue.SOURCE_PRODUCT_TYPE);
                source_product_h2h = bundle.getString(DefineValue.SOURCE_PRODUCT_H2H);
                source_product_name = bundle.getString(DefineValue.SOURCE_PRODUCT_NAME);
                no_source = bundle.getString(DefineValue.SOURCE_ACCT_NO);

                setBankDataBenef();
                if (cashOutHistoryModel!=null)
                {
                    benef_product_code = (cashOutHistoryModel.getBenef_product_code());
                    benef_product_type = (cashOutHistoryModel.getBenef_product_type());
                    benef_product_name = (cashOutHistoryModel.getBenef_product_name());
                }
            }

            CircleStepView mCircleStepView = ((CircleStepView) v.findViewById(R.id.circle_step_view));
            mCircleStepView.setTextBelowCircle(getString(R.string.transaction), getString(R.string.informasi), getString(R.string.konfirmasi));
            mCircleStepView.setCurrentCircleIndex(1, false);

            tvTitle = (TextView) v.findViewById(R.id.tv_title);
            btnNext = (Button) v.findViewById(R.id.proses_btn);
            btnBack = (Button) v.findViewById(R.id.back_btn);
            ViewStub stub = (ViewStub) v.findViewById(R.id.informasi_stub);

            tvTitle.setText(transaksi);
            if (transaksi.equalsIgnoreCase(getString(R.string.cash_in))) {
                stub.setLayoutResource(R.layout.bbs_cashin_informasi);
                View cashin_layout = stub.inflate();
                actv_rekening_cta = (CustomAutoCompleteTextView) cashin_layout.findViewById(R.id.rekening_agen_value);
                etNoHp = (EditText) cashin_layout.findViewById(R.id.no_hp_pengirim_value);
                etRemark = (EditText) cashin_layout.findViewById(R.id.message_value);// Keys used in Hashmap

                if(bundle.containsKey(DefineValue.KEY_CODE))
                {
                    etNoHp.setText(bundle.getString(DefineValue.KEY_CODE));
                }
                else{
                    if (cashInHistoryModel!=null)
                    {
                        etNoHp.setText(cashInHistoryModel.getMember_shop_phone());
                    }
                }
                if (cashInHistoryModel!=null)
                {
                    actv_rekening_cta.setText(cashInHistoryModel.getSource_product_name());
                    etRemark.setText(cashInHistoryModel.getPesan());
                }

                String[] from = {"flag", "txt"};

                // Ids of views in listview_layout
                int[] to = {R.id.flag, R.id.txt};

                aListAgent = new ArrayList<>();
                // Instantiating an adapter to store each items
                // R.layout.listview_layout defines the layout of each item
                adapterAgent = new SimpleAdapter(getActivity().getBaseContext(), aListAgent, R.layout.bbs_autocomplete_layout, from, to);
                setAgent(listbankSource);
                actv_rekening_cta.setAdapter(adapterAgent);
                actv_rekening_cta.addTextChangedListener(textWatcher);
            } else {
                stub.setLayoutResource(R.layout.bbs_cashout_informasi);
                View cashout_layout = stub.inflate();
                sp_rekening_act = (Spinner) cashout_layout.findViewById(R.id.rekening_agen_value);
                etRemark = (EditText) cashout_layout.findViewById(R.id.message_value);
                String[] from = {"flag", "txt"};
                // Ids of views in listview_layout
                int[] to = {R.id.flag, R.id.txt};

                aListAgent = new ArrayList<>();
                // Instantiating an adapter to store each items
                // R.layout.listview_layout defines the layout of each item
                adapterAgent = new SimpleAdapter(getActivity().getBaseContext(), aListAgent, R.layout.bbs_autocomplete_layout, from, to);
                setAgentATC(listbankBenef);
                sp_rekening_act.setAdapter(adapterAgent);
                sp_rekening_act.setOnItemSelectedListener(spAgentListener);

                if(cashOutHistoryModel!=null)
                {
                    for(int i = 0 ; i < aListAgent.size();i++){
                        if (aListAgent.get(i).get("txt").equalsIgnoreCase(benef_product_name)){
                            sp_rekening_act.setSelection(i);
                            break;
                        }
                    }
                    etRemark.setText(cashOutHistoryModel.getPesan());
                }

            }

            btnBack.setOnClickListener(backListener);
            btnNext.setOnClickListener(nextListener);
        }
        else {
            getFragmentManager().popBackStack();
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            source_product_code="";
            source_product_name="";
            source_product_type="";
            source_product_h2h="";
            int position;
            String nameAcct = actv_rekening_cta.getText().toString();
            for(int i = 0 ; i < aListAgent.size() ; i++) {
                if(nameAcct.equalsIgnoreCase(aListAgent.get(i).get("txt"))) {
                    position = i;
                    source_product_code = listbankSource.get(position).getProduct_code();
                    source_product_type = listbankSource.get(position).getProduct_type();
                    source_product_h2h = listbankSource.get(position).getProduct_h2h();
                    source_product_name = listbankSource.get(position).getProduct_name();
                    break;
                }
            }

        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    Spinner.OnItemSelectedListener spAgentListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            benef_product_code = listbankBenef.get(position).getProduct_code();
            benef_product_type = listbankBenef.get(position).getProduct_type();
            benef_product_name = listbankBenef.get(position).getProduct_name();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    Button.OnClickListener backListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            }
            else
                getActivity().finish();
        }
    };

    Button.OnClickListener nextListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(InetHandler.isNetworkAvailable(getActivity())) {
                isSMSBanking = source_product_code.equalsIgnoreCase(MANDIRISMS);

                if(transaksi.equalsIgnoreCase(getString(R.string.cash_in))) {
                    if (isSMSBanking) {
                        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.READ_PHONE_STATE)) {
                            initializeSmsClass();
                            if (isSimExist)
                                SubmitAction();
                        } else {
                            // Ask for one permission
                            EasyPermissions.requestPermissions(BBSTransaksiInformasi.this, getString(R.string.rationale_phone_state),
                                    RC_READ_PHONE_STATE, Manifest.permission.READ_PHONE_STATE);
                        }
                    } else {
                        SubmitAction();
                    }
                }
                else {
                    btnNext.setEnabled(false);
                    if (inputValidation()) {
                        sentInsertA2C();
                    }
                    else btnNext.setEnabled(true);
                }
            }
            else DefinedDialog.showErrorDialog(getActivity(), getString(R.string.inethandler_dialog_message));
        }
    };

    private void SubmitAction(){
        btnNext.setEnabled(false);
        if (inputValidation()) {
            sentInsertC2A();
        }
        else
            btnNext.setEnabled(true);
    }

    private void setAgent(List<BBSBankModel> bankAgen) {
        aListAgent.clear();

        for(int i=0;i<bankAgen.size();i++){
            HashMap<String, String> hm = new HashMap<>();
            hm.put("txt", bankAgen.get(i).getProduct_name());

            if(bankAgen.get(i).getProduct_name().toLowerCase().contains("mandiri"))
                hm.put("flag", Integer.toString(R.drawable.logo_mandiri_bank_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("bri"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_bri_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("permata"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_permata_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("uob"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_uob_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("maspion"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_maspion_rev1_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("bii"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_bii_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("jatim"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_jatim_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("bca"))
                hm.put("flag", Integer.toString(R.drawable.logo_bca_bank_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("nobu"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_nobu));
            else
                hm.put("flag", Integer.toString(R.drawable.ic_square_gate_one));
            aListAgent.add(hm);
        }
        adapterAgent.notifyDataSetChanged();
    }

    private void setAgentATC(List<BBSAccountACTModel> bankAgen) {
        aListAgent.clear();

        for(int i=0;i<bankAgen.size();i++){
            HashMap<String, String> hm = new HashMap<>();
            hm.put("txt", bankAgen.get(i).getProduct_name());

            if(bankAgen.get(i).getProduct_name().toLowerCase().contains("mandiri"))
                hm.put("flag", Integer.toString(R.drawable.logo_mandiri_bank_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("bri"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_bri_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("permata"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_permata_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("uob"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_uob_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("maspion"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_maspion_rev1_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("bii"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_bii_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("jatim"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_jatim_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("bca"))
                hm.put("flag", Integer.toString(R.drawable.logo_bca_bank_small));
            else if(bankAgen.get(i).getProduct_name().toLowerCase().contains("nobu"))
                hm.put("flag", Integer.toString(R.drawable.logo_bank_nobu));
            else
                hm.put("flag", Integer.toString(R.drawable.ic_square_gate_one));
            aListAgent.add(hm);
        }
        adapterAgent.notifyDataSetChanged();
    }



    private void setBankDataBenef(){
        listbankBenef = realmBBS.where(BBSAccountACTModel.class).findAll();
    }

    private void setBankDataSourceCTA(){
        listbankSource = realmBBS.where(BBSBankModel.class)
                .equalTo(WebParams.SCHEME_CODE, CTA)
                .equalTo(WebParams.COMM_TYPE, SOURCE).findAll();
    }

    private void sentInsertC2A() {
        try {
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");
            progdialog.show();

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID, MyApiClient.LINK_GLOBAL_BBS_INSERT_C2A,
                    userID, accessKey);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.USER_ID, userID);
            params.put(WebParams.COMM_CODE, comm_code);
            params.put(WebParams.MEMBER_CODE, member_code);
            params.put(WebParams.SOURCE_PRODUCT_CODE, source_product_code);
            params.put(WebParams.SOURCE_PRODUCT_TYPE, source_product_type);
            params.put(WebParams.BENEF_PRODUCT_CODE, benef_product_code);
            params.put(WebParams.BENEF_PRODUCT_TYPE, benef_product_type);
            params.put(WebParams.BENEF_PRODUCT_VALUE_CODE, no_benef);
            params.put(WebParams.BENEF_PRODUCT_VALUE_NAME, name_benef);
            if(benef_product_type.equalsIgnoreCase(DefineValue.ACCT)) {
                params.put(WebParams.BENEF_PRODUCT_VALUE_CITY, city_id);
            }
            params.put(WebParams.CCY_ID, MyApiClient.CCY_VALUE);
            params.put(WebParams.AMOUNT, amount);
            params.put(WebParams.PAYMENT_REMARK, etRemark.getText().toString());
            params.put(WebParams.MEMBER_SHOP_PHONE, etNoHp.getText().toString());
            params.put(WebParams.USER_COMM_CODE, BuildConfig.commCodeBBSATC);

            Log.d("params insert c2a", params.toString());
            MyApiClient.sentGlobalBBSInsertC2A(getActivity(),params, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                    btnNext.setEnabled(true);
                    progdialog.dismiss();

                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            Timber.d("isi response sent insert C2A:"+response.toString());
                            if(isSMSBanking) {
                                if(smsDialog == null){
                                    smsDialog = new SMSDialog(getActivity(), null);
                                }

                                smsDialog.setListener(new SMSDialog.DialogButtonListener() {
                                    @Override
                                    public void onClickOkButton(View v, boolean isLongClick) {
                                        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.SEND_SMS)) {
                                            smsDialog.sentSms();
                                            RegSimCardReceiver(true);
                                        } else {
                                            EasyPermissions.requestPermissions(BBSTransaksiInformasi.this, getString(R.string.rationale_send_sms),
                                                    RC_SEND_SMS, Manifest.permission.SEND_SMS);
                                        }
                                    }

                                    @Override
                                    public void onClickCancelButton(View v, boolean isLongClick) {
                                        if(progdialog.isShowing())
                                            progdialog.dismiss();
                                    }

                                    @Override
                                    public void onSuccess(int user_is_new) {

                                    }

                                    @Override
                                    public void onSuccess(String product_value) {
                                        productValue = product_value;
                                        smsDialog.dismiss();
                                        smsDialog.reset();
                                        try {
                                            sentDataReqToken(response.getString(WebParams.TX_ID), response.getString(WebParams.TX_PRODUCT_CODE),
                                                    response.getString(WebParams.TX_PRODUCT_NAME), response.getString(WebParams.TX_BANK_CODE),
                                                    response.getString(WebParams.AMOUNT), response.getString(WebParams.ADMIN_FEE),
                                                    response.getString(WebParams.TOTAL_AMOUNT), response.getString(WebParams.TX_BANK_NAME),
                                                    response.getString(WebParams.MAX_RESEND_TOKEN), response.getString(WebParams.BENEF_PRODUCT_VALUE_CODE),
                                                    response.getString(WebParams.BENEF_PRODUCT_VALUE_NAME));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });


                                if(isSimExist)
                                    smsDialog.show();
                            }
                            else if(source_product_h2h.equalsIgnoreCase("Y") && source_product_type.equalsIgnoreCase(DefineValue.EMO)) {
                                sentDataReqToken(response.getString(WebParams.TX_ID), response.getString(WebParams.TX_PRODUCT_CODE),
                                        response.getString(WebParams.TX_PRODUCT_NAME), response.getString(WebParams.TX_BANK_CODE),
                                        response.getString(WebParams.AMOUNT), response.getString(WebParams.ADMIN_FEE),
                                        response.getString(WebParams.TOTAL_AMOUNT), response.getString(WebParams.TX_BANK_NAME),
                                        response.getString(WebParams.MAX_RESEND_TOKEN), response.getString(WebParams.BENEF_PRODUCT_VALUE_CODE),
                                        response.getString(WebParams.BENEF_PRODUCT_VALUE_NAME));
                            }
                            else {
                                changeToConfirmCashIn(response.getString(WebParams.TX_ID), response.getString(WebParams.TX_PRODUCT_CODE),
                                        response.getString(WebParams.TX_PRODUCT_NAME), response.getString(WebParams.TX_BANK_CODE),
                                        response.getString(WebParams.AMOUNT), response.getString(WebParams.ADMIN_FEE),
                                        response.getString(WebParams.TOTAL_AMOUNT), response.getString(WebParams.TX_BANK_NAME),
                                        response.getString(WebParams.MAX_RESEND_TOKEN), response.getString(WebParams.BENEF_PRODUCT_VALUE_CODE),
                                        response.getString(WebParams.BENEF_PRODUCT_VALUE_NAME));
                            }

                        } else if (code.equals(WebParams.LOGOUT_CODE)) {
                            Timber.d("isi response autologout:" + response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(), message);
                        }else {
                            Timber.d("isi error sent insert C2A:"+response.toString());
                            String code_msg = response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code_msg, Toast.LENGTH_LONG).show();
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
                    btnNext.setEnabled(true);
                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    if(progdialog.isShowing())
                        progdialog.dismiss();

                    Timber.w("Error Koneksi sent insert C2A:"+throwable.toString());
                }

            });
        }catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }
    }


    private void sentInsertA2C() {
        try {
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");
            progdialog.show();

            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID, MyApiClient.LINK_GLOBAL_BBS_INSERT_A2C,
                    userID, accessKey);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);
            params.put(WebParams.USER_ID, userID);
            params.put(WebParams.COMM_CODE, comm_code);
            params.put(WebParams.MEMBER_CODE, member_code);
            params.put(WebParams.SOURCE_PRODUCT_CODE, source_product_code);
            params.put(WebParams.SOURCE_PRODUCT_TYPE, source_product_type);
            params.put(WebParams.SOURCE_PRODUCT_VALUE, no_source);
            params.put(WebParams.BENEF_PRODUCT_CODE, benef_product_code);
            params.put(WebParams.BENEF_PRODUCT_TYPE, benef_product_type);
            params.put(WebParams.CCY_ID, MyApiClient.CCY_VALUE);
            params.put(WebParams.AMOUNT, amount);
            params.put(WebParams.PAYMENT_REMARK, etRemark.getText().toString());

            Log.d("params insert a2c", params.toString());
            MyApiClient.sentGlobalBBSInsertA2C(getActivity(),params, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                    btnNext.setEnabled(true);
                    progdialog.dismiss();

                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            Timber.d("isi response sent insert a2c:"+response.toString());

                            sentDataReqToken(response.getString(WebParams.TX_ID), response.getString(WebParams.TX_PRODUCT_CODE),
                                    response.getString(WebParams.TX_PRODUCT_NAME), response.getString(WebParams.TX_BANK_CODE),
                                    response.getString(WebParams.AMOUNT), response.getString(WebParams.TX_BANK_NAME), null, null,
                                    null, null, null);

                        } else if (code.equals(WebParams.LOGOUT_CODE)) {
                            Timber.d("isi response autologout:" + response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(), message);
                        }else {
                            Timber.d("isi error sent insert a2c:"+response.toString());
                            String code_msg = response.getString(WebParams.ERROR_MESSAGE);
                            Toast.makeText(getActivity(), code_msg, Toast.LENGTH_LONG).show();
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
                    btnNext.setEnabled(true);
                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    if(progdialog.isShowing())
                        progdialog.dismiss();

                    Timber.w("Error Koneksi sent insert a2c:"+throwable.toString());
                }

            });
        }catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }
    }

    public void sentDataReqToken(final String _tx_id, final String _product_code, final String _product_name, final String _bank_code,
                                 final String _amount, final String fee, final String totalAmount, final String _bank_name,
                                 final String _max_resend_token, final String _benef_acct_no, final String _benef_acct_name) {
        try{
            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");
            progdialog.show();

            RequestParams params = MyApiClient.getSignatureWithParams(comm_id,MyApiClient.LINK_REQ_TOKEN_SGOL,
                    userID,accessKey);
            params.put(WebParams.COMM_CODE, comm_code);
            params.put(WebParams.TX_ID, _tx_id);
            params.put(WebParams.PRODUCT_CODE, _product_code);
            params.put(WebParams.USER_ID, userID);
            params.put(WebParams.COMM_ID, comm_id);
            if(isSMSBanking)
                params.put(WebParams.PRODUCT_VALUE,productValue);

            Timber.d("isi params regtoken Sgo+:"+params.toString());

            MyApiClient.sentDataReqTokenSGOL(getActivity(),params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    progdialog.dismiss();
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        Timber.d("response reqtoken :"+response.toString());
                        if (code.equals(WebParams.SUCCESS_CODE)) {
                            if(transaksi.equalsIgnoreCase(getString(R.string.cash_in))) {
                                if (isSMSBanking)
                                    showDialog(_tx_id, _product_code, _product_name, _bank_code,
                                            _amount, fee, totalAmount, _bank_name, _max_resend_token,
                                            _benef_acct_no, _benef_acct_name);
                                else
                                    changeToConfirmCashIn(_tx_id, _product_code, _product_name, _bank_code,
                                            _amount, fee, totalAmount, _bank_name, _max_resend_token,
                                            _benef_acct_no, _benef_acct_name);
                            }
                            else {
                                changeToConfirmCashout(_tx_id, _product_code, _product_name, _bank_code,
                                        _amount, _bank_name);
                            }
                        }
                        else if(code.equals(WebParams.LOGOUT_CODE)){
                            Timber.d("isi response autologout:"+response.toString());
                            String message = response.getString(WebParams.ERROR_MESSAGE);
                            AlertDialogLogout test = AlertDialogLogout.getInstance();
                            test.showDialoginActivity(getActivity(),message);
                        }
                        else {
                            if(code.equals("0059")||code.equals("0164")){
                                showDialogErrorSMS(_bank_name,code,response.optString(WebParams.ERROR_MESSAGE,""));
                            }
                            else {
                                code = response.getString(WebParams.ERROR_CODE) + ":" + response.getString(WebParams.ERROR_MESSAGE);
                                Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                            }
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
                    if (MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(getActivity(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();
                    if(progdialog.isShowing())
                        progdialog.dismiss();
                    Timber.w("Error Koneksi reg token sgo input:"+throwable.toString());
                }
            });
        }catch (Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }
    }

    private void showDialog(final String _tx_id, final String _product_code, final String _product_name, final String _bank_code,
                            final String _amount, final String fee, final String totalAmount, final String _bank_name,
                            final String _max_resend_token, final String _benef_acct_no, final String _benef_acct_name) {
        // Create custom dialog object
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        // Include dialog.xml file
        dialog.setContentView(R.layout.dialog_notification);

        // set values for custom dialog components - text, image and button
        Button btnDialogOTP = (Button)dialog.findViewById(R.id.btn_dialog_notification_ok);
        TextView Title = (TextView)dialog.findViewById(R.id.title_dialog);
        TextView Message = (TextView)dialog.findViewById(R.id.message_dialog);

        Message.setVisibility(View.VISIBLE);
        Title.setText(getResources().getString(R.string.regist1_notif_title_verification));
        Message.setText(getString(R.string.appname)+" "+getString(R.string.dialog_token_message_sms));
        btnDialogOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeToConfirmCashIn(_tx_id, _product_code, _product_name, _bank_code,
                        _amount, fee, totalAmount, _bank_name, _max_resend_token,
                        _benef_acct_no, _benef_acct_name);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void changeToConfirmCashIn(String _tx_id, String _product_code, String _product_name, String _bank_code,
                                       String _amount, String fee, String totalAmount, String _bank_name, String _max_resend_token,
                                       String _benef_acct_no, String _benef_acct_name) {

        Bundle mArgs = new Bundle();
        if(benef_product_type.equalsIgnoreCase(DefineValue.ACCT)) {
            mArgs.putString(DefineValue.BENEF_CITY, city_name);
        }
        mArgs.putString(DefineValue.PRODUCT_H2H, source_product_h2h);
        mArgs.putString(DefineValue.PRODUCT_TYPE, source_product_type);
        mArgs.putString(DefineValue.PRODUCT_CODE, _product_code);
        mArgs.putString(DefineValue.BANK_CODE, _bank_code);
        mArgs.putString(DefineValue.BANK_NAME, _bank_name);
        mArgs.putString(DefineValue.PRODUCT_NAME,_product_name);
        mArgs.putString(DefineValue.FEE, fee);
        mArgs.putString(DefineValue.COMMUNITY_CODE,comm_code);
        mArgs.putString(DefineValue.TX_ID,_tx_id);
        mArgs.putString(DefineValue.AMOUNT,_amount);
        mArgs.putString(DefineValue.TOTAL_AMOUNT,totalAmount);
        mArgs.putString(DefineValue.SHARE_TYPE,"1");
        mArgs.putString(DefineValue.CALLBACK_URL,callback_url);
        mArgs.putString(DefineValue.API_KEY, api_key);
        mArgs.putString(DefineValue.COMMUNITY_ID, MyApiClient.COMM_ID);
        mArgs.putString(DefineValue.BANK_BENEF, benef_product_name);
        mArgs.putString(DefineValue.NAME_BENEF, _benef_acct_name);
        mArgs.putString(DefineValue.NO_BENEF, _benef_acct_no);
        mArgs.putString(DefineValue.TYPE_BENEF, benef_product_type);
        mArgs.putString(DefineValue.NO_HP_BENEF, etNoHp.getText().toString());
        mArgs.putString(DefineValue.REMARK, etRemark.getText().toString());
        mArgs.putString(DefineValue.SOURCE_ACCT, source_product_name);
        mArgs.putString(DefineValue.MAX_RESEND, _max_resend_token);
        mArgs.putString(DefineValue.TRANSACTION, transaksi);
        btnNext.setEnabled(true);
        cashInHistory();

        Fragment mFrag = new BBSCashInConfirm();
        mFrag.setArguments(mArgs);

        getFragmentManager().beginTransaction().addToBackStack(TAG)
                .replace(R.id.bbsTransaksiFragmentContent , mFrag, BBSCashInConfirm.TAG).commit();
        ToggleKeyboard.hide_keyboard(act);
//        switchFragment(mFrag, getString(R.string.cash_in), true);
    }

    private void cashInHistory ()
    {
        if (cashInHistoryModel==null)
        {
            cashInHistoryModel = new CashInHistoryModel();
        }

        cashInHistoryModel.setAmount(amount);
        cashInHistoryModel.setBenef_product_code(benef_product_code);
        cashInHistoryModel.setBenef_product_name(benef_product_name);
        cashInHistoryModel.setBenef_product_type(benef_product_type);
        cashInHistoryModel.setBenef_product_value_code(no_benef);
        cashInHistoryModel.setSource_product_code(source_product_code);
        cashInHistoryModel.setSource_product_name(actv_rekening_cta.getText().toString());
        cashInHistoryModel.setSource_product_type(source_product_type);
        cashInHistoryModel.setSource_product_h2h(source_product_h2h);
        cashInHistoryModel.setMember_shop_phone(etNoHp.getText().toString());
        cashInHistoryModel.setPesan(etRemark.getText().toString());

       if (!benef_product_type.equalsIgnoreCase(DefineValue.EMO))
       {
            cashInHistoryModel.setBenef_product_value_city(city_name);
       }

        Gson gson = new Gson();
        String jsonObject = gson.toJson(cashInHistoryModel, CashInHistoryModel.class);

        SecurePreferences.Editor editor = sp.edit();
        editor.putString(DefineValue.CASH_IN_HISTORY_TEMP, jsonObject);
        editor.apply();
    }


    private void changeToConfirmCashout(String _tx_id, String _product_code, String _product_name, String _bank_code,
                                        String _amount, String _bank_name) {

        Bundle mArgs = new Bundle();
        mArgs.putString(DefineValue.PRODUCT_H2H, source_product_h2h);
        mArgs.putString(DefineValue.PRODUCT_TYPE, source_product_type);
        mArgs.putString(DefineValue.PRODUCT_CODE, _product_code);
        mArgs.putString(DefineValue.BANK_CODE, _bank_code);
        mArgs.putString(DefineValue.BANK_NAME, _bank_name);
        mArgs.putString(DefineValue.PRODUCT_NAME,_product_name);
        mArgs.putString(DefineValue.COMMUNITY_CODE,comm_code);
        mArgs.putString(DefineValue.TX_ID,_tx_id);
        mArgs.putString(DefineValue.AMOUNT,_amount);
        mArgs.putString(DefineValue.SHARE_TYPE,"1");
        mArgs.putString(DefineValue.CALLBACK_URL,callback_url);
        mArgs.putString(DefineValue.API_KEY, api_key);
        mArgs.putString(DefineValue.COMMUNITY_ID, MyApiClient.COMM_ID);
        mArgs.putString(DefineValue.BANK_BENEF, benef_product_name);
        mArgs.putString(DefineValue.USER_ID, no_source);
        mArgs.putString(DefineValue.REMARK, etRemark.getText().toString());
        mArgs.putString(DefineValue.SOURCE_ACCT, source_product_name);
        mArgs.putString(DefineValue.TRANSACTION, transaksi);
        btnNext.setEnabled(true);
        cashOutHistory();

        Fragment mFrag = new CashOutBBS_confirm_agent();
        mFrag.setArguments(mArgs);
        getFragmentManager().beginTransaction().addToBackStack(TAG)
                .replace(R.id.bbsTransaksiFragmentContent , mFrag, CashOutBBS_confirm_agent.TAG).commit();
        ToggleKeyboard.hide_keyboard(act);
//        switchFragment(mFrag, getString(R.string.cash_out), true);
    }

    private void cashOutHistory ()
    {

        if (cashOutHistoryModel==null)
        {
            cashOutHistoryModel = new CashOutHistoryModel();
        }

        cashOutHistoryModel.setAmount(amount);
        cashOutHistoryModel.setBenef_product_code(benef_product_code);
        cashOutHistoryModel.setBenef_product_name(benef_product_name);
        cashOutHistoryModel.setBenef_product_type(benef_product_type);
        cashOutHistoryModel.setSource_product_code(source_product_code);
        cashOutHistoryModel.setSource_product_name(source_product_name);
        cashOutHistoryModel.setSource_product_type(source_product_type);
        cashOutHistoryModel.setSource_product_h2h(source_product_h2h);
        cashOutHistoryModel.setMember_shop_phone(no_source);
        cashOutHistoryModel.setPesan(etRemark.getText().toString());

        Gson gson1 = new Gson();
        String jsonObject = gson1.toJson(cashOutHistoryModel, CashOutHistoryModel.class);

        SecurePreferences.Editor editor = sp.edit();
        editor.putString(DefineValue.CASH_OUT_HISTORY_TEMP, jsonObject);
        editor.apply();
    }

    void showDialogErrorSMS(final String _nama_bank, String error_code, String error_msg) {
        // Create custom dialog object
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        // Include dialog.xml file
        dialog.setContentView(R.layout.dialog_notification);

        // set values for custom dialog components - text, image and button
        Button btnDialogOTP = (Button)dialog.findViewById(R.id.btn_dialog_notification_ok);
        TextView Title = (TextView)dialog.findViewById(R.id.title_dialog);
        TextView Message = (TextView)dialog.findViewById(R.id.message_dialog);

        Message.setVisibility(View.VISIBLE);
        Title.setText(getString(R.string.topup_dialog_not_registered));
        if(error_code.equals("0059")){
            Message.setText(error_msg);
            btnDialogOTP.setText(getString(R.string.firstscreen_button_daftar));
            btnDialogOTP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent newIntent = new Intent(getActivity(), RegisterSMSBankingActivity.class);
                    newIntent.putExtra(DefineValue.BANK_NAME,_nama_bank);
                    actionListener.ChangeActivityFromCashInput(newIntent);
                    dialog.dismiss();
                }
            });
        }
        else if(error_code.equals("0164")) {
            Message.setText(error_msg);
            btnDialogOTP.setText(getString(R.string.close));
            btnDialogOTP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    getActivity().finish();
                }
            });
        }

        dialog.show();
    }

    private void initializeSmsClass(){
        if(smSclass == null)
            smSclass = new SMSclass(getActivity(), CustomSimcardListener);

        smSclass.isSimExists(new SMSclass.SMS_SIM_STATE() {
            @Override
            public void sim_state(Boolean isExist, String msg) {
                if(!isExist){
                    isSimExist = false;
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    builder.setMessage(msg)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
                else
                    isSimExist = true;
            }
        });
    }

    private BroadcastReceiver CustomSimcardListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase("android.intent.action.SIM_STATE_CHANGED")) {
                if(intent.getStringExtra("ss").equalsIgnoreCase("ABSENT")){
                    if(smsDialog!= null && smsDialog.isShowing()) {
                        Toast.makeText(getActivity(), R.string.smsclass_simcard_listener_absent_toast, Toast.LENGTH_LONG).show();
                        smsDialog.dismiss();
                        smsDialog.reset();
                    }
                }

            }
        }
    };

    private void RegSimCardReceiver(Boolean isReg){
        if (isSMSBanking) {
            if(isReg){
                try{
                    getActivity().unregisterReceiver(CustomSimcardListener);
                }
                catch (Exception ignored){}
                getActivity().registerReceiver(CustomSimcardListener,SMSclass.simStateIntentFilter);
            }
            else {
                try{
                    getActivity().unregisterReceiver(CustomSimcardListener);
                }
                catch (Exception ignored){}
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(requestCode == RC_READ_PHONE_STATE){
            initializeSmsClass();
            if(isSimExist)
                SubmitAction();
        }
        else if(requestCode == RC_SEND_SMS ){
            smsDialog.sentSms();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(getActivity(), getString(R.string.cancel_permission_read_contacts), Toast.LENGTH_SHORT).show();
        if(requestCode == RC_SEND_SMS ){
            if(progdialog.isShowing())
                progdialog.dismiss();
            if (smsDialog != null) {
                smsDialog.dismiss();
                smsDialog.reset();
            }
        }
    }

    private boolean inputValidation() {
        if(transaksi.equalsIgnoreCase(getString(R.string.cash_in))) {
            if(actv_rekening_cta.getText().toString().length()==0){
                actv_rekening_cta.requestFocus();
                actv_rekening_cta.setError(getString(R.string.rekening_agent_error_message));
                return false;
            }
            if (etNoHp.getText().toString().length() == 0) {
                etNoHp.requestFocus();
                etNoHp.setError(getString(R.string.no_hp_pengirim_validation));
                return false;
            }
            if(source_product_code.equals("")) {
                Toast.makeText(act, getString(R.string.no_match_agent_acct_message), Toast.LENGTH_LONG).show();
                return false;
            }
        }
        else {
            if(benef_product_code.equals("")) {
                Toast.makeText(act, getString(R.string.no_match_agent_acct_message), Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
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

    @Override
    public void onResume() {
        super.onResume();
        RegSimCardReceiver(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        RegSimCardReceiver(false);
    }
}