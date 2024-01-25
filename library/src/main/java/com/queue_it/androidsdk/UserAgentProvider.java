package com.queue_it.androidsdk;

/**
 * Allow to change the user agent used by the WebView.
 * Implement this interface on the Application class.
 */
public interface UserAgentProvider {
    String getUserAgent();
}
