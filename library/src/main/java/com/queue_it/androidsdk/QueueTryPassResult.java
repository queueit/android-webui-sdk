package com.queue_it.androidsdk;

public class QueueTryPassResult {
    private final String _queueItToken;

    private final boolean _isPassedThrough;
    private final String _queueUrl;
    private final String _targetUrl;
    private final int _urlTTLInMinutes;
    private final RedirectType _RedirectType;

    public QueueTryPassResult(String queueItToken, String queueUrl, String targetUrl, int urlTTLInMinutes, boolean isPassedThrough, RedirectType redirectType) {
        _queueItToken = queueItToken;
        _queueUrl = queueUrl;
        _targetUrl = targetUrl;
        _urlTTLInMinutes = urlTTLInMinutes;
        _isPassedThrough = isPassedThrough;
        _RedirectType = redirectType;
    }

    public String getQueueItToken()
    {
        return _queueItToken;
    }

    public String getQueueUrl() {
        return _queueUrl;
    }

    public String getTargetUrl() {
        return _targetUrl;
    }

    public int getUrlTTLInMinutes() {
        return _urlTTLInMinutes;
    }

    public Boolean isPassedThrough(){
        return _isPassedThrough;
    }

    public RedirectType getRedirectType() {
        return _RedirectType;
    }
}
