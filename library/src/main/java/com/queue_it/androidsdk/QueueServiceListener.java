package com.queue_it.androidsdk;

public interface QueueServiceListener {
    void onSuccess(String queueId, String queueUrlString, int requeryInterval, int queueUrlTtlInMinutes, String eventTargetUrl);
    void onFailure(String errorMessage, int errorCode);
}
