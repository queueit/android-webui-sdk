package com.queue_it.androidsdk;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Calendar;

public class QueueCache {

    private final String _cacheKey;
    private static final String KEY_QUEUE_URL = "queueUrl";
    private static final String KEY_URL_TTL = "url_ttl";
    private static final String KEY_TARGET_URL = "target_url";
    private final Context _context;

    public QueueCache(Context context, String customerId, String eventOrAliasId)
    {
        _context = context;
        _cacheKey = "queueit_" + customerId + eventOrAliasId;
    }

    public boolean isEmpty()
    {
        return TextUtils.isEmpty(getQueueUrl());
    }

    public Calendar getUrlTtl()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        long time = sharedPreferences.getLong(_cacheKey + KEY_URL_TTL, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar;
    }

    public String getQueueUrl()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        return sharedPreferences.getString(_cacheKey + KEY_QUEUE_URL, "");
    }

    public String getTargetUrl()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        return sharedPreferences.getString(_cacheKey + KEY_TARGET_URL, "");
    }

    public void update(String queueUrl, int urlTTLInMinutes, String targetUrl){
        if (urlTTLInMinutes <= 0) {
            return;
        }

        Calendar queueUrlTtl = Calendar.getInstance();
        queueUrlTtl.add(Calendar.MINUTE, urlTTLInMinutes);
        update(queueUrl, queueUrlTtl, targetUrl);
    }

    public void update(String queueUrl, Calendar urlTtl, String targetUrl)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(_cacheKey + KEY_QUEUE_URL, queueUrl);
        editor.putLong(_cacheKey + KEY_URL_TTL, urlTtl.getTimeInMillis());
        editor.putString(_cacheKey + KEY_TARGET_URL, targetUrl);
        editor.commit();
    }

    public void clear()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(_cacheKey + KEY_QUEUE_URL);
        editor.remove(_cacheKey + KEY_URL_TTL);
        editor.remove(_cacheKey + KEY_TARGET_URL);
        editor.commit();
    }
}
