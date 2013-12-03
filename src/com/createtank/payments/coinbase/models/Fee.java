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

public class Fee implements IJsonSerializable {
    private int cents;
    private String currency;

    public int getCents() {
        return cents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCents(int cents) {
        this.cents = cents;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public static Fee fromJson(JsonObject json) {
        Fee fee = new Fee();
        fee.setCents(json.get("cents").getAsInt());
        fee.setCurrency(json.get("currency").getAsString());

        return fee;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("cents", cents);
        json.addProperty("currency_iso", currency);

        return json;
    }
}
