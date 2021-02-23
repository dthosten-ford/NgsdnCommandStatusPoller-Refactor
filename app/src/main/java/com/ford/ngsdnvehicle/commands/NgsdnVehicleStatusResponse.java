package com.ford.ngsdnvehicle.commands;

import java.util.Optional;

import io.reactivex.Single;

public interface NgsdnVehicleStatusResponse {
    int getStatus();

    NgsdnVehicleStatusImpl getVehicleStatus();

    Optional<Boolean> getDoorPresentStatuses();

    Optional<CommandEventData> getCommandEventData();

    Optional<WifiSettingsData> getWifiSettingsData();

    Optional<String> getTrailerLightCheckFailureReason();

    Optional<RemoteStartFailures> getRemoteStartFailures();

    Optional<RemoteLockFailures> getRemoteLockFailures();
}
