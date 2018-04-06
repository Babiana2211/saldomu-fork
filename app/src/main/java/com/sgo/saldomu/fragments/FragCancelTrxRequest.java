package com.sgo.saldomu.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.BuildConfig;
import com.sgo.saldomu.R;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.MyApiClient;
import com.sgo.saldomu.coreclass.WebParams;
import com.sgo.saldomu.dialogs.DefinedDialog;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link FragCancelTrxRequest#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragCancelTrxRequest extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = "FragCancelTrxRequest";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Button btnProses, btnCancel;
    private EditText etReason;
    private String txId, userId;
    SecurePreferences sp;

    CancelTrxRequestListener cpl;
    ProgressDialog progdialog;

    public interface CancelTrxRequestListener {
        public void onSuccessCancelTrx(String txId);
    }

    public FragCancelTrxRequest() {
        // Required empty public constructor
        super();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragCancelTrxRequest.
     */
    // TODO: Rename and change types and number of parameters
    public static FragCancelTrxRequest newInstance(String param1, String param2) {
        FragCancelTrxRequest fragment = new FragCancelTrxRequest();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp                      = CustomSecurePref.getInstance().getmSecurePrefs();

        if (getArguments() != null) {
            txId = getArguments().getString(DefineValue.TX_ID);
            userId = getArguments().getString(DefineValue.CUST_ID);
        }

        try {
            cpl = (FragCancelTrxRequest.CancelTrxRequestListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling fragment must implement CancelTrxRequestListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frag_cancel_trx_request, container, false);

        btnProses   = (Button) v.findViewById(R.id.btnProses);
        btnCancel   = (Button) v.findViewById(R.id.btnCancel);
        etReason    = (EditText) v.findViewById(R.id.etReason);

        btnProses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reason       = etReason.getText().toString().trim();
                Boolean hasError    = false;

                if ( reason.equals("") ) {
                    hasError = true;
                    etReason.setError(getString(R.string.err_empty_cancel_reason));
                }

                if ( !hasError ) {
                    //call webservice

                    progdialog              = DefinedDialog.CreateProgressDialog(getContext());

                    String extraSignature   = txId;
                    RequestParams params            = MyApiClient.getSignatureWithParams(sp.getString(DefineValue.COMMUNITY_ID, ""),
                            MyApiClient.LINK_CANCEL_SEARCH_AGENT,
                            userId, sp.getString(DefineValue.ACCESS_KEY, ""), extraSignature);

                    params.put(WebParams.APP_ID, BuildConfig.APP_ID);
                    params.put(WebParams.SENDER_ID, DefineValue.BBS_SENDER_ID);
                    params.put(WebParams.RECEIVER_ID, DefineValue.BBS_RECEIVER_ID);
                    params.put(WebParams.TX_ID, txId);
                    params.put(WebParams.CUST_ID, userId);
                    params.put(WebParams.TX_REMARKS, reason);
                    params.put(WebParams.USER_ID, userId);

                    MyApiClient.cancelSearchAgent(getContext(), params, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                            Timber.d("Response Cancel Search Agent:" + response.toString());

                            if ( progdialog.isShowing())
                                progdialog.dismiss();

                            try {

                                String code = response.getString(WebParams.ERROR_CODE);
                                if (code.equals(WebParams.SUCCESS_CODE)) {
                                    cpl.onSuccessCancelTrx(txId);
                                } else {
                                    Toast.makeText(getContext(), response.getString(WebParams.ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
                                }

                                getDialog().dismiss();

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
                                Toast.makeText(getContext(), getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getContext(), throwable.toString(), Toast.LENGTH_SHORT).show();

                            Timber.w("Error Cancel Search Agent:" + throwable.toString());

                            if ( progdialog.isShowing() )
                                progdialog.dismiss();

                        }

                    });

                }
                //getDialog().dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });


        // Inflate the layout for this fragment
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
