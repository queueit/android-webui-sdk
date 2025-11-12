package com.queue_it.androidsdk;

import android.content.Context;
import android.webkit.WebView;

public class UserAgentManager {
    public static final String SDKVersion = com.queue_it.androidsdk.BuildConfig.LIBRARY_PACKAGE_NAME + "@" + BuildConfig.VERSION_NAME;
    private static String DeviceUserAgent;

    private UserAgentManager() { }

    public static void initialize(Context context) {
        DeviceUserAgent = new WebView(context).getSettings().getUserAgentString();
    }

    public static String getUserAgent() {
        return DeviceUserAgent + " (sdk: " + SDKVersion + ")";
    }
}
