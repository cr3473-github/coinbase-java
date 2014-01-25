package com.createtank.payments.coinbase.exceptions;

public class UnsupportedRequestVerbException extends Exception {
    public UnsupportedRequestVerbException() {
        super("Json request cannot have verb GET or DELETE");
    }
}
