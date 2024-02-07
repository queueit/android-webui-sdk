package com.queue_it.androidsdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

public class QueueITWaitingRoomView {
    private final IWaitingRoomStateBroadcaster _stateBroadcaster;
    private final QueueListener _queueListener;
    private final QueueItEngineOptions _options;
    private Context _context;

    private int _delayInterval = 0;

    public QueueITWaitingRoomView(Context activityContext,
                                  QueueListener queueListener) {
        this(activityContext,
                queueListener,
                QueueItEngineOptions.getDefault());
    }

    public QueueITWaitingRoomView(Context activityContext,
                                  QueueListener queueListener,
                                  QueueItEngineOptions options) {
        if (options == null) {
            options = QueueItEngineOptions.getDefault();
        }
        UserAgentManager.initialize(activityContext);
        _context = activityContext;
        _queueListener = queueListener;
        _stateBroadcaster = new WaitingRoomStateBroadcaster(_context);
        _options = options;
    }

    public void showQueue(final QueueTryPassResult queueTryPassResult, String webViewUserAgent) {
        if(queueTryPassResult == null){
            Log.e("QueueITWaitingRoomView", "queuePassedInfo parameter is empty");
            return;
        }
        raiseQueueViewWillOpen();

        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                showQueuePage(queueTryPassResult.getQueueUrl(), queueTryPassResult.getTargetUrl(), webViewUserAgent);
            }
        };
        handler.postDelayed(r, _delayInterval);
    }


    public void setViewDelay(int delayInterval) {
        _delayInterval = delayInterval;
    }


    private void showQueuePage(String queueUrl, final String targetUrl, String webViewUserAgent) {
        _stateBroadcaster.registerReceivers(_queuePassedBroadcastReceiver,
                _queueUrlChangedBroadcastReceiver,
                _queueActivityClosedBroadcastReceiver,
                _queueUserExitedBroadcastReceiver,
                _queueErrorBroadcastReceiver,
                _webViewClosedBroadcastReceiver,
                _webViewOnSessionRestartReceiver);

        Intent intent = new Intent(_context, QueueActivity.class);
        intent.putExtra("queueUrl", queueUrl);
        intent.putExtra("targetUrl", targetUrl);
        intent.putExtra("webViewUserAgent", webViewUserAgent);
        intent.putExtra("userId", getUserId());
        intent.putExtra("options", _options);
        _context.startActivity(intent);
    }

    private void raiseQueueViewWillOpen() {
        _queueListener.onQueueViewWillOpen();
    }

    private void raiseUserExited() {
        _queueListener.onUserExited();
    }

    private void raiseQueuePassed(String queueItToken) {
        _queueListener.onQueuePassed(new QueuePassedInfo(queueItToken));
    }

    private void raiseWebViewClosed() {
        _queueListener.onWebViewClosed();
    }

    private void raiseOnSessionRestart() {
        _queueListener.onSessionRestart(null);
    }

    private void raiseQueueUrlChanged(String url) {
        _queueListener.onQueueUrlChanged(url);
    }


    private final BroadcastReceiver _queuePassedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            raiseQueuePassed(intent.getStringExtra("queue-it-token"));
        }
    };

    private final BroadcastReceiver _queueErrorBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            _queueListener.onError(Error.SSL_ERROR, intent.getStringExtra("error-message"));
        }
    };

    private final BroadcastReceiver _queueUrlChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String url = intent.getExtras().getString("url");
            raiseQueueUrlChanged(url);
        }
    };

    private final BroadcastReceiver _queueUserExitedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            raiseUserExited();
        }
    };

    private final BroadcastReceiver _webViewClosedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            raiseWebViewClosed();
        }
    };

    private final BroadcastReceiver _webViewOnSessionRestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            raiseOnSessionRestart();
        }
    };

    private final BroadcastReceiver _queueActivityClosedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            _stateBroadcaster.unregisterReceivers(_queuePassedBroadcastReceiver, _queueUrlChangedBroadcastReceiver,
                    _queueActivityClosedBroadcastReceiver, _queueUserExitedBroadcastReceiver,
                    _queueErrorBroadcastReceiver, _webViewClosedBroadcastReceiver, _webViewOnSessionRestartReceiver);
        }
    };


    private String getUserId() {
        return Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
