/*
Copyright 2013 createTank L.L.C.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

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
