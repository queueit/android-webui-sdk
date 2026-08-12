package com.queue_it.androidsdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class QueueActivityBase {
    private final Activity _context;
    private String queueUrl;
    private String targetUrl;
    private WebView webview;
    private String webViewUserAgent;
    private String waitingRoomDomain;
    private String queuePathPrefix;
    @SuppressLint("StaticFieldLeak")
    private static WebView previousWebView;
    private UriOverrider uriOverrider;
    private final WaitingRoomStateBroadcaster broadcaster;
    private QueueItEngineOptions options;

    private static final long RELOAD_BASE_DELAY_MS = 1000;
    private static final long RELOAD_MAX_DELAY_MS = 30000;
    // Stop retrying after the backoff reaches its cap (delays 1,2,4,8,16,30s).
    private static final int MAX_RELOAD_ATTEMPTS = 6;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private int reloadAttempts = 0;
    private boolean currentLoadErrored = false;

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
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            currentLoadErrored = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            CookieSyncManager.getInstance().sync();
            if (!currentLoadErrored) {
                // Successful load: stop any pending reconnect attempts.
                reloadAttempts = 0;
                stopReconnect();
            }
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

            // A main-frame load failure (e.g. the cold-network reload right after the
            // OS restarted the process) would otherwise leave the user stuck on the
            // Chromium error page with no recovery. Retry with backoff and reload as
            // soon as connectivity returns.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request.isForMainFrame()) {
                currentLoadErrored = true;
                if (reloadAttempts >= MAX_RELOAD_ATTEMPTS) {
                    giveUpReloading();
                } else {
                    scheduleQueueReload();
                }
            }
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
                    // Persist the token durably before disposing the WebView, so the
                    // pass survives the app process being killed while backgrounded.
                    // Recovered on the next resume via QueueITEngine.consumePendingPass().
                    PendingPassStore.save(_context, queueItToken);
                    broadcaster.broadcastQueuePassed();
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
        readActivityExtras(savedInstanceState);
        cleanupWebView();

        if (queueUrl == null || targetUrl == null) {
            broadcaster.broadcastQueueError("Failed to load the queue. Queue Url or Target Url are missing from the running Activity. " +
                    "Please, check the error logs for more details.");
            _context.finish();
            return;
        }

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
        setUserAgent(webViewUserAgent);
        webview.loadUrl(queueUrl);
    }

    public void saveInstanceState(Bundle outState) {
        outState.putString("queueUrl", queueUrl);
        outState.putString("targetUrl", targetUrl);
        outState.putString("webViewUserAgent", webViewUserAgent);
        outState.putString("userId", uriOverrider.getUserId());
        outState.putString("waitingRoomDomain", waitingRoomDomain);
        outState.putString("queuePathPrefix", queuePathPrefix);

        Log.i("QueueITEngine", "Saving instance state:");
        Log.i("QueueITEngine", "queueUrl: " + queueUrl);
        Log.i("QueueITEngine", "targetUrl: " + targetUrl);
        Log.i("QueueITEngine", "webViewUserAgent: " + webViewUserAgent);
        Log.i("QueueITEngine", "userId: " + uriOverrider.getUserId());
    }

    public void destroy() {
        stopReconnect();
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
                webViewUserAgent = null;
            } else {
                queueUrl = extras.getString("queueUrl");
                targetUrl = extras.getString("targetUrl");
                webViewUserAgent = extras.getString("webViewUserAgent");
                uriOverrider.setUserId(extras.getString("userId"));
                options = (QueueItEngineOptions)extras.getParcelable("options");
                waitingRoomDomain = extras.getString("waitingRoomDomain");
                queuePathPrefix = extras.getString("queuePathPrefix");
            }
        } else {
            queueUrl = (String) savedInstanceState.getSerializable("queueUrl");
            targetUrl = (String) savedInstanceState.getSerializable("targetUrl");
            webViewUserAgent = (String) savedInstanceState.getSerializable("webViewUserAgent");
            uriOverrider.setUserId((String) savedInstanceState.getSerializable("userId"));
            waitingRoomDomain = (String) savedInstanceState.getSerializable("waitingRoomDomain");
            queuePathPrefix = (String) savedInstanceState.getSerializable("queuePathPrefix");
        }

        if (targetUrl != null) {
            uriOverrider.setTarget(Uri.parse(targetUrl));
        } else {
            Log.e("QueueITEngine", "targetUrl is null, cannot set target Uri");
        }

        if (queueUrl != null) {
            uriOverrider.setQueue(Uri.parse(queueUrl));
        } else {
            Log.e("QueueITEngine", "queueUrl is null, cannot set queue Uri");
        }

        uriOverrider.setWaitingRoomDomain(waitingRoomDomain);
        uriOverrider.setQueuePathPrefix(queuePathPrefix);
    }

    private final Runnable reloadRunnable = new Runnable() {
        @Override
        public void run() {
            mainHandler.removeCallbacks(this);
            if (_context.isFinishing()) {
                return;
            }
            if (webview != null && queueUrl != null) {
                Log.v("QueueITEngine", "Retrying queue reload (attempt " + reloadAttempts + "): " + queueUrl);
                webview.loadUrl(queueUrl);
            }
        }
    };

    private void scheduleQueueReload() {
        registerNetworkCallbackIfNeeded();
        long delay = Math.min(RELOAD_MAX_DELAY_MS,
                RELOAD_BASE_DELAY_MS * (1L << Math.min(reloadAttempts, 5)));
        reloadAttempts++;
        mainHandler.removeCallbacks(reloadRunnable);
        mainHandler.postDelayed(reloadRunnable, delay);
    }

    private void registerNetworkCallbackIfNeeded() {
        if (networkCallback != null) {
            return;
        }
        connectivityManager = (ConnectivityManager) _context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Connectivity is back: reload immediately instead of waiting for the backoff timer.
                mainHandler.post(reloadRunnable);
            }
        };
        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        } catch (RuntimeException e) {
            // Missing ACCESS_NETWORK_STATE or too many callbacks: fall back to the backoff timer.
            networkCallback = null;
        }
    }

    private void giveUpReloading() {
        // Stop retrying after the cap so a permanently-failing load does not
        // drain the battery. The WebView stays on its error page.
        Log.v("QueueITEngine", "Giving up queue reload after " + reloadAttempts + " attempts");
        stopReconnect();
    }

    private void stopReconnect() {
        mainHandler.removeCallbacks(reloadRunnable);
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (RuntimeException ignored) {
            }
        }
        networkCallback = null;
    }

    private void disposeWebview(WebView webView) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadUrl("about:blank");
        _context.finish();
    }

    private void setUserAgent(String userAgent) {
        userAgent = (userAgent != null) ? userAgent : UserAgentManager.getUserAgent();
        System.setProperty("http.agent", userAgent);
        webview.getSettings().setUserAgentString(userAgent);
    }
}
