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

public class User implements IJsonSerializable {
    protected String id;
    protected String email;
    protected String name;
    protected String timezone;
    protected String nativeCurrency;
    protected int buyLevel;
    protected int sellLevel;
    protected Amount balance;
    protected Amount buyLimit;
    protected Amount sellLimit;

    public String getId() {
        return id;
    }

    public String getEmail() {
        return  email;
    }

    public String getName() {
        return name;
    }
    public String getTimezone() {
        return timezone;
    }

    public String getNativeCurrency() {
        return nativeCurrency;
    }

    public int getBuyLevel() {
        return buyLevel;
    }

    public int getSellLevel() {
        return sellLevel;
    }

    public Amount getBalance() {
        return balance;
    }

    public Amount getBuyLimit() {
        return buyLimit;
    }

    public Amount getSellLimit() {
        return sellLimit;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setNativeCurrency(String nativeCurrency) {
        this.nativeCurrency = nativeCurrency;
    }

    public void setBuyLevel(int buyLevel) {
        this.buyLevel = buyLevel;
    }

    public void setSellLevel(int sellLevel) {
        this.sellLevel = sellLevel;
    }

    public void setBalance(Amount balance) {
        this.balance = balance;
    }

    public void setBuyLimit(Amount buyLimit) {
        this.buyLimit = buyLimit;
    }

    public void setSellLimit(Amount sellLimit) {
        this.sellLimit = sellLimit;
    }

    public static User fromJson(JsonObject json) {
        User user = new User();
        user.setId(json.get("id").getAsString());
        user.setEmail(json.get("email").getAsString());
        user.setName(json.get("name").getAsString());
        user.setTimezone(json.get("time_zone").getAsString());
        user.setNativeCurrency(json.get("native_currency").getAsString());
        user.setBuyLevel(json.get("buy_level").getAsInt());
        user.setSellLevel(json.get("sell_level").getAsInt());
        user.setBalance(Amount.fromJson(json.getAsJsonObject("balance")));
        user.setBuyLimit(Amount.fromJson(json.getAsJsonObject("buy_limit")));
        user.setSellLimit(Amount.fromJson(json.getAsJsonObject("sell_limit")));

        return user;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("email", email);
        json.addProperty("name", name);
        json.addProperty("time_zone", timezone);
        json.addProperty("native_currency", nativeCurrency);
        json.addProperty("buy_level", buyLevel);
        json.addProperty("sell_level", sellLevel);
        json.add("balance", balance.toJson());
        json.add("buy_limit", buyLimit.toJson());
        json.add("sell_limit", sellLimit.toJson());

        return json;
    }
}
