package com.ford.ngsdnvehicle.commands;

import java.util.Optional;

public interface NgsdnVehicleCommandStrategy {
    Optional<NgsdnVehicleStatusResponse> getCommandStatus(String vin, String commandId);
}
