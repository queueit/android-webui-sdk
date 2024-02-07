package com.queue_it.androidsdk;

import android.content.Context;
import android.util.Log;
import java.util.Calendar;

public class QueueITEngine {

    private QueueITWaitingRoomProvider _queueITWaitingRoomProvider;
    private QueueITWaitingRoomView _queueITWaitingRoomView;

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
        _queueITEngineListener = queueListener;

        QueueListener queueITQueueListener = new QueueListener() {
            @Override
            protected void onQueuePassed(QueuePassedInfo queuePassedInfo) {
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
            }
        };

        final String webViewUserAgent = options.getWebViewUserAgent();

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
                _queueITWaitingRoomView.showQueue(_queueTryPassResult, webViewUserAgent);
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



    public void run(Context activityContext) throws QueueITException {
            _queueITWaitingRoomProvider.tryPass();

    }


    public void runWithEnqueueToken(Context activityContext, String enqueueToken)
            throws QueueITException {
        if (_queueITWaitingRoomProvider.IsRequestInProgress()){
            throw new QueueITException("Request is already in progress");
        }

            _queueITWaitingRoomProvider.tryPassWithEnqueueToken(enqueueToken);

    }


    public void runWithEnqueueKey(Context activityContext, String enqueueKey)
            throws QueueITException {
        if (_queueITWaitingRoomProvider.IsRequestInProgress()){
            throw new QueueITException("Request is already in progress");
        }

        _queueITWaitingRoomProvider.tryPassWithEnqueueKey(enqueueKey);
    }

    public String getSdkVersion() {
        return _queueITWaitingRoomProvider.getSdkVersion();
    }
}