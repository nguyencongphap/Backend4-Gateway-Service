package com.github.klefstad_teaching.cs122b.gateway.repo.entity;

import java.sql.Timestamp;

public class GatewayRequestObject {
    private String ipAddress;
    private Timestamp callTime;
    private String path;

    public String getIpAddress() {
        return ipAddress;
    }

    public GatewayRequestObject setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public Timestamp getCallTime() {
        return callTime;
    }

    public GatewayRequestObject setCallTime(Timestamp callTime) {
        this.callTime = callTime;
        return this;
    }

    public String getPath() {
        return path;
    }

    public GatewayRequestObject setPath(String path) {
        this.path = path;
        return this;
    }
}
