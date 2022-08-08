package com.example.demowithprotectedapi.http;

import java.util.HashSet;

public class CookieStorage {
    private HashSet<String> _cookies;

    public CookieStorage() {
        _cookies = new HashSet<>();
    }

    public void store(HashSet<String> cookies) {
        _cookies = cookies;
    }

    public HashSet<String> getCookies() {
        return _cookies;
    }

    public void clear() {
        _cookies.clear();
    }
}
