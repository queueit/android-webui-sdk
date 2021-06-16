package com.example.demowithprotectedapi.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ProductsService {
    @GET("safeaction?queue-event1-nodomain=t")
    Call<Product> getProduct();
}

