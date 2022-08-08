package com.example.demowithprotectedapi.http;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ReceivedCookiesInterceptor implements Interceptor {

    private final CookieStorage _storage;

    public ReceivedCookiesInterceptor(CookieStorage storage) {
        _storage = storage;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());

        if (!originalResponse.headers("Set-Cookie").isEmpty()) {
            HashSet<String> cookies = new HashSet<>();

            for (String header : originalResponse.headers("Set-Cookie")) {
                cookies.add(header);
            }

            _storage.store(cookies);
        }
        if (originalResponse.priorResponse() != null && !originalResponse.priorResponse().headers("Set-Cookie").isEmpty()) {
            HashSet<String> cookies = new HashSet<>();

            for (String header : originalResponse.priorResponse().headers("Set-Cookie")) {
                cookies.add(header);
            }

            _storage.store(cookies);
        }

        return originalResponse;
    }
}
