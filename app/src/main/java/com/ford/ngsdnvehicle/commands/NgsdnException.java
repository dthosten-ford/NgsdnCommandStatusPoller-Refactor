package com.ford.ngsdnvehicle.commands;

public class NgsdnException extends StatusCarryingException {

    private int statusCode;

    public NgsdnException(int statusCode) {
        super(String.valueOf(statusCode));
        this.statusCode = statusCode;
    }

    public NgsdnException(int statusCode, Throwable cause) {
        super(String.valueOf(statusCode), cause);
        this.statusCode = statusCode;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    //Generated

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NgsdnException that = (NgsdnException) o;

        return statusCode == that.statusCode;

    }

    @Override
    public int hashCode() {
        return statusCode;
    }
}
