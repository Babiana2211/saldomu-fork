package com.sgo.saldomu.coreclass;/*
  Created by Administrator on 3/30/2015.
 */

import android.content.Context;

import com.sgo.saldomu.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import javax.net.ssl.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static com.sgo.saldomu.coreclass.MyApiClient.PRIVATE_KEY;

public class MyPicasso {

    private static Picasso sPicasso;

    private MyPicasso() {
    }

    public static Picasso getImageLoader(Context context) {
        if (sPicasso == null) {
            Picasso.Builder builder = new Picasso.Builder(context);
            OkHttpClient okHttpClient = new OkHttpClient();

            SSLContext sslContext;
            try {
                KeyStore keyStore  = KeyStore.getInstance("BKS");
                InputStream is = context.getResources().openRawResource(R.raw.mobile_espay_id);
                keyStore.load(is, PRIVATE_KEY.toCharArray());
                is.close();
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
                keyManagerFactory.init(keyStore, PRIVATE_KEY.toCharArray());
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{}, null);
                okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
                OkHttpDownloader okHttpDownloader = new OkHttpDownloader(okHttpClient);
                builder.downloader(okHttpDownloader);
                builder.loggingEnabled(true);
                sPicasso = builder.build();
                sPicasso.setLoggingEnabled(true);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failure initializing default SSL context", e);
            } catch (KeyManagementException e) {
                throw new IllegalStateException("Failure initializing default SSL context", e);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sPicasso;
    }

    public static Picasso getUnsafeImageLoader(Context context) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            if (sPicasso == null) {
                Picasso.Builder builder = new Picasso.Builder(context);
                builder.downloader(new OkHttpDownloader(okHttpClient));
                sPicasso = builder.build();
            }
            return sPicasso;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
