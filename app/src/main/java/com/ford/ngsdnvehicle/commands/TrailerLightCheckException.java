package com.ford.ngsdnvehicle.commands;

public class TrailerLightCheckException extends Throwable{
    //smell, should pass around status, not int.

    public TrailerLightCheckException(int status, Object o) {
    }
}
