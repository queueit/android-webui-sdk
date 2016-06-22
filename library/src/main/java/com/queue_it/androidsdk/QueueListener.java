package com.queue_it.androidsdk;


public interface QueueListener {
    void onQueuePassed();
    void onQueueViewWillOpen();
    void onQueueDisabled();
    void onQueueItUnavailable();
    void onError(Error error, String errorMessage);
}
