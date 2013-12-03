package com.createtank.payments.coinbase.models;

import com.google.gson.JsonObject;

public class Address {
    private String address;
    private String callbackUrl;
    private String label;
    private String createdAt;

    public Address(String address, String callbackUrl, String label, String createdAt) {
        this.address = address;
        this.callbackUrl = callbackUrl;
        this.label = label;
        this.createdAt = createdAt;
    }

    public String getAddress() {
        return address;
    }

    public String getCallbackUrl() {
        return  callbackUrl;
    }

    public String getLabel() {
        return label;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public static Address fromJson(JsonObject json) {
        return new Address(json.get("address").getAsString(),
                json.get("callback_url").getAsString(),
                json.get("label").getAsString(),
                json.get("created_at").getAsString());
    }
}
