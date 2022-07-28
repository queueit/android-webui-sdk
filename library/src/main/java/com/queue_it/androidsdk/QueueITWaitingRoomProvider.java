package com.queue_it.androidsdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueITWaitingRoomProvider {

    private final String _customerId;
    private final String _eventOrAliasId;
    private final String _layoutName;
    private final String _language;
    private final QueueITWaitingRoomProviderListener _queueITWaitingRoomProviderListener;
    private Context _context;
    private final AtomicBoolean _requestInProgress;

    private static final int INITIAL_WAIT_RETRY_SEC = 1;
    private static final int MAX_RETRY_SEC = 10;
    private int _isOnlineRetry = 0;
    private int _deltaSec;

    private EnqueueRunner _checkConnectionRunner = new EnqueueRunner();
    private Handler _checkConnectionHandler;

    private static final Pattern pattern = Pattern.compile("\\~rt_(.*?)\\~");

    public QueueITWaitingRoomProvider(Context activityContext,
                                      String customerId,
                                      String eventOrAliasId,
                                      QueueITWaitingRoomProviderListener queueITWaitingRoomProviderListener) {
        this(activityContext,
                customerId,
                eventOrAliasId,
                "",
                "",
                queueITWaitingRoomProviderListener
        );
    }

    public QueueITWaitingRoomProvider(Context activityContext,
                                      String customerId,
                                      String eventOrAliasId,
                                      String layoutName,
                                      String language,
                                      QueueITWaitingRoomProviderListener queueITWaitingRoomProviderListener) {
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
        _queueITWaitingRoomProviderListener = queueITWaitingRoomProviderListener;
        _deltaSec = INITIAL_WAIT_RETRY_SEC;
    }


    public void tryPass() throws QueueITException {
        if (_requestInProgress.getAndSet(true)) {
            throw new QueueITException("Request is already in progress");
        }
        _checkConnectionRunner = new QueueITWaitingRoomProvider.EnqueueRunner();
        _checkConnectionHandler = new Handler();
        _checkConnectionRunner.run();
    }

    public void tryPassWithEnqueueToken(String enqueueToken)
            throws QueueITException {
        if (_requestInProgress.getAndSet(true)) {
            throw new QueueITException("Request is already in progress");
        }

        _checkConnectionRunner = new QueueITWaitingRoomProvider.EnqueueRunner(enqueueToken, null);
        _checkConnectionHandler = new Handler();
        _checkConnectionRunner.run();
    }

    public void tryPassWithEnqueueKey(String enqueueKey)
            throws QueueITException {
        if (_requestInProgress.getAndSet(true)) {
            throw new QueueITException("Request is already in progress");
        }

        _checkConnectionRunner = new QueueITWaitingRoomProvider.EnqueueRunner(null, enqueueKey);
        _checkConnectionHandler = new Handler();
        _checkConnectionRunner.run();
    }

    private void runWithConnection(String enqueueToken, String enqueueKey) {
        tryEnqueue(enqueueToken, enqueueKey);
        _requestInProgress.set(false);
    }

    private void tryEnqueue(String enqueueToken, String enqueueKey) {
        String userId = getUserId();
        String userAgent = UserAgentManager.getUserAgent();
        String sdkVersion = getSdkVersion();

        QueueITApiClientListener queueITApiClientListener = new QueueITApiClientListener() {
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
                Log.v("WaitingRoomProvider", String.format("Error: %s: %s", errorCode, errorMessage));
                if (errorCode >= 400 && errorCode < 500) {
                    _queueITWaitingRoomProviderListener.onFailure(String.format("Error %s (%s)",errorMessage , errorCode), Error.INVALID_RESPONSE);
                    _requestInProgress.set(false);
                } else {
                    QueueITWaitingRoomProvider.this.enqueueRetryMonitor(enqueueToken, enqueueKey);
                }
            }
        };

        QueueITApiClient queueITApiClient = new QueueITApiClient(_customerId, _eventOrAliasId, userId, userAgent, sdkVersion,
                _layoutName, _language, enqueueToken, enqueueKey, queueITApiClientListener);
        queueITApiClient.init(_context);
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
            _queueITWaitingRoomProviderListener.onFailure("Error! Queue is unavailable.", Error.Queueit_NotAvailable);
        }
    }

    public boolean IsRequestInProgress() {
        return _requestInProgress.get();
    }

    private void handleAppEnqueueResponse(String queueId, String queueUrlString, int queueUrlTtlInMinutes,
                                          String eventTargetUrl, String queueItToken) {
        Boolean isPassedThrough = queueItToken != null && !queueItToken.isEmpty();
        RedirectType redirectType = getRedirectTypeFromToken(queueItToken);
        QueueTryPassResult queueTryPassResult = new QueueTryPassResult(queueItToken, queueUrlString, eventTargetUrl, queueUrlTtlInMinutes, isPassedThrough, redirectType);
        _queueITWaitingRoomProviderListener.onSuccess(queueTryPassResult);
        _requestInProgress.set(false);
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
                _queueITWaitingRoomProviderListener.onFailure( "No connection", Error.NO_CONNECTION);
                _requestInProgress.set(false);
                return;
            }

            _checkConnectionHandler.postDelayed(this, 1000);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return true;
        }
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private String getUserId() {
        return Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static RedirectType getRedirectTypeFromToken(String queueItToken){
        if(queueItToken == null || queueItToken.isEmpty()){
            return RedirectType.queue;
        }

        Matcher matcher = pattern.matcher(queueItToken);

        if (matcher.find()) {
            String matchGroups = matcher.group(0);
            String statusString = matchGroups.length() > 0 ? matchGroups.replace("~rt_", "").replace("~", "") : "";
            return RedirectType.valueOf(statusString);
        }else {
            Log.e("QueueEngine", String.format("Waiting room status not found in the token: %s", queueItToken));
            return RedirectType.unknown;
        }
    }

    public String getSdkVersion() {
        return "Android-" + BuildConfig.VERSION_NAME;
    }
}
