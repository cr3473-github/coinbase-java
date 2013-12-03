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

public class Amount implements IJsonSerializable {

    protected String amount;
    protected String currency;

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public static Amount fromJson(JsonObject json) {
        Amount amount = new Amount();
        amount.setAmount(json.get("amount").getAsString());
        amount.setCurrency(json.get("currency").getAsString());

        return amount;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("amount", amount);
        json.addProperty("currency", currency);

        return json;
    }
}
