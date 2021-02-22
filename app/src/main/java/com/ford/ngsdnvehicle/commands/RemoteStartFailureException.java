package com.ford.ngsdnvehicle.commands;

import java.util.List;

public class RemoteStartFailureException extends Throwable{
    public RemoteStartFailureException(int status, List<String> remoteStartFailureErrors) {
    }
}
