package com.example.demowithprotectedapi.http;

import android.util.Log;

import java.io.IOException;
import java.util.HashSet;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddCookiesInterceptor implements Interceptor {

    private final CookieStorage _storage;

    public AddCookiesInterceptor(CookieStorage storage){
        _storage = storage;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        HashSet<String> preferences = _storage.getCookies();
        for (String cookie : preferences) {
            builder.addHeader("Cookie", cookie);
            Log.v("OkHttp", "Adding Header: " + cookie); // This is done so I know which headers are being added; this interceptor is used after the normal logging of OkHttp
        }

        return chain.proceed(builder.build());
    }
}
