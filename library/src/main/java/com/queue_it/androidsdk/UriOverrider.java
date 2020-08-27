package com.queue_it.androidsdk;


import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

public class UriOverrider implements IUriOverrider {
  private Uri queue;
  private Uri target;
  private String userId;

  @Override
  public Uri getQueue() {
    return queue;
  }

  @Override
  public void setQueue(Uri queue) {
    this.queue = queue;
  }

  @Override
  public Uri getTarget() {
    return target;
  }

  @Override
  public void setTarget(Uri target) {
    this.target = target;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public boolean handleNavigationRequest(String uriString, WebView webview, UriOverrideWrapper uriOverride) {
    Log.v("QueueITEngine", "URI loading: " + uriString);
    Uri navigationUri = Uri.parse(uriString);
    boolean isWeb = navigationUri.getScheme() != null && (navigationUri.getScheme().equals("http")
            || navigationUri.getScheme().equals("https"));
    if(!isWeb){
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
      webview.getContext().startActivity(browserIntent);
      return true;
    }
    String navigationHost = navigationUri.getHost();
    String queueHost = queue.getHost();
    boolean isQueueItUrl = navigationHost != null && queueHost!=null && queueHost.contains(navigationHost);
    if (isQueueItUrl) {
      boolean needsRewrite = QueueUrlHelper.urlUpdateNeeded(uriString, userId);
      if (needsRewrite) {
        uriString = QueueUrlHelper.updateUrl(uriString, userId);
        Log.v("QueueITEngine", "URL intercepting: " + uriString);
      }
      uriOverride.onQueueUrlChange(uriString);
      if (needsRewrite) {
        webview.loadUrl(uriString);
        return true;
      }
    }
    String targetHost = target.getHost();
    boolean isTarget = navigationHost != null && targetHost!=null && targetHost.contains(navigationHost);
    if (isTarget) {
      Uri uri = Uri.parse(uriString);
      String queueItToken = uri.getQueryParameter("queueittoken");
      uriOverride.onPassed(queueItToken);
      return true;
    }
    if (!isQueueItUrl) {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
      webview.getContext().startActivity(browserIntent);
      return true;
    }
    return false;
  }
}
