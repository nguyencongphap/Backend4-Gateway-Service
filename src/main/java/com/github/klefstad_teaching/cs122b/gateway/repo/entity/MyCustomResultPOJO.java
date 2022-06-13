package com.github.klefstad_teaching.cs122b.gateway.repo.entity;

public class MyCustomResultPOJO {
    private Integer code;
    private String message;

    public Integer getCode() {
        return code;
    }

    public MyCustomResultPOJO setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public MyCustomResultPOJO setMessage(String message) {
        this.message = message;
        return this;
    }
}
