package com.ford.ngsdnvehicle.commands;

public class StatusCarryingException extends Throwable {
    private  Throwable theCause;
    private int statusCode;

    public StatusCarryingException(String valueOf) {
        statusCode = Integer.parseInt(valueOf);
    }

    public StatusCarryingException(String valueOf, Throwable cause) {
        statusCode = Integer.parseInt(valueOf);
        theCause = cause;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
