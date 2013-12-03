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
