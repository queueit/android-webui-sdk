package com.queue_it.androidsdk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class QueueActivity extends AppCompatActivity {

  private String queueUrl;
  private String targetUrl;
  private WebView webview;
  @SuppressLint("StaticFieldLeak")
  private static WebView previousWebView;
  private IUriOverrider uriOverrider;

  WebViewClient webviewClient = new WebViewClient() {

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      CookieSyncManager.getInstance().sync();
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
      String errorMessage;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        errorMessage = String.format("%s %s: %s %s", request.getMethod(), request.getUrl(), errorResponse.getStatusCode(), errorResponse.getReasonPhrase());
      } else {
        errorMessage = errorResponse.toString();
      }
      Log.v("QueueActivity", String.format("%s: %s", "onReceivedHttpError", errorMessage));
      super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
      String errorMessage;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        errorMessage = String.format("%s %s: %s %s", request.getMethod(), request.getUrl(), error.getErrorCode(), error.getDescription());
      } else {
        errorMessage = error.toString();
      }
      Log.v("QueueActivity", String.format("%s: %s", "onReceivedError", errorMessage));
      super.onReceivedError(view, request, error);
    }

    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
      handler.cancel();
      broadcastQueueError("SslError, code: " + error.getPrimaryError());
      disposeWebview(webview);
    }

    public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
      return uriOverrider.handleNavigationRequest(urlString, webview, new UriOverrideWrapper() {
        @Override
        void onQueueUrlChange(String uri) {
          broadcastChangedQueueUrl(uri);
        }

        @Override
        void onPassed(String queueItToken) {
          broadcastQueuePassed(queueItToken);
          disposeWebview(webview);
        }
      });
    }

  };

  private static void cleanupWebView() {
    if (previousWebView == null) return;
    previousWebView.destroy();
    previousWebView = null;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    uriOverrider = new UriOverrider();
    setContentView(R.layout.activity_queue);
    readActivityUrls(savedInstanceState);
    cleanupWebView();
    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

    FrameLayout layout = (FrameLayout) findViewById(R.id.relativeLayout);
    webview = new WebView(this);
    layout.addView(webview);
    previousWebView = webview;
    webview.getSettings().setJavaScriptEnabled(true);
    webview.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int newProgress) {
        Log.v("Progress", Integer.toString(newProgress));
        if (newProgress < 100) {
          progressBar.setVisibility(View.VISIBLE);
        } else {
          progressBar.setVisibility(View.GONE);
        }
        progressBar.setProgress(newProgress);
        super.onProgressChanged(view, newProgress);
      }
    });
    webview.setWebViewClient(webviewClient);
    Log.v("QueueITEngine", "Loading initial URL: " + queueUrl);
    webview.loadUrl(queueUrl);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("queueUrl", queueUrl);
    outState.putString("targetUrl", targetUrl);
    outState.putString("userId", uriOverrider.getUserId());
  }

  @Override
  protected void onDestroy() {
    if (isFinishing()) {
      broadcastQueueActivityClosed();
    }
    super.onDestroy();
  }

  private void readActivityUrls(Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      Bundle extras = getIntent().getExtras();
      if (extras == null) {
        queueUrl = null;
        targetUrl = null;
      } else {
        queueUrl = extras.getString("queueUrl");
        targetUrl = extras.getString("targetUrl");
        uriOverrider.setUserId(extras.getString("userId"));
      }
    } else {
      queueUrl = (String) savedInstanceState.getSerializable("queueUrl");
      targetUrl = (String) savedInstanceState.getSerializable("targetUrl");
      uriOverrider.setUserId((String) savedInstanceState.getSerializable("userId"));
    }

    uriOverrider.setTarget(Uri.parse(targetUrl));
    uriOverrider.setQueue(Uri.parse(queueUrl));
  }

  private void broadcastChangedQueueUrl(String urlString) {
    Intent intentChangedQueueUrl = new Intent("on-changed-queue-url");
    intentChangedQueueUrl.putExtra("url", urlString);
    LocalBroadcastManager.getInstance(QueueActivity.this).sendBroadcast(intentChangedQueueUrl);
  }

  private void broadcastQueuePassed(String queueItToken) {
    Intent intent = new Intent("on-queue-passed");
    intent.putExtra("queue-it-token", queueItToken);
    LocalBroadcastManager.getInstance(QueueActivity.this).sendBroadcast(intent);
  }

  private void broadcastQueueActivityClosed() {
    Intent intent = new Intent("queue-activity-closed");
    LocalBroadcastManager.getInstance(QueueActivity.this).sendBroadcast(intent);
  }

  public void broadcastUserExited() {
    Intent intent = new Intent("queue-user-exited");
    LocalBroadcastManager.getInstance(QueueActivity.this).sendBroadcast(intent);
  }

  private void broadcastQueueError(String errorMessage) {
    Intent intent = new Intent("on-queue-error");
    intent.putExtra("error-message", errorMessage);
    LocalBroadcastManager.getInstance(QueueActivity.this).sendBroadcast(intent);
  }

  private void disposeWebview(WebView webView) {
    webView.loadUrl("about:blank");
    finish();
  }
}
