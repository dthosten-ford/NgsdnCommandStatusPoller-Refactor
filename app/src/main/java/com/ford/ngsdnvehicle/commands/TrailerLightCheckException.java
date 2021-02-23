package com.ford.ngsdnvehicle.commands;

public class TrailerLightCheckException extends Throwable{
    public static final String IGNITION_NOT_ON = "sampleValue-IGNITION_NOT_ON";
    //smell, should pass around status, not int.

    public TrailerLightCheckException(int status, Object o) {
    }
}
