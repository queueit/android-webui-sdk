package com.queue_it.androidsdk;

public interface QueueServiceListener {
    void onSuccess(String queueId, String queueUrlString, int queueUrlTtlInMinutes, String eventTargetUrl);
    void onFailure(String errorMessage, int errorCode);
}
