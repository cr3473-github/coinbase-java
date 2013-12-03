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

import java.util.HashMap;
import java.util.Map;

public class Transfer {

    private String type;
    private String code;
    private String createdAt;
    private Map<String, Fee> fees;
    private String payoutDate;
    private String transactionId;
    private TransactionStatus status;
    private Amount btc;
    private Amount subtotal;
    private Amount total;
    private String description;

    private Transfer(String type, String code, String createdAt, Map<String, Fee> fees, String payoutDate,
                       String transactionId, TransactionStatus status, Amount btc, Amount subtotal, Amount total,
                       String description) {
        this.type = type;
        this.code = code;
        this.createdAt = createdAt;
        this.fees = fees;
        this.payoutDate = payoutDate;
        this.transactionId = transactionId;
        this.status = status;
        this.btc = btc;
        this.subtotal = subtotal;
        this.total = total;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Map<String, Fee> getFees() {
        return fees;
    }

    public String getPayoutDate() {
        return payoutDate;
    }

    public TransactionStatus getTransactionStatus() {
        return  status;
    }

    public Amount getBtc() {
        return btc;
    }

    public Amount getSubtotal() {
        return subtotal;
    }

    public Amount getTotal() {
        return total;
    }

    public String getDescription() {
        return description;
    }

    public static Transfer fromJson(JsonObject json) {
        Map<String, Fee> fees = new HashMap<String, Fee>();
        JsonObject feesJson = json.getAsJsonObject("fees");
        fees.put("coinbase", Fee.fromJson(feesJson.getAsJsonObject("coinbase")));
        fees.put("bank", Fee.fromJson(feesJson.getAsJsonObject("bank")));
        return new Transfer(json.get("type").getAsString(),
                json.get("code").getAsString(),
                json.get("created_at").getAsString(),
                fees,
                json.get("payout_date").getAsString(),
                json.get("transaction_id").getAsString(),
                TransactionStatus.valueOf(json.get("status").getAsString().toUpperCase()),
                Amount.fromJson(json.getAsJsonObject("btc")),
                Amount.fromJson(json.getAsJsonObject("subtotal")),
                Amount.fromJson(json.getAsJsonObject("total")),
                json.get("description").getAsString());
    }

}
