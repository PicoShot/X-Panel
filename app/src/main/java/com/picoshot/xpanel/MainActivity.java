package com.picoshot.xpanel;



import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private String currentVersion = "1.1.0";
    private static final String UPDATE_CHECK_URL = "https://www.picoshot.net/update.php";
    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new UpdateCheckTask().execute();

        myWebView = findViewById(R.id.myWebView);
        myWebView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);

        DeviceFingerprintGenerator generator = new DeviceFingerprintGenerator(this);
        String fingerprint = generator.generateFingerprint();

        loadPreviousUrl();
        String mainUrl = "https://panel.picoshot.net/server/kontrol.php?action=app&fp=" + fingerprint + "&name=" + Build.MANUFACTURER;
        findViewById(R.id.homeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myWebView.loadUrl(mainUrl);
                Toast.makeText(MainActivity.this, "Go Home", Toast.LENGTH_SHORT).show();
            }
        });


        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Toast.makeText(MainActivity.this, url, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        myWebView.saveState(new Bundle());
        CookieSyncManager.getInstance().sync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreWebViewState();
    }

    private void loadPreviousUrl() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String lastUrl = preferences.getString("lastUrl", null);

        if (lastUrl != null) {
            myWebView.loadUrl(lastUrl);
        } else {
            // Load default URL if no previous one
            DeviceFingerprintGenerator generator = new DeviceFingerprintGenerator(this);
            String fingerprint = generator.generateFingerprint();
            String myWebsiteURL = "https://panel.picoshot.net/server/kontrol.php?action=app&fp=" + fingerprint + "&name=" + Build.MANUFACTURER;
            myWebView.loadUrl(myWebsiteURL);
        }
    }

    private void saveUrl(String url) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastUrl", url);
        editor.apply();
    }

    private void restoreWebViewState() {
        if (myWebView.restoreState(new Bundle()) == null) {
            loadPreviousUrl();
        }
    }

    public static class DeviceFingerprintGenerator {

        private final Context context;

        public DeviceFingerprintGenerator(Context context) {
            this.context = context;
        }

        public String generateFingerprint() {
            String fingerprintData = collectDeviceData();
            return getHash(fingerprintData);
        }

        private String collectDeviceData() {
            StringBuilder builder = new StringBuilder();

            builder.append("Android:").append(Build.VERSION.RELEASE);

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            builder.append(display.getDisplayId());

            builder.append(", Model:").append(Build.MODEL);
            builder.append(", MANUFACTURER:").append(Build.MANUFACTURER);
            builder.append(", Device:").append(Build.DEVICE);
            builder.append(", BOARD:").append(Build.BOARD);
            builder.append(", HARDWARE:").append(Build.HARDWARE);
            builder.append(", BOOTLOADER:").append(Build.BOOTLOADER);
            builder.append(", DISPLAY:").append(Build.DISPLAY);
            builder.append(", FINGERPRINT:").append(Build.FINGERPRINT);
            builder.append(", ID:").append(Build.ID);
            builder.append(", HOST:").append(Build.HOST);

            return builder.toString();
        }

        private String getHash(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md.digest(input.getBytes());
                return bytesToHex(hashBytes);
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            saveUrl(url);
            CookieSyncManager.getInstance().sync();
        }
    }

    private class UpdateCheckTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("version", currentVersion)
                    .build();
            Request request = new Request.Builder()
                    .url(UPDATE_CHECK_URL)
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                if (response != null) {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean updateAvailable = jsonResponse.getBoolean("status");
                    String updateUrl = jsonResponse.getString("url");
                    String lastVersion = jsonResponse.getString("version");
                    boolean forceUpdate = jsonResponse.getBoolean("force");

                    if (updateAvailable) {
                        Toast.makeText(MainActivity.this, "Update Available! New Version:" + lastVersion, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        startActivity(intent);
                        if (forceUpdate) {
                            Toast.makeText(MainActivity.this, "Please Update app", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else
                        Toast.makeText(MainActivity.this, "App Is Updated! Version:" + lastVersion, Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this, "Failed to Check Update", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to Check Update", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

}


