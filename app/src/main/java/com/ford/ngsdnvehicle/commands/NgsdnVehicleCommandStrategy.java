package com.ford.ngsdnvehicle.commands;


import io.reactivex.Single;

public interface NgsdnVehicleCommandStrategy {
    Single<NgsdnVehicleStatusResponse> getCommandStatus(String vin, String commandId);
}
