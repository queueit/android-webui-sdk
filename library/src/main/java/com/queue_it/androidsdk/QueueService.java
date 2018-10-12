package com.queue_it.androidsdk;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QueueService {

    private String _customerId;
    private String _eventOrAliasId;
    private String _userId;
    private String _userAgent;
    private String _sdkVersion;
    private String _layoutName;
    private String _language;
    private QueueServiceListener _queueServiceListener;

    public static boolean IsTest = false;

    private String getApiUrl()
    {
        if (IsTest) {
            return "https://%s.test.queue-it.net/api/queue/%s/%s/appenqueue";
        } else {
            return "https://%s.queue-it.net/api/queue/%s/%s/appenqueue";
        }
    }

    public QueueService(String customerId, String eventOrAliasId, String userId,
                        String userAgent, String sdkVersion, String layoutName,
                        String language, QueueServiceListener queueServiceListener)
    {
        _customerId = customerId;
        _eventOrAliasId = eventOrAliasId;
        _userId = userId;
        _userAgent = userAgent;
        _sdkVersion = sdkVersion;
        _layoutName = layoutName;
        _language = language;
        _queueServiceListener = queueServiceListener;
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public void init(final Context context)
    {
        String url = String.format(getApiUrl(), _customerId, _customerId, _eventOrAliasId);

        OkHttpClient client = new OkHttpClient();

        String putBody = getJsonObject().toString();
        RequestBody body = RequestBody.create(JSON, putBody);

        Log.v("QueueITEngine", "API call " + getISO8601StringForDate(Calendar.getInstance().getTime()) + ": " + url + ": " + putBody);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            Handler mainHandler = new Handler(context.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                final String message = e.toString();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        _queueServiceListener.onFailure(message, 0);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful())
                {
                    final String errorMessage = String.format("%s %s", response.message(), response.body().string());
                    final int code = response.code();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            _queueServiceListener.onFailure(errorMessage, code);
                        }
                    });

                    return;
                }

                final String body = response.body().string();

                try {

                    JSONObject jsonObject = new JSONObject(body);
                    final String queueId = optString(jsonObject, "QueueId");
                    final String queueUrl = optString(jsonObject, "QueueUrl");
                    final int queueUrlTtlInMinutes = optInt(jsonObject, "QueueUrlTTLInMinutes");
                    final String eventTargetUrl = optString(jsonObject, "EventTargetUrl");

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            _queueServiceListener.onSuccess(queueId, queueUrl, queueUrlTtlInMinutes, eventTargetUrl);
                        }
                    });
                } catch (JSONException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            _queueServiceListener.onFailure("Server did not return valid JSON: " + body, 0);
                        }
                    });
                }
            }
        });
    }

    private static String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    private String optString(JSONObject json, String key)
    {
        if (json.isNull(key))
            return null;
        else
            return json.optString(key, null);
    }

    private int optInt(JSONObject json, String key)
    {
        if (json.isNull(key))
            return 0;
        else
            return json.optInt(key, 0);
    }

    private JSONObject getJsonObject()
    {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", _userId);
            jsonObject.put("userAgent", _userAgent);
            jsonObject.put("sdkVersion", _sdkVersion);
            if (!TextUtils.isEmpty(_layoutName)) {
                jsonObject.put("layoutName", _layoutName);
            }
            if (!TextUtils.isEmpty(_language)) {
                jsonObject.put("language", _language);
            }
            return jsonObject;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
