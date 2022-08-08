package com.example.demowithprotectedapi.exceptions;

import java.io.IOException;

public class MustBeQueued extends IOException {
    private final String value;

    public MustBeQueued(String s) {
        super("Must be queued in: " + s);
        this.value = s;
    }

    public String getValue() {
        return value;
    }
}
