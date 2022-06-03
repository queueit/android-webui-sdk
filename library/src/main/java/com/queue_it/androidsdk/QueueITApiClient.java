package com.queue_it.androidsdk;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QueueITApiClient {

    private final String _customerId;
    private final String _eventOrAliasId;
    private final String _userId;
    private final String _userAgent;
    private final String _sdkVersion;
    private final String _layoutName;
    private final String _language;
    private final String _enqueueToken;
    private final String _enqueueKey;
    private final QueueITApiClientListener _queueITApiClientListener;

    public static boolean IsTest = false;

    private URL getApiUrl() {
        String hostname = String.format(IsTest ?
                        "%s.test.queue-it.net" :
                        "%s.queue-it.net",
                _customerId);

        String path = String.format("api/mobileapp/queue/%s/%s/enqueue", _customerId, _eventOrAliasId);
        return new HttpUrl.Builder()
                .scheme("https")
                .host(hostname)
                .addPathSegments(path)
                .addQueryParameter("userId", _userId)
                .build().url();
    }

    public QueueITApiClient(String customerId,
                            String eventOrAliasId,
                            String userId,
                            String userAgent,
                            String sdkVersion,
                            String layoutName,
                            String language,
                            String enqueueToken,
                            String enqueueKey,
                            QueueITApiClientListener queueITApiClientListener){
        _customerId = customerId;
        _eventOrAliasId = eventOrAliasId;
        _userId = userId;
        _userAgent = userAgent;
        _sdkVersion = sdkVersion;
        _layoutName = layoutName;
        _language = language;
        _enqueueToken = enqueueToken;
        _enqueueKey = enqueueKey;
        _queueITApiClientListener = queueITApiClientListener;
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public void init(final Context context) {
        URL enqueueUrl = getApiUrl();
        OkHttpClient client = new OkHttpClient();
        String putBody = getJsonObject().toString();
        RequestBody body = RequestBody.create(JSON, putBody);

        Log.v("QueueITApiClient", "API call " + getISO8601StringForDate(Calendar.getInstance().getTime()) + ": " + enqueueUrl.toString() + ": " + putBody);

        Request request = new Request.Builder()
                .url(enqueueUrl)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            final Handler mainHandler = new Handler(context.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                final String message = e.toString();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        _queueITApiClientListener.onFailure(message, 0);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    final String errorMessage = String.format("%s %s", response.message(), response.body().string());
                    final int code = response.code();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            _queueITApiClientListener.onFailure(errorMessage, code);
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
                    final String queueItToken = optString(jsonObject, "QueueitToken");
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String updatedQueueUrl = QueueUrlHelper.urlUpdateNeeded(queueUrl, _userId) ?
                                    QueueUrlHelper.updateUrl(queueUrl, _userId).toString() :
                                    queueUrl;
                            _queueITApiClientListener.onSuccess(queueId, updatedQueueUrl, queueUrlTtlInMinutes, eventTargetUrl, queueItToken);
                        }
                    });
                } catch (JSONException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            _queueITApiClientListener.onFailure("Server did not return valid JSON: " + body, 0);
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

    private String optString(JSONObject json, String key) {
        if (json.isNull(key))
            return null;
        else
            return json.optString(key, null);
    }

    private int optInt(JSONObject json, String key) {
        if (json.isNull(key))
            return 0;
        else
            return json.optInt(key, 0);
    }

    private JSONObject getJsonObject() {
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
            if(!TextUtils.isEmpty(_enqueueToken)){
                jsonObject.put("enqueueToken", _enqueueToken);
            }
            if(!TextUtils.isEmpty(_enqueueKey)){
                jsonObject.put("enqueueKey", _enqueueKey);
            }

            return jsonObject;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
