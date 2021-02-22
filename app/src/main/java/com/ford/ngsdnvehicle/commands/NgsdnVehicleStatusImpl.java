package com.ford.ngsdnvehicle.commands;

import java.util.Optional;

public interface NgsdnVehicleStatusImpl {
    Optional<Object> getDeepSleepInProgress();

    Optional<Object> getFirmwareUpgInProgress();
}
