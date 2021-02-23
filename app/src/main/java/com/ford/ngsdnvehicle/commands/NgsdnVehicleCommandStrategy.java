package com.ford.ngsdnvehicle.commands;

import java.util.Optional;

import io.reactivex.Single;

public interface NgsdnVehicleCommandStrategy {
    Single<NgsdnVehicleStatusResponse> getCommandStatus(String vin, String commandId);
}
