package com.example.demowithprotectedapi.repos;

import com.example.demowithprotectedapi.exceptions.MustBeQueued;
import com.example.demowithprotectedapi.api.Product;

import java.io.IOException;

public interface IProductRepository {
    Product getProduct() throws IOException, MustBeQueued;

    void addQueueToken(String queueItToken);
}
