package com.example.demowithprotectedapi.exceptions;

import java.io.IOException;

public class MustBeQueued extends IOException {
    private final String value;

    public MustBeQueued(String queueUrl) {
        super("Must be queued in: " + queueUrl);
        this.value = queueUrl;
    }

    public String getValue() {
        return value;
    }
}
