package com.ford.ngsdnvehicle.commands;

import java.util.List;

public interface RemoteStartFailures {
    default List<String> getRemoteStartFailureErrors() {
        return null;
    }

}
