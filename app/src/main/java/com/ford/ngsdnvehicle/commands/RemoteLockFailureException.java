package com.ford.ngsdnvehicle.commands;

import com.google.common.base.Optional;

import java.util.List;

public class RemoteLockFailureException extends Throwable{
    public RemoteLockFailureException(int status, List<RemoteLockFailure> remoteLockFailureErrors) {
    }
}
