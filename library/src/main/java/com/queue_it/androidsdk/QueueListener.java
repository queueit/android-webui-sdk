package com.queue_it.androidsdk;


public abstract class QueueListener {
    protected abstract void onQueuePassed(QueuePassedInfo queuePassedInfo);
    protected abstract void onQueueViewWillOpen();
    protected abstract void onQueueDisabled();
    protected abstract void onQueueItUnavailable();
    protected abstract void onError(Error error, String errorMessage);
    protected void onWebViewClosed(){

    }
    protected void onUserExited(){

    }
}
