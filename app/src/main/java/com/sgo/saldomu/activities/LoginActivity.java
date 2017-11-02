package com.sgo.saldomu.activities;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;

import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.R;
import com.sgo.saldomu.coreclass.BaseActivity;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.coreclass.InetHandler;
import com.sgo.saldomu.coreclass.MyApiClient;
import com.sgo.saldomu.fragments.Login;
import com.sgo.saldomu.loader.UtilsLoader;

import timber.log.Timber;

/*
  Created by Administrator on 11/4/2014.
 */
public class LoginActivity extends BaseActivity {

    private static final int REQUEST_EXIT = 0 ;
    public static final int RESULT_PIN = 1 ;
    public static final int RESULT_NORMAL = 2 ;
    public static final int RESULT_FINISHING = 5 ;
    public static final int ACTIVITY_RESULT = 3;

    private FragmentManager fragmentManager;
    private Bundle bundleNextLogin = new Bundle();
    private SecurePreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(InetHandler.isNetworkAvailable(this))
            new UtilsLoader(this).getAppVersion();

        if (findViewById(R.id.loginContent) != null) {
            if (savedInstanceState != null) {
                return;
            }

            bundleNextLogin = getIntent().getExtras();

            sp = CustomSecurePref.getInstance().getmSecurePrefs();
            String flagLogin = sp.getString(DefineValue.FLAG_LOGIN, DefineValue.STRING_NO);
            if ( flagLogin == null )
                flagLogin = DefineValue.STRING_NO;

            if ( flagLogin.equals(DefineValue.STRING_YES) ) {
                Intent i = new Intent(this,MainPage.class);
                startActivity(i);
                finish();
            }


            Login login = new Login();
            fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if ( bundleNextLogin != null ) {
                login.setArguments(bundleNextLogin);
            }
            fragmentTransaction.add(R.id.loginContent, login,"login");
            fragmentTransaction.commit();
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_login;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == LoginActivity.ACTIVITY_RESULT){
            if(resultCode == LoginActivity.RESULT_FINISHING)
                this.finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void switchContent(Fragment mFragment,String fragName,Boolean isBackstack) {

        if(isBackstack){
            Timber.d("backstack");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.loginContent, mFragment, fragName)
                    .addToBackStack(null)
                    .commit();
        }
        else {
            Timber.d("bukan backstack");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.loginContent, mFragment, fragName)
                    .commit();

        }
    }

    public void switchActivity(Intent mIntent) {
                startActivityForResult(mIntent, REQUEST_EXIT);
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().findFragmentByTag("reg2") == null)
            super.onBackPressed();
    }


    public void togglerBroadcastReceiver(Boolean _on, BroadcastReceiver _myreceiver){

        if(_on){
            Timber.wtf("masuk turnOnBR");
            IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            filter.addCategory("android.intent.category.DEFAULT");
            registerReceiver(_myreceiver,filter);
        }
        else {
            Timber.wtf("masuk turnOffBR");
            unregisterReceiver(_myreceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApiClient.CancelRequestWS(this, true);
    }
}