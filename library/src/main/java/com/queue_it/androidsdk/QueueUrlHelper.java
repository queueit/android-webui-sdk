package com.queue_it.androidsdk;

import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.HttpUrl;

public abstract class QueueUrlHelper {

    public static String updateUrl(String queueUrl, String userId) {
        try {
            URL currentUrl = new URL(queueUrl);
            return new HttpUrl.Builder()
                    .scheme(currentUrl.getProtocol())
                    .host(currentUrl.getHost())
                    .encodedPath(currentUrl.getPath())
                    .query(currentUrl.getQuery())
                    .addQueryParameter("userId", userId)
                    .build()
                    .url()
                    .toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return queueUrl;
    }

    public static boolean urlUpdateNeeded(String queueUrl, String userId) {
        try {
            URL currentUrl = new URL(queueUrl);
            String query = currentUrl.getQuery();
            String userIdQuery = String.format("userId=%s", userId);
            return !query.startsWith(userIdQuery) && !query.contains("&" + userIdQuery);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
