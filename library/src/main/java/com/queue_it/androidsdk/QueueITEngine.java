package com.queue_it.androidsdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import java.util.Calendar;

public class QueueITEngine {

    private String _customerId;
    private String _eventOrAliasId;
    private String _layoutName;
    private String _language;
    private QueueListener _queueListener;
    private QueueCache _queueCache;
    private Activity _activity;

    private boolean _requestInProgress;
    private boolean _isInQueue;

    private static final int INITIAL_WAIT_RETRY_SEC = 1;
    private static final int MAX_RETRY_SEC = 10;

    private int _deltaSec;
    private int _delayInterval = 0;

    private Handler _checkConnectionHandler;
    private int _isOnlineRetry = 0;

    public QueueITEngine(Activity activity, String customerId, String eventOrAliasId, QueueListener queueListener)
    {
        this(activity, customerId, eventOrAliasId, "", "", queueListener);
    }

    public QueueITEngine(Activity activity, String customerId, String eventOrAliasId, String layoutName,
                         String language, QueueListener queueListener)
    {
        if (TextUtils.isEmpty(customerId))
        {
            throw new IllegalArgumentException("customerId must have a value");
        }
        if (TextUtils.isEmpty(eventOrAliasId))
        {
            throw new IllegalArgumentException("eventOrAliasId must have a value");
        }
        _activity = activity;
        _customerId = customerId;
        _eventOrAliasId = eventOrAliasId;
        _layoutName = layoutName;
        _language = language;
        _queueListener = queueListener;
        _queueCache = new QueueCache(activity, customerId, eventOrAliasId);
        _deltaSec = INITIAL_WAIT_RETRY_SEC;
    }

    public void setViewDelay(int delayInterval)
    {
        _delayInterval = delayInterval;
    }

    public boolean isInQueue()
    {
        return _isInQueue;
    }

    public boolean IsRequestInProgress()
    {
        return _requestInProgress;
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) _activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public void run(boolean clearCache) throws QueueITException
    {
        if (clearCache)
        {
            _queueCache.clear();
        }
        run();
    }

    public void run() throws QueueITException
    {
        registerReceivers();

        if (_requestInProgress)
        {
            throw new QueueITException("Request is already in progress");
        }

        _checkConnectionHandler = new Handler();
        _checkConnection.run();
    }

    private Runnable _checkConnection = new Runnable() {
        public void run() {
            if (isOnline())
            {
                runWithConnection();
                return;
            }
            _isOnlineRetry++;
            if (_isOnlineRetry > 5) {
                _queueListener.onError(Error.NO_CONNECTION, "No connection");
                return;
            }

            _checkConnectionHandler.postDelayed(this, 1000);
        }
    };

    private void runWithConnection()
    {
        _requestInProgress = true;

        if (!tryToShowQueueFromCache())
        {
            tryEnqueue();
        }
    }

