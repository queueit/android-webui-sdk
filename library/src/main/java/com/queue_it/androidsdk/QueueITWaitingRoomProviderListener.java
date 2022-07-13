package com.queue_it.androidsdk;

public interface QueueITWaitingRoomProviderListener {
    void onSuccess(QueueTryPassResult queueTryPassResult);
    void onFailure(String errorMessage, Error errorCode);
}
