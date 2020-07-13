package com.queue_it.androidsdk;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

public class QueueActivity extends AppCompatActivity {

    private String queueUrl;
    private String targetUrl;
    private String userId;
    private WebView webview;
    private URL target;
    private URL queue;
    private static WebView previousWebView;

    WebViewClient webviewClient = new WebViewClient() {

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
            Log.v("QueueITEngine", "URL loading: " + urlString);
            URL url;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            boolean isQueueItUrl = queue.getHost().contains(url.getHost());
            if (isQueueItUrl) {
                boolean needsRewrite = QueueUrlHelper.urlUpdateNeeded(urlString, userId);
                if (needsRewrite) {
                    urlString = QueueUrlHelper.updateUrl(urlString, userId);
                    Log.v("QueueITEngine", "URL intercepting: " + urlString);
                }
                if(isLeaveRequest(urlString)){
                    broadcastQueueLeft();
                }
                if(needsRewrite){
                    webview.loadUrl(urlString);
                    return true;
                }
            }
            boolean isTarget = target.getHost().contains(url.getHost());
            if (isTarget) {
                Uri uri = Uri.parse(urlString);
                String queueItToken = uri.getQueryParameter("queueittoken");
                broadcastQueuePassed(queueItToken);
                disposeWebview(webview);
                return true;
            }
            if (!isQueueItUrl) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                startActivity(browserIntent);
                return true;
            }
            return false;
        }
    };

    private boolean isLeaveRequest(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        return url.getPath().equals("/exitline.aspx");
    }

    private static void cleanupWebViews(){
        if(previousWebView==null) return;
        previousWebView.destroy();
        previousWebView = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        readActivityUrls(savedInstanceState);
        cleanupWebViews();
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
        outState.putString("userId", userId);
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
                userId = extras.getString("userId");
            }
        } else {
            queueUrl = (String) savedInstanceState.getSerializable("queueUrl");
            targetUrl = (String) savedInstanceState.getSerializable("targetUrl");
            userId = (String) savedInstanceState.getSerializable("userId");
        }

        try {
            target = new URL(targetUrl);
            queue = new URL(queueUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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

    private void broadcastQueueLeft(){
        Intent intent = new Intent("on-queue-left");
        LocalBroadcastManager.getInstance(QueueActivity.this).sendBroadcast(intent);
    }

    private void broadcastQueueActivityClosed() {
        Intent intent = new Intent("queue-activity-closed");
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
