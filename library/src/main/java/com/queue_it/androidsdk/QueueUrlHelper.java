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
        String encodedQuery = queueUrl.getEncodedQuery();
        if(encodedQuery==null){
            encodedQuery = "";
        }
        if(!encodedQuery.contains("userId=")){
            encodedQuery = "userId=" + userId + "&" + encodedQuery;
        }
        String updatedUrl = new HttpUrl.Builder()
                .scheme(queueUrl.getScheme())
                .host(queueUrl.getHost())
                .encodedPath(queueUrl.getPath())
                .encodedQuery(encodedQuery)
                .build()
                .url()
                .toString();
        return Uri.parse(updatedUrl);
    }

    public static boolean urlUpdateNeeded(String queueUrl, String userId) {
        if(queueUrl==null || userId==null){
            return false;
        }
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
