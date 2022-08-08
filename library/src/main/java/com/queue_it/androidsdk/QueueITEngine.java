package com.queue_it.androidsdk;

import android.content.Context;
import android.util.Log;
import java.util.Calendar;

public class QueueITEngine {

    private QueueITWaitingRoomProvider _queueITWaitingRoomProvider;
    private QueueITWaitingRoomView _queueITWaitingRoomView;

    private final QueueCache _queueCache;

    private QueueListener _queueITEngineListener;
    private QueueTryPassResult _queueTryPassResult;

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
        UserAgentManager.initialize(activityContext);
        _queueCache = new QueueCache(activityContext, customerId, eventOrAliasId);
        _queueITEngineListener = queueListener;

        QueueListener queueITQueueListener = new QueueListener() {
            @Override
            protected void onQueuePassed(QueuePassedInfo queuePassedInfo) {
                _queueCache.clear();
                _queueITEngineListener.onQueuePassed(queuePassedInfo);
            }

            @Override
            protected void onQueueViewWillOpen() {
                _queueITEngineListener.onQueueViewWillOpen();
            }

            @Override
            protected void onQueueDisabled(QueueDisabledInfo queueDisabledInfo) {
                _queueITEngineListener.onQueueDisabled(queueDisabledInfo);
            }

            @Override
            protected void onQueueItUnavailable() {
                _queueITEngineListener.onQueueItUnavailable();
            }

            @Override
            protected void onError(Error error, String errorMessage) {
                _queueITEngineListener.onError(error, errorMessage);
            }

            @Override
            public void onSessionRestart(QueueITEngine queueITEngine) {
                _queueCache.clear();
                _queueITEngineListener.onSessionRestart(QueueITEngine.this);
            }

            @Override
            public void onUserExited() {
                _queueITEngineListener.onUserExited();
            }

            @Override
            public void onWebViewClosed() {
                _queueITEngineListener.onWebViewClosed();
            }

            @Override
            protected void onQueueUrlChanged(String url) {
                _queueITEngineListener.onQueueUrlChanged(url);
                updateQueuePageUrl(url);
            }
        };

        QueueITWaitingRoomProviderListener queueITWaitingRoomProviderListener = new QueueITWaitingRoomProviderListener() {
            @Override
            public void onSuccess(QueueTryPassResult queueTryPassResult) {
                if (queueTryPassResult.getRedirectType() == RedirectType.safetynet){
                    queueITQueueListener.onQueuePassed(new QueuePassedInfo(queueTryPassResult.getQueueItToken()));
                    return;
                }
                if (queueTryPassResult.getRedirectType() == RedirectType.disabled || queueTryPassResult.getRedirectType() == RedirectType.afterevent || queueTryPassResult.getRedirectType() == RedirectType.idle) {
                    queueITQueueListener.onQueueDisabled(new QueueDisabledInfo(queueTryPassResult.getQueueItToken()));
                    return;
                }

                _queueTryPassResult = queueTryPassResult;
                _queueITWaitingRoomView.showQueue(_queueTryPassResult);
                _queueCache.update(queueTryPassResult.getQueueUrl(), queueTryPassResult.getUrlTTLInMinutes(), queueTryPassResult.getTargetUrl());
            }

            @Override
            public void onFailure(String errorMessage, Error errorCode) {
                if (errorCode == Error.Queueit_NotAvailable){
                    _queueITEngineListener.onQueueItUnavailable();
                    return;
                }
                _queueITEngineListener.onError(errorCode,errorMessage);
            }
        };

        _queueITWaitingRoomProvider = new QueueITWaitingRoomProvider(activityContext, customerId,eventOrAliasId,layoutName,language, queueITWaitingRoomProviderListener);
        _queueITWaitingRoomView = new QueueITWaitingRoomView(activityContext, queueITQueueListener, options);
    }

    public void setViewDelay(int delayInterval) {
        _queueITWaitingRoomView.setViewDelay(delayInterval);
    }

    public boolean IsRequestInProgress() {
        return _queueITWaitingRoomProvider.IsRequestInProgress();
    }

    public void run(Context activityContext, boolean clearCache) throws QueueITException {
        if (clearCache) {
            _queueCache.clear();
        }
        run(activityContext);
    }

    public void run(Context activityContext) throws QueueITException {
        if(!tryToShowQueueFromCache()){
            _queueITWaitingRoomProvider.tryPass();
        }
    }

    public void runWithEnqueueToken(Context activityContext, String enqueueToken) throws QueueITException {
        runWithEnqueueToken(activityContext, enqueueToken, false);
    }

    public void runWithEnqueueToken(Context activityContext, String enqueueToken, boolean clearCache)
            throws QueueITException {
        if (_queueITWaitingRoomProvider.IsRequestInProgress()){
            throw new QueueITException("Request is already in progress");
        }
        if (clearCache) {
            _queueCache.clear();
        }

        if(!tryToShowQueueFromCache()){
            _queueITWaitingRoomProvider.tryPassWithEnqueueToken(enqueueToken);
        }

    }

    public void runWithEnqueueKey(Context activityContext, String enqueueKey) throws QueueITException {
        runWithEnqueueKey(activityContext, enqueueKey, false);
    }

    public void runWithEnqueueKey(Context activityContext, String enqueueKey, boolean clearCache)
            throws QueueITException {
        if (_queueITWaitingRoomProvider.IsRequestInProgress()){
            throw new QueueITException("Request is already in progress");
        }
        if (clearCache) {
            _queueCache.clear();
        }
        if(!tryToShowQueueFromCache()){
            _queueITWaitingRoomProvider.tryPassWithEnqueueKey(enqueueKey);
        }
    }

    private boolean tryToShowQueueFromCache() {
        if (_queueCache.isEmpty()) {
            return false;
        }

        Calendar cachedTime = _queueCache.getUrlTtl();
        Calendar currentTime = Calendar.getInstance();

        if (currentTime.compareTo(cachedTime) < 0) {
            String queueUrl = _queueCache.getQueueUrl();
            Log.v("QueueITEngine", String.format("Using queueUrl from cache: %s", queueUrl));
            _queueITWaitingRoomView.showQueue(_queueTryPassResult);
            return true;
        }
        return false;
    }

    private void updateQueuePageUrl(String queueUrl) {
        if (!_queueCache.isEmpty()) {
            _queueCache.update(queueUrl, _queueCache.getUrlTtl(), _queueCache.getTargetUrl());
        }
    }

    public String getSdkVersion() {
        return _queueITWaitingRoomProvider.getSdkVersion();
    }
}