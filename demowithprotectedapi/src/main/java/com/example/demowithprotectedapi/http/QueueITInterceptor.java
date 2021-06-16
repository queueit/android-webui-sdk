package com.example.demowithprotectedapi.http;

import com.example.demowithprotectedapi.exceptions.MustBeQueued;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class QueueITInterceptor implements Interceptor {

    private final CookieStorage _cookies;
    private String _queueItToken;

    public QueueITInterceptor(CookieStorage cookies) {
        _cookies = cookies;
    }

    public void setToken(String token){
        _queueItToken = token;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        HttpUrl.Builder urlBuilder = chain.request().url().newBuilder();
        if (_queueItToken!=null && _queueItToken.length() > 0) {
            urlBuilder.addQueryParameter("queueittoken", _queueItToken);
        }

        Request chainReq = chain.request();
        Request req = chainReq.newBuilder()
                .addHeader("x-queueit-ajaxpageurl", chainReq.url().toString())
                .url(urlBuilder.build())
                .build();

        Response res = chain.proceed(req);
        if (mustQueue(res)) {
            resetToken();
            _cookies.clear();
            Headers resHeaders = res.headers();
            throw new MustBeQueued(resHeaders.get("x-queueit-redirect"));
        }

        return res;
    }

    public void resetToken() {
        _queueItToken = null;
    }

    public boolean mustQueue(Response response) {
        Headers responseHeaders = response.headers();
        return responseHeaders.names().contains("x-queueit-redirect");
    }
}
