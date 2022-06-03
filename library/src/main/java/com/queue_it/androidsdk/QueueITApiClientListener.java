package com.queue_it.androidsdk;

interface QueueITApiClientListener {
    void onSuccess(String queueId, String queueUrlString, int queueUrlTtlInMinutes, String eventTargetUrl, String queueItToken);
    void onFailure(String errorMessage, int errorCode);
}
