package com.github.klefstad_teaching.cs122b.gateway.repo.entity;


import com.github.klefstad_teaching.cs122b.core.result.Result;

public class AuthResponse {
    private MyCustomResultPOJO result;

    public MyCustomResultPOJO getResult() {
        return result;
    }

    public AuthResponse setResult(MyCustomResultPOJO result) {
        this.result = result;
        return this;
    }
}
