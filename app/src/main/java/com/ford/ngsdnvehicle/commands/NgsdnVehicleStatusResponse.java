package com.ford.ngsdnvehicle.commands;

import java.util.Optional;

public interface NgsdnVehicleStatusResponse {
    StatusCodes getStatus();

    NgsdnVehicleStatusImpl getVehicleStatus();

    Optional<Boolean> getDoorPresentStatuses();

    Optional<Object> getCommandEventData();

    Optional<Object> getWifiSettingsData();

    Optional<Object> getTrailerLightCheckFailureReason();

    Optional<Object> getRemoteStartFailures();

    Optional<Object> getRemoteLockFailures();
}
