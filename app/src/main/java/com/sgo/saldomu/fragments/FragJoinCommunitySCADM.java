package com.sgo.saldomu.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.sgo.saldomu.activities.JoinCommunitySCADMActivity;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.Singleton.MyApiClient;
import com.sgo.saldomu.coreclass.Singleton.RetrofitService;
import com.sgo.saldomu.coreclass.WebParams;
import com.sgo.saldomu.dialogs.AlertDialogLogout;
import com.sgo.saldomu.dialogs.DefinedDialog;
import com.sgo.saldomu.interfaces.ObjListeners;
import com.sgo.saldomu.widgets.BaseFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by Lenovo Thinkpad on 5/15/2018.
 */

public class FragJoinCommunitySCADM extends BaseFragment {
    View v;
    SecurePreferences sp;
    String comm_name, comm_code, member_code, member_name, comm_id_scadm;
    TextView community_name, community_code;
    EditText et_member_code;
    Button btn_next;
    private ProgressDialog progdialog;
    protected String memberIDLogin, commIDLogin, userPhoneID, accessKey;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.frag_join_community, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sp = CustomSecurePref.getInstance().getmSecurePrefs();

        memberIDLogin = sp.getString(DefineValue.MEMBER_ID,"");
        commIDLogin = sp.getString(DefineValue.COMMUNITY_ID,"");
        userPhoneID = sp.getString(DefineValue.USERID_PHONE,"");
        accessKey = sp.getString(DefineValue.ACCESS_KEY, "");

        Bundle bundle = getArguments();
        comm_name = bundle.getString(DefineValue.COMMUNITY_NAME);
        comm_code = bundle.getString(DefineValue.COMMUNITY_CODE);
        comm_id_scadm = bundle.getString(DefineValue.COMM_ID_SCADM);

        community_name = v.findViewById(R.id.community_name);
        community_code = v.findViewById(R.id.community_code);
        et_member_code = v.findViewById(R.id.member_code);
        btn_next = v.findViewById(R.id.btn_next);

        community_name.setText(comm_name);
        community_code.setText(comm_code);

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (et_member_code.getText().toString() == null || et_member_code.getText().toString().equalsIgnoreCase(""))
                {
                    et_member_code.requestFocus();
                    et_member_code.setError("Kode Member harus diisi!");
                }else nextlistener();
            }
        });
    }

    public void nextlistener() {
        et_member_code.setEnabled(false);
        sentPreviewJoinCommunitySCADM();
    }

    public void sentPreviewJoinCommunitySCADM() {
        try {

            progdialog = DefinedDialog.CreateProgressDialog(getActivity(), "");

            extraSignature = comm_id_scadm + et_member_code.getText().toString();
            RequestParams param = MyApiClient.getSignatureWithParams(commIDLogin, MyApiClient.LINK_GET_PREVIEW_COMMUNITY_SCADM,
                    userPhoneID, accessKey, extraSignature);
            HashMap<String, Object> params = RetrofitService.getInstance().getSignature(MyApiClient.LINK_GET_PREVIEW_COMMUNITY_SCADM, extraSignature);
            params.put(WebParams.USER_ID, userPhoneID);
            params.put(WebParams.COMM_ID_SCADM, comm_id_scadm);
            params.put(WebParams.MEMBER_CODE, et_member_code.getText().toString());

            Timber.d("isi params sent preview join community scadm:" + params.toString());

            RetrofitService.getInstance().PostJsonObjRequest(MyApiClient.LINK_GET_PREVIEW_COMMUNITY_SCADM, params,
                    new ObjListeners() {
                        @Override
                        public void onResponses(JSONObject response) {
                            try {
                                String code = response.getString(WebParams.ERROR_CODE);
                                Timber.d("isi response sent preview join community scadm:" + response.toString());
                                if (code.equals(WebParams.SUCCESS_CODE)) {

                                    comm_name = response.getString(WebParams.COMM_NAME);
                                    comm_code = response.getString(WebParams.COMM_CODE);
                                    comm_id_scadm = response.getString(WebParams.COMM_ID);
                                    member_code = response.getString(WebParams.MEMBER_CODE);
                                    member_name = response.getString(WebParams.MEMBER_NAME);

                                    changeToConfirm();

                                } else if (code.equals(WebParams.LOGOUT_CODE)) {
                                    Timber.d("isi response autologout:" + response.toString());
                                    String message = response.getString(WebParams.ERROR_MESSAGE);
                                    AlertDialogLogout test = AlertDialogLogout.getInstance();
                                    test.showDialoginActivity(getActivity(), message);
                                } else {
                                    Timber.d("Error isi response sent preview join community scadm:" + response.toString());
                                    code = response.getString(WebParams.ERROR_CODE) + ":" + response.getString(WebParams.ERROR_MESSAGE);

                                    Toast.makeText(getActivity(), code, Toast.LENGTH_LONG).show();
                                    getActivity().finish();
                                }

                            } catch (JSONException e) {
                                Toast.makeText(getActivity(), getString(R.string.internal_error), Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            getActivity().finish();

                        }

                        @Override
                        public void onComplete() {
                            if (progdialog.isShowing())
                                progdialog.dismiss();
                        }
                    });

        } catch (Exception e) {
            Timber.d("httpclient:" + e.getMessage());
        }
    }

    public void changeToConfirm() {
        Bundle bundle = new Bundle();
        bundle.putString(DefineValue.COMMUNITY_NAME, comm_name);
        bundle.putString(DefineValue.COMMUNITY_CODE, comm_code);
        bundle.putString(DefineValue.COMM_ID_SCADM, comm_id_scadm);
        bundle.putString(DefineValue.MEMBER_CODE, member_code);
        bundle.putString(DefineValue.MEMBER_NAME, member_name);
        Fragment mFrag = new FragJoinCommunityConfirm();
        mFrag.setArguments(bundle);

        JoinCommunitySCADMActivity ftf = (JoinCommunitySCADMActivity) getActivity();
        ftf.switchContent(mFrag, "Konfirmasi Gabung Komunitas", true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
