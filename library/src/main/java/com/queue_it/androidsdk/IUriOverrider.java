package com.queue_it.androidsdk;

import android.net.Uri;
import android.webkit.WebView;

public interface IUriOverrider {
  Uri getQueue();

  void setQueue(Uri queue);

  Uri getTarget();

  void setTarget(Uri target);

  String getUserId();

  void setUserId(String userId);

  boolean handleNavigationRequest(String uriString, WebView webview, UriOverrideWrapper uriOverride);
}
