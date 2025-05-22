package com.atos.dynamicdiscount.exceptions;

public  class MultipleOngoingRequestsException extends RuntimeException {
    public MultipleOngoingRequestsException(String message) {
        super(message);
    }
}