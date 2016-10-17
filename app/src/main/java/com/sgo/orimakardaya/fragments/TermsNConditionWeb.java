package com.sgo.orimakardaya.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.sgo.orimakardaya.R;
import com.sgo.orimakardaya.coreclass.MyApiClient;

import timber.log.Timber;

/*
  Created by Administrator on 11/5/2014.
 */
public class TermsNConditionWeb extends Fragment {

    private WebView webview;
    ViewGroup container;
    Boolean isDisconnected;
    View v;
    ProgressBar pb_webview;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.frag_terms_condition_web, container,false);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pb_webview = (ProgressBar) v.findViewById(R.id.loading_bar_web);

        isDisconnected = !isOnline();
        String url = MyApiClient.URL_TERMS;
        Timber.e(url);
        loadUrl(url);
    }


    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    @SuppressWarnings("deprecation")
    public void loadUrl(String url) {
        webview = (WebView) v.findViewById(R.id.webview);
//        webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (android.os.Build.VERSION.SDK_INT<=11) {
            webSettings.setAppCacheMaxSize(1024 * 1024 * 8);
        }


        //final Activity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                //setSupportProgress(progress * 100);
//               getProgressSpinner().setVisibility(View.VISIBLE);
                //activity.setProgress(progress * 100);
        /*if(progress == 100)
            getProgressSpinner().setVisibility(View.GONE);*/

                //pb_webview.setVisibility(View.VISIBLE);
                pb_webview.setIndeterminate(true);
                if (progress == 100) {
                    webview.setVisibility(View.VISIBLE);
                    pb_webview.setVisibility(View.GONE);
                }
            }
        });
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                webview.loadUrl("javascript:(function () {document.getElementsByTagName('body')[0].style.marginBottom = '0'})()");
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Timber.d("isi url tombol-tombolnya:" + url);
                //view.loadUrl(url);

                return true;
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                isDisconnected = true;
            }


            @Override
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                super.onReceivedClientCertRequest(view, request);
            }
        });


        webview.loadUrl(url);
    }

}