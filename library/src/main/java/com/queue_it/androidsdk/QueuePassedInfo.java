package com.queue_it.androidsdk;

public class QueuePassedInfo {
    private final String _queueItToken;

    public QueuePassedInfo(String queueItToken) {
        _queueItToken = queueItToken;
    }

    public String getQueueItToken()
    {
        return _queueItToken;
    }
}