    private void registerReceivers()
    {
        LocalBroadcastManager.getInstance(_activity).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                raiseQueuePassed(intent.getStringExtra("queue-it-token"));
            }}, new IntentFilter("on-queue-passed"));

        LocalBroadcastManager.getInstance(_activity).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String url = intent.getExtras().getString("url");
                updateQueuePageUrl(url);
            }}, new IntentFilter("on-changed-queue-url"));
    }

    private boolean tryToShowQueueFromCache()
    {
        if (_queueCache.isEmpty()) {
            return false;
        }

        Calendar cachedTime = _queueCache.getUrlTtl();
        Calendar currentTime = Calendar.getInstance();

        if (currentTime.compareTo(cachedTime) == -1)
        {
            String queueUrl = _queueCache.getQueueUrl();
            String targetUrl = _queueCache.getTargetUrl();
            Log.v("QueueITEngine", String.format("Using queueUrl from cache: %s", queueUrl));
            showQueueWithOptionalDelay(queueUrl, targetUrl);
            return true;
        }
        return false;
    }

    private void showQueueWithOptionalDelay(final String queueUrl, final String targetUrl)
    {
        raiseQueueViewWillOpen();

        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                showQueue(queueUrl, targetUrl);
            }
        };
        handler.postDelayed(r, _delayInterval);
    }

    private void showQueue(String queueUrl, final String targetUrl)
    {
        Intent intent = new Intent(_activity, QueueActivity.class);
        intent.putExtra("queueUrl", queueUrl);
        intent.putExtra("targetUrl", targetUrl);
        _activity.startActivity(intent);
    }

    private void raiseQueueViewWillOpen()
    {
        _queueListener.onQueueViewWillOpen();
        _isInQueue = true;
    }

    private void raiseQueuePassed(String queueItToken)
    {
        _queueCache.clear();

        _queueListener.onQueuePassed(new QueuePassedInfo(queueItToken));
        _isInQueue = false;
        _requestInProgress = false;
    }

    private void raiseQueueDisabled()
    {
        _queueListener.onQueueDisabled();
    }

    private void tryEnqueue()
    {
        String userId = Settings.Secure.getString(_activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        String userAgent = new WebView(_activity).getSettings().getUserAgentString();
        String sdkVersion = getSdkVersion();

        QueueServiceListener queueServiceListener = new QueueServiceListener() {
            @Override
            public void onSuccess(String queueId, String queueUrlString, int queueUrlTtlInMinutes, String eventTargetUrl) {
                if (IsSafetyNet(queueId, queueUrlString))
                {
                    QueueITEngine.this.raiseQueuePassed("");
                }
                else if (IsInQueue(queueId, queueUrlString))
                {
                    showQueueWithOptionalDelay(queueUrlString, eventTargetUrl);

                    Calendar queueUrlTtl = Calendar.getInstance();
                    queueUrlTtl.add(Calendar.MINUTE, queueUrlTtlInMinutes);

                    _queueCache.update(queueUrlString, queueUrlTtl, eventTargetUrl);
                }
                else if (IsIdle(queueId, queueUrlString))
                {
                    showQueueWithOptionalDelay(queueUrlString, eventTargetUrl);
                }
                else
                {
                    _requestInProgress = false;
                    QueueITEngine.this.raiseQueueDisabled();
                }
            }

            @Override
            public void onFailure(String errorMessage, int errorCode) {
                Log.v("QueueITEngine", String.format("Error: %s: %s", errorCode, errorMessage));
                if (errorCode >= 400 && errorCode < 500) {
                    _queueListener.onError(Error.INVALID_RESPONSE, String.format("Error %s (%s)", errorCode, errorMessage));
                }
                else
                {
                    QueueITEngine.this.enqueueRetryMonitor();
                }
            }
        };

        QueueService queueService = new QueueService(_customerId, _eventOrAliasId, userId,
                userAgent, sdkVersion, _layoutName, _language, queueServiceListener);
        queueService.init(_activity);
    }

    private boolean IsSafetyNet(String queueId, String queueUrlString) {
        return !TextUtils.isEmpty(queueId) && TextUtils.isEmpty(queueUrlString);
    }

    private boolean IsInQueue(String queueId, String queueUrlString) {
        return !TextUtils.isEmpty(queueId) && !TextUtils.isEmpty(queueUrlString);
    }

    private boolean IsIdle(String queueId, String queueUrlString) {
        return TextUtils.isEmpty(queueId) && !TextUtils.isEmpty(queueUrlString);
    }

    private void enqueueRetryMonitor()
    {
        if (_deltaSec < MAX_RETRY_SEC)
        {
            Handler handler = new Handler();
            Runnable r = new Runnable() {
                public void run() {
                    tryEnqueue();
                }
            };
            handler.postDelayed(r, _deltaSec * 1000);

            _deltaSec = _deltaSec * 2;
        }
        else
        {
            _deltaSec = INITIAL_WAIT_RETRY_SEC;
            _requestInProgress = false;
            _queueListener.onQueueItUnavailable();
        }
    }

    private void updateQueuePageUrl(String queueUrl)
    {
        if (!_queueCache.isEmpty())
        {
            _queueCache.update(queueUrl, _queueCache.getUrlTtl(), _queueCache.getTargetUrl());
        }
    }

    private String getSdkVersion()
    {
        return "Android-" + BuildConfig.VERSION_NAME;
    }
}