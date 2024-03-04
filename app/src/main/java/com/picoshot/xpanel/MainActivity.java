package com.picoshot.xpanel;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DeviceFingerprintGenerator generator = new DeviceFingerprintGenerator(this);
        String fingerprint = generator.generateFingerprint();

        myWebView = findViewById(R.id.myWebView);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // String myWebsiteURL = "https://panel.picoshot.net";
        String myWebsiteURL = "https://picoshot.net/headers.php";

        Map<String, String> headers = new HashMap<>();
        headers.put("information", "true");
        headers.put("fingerprint", fingerprint);

        myWebView.loadUrl(myWebsiteURL, headers);
    }

    public class DeviceFingerprintGenerator {

        private Context context;

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
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);

            builder.append(",").append(Build.MODEL);

            builder.append(",").append(Build.MANUFACTURER);
            builder.append(",").append(Build.DEVICE);

            return builder.toString();
        }

        private String getHash(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
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

}