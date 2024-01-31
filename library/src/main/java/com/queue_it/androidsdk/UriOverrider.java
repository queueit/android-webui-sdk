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

    private boolean isBlockedUri(Uri uri) {
        String path = uri.getPath();
        return path.startsWith("/what-is-this.html");
    }

    private boolean isTargetUri(Uri destinationUri) {
        String destinationHost = destinationUri.getHost();
        String destinationPath = destinationUri.getPath();

        String targetHost = target.getHost();
        String targetPath = target.getPath();

        return destinationHost.equalsIgnoreCase(targetHost)
                && destinationPath.equals(targetPath);
    }

    private boolean isQueueItUri(Uri uri) {
        return uri.getScheme().equals("queueit");
    }

    private boolean isCloseLink(Uri uri) {
        if (!isQueueItUri(uri)) {
            return false;
        }
        return uri.getHost().equals("close");
    }

    private boolean isSessionRestartLink(Uri uri) {
        if (!isQueueItUri(uri)) {
            return false;
        }
        return uri.getHost().equals("restartSession");
    }

    private boolean handleDeepLink(WebView webview, Uri destinationUri, UriOverrideWrapper uriOverride) {
        if (isCloseLink(destinationUri)) {
            uriOverride.onCloseClicked();
            return true;
        } else if (isSessionRestartLink(destinationUri)) {
            uriOverride.onSessionRestart();
            return true;
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, destinationUri);
        webview.getContext().startActivity(browserIntent);
        return true;
    }

    @Override
    public boolean handleNavigationRequest(final String destinationUrlStr, WebView webview, UriOverrideWrapper uriOverride) {
        Log.v("QueueITEngine", "URI loading: " + destinationUrlStr);
        Uri destinationUri = Uri.parse(destinationUrlStr);
        boolean isWeb = destinationUri.getScheme() != null && (destinationUri.getScheme().equals("http")
                || destinationUri.getScheme().equals("https"));
        if (!isWeb) {
            return handleDeepLink(webview, destinationUri, uriOverride);
        }
        if (isBlockedUri(destinationUri)) {
            return true;
        }

        String navigationHost = destinationUri.getHost();
        String queueHost = queue.getHost();
        boolean isQueueItUrl = navigationHost != null && queueHost != null && queueHost.equals(navigationHost);
        if (isQueueItUrl) {
            boolean needsRewrite = QueueUrlHelper.urlUpdateNeeded(destinationUri, userId);
            if (needsRewrite) {
                destinationUri = QueueUrlHelper.updateUrl(destinationUri, userId);
                Log.v("QueueITEngine", "URL intercepting: " + destinationUri);
            }
            uriOverride.onQueueUrlChange(destinationUri.toString());
            if (needsRewrite) {
                webview.loadUrl(destinationUri.toString());
                return true;
            }
        }

        if (isTargetUri(destinationUri)) {
            String queueItToken = destinationUri.getQueryParameter("queueittoken");
            uriOverride.onPassed(queueItToken);
            return true;
        }
        if (!isQueueItUrl) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, destinationUri);
            webview.getContext().startActivity(browserIntent);
            return true;
        }
        return false;
    }
}
