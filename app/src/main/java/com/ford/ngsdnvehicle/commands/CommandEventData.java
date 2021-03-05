package com.ford.ngsdnvehicle.commands;

public interface CommandEventData {
    DoorStatuses getDoorStatuses();

    DoorPresentStatuses getDoorPresentStatuses();

    ZoneStatuses getZoneStatuses();

    Integer getLockSecureWarning();
}
