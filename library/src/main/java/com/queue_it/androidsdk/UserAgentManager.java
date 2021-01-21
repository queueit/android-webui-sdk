package com.queue_it.androidsdk;

import android.app.Activity;
import android.webkit.WebView;

public class UserAgentManager {
    public static final String SDKVersion = BuildConfig.LIBRARY_PACKAGE_NAME + "@" + BuildConfig.VERSION_NAME;
    private static String DeviceUserAgent;

    private UserAgentManager(){
    }

    public static void initialize(Activity context) {
        DeviceUserAgent = new WebView(context).getSettings().getUserAgentString();
    }

    public static String getUserAgent() {
        return DeviceUserAgent + " (sdk: " + SDKVersion + ")";
    }
}
