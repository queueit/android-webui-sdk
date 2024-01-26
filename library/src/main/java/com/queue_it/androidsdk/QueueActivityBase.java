package com.queue_it.androidsdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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

public class QueueActivityBase {
    private final Activity _context;
    private String queueUrl;
    private String targetUrl;
    private WebView webview;
    @SuppressLint("StaticFieldLeak")
    private static WebView previousWebView;
    private IUriOverrider uriOverrider;
    private final IWaitingRoomStateBroadcaster broadcaster;
    private QueueItEngineOptions options;

    public QueueActivityBase(Activity context) {
        _context = context;
        options = QueueItEngineOptions.getDefault();
        broadcaster = new WaitingRoomStateBroadcaster(_context);
    }

    public QueueItEngineOptions getOptions(){
        return options;
    }

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
            broadcaster.broadcastQueueError("SslError, code: " + error.getPrimaryError());
            disposeWebview(webview);
        }

        public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
            return uriOverrider.handleNavigationRequest(urlString, webview, new UriOverrideWrapper() {

                @Override
                protected void onQueueUrlChange(String uri) {
                    broadcaster.broadcastChangedQueueUrl(uri);
                }

                @Override
                protected void onPassed(String queueItToken) {
                    broadcaster.broadcastQueuePassed(queueItToken);
                    disposeWebview(webview);
                }

                @Override
                protected void onCloseClicked() {
                    broadcaster.broadcastWebViewClosed();
                    disposeWebview(webview);
                }

                @Override
                protected void onSessionRestart() {
                    broadcaster.broadcastOnSessionRestart();
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

    //was onCreated
    public void initialize(Bundle savedInstanceState) {
        uriOverrider = new UriOverrider();
        _context.setContentView(R.layout.activity_queue);
        _context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        readActivityExtras(savedInstanceState);
        cleanupWebView();
        final ProgressBar progressBar = _context.findViewById(R.id.progressBar);

        FrameLayout layout = _context.findViewById(R.id.relativeLayout);
        webview = new WebView(_context);
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
        setUserAgent(UserAgentManager.getUserAgent());
        webview.loadUrl(queueUrl);
    }

    public void saveInstanceState(Bundle outState) {
        outState.putString("queueUrl", queueUrl);
        outState.putString("targetUrl", targetUrl);
        outState.putString("userId", uriOverrider.getUserId());
    }

    public void destroy() {
        if (_context.isFinishing()) {
            broadcaster.broadcastQueueActivityClosed();
        }
    }

    private void readActivityExtras(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Bundle extras = _context.getIntent().getExtras();
            if (extras == null) {
                queueUrl = null;
                targetUrl = null;
            } else {
                queueUrl = extras.getString("queueUrl");
                targetUrl = extras.getString("targetUrl");
                uriOverrider.setUserId(extras.getString("userId"));
                options = (QueueItEngineOptions)extras.getParcelable("options");
            }
        } else {
            queueUrl = (String) savedInstanceState.getSerializable("queueUrl");
            targetUrl = (String) savedInstanceState.getSerializable("targetUrl");
            uriOverrider.setUserId((String) savedInstanceState.getSerializable("userId"));
        }

        uriOverrider.setTarget(Uri.parse(targetUrl));
        uriOverrider.setQueue(Uri.parse(queueUrl));
    }

    private void disposeWebview(WebView webView) {
        webView.loadUrl("about:blank");
        _context.finish();
    }

    private void setUserAgent(String userAgent) {
        System.setProperty("http.agent", userAgent);
        webview.getSettings().setUserAgentString(userAgent);
    }
}
