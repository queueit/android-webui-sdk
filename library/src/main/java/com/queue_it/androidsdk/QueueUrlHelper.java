package com.queue_it.androidsdk;

import android.net.Uri;

import okhttp3.HttpUrl;

public abstract class QueueUrlHelper {

    public static Uri updateUrl(String queueUrl, String userId) {
        try {
            return updateUrl(Uri.parse(queueUrl), userId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Uri.parse(queueUrl);
        }
    }

    public static Uri updateUrl(Uri queueUrl, String userId) {
        String updatedUrl = new HttpUrl.Builder()
                .scheme(queueUrl.getScheme())
                .host(queueUrl.getHost())
                .encodedPath(queueUrl.getPath())
                .query(queueUrl.getQuery())
                .addQueryParameter("userId", userId)
                .build()
                .url()
                .toString();
        return Uri.parse(updatedUrl);
    }

    public static boolean urlUpdateNeeded(String queueUrl, String userId) {
        Uri uri = Uri.parse(queueUrl);
        return urlUpdateNeeded(uri, userId);
    }

    public static boolean urlUpdateNeeded(Uri queueUrl, String userId) {
        if (queueUrl == null) return false;
        String query = queueUrl.getQuery();
        if (query == null) query = "";
        String userIdQuery = String.format("userId=%s", userId);
        boolean containsUserId = query.startsWith(userIdQuery) || query.contains("&" + userIdQuery);
        return !containsUserId;
    }
}
