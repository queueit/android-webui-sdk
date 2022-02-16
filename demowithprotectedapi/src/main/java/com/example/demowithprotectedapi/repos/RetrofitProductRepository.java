package com.example.demowithprotectedapi.repos;

import com.example.demowithprotectedapi.api.Product;
import com.example.demowithprotectedapi.api.ProductFilter;
import com.example.demowithprotectedapi.api.ProductsService;
import com.example.demowithprotectedapi.http.AddCookiesInterceptor;
import com.example.demowithprotectedapi.http.CookieStorage;
import com.example.demowithprotectedapi.http.QueueITInterceptor;
import com.example.demowithprotectedapi.http.ReceivedCookiesInterceptor;
import com.example.demowithprotectedapi.http.UserAgentInterceptor;

import java.io.IOException;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitProductRepository implements IProductRepository {
    private final ProductsService _service;
    private final String _baseUrl;
    private final QueueITInterceptor _queueItInterceptor;

    public RetrofitProductRepository(String baseUrl) {
        _baseUrl = baseUrl;
        CookieStorage cookies = new CookieStorage();
        _queueItInterceptor = new QueueITInterceptor(cookies);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(_queueItInterceptor)
                .addInterceptor(new AddCookiesInterceptor(cookies))
                .addInterceptor(new ReceivedCookiesInterceptor(cookies))
                .addInterceptor(new UserAgentInterceptor("demoapp", "1.0.0"))
                .hostnameVerifier((hostname, session) -> true)
                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(_baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        _service = retrofit.create(ProductsService.class);
    }

    public Product getProduct() throws IOException {
        Call<Product> pCall = _service.getProduct();
        retrofit2.Response<Product> response = pCall.execute();
        return response.body();
    }

    public void addQueueToken(String queueItToken) {
        _queueItInterceptor.setToken(queueItToken);
    }
}
