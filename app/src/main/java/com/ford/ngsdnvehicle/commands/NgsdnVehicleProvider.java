package com.ford.ngsdnvehicle.commands;

public interface NgsdnVehicleProvider {
    void updateWifiSettings(NgsdnVehicleStatusResponse ngsdnVehicleStatusResponse);

    void updateCommandEventStatus(NgsdnVehicleStatusResponse ngsdnVehicleStatusResponse);
}
