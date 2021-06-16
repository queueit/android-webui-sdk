package com.example.demowithprotectedapi.repos;

import com.example.demowithprotectedapi.exceptions.MustBeQueued;
import com.example.demowithprotectedapi.api.Product;
import com.example.demowithprotectedapi.http.AddCookiesInterceptor;
import com.example.demowithprotectedapi.http.CookieStorage;
import com.example.demowithprotectedapi.http.QueueITInterceptor;
import com.example.demowithprotectedapi.http.ReceivedCookiesInterceptor;
import com.example.demowithprotectedapi.http.UserAgentInterceptor;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductRepository implements IProductRepository {
    private final OkHttpClient _client;
    private final String _baseUrl;
    private final QueueITInterceptor _queueItInterceptor;

    public ProductRepository() {
        _baseUrl = "https://fastly.v3.ticketania.com";
        CookieStorage cookies = new CookieStorage();
        _queueItInterceptor = new QueueITInterceptor(cookies);
        _client = new OkHttpClient().newBuilder()
                .addInterceptor(_queueItInterceptor)
                .addInterceptor(new AddCookiesInterceptor(cookies))
                .addInterceptor(new ReceivedCookiesInterceptor(cookies))
                .hostnameVerifier((hostname, session) -> true)
                .build();
    }

    private Request.Builder getRequestBuilder() {
        return new Request.Builder()
                .header("x-queueit-ajaxpageurl", "someValue");
    }

    public Product getProduct() throws IOException, MustBeQueued {
        String fullURL = _baseUrl + "/safeaction?queue-event1-nodomain=t&";
        Request request = getRequestBuilder()
                .url(fullURL)
                .build();
        Response response = _client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        return new Gson().fromJson(response.body().string(), Product.class);
    }

    public void addQueueToken(String queueItToken) {
        _queueItInterceptor.setToken(queueItToken);
    }
}
