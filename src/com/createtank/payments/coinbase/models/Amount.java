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
