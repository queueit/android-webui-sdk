package com.queue_it.androidsdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueITEngine {

    private final IWaitingRoomStateBroadcaster _stateBroadcaster;
    private final String _customerId;
    private final String _eventOrAliasId;
    private final String _layoutName;
    private final String _language;
    private final QueueListener _queueListener;
    private final QueueCache _queueCache;
    private final QueueItEngineOptions _options;
    private Context _context;
    private final AtomicBoolean _requestInProgress;
    private boolean _isInQueue;

    private static final int INITIAL_WAIT_RETRY_SEC = 1;
    private static final int MAX_RETRY_SEC = 10;

    private int _deltaSec;
    private int _delayInterval = 0;

    private Handler _checkConnectionHandler;
    private int _isOnlineRetry = 0;

    public QueueITEngine(Context activityContext,
                         String customerId,
                         String eventOrAliasId,
                         QueueListener queueListener) {
        this(activityContext,
                customerId,
                eventOrAliasId,
                "",
                "",
                queueListener,
                QueueItEngineOptions.getDefault());
    }

    public QueueITEngine(Context activityContext,
                         String customerId,
                         String eventOrAliasId,
                         String layoutName,
                         String language,
                         QueueListener queueListener) {
        this(activityContext,
                customerId,
                eventOrAliasId,
                layoutName,
                language,
                queueListener,
                QueueItEngineOptions.getDefault());
    }

    public QueueITEngine(Context activityContext,
                         String customerId,
                         String eventOrAliasId,
                         String layoutName,
                         String language,
                         QueueListener queueListener,
                         QueueItEngineOptions options) {
        if (options == null) {
            options = QueueItEngineOptions.getDefault();
        }
        _requestInProgress = new AtomicBoolean(false);
        UserAgentManager.initialize(activityContext);
        if (TextUtils.isEmpty(customerId)) {
            throw new IllegalArgumentException("customerId must have a value");
        }
        if (TextUtils.isEmpty(eventOrAliasId)) {
            throw new IllegalArgumentException("eventOrAliasId must have a value");
        }
        _context = activityContext;
        _customerId = customerId;
        _eventOrAliasId = eventOrAliasId;
        _layoutName = layoutName;
        _language = language;
        _queueListener = queueListener;
        _queueCache = new QueueCache(_context, customerId, eventOrAliasId);
        _deltaSec = INITIAL_WAIT_RETRY_SEC;
        _stateBroadcaster = new WaitingRoomStateBroadcaster(_context);
        _options = options;
    }

    public void setViewDelay(int delayInterval) {
        _delayInterval = delayInterval;
    }

    public boolean isInQueue() {
        return _isInQueue;
    }

    public boolean IsRequestInProgress() {
        return _requestInProgress.get();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return true;
        }
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public void run(Context activityContext, boolean clearCache) throws QueueITException {
        if (clearCache) {
            _queueCache.clear();
        }
        run(activityContext);
    }

    public void run(Context activityContext) throws QueueITException {
        if (_requestInProgress.getAndSet(true)) {
            throw new QueueITException("Request is already in progress");
        }
        _checkConnection = new EnqueueRunner();
        _context = activityContext;
        _checkConnectionHandler = new Handler();
        _checkConnection.run();
    }

    public void runWithEnqueueToken(Context activityContext, String enqueueToken) throws QueueITException {
        runWithEnqueueToken(activityContext, enqueueToken, false);
    }

    public void runWithEnqueueToken(Context activityContext, String enqueueToken, boolean clearCache)
            throws QueueITException {
        if (_requestInProgress.getAndSet(true)) {
            throw new QueueITException("Request is already in progress");
        }
        if (clearCache) {
            _queueCache.clear();
        }
        _checkConnection = new EnqueueRunner(enqueueToken, null);
        _context = activityContext;
        _checkConnectionHandler = new Handler();
        _checkConnection.run();
    }

    public void runWithEnqueueKey(Context activityContext, String enqueueKey) throws QueueITException {
        runWithEnqueueKey(activityContext, enqueueKey, false);
    }

    public void runWithEnqueueKey(Context activityContext, String enqueueKey, boolean clearCache)
            throws QueueITException {
        if (_requestInProgress.getAndSet(true)) {
            throw new QueueITException("Request is already in progress");
        }
        if (clearCache) {
            _queueCache.clear();
        }
        _checkConnection = new EnqueueRunner(null, enqueueKey);
        _context = activityContext;
        _checkConnectionHandler = new Handler();
        _checkConnection.run();
    }

    private class EnqueueRunner implements Runnable {
        private final String _enqueueKey;
        private final String _enqueueToken;

        public EnqueueRunner(String enqueueToken, String enqueueKey) {
            _enqueueToken = enqueueToken;
            _enqueueKey = enqueueKey;
        }

        public EnqueueRunner() {
            this(null, null);
        }

        @Override
        public void run() {
            if (isOnline()) {
                runWithConnection(_enqueueToken, _enqueueKey);
                return;
            }
            _isOnlineRetry++;
            if (_isOnlineRetry > 5) {
                _queueListener.onError(Error.NO_CONNECTION, "No connection");
                return;
            }

            _checkConnectionHandler.postDelayed(this, 1000);
        }
    }

    private EnqueueRunner _checkConnection = new EnqueueRunner();

    private void runWithConnection(String enqueueToken, String enqueueKey) {
        if (!tryToShowQueueFromCache()) {
            tryEnqueue(enqueueToken, enqueueKey);
        }
        _requestInProgress.set(false);
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
            updateQueuePageUrl(url);
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

    private boolean tryToShowQueueFromCache() {
        if (_queueCache.isEmpty()) {
            return false;
        }

        Calendar cachedTime = _queueCache.getUrlTtl();
        Calendar currentTime = Calendar.getInstance();

        if (currentTime.compareTo(cachedTime) < 0) {
            String queueUrl = _queueCache.getQueueUrl();
            String targetUrl = _queueCache.getTargetUrl();
            Log.v("QueueITEngine", String.format("Using queueUrl from cache: %s", queueUrl));
            showQueueWithOptionalDelay(queueUrl, targetUrl);
            return true;
        }
        return false;
    }

    private void showQueueWithOptionalDelay(final String queueUrl, final String targetUrl) {
        raiseQueueViewWillOpen();

        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                showQueue(queueUrl, targetUrl);
            }
        };
        handler.postDelayed(r, _delayInterval);
    }

    private void showQueue(String queueUrl, final String targetUrl) {
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
        intent.putExtra("userId", getUserId());
        intent.putExtra("options", _options);
        _context.startActivity(intent);
    }

    private void raiseQueueViewWillOpen() {
        _queueListener.onQueueViewWillOpen();
        _isInQueue = true;
    }

    private void raiseUserExited() {
        _queueListener.onUserExited();
    }

    private void raiseQueuePassed(String queueItToken) {
        _queueCache.clear();
        _queueListener.onQueuePassed(new QueuePassedInfo(queueItToken));
        _isInQueue = false;
        _requestInProgress.set(false);
    }

    private void raiseQueueDisabled(String queueItToken) {
        _queueListener.onQueueDisabled(new QueueDisabledInfo(queueItToken));
        _isInQueue = false;
    }

    private void raiseWebViewClosed() {
        _queueListener.onWebViewClosed();
    }

    private void raiseOnSessionRestart() {
        _queueCache.clear();
        _queueListener.onSessionRestart(this);
    }

    private String getUserId() {
        return Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void tryEnqueue(String enqueueToken, String enqueueKey) {
        String userId = getUserId();
        String userAgent = UserAgentManager.getUserAgent();
        String sdkVersion = getSdkVersion();

        QueueServiceListener queueServiceListener = new QueueServiceListener() {
            @Override
            public void onSuccess(String queueId,
                                  String queueUrlString,
                                  int queueUrlTtlInMinutes,
                                  String eventTargetUrl,
                                  String queueItToken) {
                handleAppEnqueueResponse(queueId, queueUrlString, queueUrlTtlInMinutes, eventTargetUrl, queueItToken);
                _requestInProgress.set(false);
            }

            @Override
            public void onFailure(String errorMessage, int errorCode) {
                Log.v("QueueITEngine", String.format("Error: %s: %s", errorCode, errorMessage));
                if (errorCode >= 400 && errorCode < 500) {
                    _queueListener.onError(Error.INVALID_RESPONSE,
                            String.format("Error %s (%s)", errorCode, errorMessage));
                } else {
                    QueueITEngine.this.enqueueRetryMonitor(enqueueToken, enqueueKey);
                }
            }
        };

        QueueService queueService = new QueueService(_customerId, _eventOrAliasId, userId, userAgent, sdkVersion,
                _layoutName, _language, enqueueToken, enqueueKey, queueServiceListener);
        queueService.init(_context);
    }

    private void handleAppEnqueueResponse(String queueId, String queueUrlString, int queueUrlTtlInMinutes,
                                          String eventTargetUrl, String queueItToken) {
        if (IsSafetyNet(queueId, queueUrlString)) {
            QueueITEngine.this.raiseQueuePassed(queueItToken);
            return;
        } else if (IsDisabled(queueId, queueUrlString)) {
            QueueITEngine.this.raiseQueueDisabled(queueItToken);
            return;
        }

        showQueueWithOptionalDelay(queueUrlString, eventTargetUrl);
        _queueCache.update(queueUrlString, queueUrlTtlInMinutes, eventTargetUrl);
    }

    private boolean IsSafetyNet(String queueId, String queueUrl) {
        return !TextUtils.isEmpty(queueId) && TextUtils.isEmpty(queueUrl);
    }

    private boolean IsDisabled(String queueId, String queueUrl) {
        return TextUtils.isEmpty(queueId) && TextUtils.isEmpty(queueUrl);
    }

    private void enqueueRetryMonitor(String enqueueToken, String enqueueKey) {
        if (_deltaSec < MAX_RETRY_SEC) {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    tryEnqueue(enqueueToken, enqueueKey);
                }
            };
            handler.postDelayed(r, _deltaSec * 1000);

            _deltaSec = _deltaSec * 2;
        } else {
            _deltaSec = INITIAL_WAIT_RETRY_SEC;
            _requestInProgress.set(false);
            _queueListener.onQueueItUnavailable();
        }
    }

    private void updateQueuePageUrl(String queueUrl) {
        if (!_queueCache.isEmpty()) {
            _queueCache.update(queueUrl, _queueCache.getUrlTtl(), _queueCache.getTargetUrl());
        }
    }

    public String getSdkVersion() {
        return "Android-" + BuildConfig.VERSION_NAME;
    }
}