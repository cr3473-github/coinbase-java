package com.createtank.payments.coinbase.models;

import com.google.gson.JsonObject;

public class Transaction {

    private String id;
    private String createdAt;
    private Amount amount;
    private boolean request;
    private TransactionStatus status;
    private User sender;
    private User recipient;
    private String recipientAddress;

    private Transaction(String id, String createdAt, Amount amount, boolean request,
                        TransactionStatus status, User sender, String recipientAddress) {
        this.id = id;
        this.createdAt = createdAt;
        this.amount = amount;
        this.request = request;
        this.status = status;
        this.sender = sender;
        this.recipientAddress = recipientAddress;
    }

    private Transaction(String id, String createdAt, Amount amount, boolean request,
                        TransactionStatus status, User sender, User recipient) {
        this.id = id;
        this.createdAt = createdAt;
        this.amount = amount;
        this.request = request;
        this.status = status;
        this.sender = sender;
        this.recipientAddress = recipientAddress;
    }

    //region accessors
    public String getId() {
        return id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Amount getAmount() {
        return amount;
    }

    public boolean isRequest() {
        return request;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public User getSender() {
        return sender;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public static Transaction fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String createdAt = json.get("created_at").getAsString();
        Amount amount = Amount.fromJson(json.getAsJsonObject("amount"));
        boolean request = json.get("request").getAsBoolean();
        TransactionStatus status = TransactionStatus.valueOf(json.get("status").getAsString().toUpperCase());
        User sender = User.fromJson(json.getAsJsonObject("sender"));

        return json.has("recipient_address") ? new Transaction(id, createdAt, amount, request, status, sender,
                json.get("recipient_address").getAsString()) : new Transaction(id, createdAt, amount, request, status,
                sender, User.fromJson(json.getAsJsonObject("recipient")));
    }
    //endregion
}
